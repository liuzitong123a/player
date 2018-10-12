package com.kwunai.rx.player.view

import android.content.Context
import android.os.Build

import android.util.AttributeSet
import android.util.Log
import android.view.View
import com.kwunai.rx.player.R
import com.kwunai.rx.player.modal.PlayerMode
import android.widget.SeekBar
import com.kwunai.rx.player.core.PlayerCommand
import com.kwunai.rx.player.ext.*
import com.kwunai.rx.player.modal.PlayerState.*
import kotlinx.android.synthetic.main.widgets_video_controller.view.*
import kotlinx.android.synthetic.main.widgets_bottom_controller.view.*
import kotlinx.android.synthetic.main.widgets_loading_controller.view.*
import kotlinx.android.synthetic.main.widgets_change_brightness_controller.view.*
import kotlinx.android.synthetic.main.widgets_change_progress_controller.view.*
import kotlinx.android.synthetic.main.widgets_change_volume_controller.view.*


/**
 * 点播视频播放器控制层
 */
class VodPlayerController @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : DefaultVideoController(context, attrs, defStyleAttr), View.OnClickListener, SeekBar.OnSeekBarChangeListener {

    private var controllerVisible: Boolean = false

    private var isLock: Boolean = false

    private var systemFlag = systemUiVisibility

    init {
        View.inflate(context, R.layout.widgets_video_controller, this)
        ibFullScreen.setOnClickListener(this)
        ivPause.setOnClickListener(this)
        ivLock.setOnClickListener(this)
        seekBar.setOnSeekBarChangeListener(this)
        setOnTouchListener(this)
    }

    /**
     * 播放器的播放回调发生变化时
     */
    override fun onPlayCommandChanged(command: PlayerCommand) {
        when (command) {
            is PlayerCommand.Preparing -> {
                Log.e("lzt", "Preparing")
                mLoadingView.visibility = View.VISIBLE
            }
            is PlayerCommand.Prepared -> {
                Log.e("lzt", "Prepared")
                mLoadingView.visibility = View.GONE
                mLVideoBottom.visibility = View.VISIBLE
            }
            is PlayerCommand.BufferingStart -> {
                Log.e("lzt", "BufferingStart")
                mLoadingView.visibility = View.VISIBLE
            }
            is PlayerCommand.BufferingEnd -> {
                Log.e("lzt", "BufferingEnd")
                mLoadingView.visibility = View.GONE
            }
            is PlayerCommand.Error -> {
                Log.e("lzt", "Error:${command.code}${command.extra}")
                mLoadingView.visibility = View.GONE
                mLVideoBottom.visibility = View.GONE
            }
            is PlayerCommand.StateChanged -> {
                when {
                    command.stateInfo.state == PLAYING -> ivPause.isSelected = false
                    command.stateInfo.state == PAUSED -> ivPause.isSelected = true
                    command.stateInfo.state == STOPPED -> {

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
                ibFullScreen.isSelected = false
                isLock = false
            }
            PlayerMode.MODE_FULL_SCREEN -> {
                ivLock.visible(true)
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
        (player.getCurrentState().state == PLAYING ||
                player.getCurrentState().state == PAUSED).yes {
            mLVideoBottom.visible(visible)
            (player.getPlayMode() == PlayerMode.MODE_FULL_SCREEN).yes {
                visible.no { context.hideBar() }
                        .otherwise { context.showBar(systemFlag) }
            }
            controllerVisible = !visible
        }
    }

    override fun onClickUiToggle() {
        if (player.getPlayMode() == PlayerMode.MODE_FULL_SCREEN) {
            ivLock.visible(ivLock.visibility == View.GONE)
        }
        isLock.no { doShowHideTopOrBottom(controllerVisible) }
    }

    override fun touchDoubleUp() {
        super.touchDoubleUp()
        isLock.no { doPauseOrRestart() }
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

    override fun onStartTrackingTouch(seekBar: SeekBar?) {
        player.stopTimer()
    }

    override fun onStopTrackingTouch(seekBar: SeekBar) {
        (player.getCurrentState().state == PAUSED).yes {
            player.start()
        }
        val position = (player.getDuration() * seekBar.progress / 100f).toLong()
        player.seekTo(position)
        player.startTimer()
    }
}