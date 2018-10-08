package com.kwunai.rx.player.core

import android.content.Context
import android.support.annotation.FloatRange
import android.util.Log
import android.view.Surface
import com.kwunai.rx.player.ext.*
import com.kwunai.rx.player.modal.*
import com.kwunai.rx.player.view.IRenderView
import com.netease.neliveplayer.sdk.NELivePlayer
import com.netease.neliveplayer.sdk.constant.NEBufferStrategy
import com.netease.neliveplayer.sdk.constant.NEErrorType
import com.netease.neliveplayer.sdk.constant.NEPlayStatusType
import com.orhanobut.logger.Logger
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 播放核心类
 * 和UI无关，只关心播放逻辑，这里使用Kt的密封类+RxJAVA进行回调
 */
class PlayerDispatcherImpl(
        private val context: Context
) : PlayerDispatcher() {

    companion object {
        private const val BACKGROUND_RESET_TIME = (30 * 60 * 1000).toLong()
    }

    private lateinit var path: String

    private val disposable = CompositeDisposable()

    val subject: Subject<PlayerCommand> = PublishSubject.create()
    private var player: NELivePlayer? = null
    private var renderView: IRenderView? = null
    // 保护player的锁
    private val lock = Any()

    // status
    private var hasReset = AtomicBoolean(false)
    private var currentState = PlayerState.IDLE
    private var cause: Int = 0
    // 上一次播放的位置（点播用）
    private var lastPlayPosition: Long = 0
    private var lastAudioTrack = -1

    // video size
    private var videoWidth: Int = 0
    private var videoHeight: Int = 0
    private var videoSarNum: Int = 0
    private var videoSarDen: Int = 0

    //video mode
    private var scaleMode = VideoScaleMode.NONE

    private var vodTimer: Timer? = null
    private var vodTimerTask: TimerTask? = null

    //net
    private var connectWatcher: ConnectWatcher? = null
    private var netAvailable: Boolean = false // 网络是否连通
    private var foreground = true // app是否在前台(默认在前台)
    private var backgroundTime: Long = 0 // 退到后台的时间点

    init {
        setCurrentState(PlayerState.IDLE, 0)
    }

    override fun setUp(url: String) {
        path = url
    }

    /**
     * 开始播放
     */
    override fun start() {
        synchronized(lock) {
            //点播暂停后重新开始
            (player != null && currentState == PlayerState.PAUSED).yes {
                Logger.d("player restart...")
                restart()
                return
            }

            Logger.d("player async init...")
            // 正在播放中，如果要重新初始化，那么要重置
            player?.let {
                (it.isPlaying && !hasReset.get()).yes {
                    Logger.d("reset current player before async init...")
                    resetPlayer()
                }
            }

            setCurrentState(PlayerState.PREPARING, 0)

            netAvailable = context.isNetAvailable()

            // observer
            //没有开启循环播放时打开网络监听进行网络重连，如果开启循环播放，播放器SDK内部会进行数据缓冲，所以这里以网络不重连进行示例，开发中可以根据需求进行自行设置
            if (connectWatcher == null) {
                connectWatcher = ConnectWatcher(this.context, connectCallback)
                connectWatcher!!.startup()
                Logger.v("connectivity watcher startup...")
            }

            // 异步初始化播放器
            disposable.add(
                    async {
                        initPlayer()
                    }
            )
        }
    }

    /**
     * 点播暂停后重新开始
     */
    private fun restart() {
        player!!.start()
        reSetupRenderView()
        // state
        setCurrentState(PlayerState.PLAYING, 0)
        val mediaInfo = MediaInfo(player!!.mediaInfo, player!!.duration)

        (player!!.duration > 0).yes { startVodTimer() }

        subject.onNext(PlayerCommand.Prepared(mediaInfo))
    }

    /**
     * player reset后、切后台切回前台，原来绑定的surfaceView的被置null了，重新初始化时，要重新绑上去
     */
    private fun reSetupRenderView() {
        //
        if (renderView != null && renderView?.getSurface() != null) {
            setupRenderView(renderView, scaleMode)
        }
    }

    /**
     * 绑定播放器载体（SurfaceView或者TextureView）
     * 这里推荐使用SurfaceView，在不对View做特殊操作时，surfaceView性能较高
     * 但是面对View的平移，变化，缩放，surfaceView无法处理，surfaceView主要在surface里处理bitmap图像，
     * 相当于另一个进程，无法使用View的特性，因此面对我们的需求还是建议使用SurfaceView，
     * 但是做一些特殊处理还是要使用TextureView
     */
    override fun setupRenderView(renderView: IRenderView?, videoScaleMode: VideoScaleMode) {
        Logger.d("setup render view, view=$renderView")
        renderView?.let {
            this.renderView = it
            this.scaleMode = videoScaleMode
            this.renderView!!.onSetupRenderView()
            // 将render view的surface回调告知player
            //  this.renderView!!.setCallback(surfaceCallback)
            // 触发render view的显示
            setVideoSizeToRenderView()
            setDisplaySurface(it.getSurface())
        }
    }

    /**
     * 设置视频的显示模式（对应SurfaceView或者TextureView视图显示）
     */
    override fun setVideoScaleMode(videoScaleMode: VideoScaleMode?) {
        Logger.d("setVideoScaleMode $videoScaleMode")
        if (renderView == null || videoScaleMode == null) {
            return
        }
        this.scaleMode = videoScaleMode
        // 触发render view的显示
        setVideoSizeToRenderView()
    }

    /**
     * 暂停播放
     */
    override fun pause() {
        synchronized(lock) {
            player?.let {
                (it.isPlaying && it.duration > 0).yes {
                    it.pause()
                    stopVodTimer(false)
                    Logger.d("player paused")
                    setCurrentState(PlayerState.PAUSED, CauseCode.CODE_VIDEO_PAUSED_BY_MANUAL)
                }
            }
        }
    }

    /**
     * 获取当前播放状态
     */
    override fun getCurrentState(): StateInfo {
        return StateInfo(currentState, cause)
    }

    /**
     * 停止播放
     */
    override fun stop() {

        disposable.dispose()
        disposable.clear()

        // timer销毁
        stopVodTimer(false)

        // 销毁播放器
        synchronized(lock) {
            renderView?.let {
                it.setCallback(null)
                setDisplaySurface(null)
            }
            player?.release()
            player = null
        }

        if (connectWatcher != null) {
            connectWatcher!!.shutdown()
            connectWatcher = null
            Logger.v("connectivity watcher shutdown")
        }
        setCurrentState(PlayerState.STOPPED, CauseCode.CODE_VIDEO_STOPPED_BY_MANUAL)

    }

    /**
     * 是否正在播放
     */
    override fun isPlaying(): Boolean {
        synchronized(lock) {
            return player?.isPlaying ?: false
        }
    }

    /**
     * 获取视频总长度
     */
    override fun getDuration(): Long {
        synchronized(lock) {
            return player?.duration ?: 0
        }
    }

    /**
     * 获取当前播放的位置
     */
    override fun getCurrentPosition(): Long {
        synchronized(lock) {
            return player?.currentPosition ?: 0
        }
    }

    /**
     * 获取缓存的位置
     */
    override fun getCachedPosition(): Long {
        synchronized(lock) {
            return player?.playableDuration ?: 0
        }

    }

    /**
     * 跳转到position位置播放
     */
    override fun seekTo(position: Long) {
        synchronized(lock) {
            player?.seekTo(position)
        }
    }

    /**
     * 设置倍速播放
     * @param speed 0.5f-2.0f
     */
    override fun setPlaybackSpeed(@FloatRange(from = 0.5, to = 2.0) speed: Float) {
        synchronized(lock) {
            player?.let {
                it.setPlaybackSpeed(speed)
                Logger.d("set playback speed to $speed")
            }
        }
    }

    /**
     * activity onResume执行
     */
    override fun onActivityResume() {
        reSetupRenderView()

        Logger.v("activity on resume")

        // 回到前台
        foreground = true

        if (player == null) {
            return
        }

        // 考虑需要重置的场景
        if (!hasReset.get()) {
            val state = getCurrentState().state
            if (System.currentTimeMillis() - backgroundTime >= BACKGROUND_RESET_TIME) {
                // 如果在后台时间太长超过了 BACKGROUND_RESET_TIME 的时长且没有重置过，那么立即重置。case: 长时间在后台，超过设置的后台重置时长，在一些极端的情况下播放会停止，但没有收到任何回调，此时回到前台需要重置后重新拉流。
                Logger.v("force reset player, as app on background for a long time! ")
                savePlayerState()
                resetPlayer()
            } else if (state === PlayerState.PLAYING && !player!!.isPlaying) {
                // 当前状态与播放器底层状态不一致，立即重置。
                Logger.v("force reset player, as current state is PLAYING, but player engine is not playing!")
                savePlayerState()
                resetPlayer()
            }
        }

        // 重新恢复拉流视频
        recoverPlayer(false)
    }

    /**
     * activity onStop执行
     */
    override fun onActivityStop() {
        Logger.v("activity on stop")

        foreground = false // 切到后台
        backgroundTime = System.currentTimeMillis()
        pause()
    }

    /**
     * 初始化播放器并异步prepare
     */
    private fun initPlayer() {
        synchronized(lock) {
            (player == null).yes {
                player = NELivePlayer.create()
                Logger.d("create player=$player")
            }
            configPlayer()
            player!!.prepareAsync()
            hasReset.set(false)
            subject.onNext(PlayerCommand.Preparing)
        }
        Logger.d("player async prepare...")
    }

    /**
     * 配置player
     * 在lock下操作player
     */
    private fun configPlayer() {
        player?.let {
            it.setBufferStrategy(NEBufferStrategy.NELPANTIJITTER)
            it.setBufferSize(150 * 1024 * 1024)
            it.setHardwareDecoder(false)
            it.setShouldAutoplay(false)
            it.setPlaybackTimeout(10) // 超时重连时间10s
            it.setLooping(0)
            it.setOnPreparedListener(onPreparedListener)
            it.setOnVideoSizeChangedListener(onVideoSizeChangedListener)
            it.setOnCompletionListener(onCompletionListener)
            it.setOnErrorListener(onErrorListener)
            it.setOnInfoListener(onInfoListener)
            it.setOnVideoParseErrorListener(onVideoParseErrorListener)
            it.dataSource = path
            reSetupRenderView()
        }
    }


    /**
     * 向render view设置视频帧大小
     * 前置条件：
     * 1) Player#onVideoSizeChanged必须回调了，存储了视频帧大小
     * 2) 已经安装了render view
     * <p>
     * 调用时机：
     * 1) onVideoSizeChanged回调中
     * 2) setupRenderView时
     */
    private fun setVideoSizeToRenderView() {
        if (videoWidth != 0 && videoHeight != 0 && renderView != null) {
            renderView!!.setVideoSize(videoWidth, videoHeight, videoSarNum, videoSarDen, scaleMode)
        }
    }


    /**
     * 播放器和显示surface的绑定
     * case 1: surfaceCreated时绑定到播放器
     * case 2: surfaceDestroyed时解除绑定
     * case 3: 播放器被reset后重新初始化时，如果surface没有被销毁，那么重新绑定到播放器
     */
    @Synchronized
    private fun setDisplaySurface(surface: Surface?) {
        if (player != null) {
            player!!.setSurface(surface)
            Logger.d("set player display surface=$surface")
        }
    }


    /**
     * 重置播放器
     * 使用场景：
     * case 1: 断网的时候,主动重置,等网络恢复后再重新初始化来恢复播放
     * case 2: 切换播放地址时，先重置，存储新地址后再重新初始化
     * case 3: 播放过程中发生错误/解析视频流错误，重置
     * case 4: 长期处于后台,回到前台时，主动重置，给予恢复的机会
     * case 5: 播放结束，重置
     */
    private fun resetPlayer() {
        // 停止定时器
        stopVodTimer(false)
        synchronized(lock) {
            player?.let {
                it.reset()
                hasReset.set(true)
                Logger.d("reset player!")
            }
        }
    }

    /**
     * 设置播放状态
     */
    @Synchronized
    fun setCurrentState(state: PlayerState, causeCode: Int) {
        currentState = state
        (causeCode < NEErrorType.NELP_EN_UNKNOWN_ERROR && cause != 0
                && cause >= NEErrorType.NELP_EN_UNKNOWN_ERROR).no {
            cause = causeCode
        }
        subject.onNext(PlayerCommand.StateChanged(StateInfo(state, cause)))
    }

    /**
     * 保存播放进度
     */
    private fun savePlayerState() {
        player?.let {
            (it.duration <= 0).no {
                lastPlayPosition = it.currentPosition
                lastAudioTrack = it.selectedAudioTrack
            }
        }
    }

    /**
     * surface callback
     */
    private val surfaceCallback: IRenderView.SurfaceCallback = object : IRenderView.SurfaceCallback {
        override fun onSurfaceCreated(surface: Surface?) {
            Log.e("lzt", "on surface created")
            // 播放器和显示surface的绑定
            setDisplaySurface(surface)
        }

        override fun onSurfaceDestroyed(surface: Surface?) {
            Log.e("lzt", "on surface destroyed")
            setDisplaySurface(null)
        }

        override fun onSurfaceSizeChanged(surface: Surface?, format: Int, width: Int, height: Int) {
            Log.e("lzt", "on surface changed, width=$width, height=$height, format=$format")

        }
    }


    /**
     * 获取到视频尺寸or视频尺寸发生变化
     * 最早回调
     */
    private val onVideoSizeChangedListener = NELivePlayer.OnVideoSizeChangedListener { p, width, height, sarNum, sarDen ->
        if (videoWidth == p.videoWidth && videoHeight == p.videoHeight
                && (videoSarNum == sarNum && videoSarDen == sarDen || sarNum <= 0 || sarDen <= 0)) {
            // the same or invalid sarNum/sarDen
            return@OnVideoSizeChangedListener
        }

        videoWidth = width
        videoHeight = height
        videoSarNum = sarNum
        videoSarDen = sarDen

        disposable.add(
                ui {
                    Log.e("lzt", "on video size changed, width=" + videoWidth + ", height=" + videoHeight
                            + ", sarNum=" + videoSarNum + ", sarDen=" + videoSarDen)
                    setVideoSizeToRenderView()
                }
        )
    }


    /**
     * 准备完成开始播放
     */
    private val onPreparedListener = NELivePlayer.OnPreparedListener { _ ->
        Logger.d("on player prepared!")

        synchronized(lock) {
            player?.let { it ->
                it.start()
                // 点播，重新seekTo上次的位置
                if (lastPlayPosition > 0) {
                    it.seekTo(lastPlayPosition)
                    lastPlayPosition = 0 // 复位
                }
                if (lastAudioTrack != -1) {
                    it.selectedAudioTrack = lastAudioTrack
                    lastAudioTrack = -1
                }

                Logger.d("player start...")

                val mediaInfo = MediaInfo(it.mediaInfo, it.duration)

                (it.duration > 0).yes { startVodTimer() }

                subject.onNext(PlayerCommand.Prepared(mediaInfo))
            }
        }
    }

    /**
     * 播放完成
     */
    private val onCompletionListener = NELivePlayer.OnCompletionListener { _ ->
        Logger.d("on player completion!")
        resetPlayer()
        subject.onNext(PlayerCommand.Completion)
        setCurrentState(PlayerState.STOPPED, CauseCode.CODE_VIDEO_STOPPED_AS_ON_COMPLETION)
    }

    /**
     * 播放过程中发生错误
     */
    private val onErrorListener = NELivePlayer.OnErrorListener { _, what, extra ->
        Logger.d("on player error!!! what=$what, extra=$extra")
        resetPlayer()
        subject.onNext(PlayerCommand.Error(what, extra))
        setCurrentState(PlayerState.ERROR, what)
        true
    }

    /**
     * 视频码流解析失败，此时音频播放正常，视频可能无画面
     */
    private val onVideoParseErrorListener = NELivePlayer.OnVideoParseErrorListener { _ ->
        Logger.d("on player parse video error!!!")
        subject.onNext(PlayerCommand.Error(CauseCode.CODE_VIDEO_PARSER_ERROR, 0))
    }

    /**
     * 视频状态变化、事件发生
     */
    private val onInfoListener = NELivePlayer.OnInfoListener { _, what, _ ->
        when (what) {
            NEPlayStatusType.NELP_BUFFERING_START -> {
                Logger.d("on player info: buffering start")
                stopVodTimer(false)
                subject.onNext(PlayerCommand.BufferingStart)
            }
            NEPlayStatusType.NELP_BUFFERING_END -> {
                Logger.d("on player info: buffering end")
                startVodTimer()
                subject.onNext(PlayerCommand.BufferingEnd)
            }
            NEPlayStatusType.NELP_NET_STATE_BAD -> {
                Logger.d("on player info: network state bad tip")
                subject.onNext(PlayerCommand.NetStateBad)
            }
        }
        false
    }

    /**
     * 回调当前播放进度
     */
    private fun onVodTickerTimer() {
        var current: Long = -1
        var duration: Long = -1
        var cached: Long = -1
        synchronized(lock) {
            player?.let {
                current = it.currentPosition
                duration = it.duration
                cached = it.playableDuration
            }
        }

        val c = current
        val d = duration
        val cc = cached
        if (c >= 0 && d > 0) {
            subject.onNext(PlayerCommand.CurrentProgress(c, d, 100.0f * c / d, cc))
        }
    }

    /**
     * ******************************* vod timer ******************************
     */

    fun startVodTimer() {
        stopVodTimer(true)
        vodTimer = Timer("TICKER_TIMER")
        vodTimerTask = object : TimerTask() {
            override fun run() {
                onVodTickerTimer()
            }
        }
        vodTimer!!.scheduleAtFixedRate(vodTimerTask, 1000, 1000)
        Logger.d("start vod timer...")
    }

    fun stopVodTimer(onStart: Boolean) {
        if (vodTimerTask != null) {
            vodTimerTask!!.cancel()
            vodTimerTask = null
        }

        if (vodTimer != null) {
            vodTimer!!.cancel()
            vodTimer!!.purge()
            vodTimer = null
        }

        if (!onStart) {
            Logger.d("stop vod timer!")
        }
    }

    /**
     * ----------------------下面时监听网络变化触发的一些行为-----------------------
     */
    private val connectCallback = object : ConnectWatcher.Callback {
        override fun onNetworkEvent(event: NetworkState) {
            when (event) {
                NetworkState.NETWORK_AVAILABLE -> onNetworkAvailable()
                NetworkState.NETWORK_UNAVAILABLE -> onNetworkUnavailable()
                NetworkState.NETWORK_CHANGE -> onNetworkChange()
            }
        }
    }

    /**
     * 网络可用
     */
    private fun onNetworkAvailable() {
        Logger.e("network available!!!")
        netAvailable = true

        recoverPlayer(true) // 可能需要重启播放器
    }

    /**
     * 网络不可用
     */
    private fun onNetworkUnavailable() {
        Logger.e("network unavailable!!!")
        netAvailable = false
        savePlayerState()
        resetPlayer()
        // state
        setCurrentState(PlayerState.STOPPED, CauseCode.CODE_VIDEO_STOPPED_AS_NET_UNAVAILABLE)
    }

    /**
     * 网络变化（可以处理UI变化）
     * wife-4g 中间会有断网通知 network unavailable -> network changed to MOBILE -> network available，这里会return掉
     * 4G -> wifi: 中间不会收到断网通知，这里必须要reset再重连，否则会出现缓冲错误
     */
    private fun onNetworkChange() {
        connectWatcher?.let {
            if (netAvailable == it.isAvailable()) {
                Logger.v("network type changed to " + it.getNetworkType() + ", recover video...")
                savePlayerState()
                resetPlayer()
                recoverPlayer(true)
            }
        }

    }

    /**
     * 恢复播放
     */
    private fun recoverPlayer(netRecovery: Boolean) {
        if (player == null) {
            return
        }

        if (!hasReset.get() && getCurrentState().state !== PlayerState.PAUSED) {
            // 没有重置过播放器并且不是点播暂停状态，这里就不需要恢复了
            return
        }

        // 没有网络或者没有在前台就不需要重新初始化视频了
        if (netRecovery && !foreground) {
            // case 1: 如果APP在后台网络恢复了，如果是后台暂停播放，那么后台网络恢复时不恢复播放，等回到前台后再重新初始化播放器，如果是后台继续播放，那么后台网络恢复时恢复播放。如果在前台网络断了恢复后，立即初始化恢复播放。
            Logger.v("cancel recover video from net recovery, as app in background!")
            return
        }

        if (!netRecovery && !netAvailable) {
            // case 2: 如果APP回到前台，发现没有网络，那么不立即初始化，等待网络连通了再初始化
            Logger.v("cancel recover video from activity on resume, as network is unavailable!")
            return
        }

        // 如果播放器已经重置过了，才需要重新初始化。比如退到后台，实际上有Service继续拉流，那么回到前台时，SurfaceView onCreate之后会继续渲染拉流
        Logger.v("recover video from " + if (netRecovery) "net available" else "activity on resume, foreground=$foreground")
        start()
    }
}