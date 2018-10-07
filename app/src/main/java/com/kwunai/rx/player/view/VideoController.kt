package com.kwunai.rx.player.view

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import com.kwunai.rx.player.R
import com.kwunai.rx.player.modal.PlayMode
import kotlinx.android.synthetic.main.widgets_video_controller.view.*
import android.view.ViewGroup
import android.widget.SeekBar
import com.kwunai.rx.player.core.PlayerCommand
import com.kwunai.rx.player.ext.*
import com.kwunai.rx.player.modal.PlayerState

/**
 * 视频播放器控制层
 */
class VideoController @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), SeekBar.OnSeekBarChangeListener, View.OnClickListener {

    private lateinit var player: ILifecyclePlayer

    private var controllerVisible: Boolean = false

    private var systemFlag = systemUiVisibility

    init {
        View.inflate(context, R.layout.widgets_video_controller, this)
        ibFullScreen.setOnClickListener(this)
        ivPause.setOnClickListener(this)
        setOnClickListener(this)
        seekBar.setOnSeekBarChangeListener(this)
    }

    /**
     * 绑定播放器
     */
    fun setVideoPlayer(player: ILifecyclePlayer) {
        this.player = player
    }

    /**
     * 播放器的播放回调发生变化时
     */
    fun onPlayCommandChanged(command: PlayerCommand) {
        when (command) {
            is PlayerCommand.Preparing -> {
                Log.e("lzt", "Preparing")
                mProgressBar.visibility = View.VISIBLE
            }
            is PlayerCommand.Prepared -> {
                Log.e("lzt", "Prepared")
                mProgressBar.visibility = View.GONE
                mLVideoBottom.visibility = View.VISIBLE
                tvEndTime.text = stringForTime(command.mediaInfo.mDuration)
            }
            is PlayerCommand.BufferingStart -> {
                Log.e("lzt", "BufferingStart")
                mProgressBar.visibility = View.VISIBLE
            }
            is PlayerCommand.BufferingEnd -> {
                Log.e("lzt", "BufferingEnd")
                mProgressBar.visibility = View.GONE
            }
            is PlayerCommand.Completion -> {
                Log.e("lzt", "Completion")
                // listener.onCompletion()
            }
            is PlayerCommand.Error -> {
                Log.e("lzt", "Error:${command.code}${command.extra}")
                mProgressBar.visibility = View.GONE
                mLVideoBottom.visibility = View.GONE
            }
            is PlayerCommand.StateChanged -> {
                if (command.stateInfo.state == PlayerState.STOPPED) {
                    Log.e("lzt", "StateChanged${command.stateInfo}")
                    mProgressBar.visibility = View.GONE
                    mLVideoBottom.visibility = View.GONE
                } else if (command.stateInfo.state == PlayerState.ERROR) {
                    mProgressBar.visibility = View.GONE
                    mLVideoBottom.visibility = View.GONE
                }
            }
            is PlayerCommand.NetStateBad -> {
                Log.e("lzt", "NetStateBad")
            }
            is PlayerCommand.CurrentProgress -> {
                tvStartTime.text = stringForTime(command.currentPosition)
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
    fun onPlayModeChanged(playerMode: PlayMode) {
        changeBottomSize(playerMode)

    }

    /**
     * 更改底部高度
     * 横屏 50dp
     * 竖屏 40dp
     */
    private fun changeBottomSize(playerMode: PlayMode) {
        val layoutParams = mLVideoBottom.layoutParams
        layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
        val height = if (playerMode == PlayMode.MODE_NORMAL) {
            mLVideoBottom.setPadding(0, 0, 0, 0)
            context.dp2px(40f)
        } else {
            (context.hasNavBar()).yes {
                mLVideoBottom.setPadding(0, 0, context.getNavBarHeight(), 0)
            }
            context.dp2px(50f)
        }
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
                    ivPause.isSelected = true
                }.otherwise {
                    player.start()
                    ivPause.isSelected = false
                }
            }
            this -> {
                showController(controllerVisible)
            }
        }
    }

    private fun showController(visible: Boolean) {
        mLVideoBottom.visible(visible)
        (player.getPlayMode() == PlayMode.MODE_FULL_SCREEN).yes {
            visible.no { context.hideBar() }.otherwise { context.showBar(systemFlag) }
        }
        controllerVisible = !visible
    }

    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {

    }

    override fun onStartTrackingTouch(seekBar: SeekBar?) {
        player.stopTimer()
    }

    override fun onStopTrackingTouch(seekBar: SeekBar) {
        player.seekTo(seekBar)
        player.startTimer()
    }
}