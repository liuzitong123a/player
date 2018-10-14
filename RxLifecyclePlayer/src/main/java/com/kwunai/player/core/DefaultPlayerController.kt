package com.kwunai.player.core

import android.content.Context
import android.os.Build
import android.os.CountDownTimer

import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.ViewParent
import com.kwunai.player.modal.PlayerMode
import android.widget.SeekBar
import com.kwunai.player.R
import com.kwunai.player.ext.*
import com.kwunai.player.modal.CauseCode.CODE_VIDEO_STOPPED_AS_NET_UNAVAILABLE
import com.kwunai.player.modal.PlayerState.*
import kotlinx.android.synthetic.main.widgets_bottom_controller.view.*
import kotlinx.android.synthetic.main.widgets_change_brightness_controller.view.*
import kotlinx.android.synthetic.main.widgets_change_progress_controller.view.*
import kotlinx.android.synthetic.main.widgets_change_volume_controller.view.*
import kotlinx.android.synthetic.main.widgets_error_controller.view.*
import kotlinx.android.synthetic.main.widgets_loading_controller.view.*
import kotlinx.android.synthetic.main.widgets_mobile_net_controller.view.*
import kotlinx.android.synthetic.main.widgets_top_controller.view.*
import kotlinx.android.synthetic.main.widgets_video_controller.view.*

/**
 * 点播视频播放器控制层
 */
class DefaultPlayerController @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : VideoController(context, attrs, defStyleAttr), View.OnClickListener, SeekBar.OnSeekBarChangeListener {

    private var topBottomVisible: Boolean = false

    private var isLock: Boolean = false

    private var systemFlag = systemUiVisibility

    private var mDismissControllerCountDownTimer: CountDownTimer? = null

    private var onCompletedCallback: OnCompletedCallback? = null

    companion object {
        private const val DEFAULT_DISMISS_TIME = 5000L
    }

    init {
        View.inflate(context, R.layout.widgets_video_controller, this)
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
    fun setVideoTitle(title: String): DefaultPlayerController {
        tvTitle.text = title
        return this
    }

    /**
     * 完成对外层的回调
     */
    fun setOnCompletedCallback(onCompletedCallback: OnCompletedCallback): DefaultPlayerController {
        this.onCompletedCallback = onCompletedCallback
        return this
    }

}