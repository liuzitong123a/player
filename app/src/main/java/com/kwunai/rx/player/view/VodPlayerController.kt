package com.kwunai.rx.player.view

import android.content.Context
import android.os.Build

import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import com.kwunai.rx.player.R
import com.kwunai.rx.player.modal.PlayMode
import kotlinx.android.synthetic.main.widgets_video_controller.view.*
import android.widget.SeekBar
import com.kwunai.rx.player.core.PlayerCommand
import com.kwunai.rx.player.ext.*
import com.kwunai.rx.player.modal.PlayerState.*

/**
 * 视频播放器控制层
 */
class VodPlayerController @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : DefaultVideoController(context, attrs, defStyleAttr),
        View.OnClickListener, SeekBar.OnSeekBarChangeListener {

    private var controllerVisible: Boolean = false

    private var systemFlag = systemUiVisibility

    init {
        View.inflate(context, R.layout.widgets_video_controller, this)
        ibFullScreen.setOnClickListener(this)
        ivPause.setOnClickListener(this)
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
    override fun onPlayModeChanged(playerMode: PlayMode) {
        changeBottomSize(playerMode)
    }

    /**
     * 更改底部高度
     * 横屏 50dp
     * 竖屏 40dp
     */
    private fun changeBottomSize(playerMode: PlayMode) {
        val layoutParams = mLVideoBottom.layoutParams
        val width = if (playerMode == PlayMode.MODE_NORMAL) {
            context.getScreenWidth()
        } else {
            (context.hasNavBar()).yes {
                context.getScreenWidth() - context.getNavBarHeight() - context.getStatusHeight()
            }.otherwise {
                context.getScreenWidth()
            }
        }
        val height = if (playerMode == PlayMode.MODE_NORMAL) {
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
            ibFullScreen -> {
                (player.getPlayMode() == PlayMode.MODE_NORMAL).yes {
                    player.startFullscreenWindow()
                }.otherwise {
                    player.exitFullscreenWindow()
                }
            }
            ivPause -> {
                (player.isPlaying()).yes {
                    player.pause()
                }.otherwise {
                    player.start()
                }
            }
        }
    }

    override fun onClickUiToggle() {
        (player.getCurrentState().state == PLAYING ||
                player.getCurrentState().state == PAUSED).yes {
            mLVideoBottom.visible(controllerVisible)
            (player.getPlayMode() == PlayMode.MODE_FULL_SCREEN).yes {
                controllerVisible.no { context.hideBar() }.otherwise { context.showBar(systemFlag) }
            }
            controllerVisible = !controllerVisible
        }
    }

    override fun isLock(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun showChangePosition(duration: Long, newPositionProgress: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun hideChangePosition() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun showChangeVolume(newVolumeProgress: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun hideChangeVolume() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun showChangeBrightness(newBrightnessProgress: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun hideChangeBrightness() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
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