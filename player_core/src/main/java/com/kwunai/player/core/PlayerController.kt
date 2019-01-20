package com.kwunai.player.core

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.CountDownTimer

import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.ViewParent
import android.widget.*
import com.kwunai.player.modal.PlayerMode
import com.kwunai.player.R
import com.kwunai.player.ext.*
import com.kwunai.player.modal.CauseCode.CODE_VIDEO_STOPPED_AS_NET_UNAVAILABLE
import com.kwunai.player.modal.PlayerState.*

/**
 * 点播视频播放器控制层
 */
open class PlayerController @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : VideoController(context, attrs, defStyleAttr), View.OnClickListener, SeekBar.OnSeekBarChangeListener {

    private var topBottomVisible: Boolean = false

    private var isLock: Boolean = false

    private var systemFlag = systemUiVisibility

    private var mDismissControllerCountDownTimer: CountDownTimer? = null

    private var onCompletedCallback: OnCompletedCallback? = null

    // 加载UI
    private val mLoadingView by lazy {
        findViewById<View>(R.id.loading_view)
    }

    // 顶部UI
    private val topLayout by lazy {
        findViewById<View>(R.id.top_layout)
    }

    // 底部UI
    private val mLVideoBottom by lazy {
        findViewById<View>(R.id.bottom_view)
    }

    // 异常页面
    private val mLError by lazy {
        findViewById<View>(R.id.error_view)
    }

    // 视频进度中间显示View
    private val mTvChangePosition by lazy {
        findViewById<TextView>(R.id.tv_change)
    }

    // 视频进度中心层UI
    private val mLChangePosition by lazy {
        findViewById<View>(R.id.change_view)
    }

    // 视频进度中间显示的ProgressBar
    private val mChangePositionProgress by lazy {
        findViewById<ProgressBar>(R.id.pb_change)
    }

    // 音量控制View
    private val mLChangeVolume by lazy {
        findViewById<View>(R.id.volume_view)
    }

    // 音量控制的进度
    private val mChangeVolumeProgress by lazy {
        findViewById<ProgressBar>(R.id.pb_volume)
    }

    // 触摸亮度改变View
    private val mLChangeBrightness by lazy {
        findViewById<View>(R.id.brightness_view)
    }

    // 亮度改变进度
    private val mChangeBrightnessProgress by lazy {
        findViewById<ProgressBar>(R.id.pb_brightness)
    }

    // 锁屏按钮
    private val ivLock by lazy {
        findViewById<ImageView>(R.id.iv_lock)
    }

    // 暂停按钮
    private val ivPause by lazy {
        findViewById<ImageView>(R.id.iv_pause)
    }

    // 切换全屏半屏按钮
    private val ibFullScreen by lazy {
        findViewById<ImageButton>(R.id.ib_full)
    }

    // 视频进度条
    private val seekBar by lazy {
        findViewById<SeekBar>(R.id.seek_bar)
    }

    // 视频当前时间
    private val tvStartTime by lazy {
        findViewById<TextView>(R.id.tv_current)
    }

    // 视频当前时间
    private val tvEndTime by lazy {
        findViewById<TextView>(R.id.tv_duration)
    }

    // 返回键
    private val ivTopBack by lazy {
        findViewById<ImageView>(R.id.iv_back)
    }

    // 视频标题
    private val tvTitle by lazy {
        findViewById<TextView>(R.id.tv_title)
    }

    // 重试按钮
    private val ivRetry by lazy {
        findViewById<ImageView>(R.id.iv_retry)
    }

    // 移动网提示
    private val mLMobileNet by lazy {
        findViewById<View>(R.id.mobile_view)
    }

    // 移动网是否继续
    private val tvContinue by lazy {
        findViewById<TextView>(R.id.tv_continue)
    }

    companion object {
        private const val DEFAULT_DISMISS_TIME = 5000L
    }

    init {
        initView()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initView() {
        View.inflate(context, getLayoutId(), this)
        ibFullScreen.setOnClickListener(this)
        ivPause.setOnClickListener(this)
        ivLock.setOnClickListener(this)
        ivRetry.setOnClickListener(this)
        tvContinue.setOnClickListener(this)
        ivTopBack.setOnClickListener(this)
        seekBar.setOnSeekBarChangeListener(this)
        seekBar.setOnTouchListener(this)
        setOnTouchListener(this)
    }

    protected open fun getLayoutId(): Int {
        return R.layout.widgets_video_controller
    }


    /**
     * 播放器的播放回调发生变化时
     */
    override fun onPlayCommandChanged(command: PlayerCommand) {
        when (command) {
            is PlayerCommand.Preparing -> {
                Log.e("lzt", "Prepared")
                mLoadingView.visible(true)
                mLVideoBottom.visible(false)
                topLayout.visible(false)
                mLError.visible(false)
            }
            is PlayerCommand.Prepared -> {
                Log.e("lzt", "Prepared")
                mLoadingView.visible(false)
                doShowHideTopOrBottom(true)
            }
            is PlayerCommand.BufferingStart -> {
                mLoadingView.visible(true)
            }
            is PlayerCommand.BufferingEnd -> {
                mLoadingView.visible(false)
            }
            is PlayerCommand.Error -> {
                Log.e("lzt", "code:${command.code},extra:${command.extra}")
                ivLock.visible(false)
                mLoadingView.visible(false)
                mLError.visible(true)
                doShowHideTopOrBottom(false)
            }
            is PlayerCommand.Completion -> {
                player.exitFullscreenWindow()
                doShowHideTopOrBottom(false)
                context.showBar(systemFlag)
                onCompletedCallback?.doCompleted()
            }
            is PlayerCommand.MobileNet -> {
                ivLock.visible(false)
                mLoadingView.visible(false)
                mLMobileNet.visible(true)
                doShowHideTopOrBottom(false)
            }
            is PlayerCommand.WifiNet -> {
                mLMobileNet.visible(false)
            }
            is PlayerCommand.StateChanged -> {
                when {
                    command.stateInfo.state == PLAYING -> {
                        ivPause.isSelected = false
                        mLoadingView.visible(false)
                    }
                    command.stateInfo.state == PAUSED -> {
                        ivPause.isSelected = true
                        mLoadingView.visible(false)
                    }
                    command.stateInfo.state == STOPPED -> {
                        if (command.stateInfo.causeCode == CODE_VIDEO_STOPPED_AS_NET_UNAVAILABLE) {
                            ivLock.visible(false)
                            mLoadingView.visible(false)
                            mLError.visible(true)
                            doShowHideTopOrBottom(false)
                        }
                    }
                }
            }
            is PlayerCommand.CurrentProgress -> {
                tvStartTime.text = stringForTime(command.currentPosition)
                tvEndTime.text = stringForTime(command.duration)
                seekBar.secondaryProgress = command.cachedPosition.toInt()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    seekBar.setProgress(command.percent.toInt(), true)
                } else {
                    seekBar.progress = command.percent.toInt()
                }
            }
        }
    }

    /**
     * 播放器的播放模式发生变化时
     */
    override fun onPlayModeChanged(playerMode: PlayerMode) {
        changeBottomSize(playerMode)
        when (playerMode) {
            PlayerMode.MODE_NORMAL -> {
                ivLock.visible(false)
                topLayout.visible(false)
                ibFullScreen.isSelected = false
                isLock = false
            }
            PlayerMode.MODE_FULL_SCREEN -> {
                ivLock.visible(true)
                topLayout.visible(true)
                ibFullScreen.isSelected = true
            }
        }
    }

    /**
     * 更改底部高度
     * 横屏 50dp
     * 竖屏 40dp
     */
    private fun changeBottomSize(playerMode: PlayerMode) {
        val layoutParams = mLVideoBottom.layoutParams
        val width = if (playerMode == PlayerMode.MODE_NORMAL) {
            context.getScreenWidth()
        } else {
            (context.hasNavBar()).yes {
                context.getScreenWidth() - context.getNavBarHeight() - context.getStatusHeight()
            }.otherwise {
                context.getScreenWidth()
            }
        }
        val height = if (playerMode == PlayerMode.MODE_NORMAL) {
            context.dp2px(40f)
        } else {
            context.dp2px(50f)
        }
        layoutParams.width = width
        layoutParams.height = height
        mLVideoBottom.layoutParams = layoutParams
        requestLayout()
    }

    override fun onClick(v: View) {
        when (v) {
            ibFullScreen -> doStartOrExitFullscreenWindow()
            ivPause -> doPauseOrRestart()
            ivLock -> doLock()
            tvContinue -> {
                mLMobileNet.visible(false)
                player.start()
            }
            ivRetry -> player.start()
            ivTopBack -> player.onBackPressed()
        }
    }

    /**
     * 进入或者退出全屏
     */
    private fun doStartOrExitFullscreenWindow() {
        (player.getPlayMode() == PlayerMode.MODE_NORMAL).yes {
            player.startFullscreenWindow()
        }.otherwise {
            player.exitFullscreenWindow()
        }
    }

    /**
     * 暂停或者重新播放
     */
    private fun doPauseOrRestart() {
        (player.isPlaying()).yes {
            player.pause()
        }.otherwise {
            player.start()
        }
    }

    /**
     * 锁屏或者解锁
     */
    private fun doLock() {
        ivLock.isSelected = !isLock
        isLock = !isLock
        doShowHideTopOrBottom(!isLock)
    }

    /**
     * 显示或隐藏底部控制器和顶部控制器
     */
    private fun doShowHideTopOrBottom(visible: Boolean) {
        mLVideoBottom.visible(visible)
        isLock.no { (visible).yes { startDismissControllerTimer() }.otherwise { cancelDismissControllerTimer() } }
        (player.getPlayMode() == PlayerMode.MODE_FULL_SCREEN).yes {
            topLayout.visible(visible)
            visible.no { context.hideBar() }
                    .otherwise { context.showBar(systemFlag) }
        }
        topBottomVisible = !visible
    }

    /**
     * 单击屏幕事件
     */
    override fun onClickUiToggle() {
        if (player.getPlayMode() == PlayerMode.MODE_FULL_SCREEN) {
            ivLock.visible(ivLock.visibility == View.GONE)
            isLock.yes { startDismissControllerTimer() }
        }
        isLock.no {
            doShowHideTopOrBottom(topBottomVisible)
        }
    }

    /**
     * 双击屏幕事件
     */
    override fun touchDoubleUp() {
        super.touchDoubleUp()
        isLock.no { doPauseOrRestart() }
    }


    /**
     * 开始倒计时
     */
    override fun startDismissControllerTimer() {
        cancelDismissControllerTimer()
        if (mDismissControllerCountDownTimer == null) {
            mDismissControllerCountDownTimer = object : CountDownTimer(DEFAULT_DISMISS_TIME, DEFAULT_DISMISS_TIME) {
                override fun onTick(millisUntilFinished: Long) {

                }

                override fun onFinish() {
                    doShowHideTopOrBottom(false)
                    ivLock.visible(false)
                }
            }
        }
        mDismissControllerCountDownTimer?.start()
    }

    /**
     * 中断倒计时
     */
    override fun cancelDismissControllerTimer() {
        mDismissControllerCountDownTimer?.cancel()
    }


    /**
     * 获取是否是锁屏模式
     * @return true表示锁屏
     */
    override fun isLock(): Boolean = isLock

    /**
     * 显示视频播放位置
     * @param duration            视频总时长ms
     * @param newPositionProgress 新的位置进度，取值0到100。
     */
    override fun showChangePosition(duration: Long, newPositionProgress: Int) {
        mLChangePosition.visible(true)
        val newPosition = (duration * newPositionProgress / 100f).toLong()
        mTvChangePosition.text = stringForTime(newPosition)
        mChangePositionProgress.progress = newPositionProgress
        seekBar.progress = newPositionProgress
        tvStartTime.text = stringForTime(newPosition)
    }

    /**
     * 隐藏视频播放位置
     */
    override fun hideChangePosition() {
        mLChangePosition.visible(false)
    }

    /**
     * 展示视频播放音量
     * @param newVolumeProgress 新的音量进度，取值1到100。
     */
    override fun showChangeVolume(newVolumeProgress: Int) {
        mLChangeVolume.visible(true)
        mChangeVolumeProgress.progress = newVolumeProgress
    }

    /**
     * 隐藏视频播放音量
     */
    override fun hideChangeVolume() {
        mLChangeVolume.visible(false)
    }

    /**
     * 展示视频播放亮度
     * @param newBrightnessProgress 新的亮度进度，取值1到100。
     */
    override fun showChangeBrightness(newBrightnessProgress: Int) {
        mLChangeBrightness.visible(true)
        mChangeBrightnessProgress.progress = newBrightnessProgress
    }

    /**
     * 隐藏视频播放亮度
     */
    override fun hideChangeBrightness() {
        mLChangeBrightness.visible(false)
    }

    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {

    }

    override fun onStartTrackingTouch(seekBar: SeekBar) {
        player.stopTimer()
        cancelDismissControllerTimer()
        var vpDown: ViewParent? = parent
        while (vpDown != null) {
            vpDown.requestDisallowInterceptTouchEvent(true)
            vpDown = vpDown.parent
        }
    }

    override fun onStopTrackingTouch(seekBar: SeekBar) {
        (player.getCurrentState().state == PAUSED).yes {
            player.start()
        }
        val position = (player.getDuration() * seekBar.progress / 100f).toLong()
        player.seekTo(position)
        player.startTimer()
        var vpUp: ViewParent? = parent
        while (vpUp != null) {
            vpUp.requestDisallowInterceptTouchEvent(false)
            vpUp = vpUp.parent
        }
        startDismissControllerTimer()
    }

    /**
     * 重置控制器
     */
    override fun reset() {
        cancelDismissControllerTimer()
        topBottomVisible = false
        seekBar.progress = 0
        seekBar.secondaryProgress = 0
        ibFullScreen.isSelected = false
        ivLock.visible(false)
        mLVideoBottom.visible(false)
        topLayout.visible(false)
        mLoadingView.visible(false)
        mLError.visible(false)
    }

    /**
     * 设置视频的title
     */
    fun setVideoTitle(title: String): PlayerController {
        tvTitle.text = title
        return this
    }

    /**
     * 完成对外层的回调
     */
    fun setOnCompletedCallback(onCompletedCallback: OnCompletedCallback): PlayerController {
        this.onCompletedCallback = onCompletedCallback
        return this
    }

}