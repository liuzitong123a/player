@file:Suppress("DEPRECATION")

package com.kwunai.rx.player.core

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.support.annotation.FloatRange
import android.util.Log
import android.view.Surface
import android.view.WindowManager
import com.kwunai.rx.player.ext.isNetAvailable
import com.kwunai.rx.player.ext.no
import com.kwunai.rx.player.ext.scanForActivity
import com.kwunai.rx.player.ext.yes
import com.kwunai.rx.player.modal.*
import com.kwunai.rx.player.view.IRenderView
import com.netease.neliveplayer.sdk.NELivePlayer
import com.netease.neliveplayer.sdk.constant.NEBufferStrategy
import com.netease.neliveplayer.sdk.constant.NEErrorType
import com.netease.neliveplayer.sdk.constant.NEPlayStatusType
import com.orhanobut.logger.Logger
import java.io.IOException
import java.util.*


/**
 * 网易播放器，基于2.0.0版本的实现
 * 可以自定义实现PlayerStrategy
 * 如SystemPlayerStrategy（系统MediaPlayer），IJKPlayerStrategy（ijkPlayer）
 * @author lzt
 */
class NEPlayerStrategy(
        private val context: Context
) : PlayerStrategy() {

    companion object {
        private const val BACKGROUND_RESET_TIME = (30 * 60 * 1000).toLong()
    }

    // 当前正在播放的url
    private lateinit var mCurrentPath: String
    // 跳转到某个位置开始播放
    private var skipToPosition: Long = 0
    // 视频播放器
    private var mMediaPlayer: NELivePlayer? = null
    // 播放器当前状态
    private var currentState = PlayerState.IDLE
    // 播放导致状态的code码
    private var cause: Int = 0
    // 音频管理器
    private var mAudioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    // 以下六个参数为视频视图显示大小相关
    private var renderView: IRenderView? = null
    private var scaleMode = VideoScaleMode.NONE
    private var videoWidth: Int = 0
    private var videoHeight: Int = 0
    private var videoSarNum: Int = 0
    private var videoSarDen: Int = 0
    // 播放进度定时器
    private var vodTimer: Timer? = null
    private var vodTimerTask: TimerTask? = null
    // app是否在前台(默认在前台)
    private var foreground = true
    // 退到后台的时间点
    private var backgroundTime: Long = 0
    // 网络监听器
    private var connectWatcher: ConnectWatcher? = null
    // 网络是否连通
    private var netAvailable: Boolean = false

    init {
        setCurrentState(PlayerState.IDLE, 0)
    }

    /**
     * 设置播放的url
     */
    override fun setUp(url: String) {
        this.mCurrentPath = url
    }

    /**
     * 设置播放所需的RenderView,实现IRenderView,可自定义实现
     */
    override fun setupRenderView(renderView: IRenderView?, videoScaleMode: VideoScaleMode) {
        renderView?.let {
            this.renderView = it
            this.scaleMode = videoScaleMode
        }
    }

    /**
     * 初始化音频管理器
     */
    private fun initAudio() {
        if (mAudioManager == null) {
            mAudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            if (mAudioManager != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val audioAttributes = AudioAttributes.Builder()
                            .setLegacyStreamType(AudioManager.STREAM_MUSIC)
                            .build()
                    audioFocusRequest = AudioFocusRequest
                            .Builder(AudioManager.AUDIOFOCUS_GAIN)
                            .setAudioAttributes(audioAttributes)
                            .build()
                    mAudioManager!!.requestAudioFocus(audioFocusRequest)
                } else {
                    mAudioManager!!.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
                }
            }
        }
    }

    /**
     * 初始化配置NELivePlayer
     */
    private fun initPlayer() {
        (mMediaPlayer == null).yes {
            mMediaPlayer = NELivePlayer.create()
        }
        mMediaPlayer?.let {
            it.setBufferStrategy(NEBufferStrategy.NELPANTIJITTER)
            it.setBufferSize(150 * 1024 * 1024)
            it.setHardwareDecoder(false)
            it.setShouldAutoplay(false)
            it.setPlaybackTimeout(10)
            it.setLooping(0)
        }
        renderView?.setCallback(object : IRenderView.SurfaceCallback {
            override fun onSurfaceCreated(surface: Surface?) {
                openPlayer(surface)
            }
        })
    }

    /**
     * 打开NELivePlayer播放器
     */
    private fun openPlayer(surface: Surface?) {
        mMediaPlayer?.let {
            // 设置准备视频播放监听事件
            it.setOnPreparedListener(onPreparedListener)
            // 设置视频大小更改监听器
            it.setOnVideoSizeChangedListener(onVideoSizeChangedListener)
            // 设置视频播放完成监听事件
            it.setOnCompletionListener(onCompletionListener)
            // 设置视频错误监听器
            it.setOnErrorListener(onErrorListener)
            // 设置视频信息监听器
            it.setOnInfoListener(onInfoListener)
            // 设置视频解析出错的监听器
            it.setOnVideoParseErrorListener(onVideoParseErrorListener)
            try {
                it.dataSource = mCurrentPath
                it.setSurface(surface)
                it.prepareAsync()
                Log.e("lzt", "STATE_PREPARING")
                setCurrentState(PlayerState.PREPARING, 0)
                subject.onNext(PlayerCommand.Preparing)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    /**
     *开始播放
     */
    override fun start() {
        //点播暂停后重新开始
        mMediaPlayer?.let {
            (currentState == PlayerState.PAUSED).yes {
                Log.e("lzt", "player restart...")
                restart()
                return
            }
        }
        Log.e("lzt", "player init...")
        // 正在播放中，如果要重新初始化，那么要重置
        mMediaPlayer?.let {
            (it.isPlaying).yes {
                Logger.d("reset current player before init...")
                resetPlayer()
            }
        }

        netAvailable = context.isNetAvailable()

        if (connectWatcher == null) {
            connectWatcher = ConnectWatcher(this.context, connectCallback)
            connectWatcher!!.startup()
            Log.e("lzt", "connect watcher startup")
        }


        // 设置常亮
        scanForActivity(context)?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        initAudio()
        initPlayer()
    }

    /**
     * 从指定位置开始播放
     */
    override fun start(position: Long) {
        this.skipToPosition = position
        start()
    }

    /**
     * 重新播放
     */
    override fun restart() {
        Log.e("lzt", "STATE_PLAYING")
        mMediaPlayer?.start()
        setCurrentState(PlayerState.PLAYING, 0)
        (mMediaPlayer!!.duration > 0).yes { startVodTimer() }
        subject.onNext(PlayerCommand.Prepared)
    }

    /**
     * 暂停播放
     */
    override fun pause() {
        mMediaPlayer?.let {
            (it.isPlaying && it.duration > 0).yes {
                Log.e("lzt", "player paused")
                setCurrentState(PlayerState.PAUSED, CauseCode.CODE_VIDEO_PAUSED_BY_MANUAL)
                stopVodTimer()
                it.pause()
            }
        }
    }

    override fun onActivityResume() {

        Log.e("lzt", "activity on resume")

        // 回到前台
        foreground = true

        mMediaPlayer?.let {
            val state = getCurrentState().state
            if (System.currentTimeMillis() - backgroundTime >= BACKGROUND_RESET_TIME) {
                Log.e("lzt", "force reset player, as app on background for a long time! ")
                savePlayerState()
                resetPlayer()
            } else if (state === PlayerState.PLAYING && !it.isPlaying) {
                // 当前状态与播放器底层状态不一致，立即重置。
                Log.e("lzt", "force reset player, as current state is PLAYING, but player engine is not playing!")
                savePlayerState()
                resetPlayer()
            }
        }
        // 重新恢复拉流视频
        recoverPlayer(false)
    }

    override fun onActivityStop() {
        Log.e("lzt", "activity on stop")
        // 切到后台
        foreground = false
        backgroundTime = System.currentTimeMillis()
        mMediaPlayer?.let {
            (it.isPlaying && it.duration > 0).yes {
                setCurrentState(PlayerState.PAUSED, CauseCode.CODE_VIDEO_PAUSED_BY_BACKGROUND)
                stopVodTimer()
                it.pause()
            }
        }
    }


    /**
     * 停止播放
     */
    override fun stop() {
        Log.e("lzt", "player stop")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mAudioManager?.abandonAudioFocusRequest(audioFocusRequest)
        } else {
            mAudioManager?.abandonAudioFocus(null)
        }
        audioFocusRequest = null
        mAudioManager = null
        stopVodTimer()
        mMediaPlayer?.let {
            it.setSurface(null)
            it.release()
        }

        mMediaPlayer = null
        renderView?.let {
            it.setCallback(null)
            it.releaseRender()
        }
        setCurrentState(PlayerState.STOPPED, CauseCode.CODE_VIDEO_STOPPED_BY_MANUAL)
    }

    /**
     * 跳转到position位置播放
     */
    override fun seekTo(position: Long) {
        mMediaPlayer?.seekTo(position)
    }


    /**
     * 设置倍速播放
     * @param speed 0.5f-2.0f
     */
    override fun setPlaybackSpeed(@FloatRange(from = 0.5, to = 2.0) speed: Float) {
        mMediaPlayer?.setPlaybackSpeed(speed)
    }

    /**
     * 获取当前的状态
     */
    override fun getCurrentState(): StateInfo = StateInfo(currentState, cause)

    /**
     * 是否正在播放
     */
    override fun isPlaying(): Boolean {
        return mMediaPlayer?.isPlaying ?: false
    }

    /**
     * 获取视频总长度
     */
    override fun getDuration(): Long {
        return mMediaPlayer?.duration ?: 0
    }

    /**
     * 获取当前播放的位置
     */
    override fun getCurrentPosition(): Long {
        return mMediaPlayer?.currentPosition ?: 0
    }

    /**
     * 获取缓存的位置
     */
    override fun getCachedPosition(): Long {
        return mMediaPlayer?.playableDuration ?: 0
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
        stopVodTimer()
        mMediaPlayer?.let {
            Log.e("lzt", "reset player!")
            it.reset()
        }
    }

    /**
     * 设置播放状态
     */
    private fun setCurrentState(state: PlayerState, causeCode: Int) {
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
        mMediaPlayer?.let {
            (it.duration <= 0).no {
                skipToPosition = it.currentPosition
            }
        }
    }

    /**
     * 获取到视频尺寸or视频尺寸发生变化
     */
    private val onVideoSizeChangedListener = NELivePlayer.OnVideoSizeChangedListener { p, width, height, sarNum, sarDen ->
        if (videoWidth == p.videoWidth && videoHeight == p.videoHeight
                && (videoSarNum == sarNum && videoSarDen == sarDen || sarNum <= 0 || sarDen <= 0)) {
            return@OnVideoSizeChangedListener
        }

        videoWidth = width
        videoHeight = height
        videoSarNum = sarNum
        videoSarDen = sarDen
        renderView!!.setVideoSize(
                videoWidth = width,
                videoHeight = height,
                videoSarDen = sarDen,
                videoSarNum = sarNum,
                scaleMode = scaleMode
        )
    }


    /**
     * 准备完成开始播放
     */
    private val onPreparedListener = NELivePlayer.OnPreparedListener {
        Log.e("lzt", "onPrepared ——> STATE_PREPARED")
        // 点播，重新seekTo上次的位置
        if (skipToPosition != 0L) {
            it.seekTo(skipToPosition)
        }
        (it.duration > 0).yes { startVodTimer() }
        it.start()
        setCurrentState(PlayerState.PREPARED, 0)
        subject.onNext(PlayerCommand.Prepared)
    }

    /**
     * 播放完成
     */
    private val onCompletionListener = NELivePlayer.OnCompletionListener { _ ->
        Log.e("lzt", "onCompletion ——> STATE_STOP")
        resetPlayer()
        scanForActivity(context)?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        subject.onNext(PlayerCommand.Completion)
        setCurrentState(PlayerState.STOPPED, CauseCode.CODE_VIDEO_STOPPED_AS_ON_COMPLETION)
    }

    /**
     * 播放过程中发生错误
     */
    private val onErrorListener = NELivePlayer.OnErrorListener { _, what, extra ->
        Log.e("lzt", "on player error, what=$what, extra=$extra")
        resetPlayer()
        subject.onNext(PlayerCommand.Error(code = what, extra = extra))
        setCurrentState(PlayerState.ERROR, what)
        true
    }

    /**
     * 视频码流解析失败，此时音频播放正常，视频可能无画面
     */
    private val onVideoParseErrorListener = NELivePlayer.OnVideoParseErrorListener { _ ->
        Log.e("lzt", "on player parse video error")
        setCurrentState(PlayerState.ERROR, CauseCode.CODE_VIDEO_PARSER_ERROR)
        subject.onNext(PlayerCommand.Error(CauseCode.CODE_VIDEO_PARSER_ERROR, 0))
    }

    /**
     * 视频状态变化、事件发生
     */
    private val onInfoListener = NELivePlayer.OnInfoListener { _, what, _ ->
        when (what) {
            NEPlayStatusType.NELP_BUFFERING_START -> {
                Logger.e("on player info: buffering start")
                stopVodTimer()
                subject.onNext(PlayerCommand.BufferingStart)
            }
            NEPlayStatusType.NELP_BUFFERING_END -> {
                Logger.e("on player info: buffering end")
                startVodTimer()
                subject.onNext(PlayerCommand.BufferingEnd)
            }
            NEPlayStatusType.NELP_NET_STATE_BAD -> {
                Logger.e("on player info: network state bad tip")
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
        mMediaPlayer?.let {
            current = it.currentPosition
            duration = it.duration
            cached = it.playableDuration
        }
        val c = current
        val d = duration
        val cc = cached
        if (c >= 0 && d > 0) {
            subject.onNext(PlayerCommand.CurrentProgress(c, d, 100.0f * c / d, cc))
        }
    }

    /**
     * 进度定时器开始
     */
    override fun startVodTimer() {
        stopVodTimer()
        vodTimer = Timer("TICKER_TIMER")
        vodTimerTask = object : TimerTask() {
            override fun run() {
                onVodTickerTimer()
            }
        }
        vodTimer!!.schedule(vodTimerTask, 0, 1000)
    }

    /**
     * 进度定时器结束
     */
    override fun stopVodTimer() {
        if (vodTimerTask != null) {
            vodTimerTask!!.cancel()
            vodTimerTask = null
        }

        if (vodTimer != null) {
            vodTimer!!.cancel()
            vodTimer!!.purge()
            vodTimer = null
        }
    }

    /**
     * 网络监听回调
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
        Log.e("lzt", "network available")
        netAvailable = true
        recoverPlayer(true) // 可能需要重启播放器
    }

    /**
     * 网络不可用
     * 这里网络断开了，保存一下播放进度，同时停止播放
     */
    private fun onNetworkUnavailable() {
        Log.e("lzt", "network unavailable")
        netAvailable = false
        savePlayerState()
        resetPlayer()
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
        mMediaPlayer?.let {
            // 回到前台不是暂停状态
            if (getCurrentState().state != PlayerState.PAUSED) {
                return
            }
            // 如果退入后台播放器本身就是暂停的，那么还是暂停状态
            if (getCurrentState().state == PlayerState.PAUSED && getCurrentState().causeCode != CauseCode.CODE_VIDEO_PAUSED_BY_BACKGROUND) {
                return
            }
            // 没有网络或者没有在前台就不需要重新初始化视频了
            if (netRecovery && !foreground) {
                return
            }
            // APP回到前台，发现没有网络，那么不立即初始化，等待网络连通了再初始化
            if (!netRecovery && !netAvailable) {
                return
            }
            start()
        }
    }
}