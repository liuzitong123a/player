package com.kwunai.player.core

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import com.kwunai.player.modal.PlayerMode
import android.view.GestureDetector
import com.kwunai.player.modal.PlayerState
import com.kwunai.player.ext.scanForActivity

abstract class VideoController @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), View.OnTouchListener {

    // 触摸的X
    private var mDownX: Float = 0F
    // 触摸的Y
    private var mDownY: Float = 0F
    // 是否改变亮度
    private var mChangeBrightness = false
    // 是否改变音量
    private var mChangeVolume = false
    // 是否改变播放进度
    private var mChangePosition = false
    // 手势偏差值
    private var mThreshold = 80
    // 当前播放的进度
    private var mGestureDownPosition: Long = 0
    // 当前亮度的值
    private var mGestureDownBrightness: Float = 0F
    // 当前音量的值
    private var mGestureDownVolume: Int = 0
    // 滑动到的新位置
    private var mNewPosition: Long = 0
    // 绑定的播放器
    protected lateinit var player: LifecyclePlayer

    /**
     * 绑定播放器
     */
    fun setVideoPlayer(player: LifecyclePlayer) {
        this.player = player
    }

    /**
     * 获取是否是锁屏模式
     * @return true表示锁屏
     */
    abstract fun isLock(): Boolean

    /**
     * 播放器Action回调
     */
    abstract fun onPlayCommandChanged(command: PlayerCommand)

    /**
     * 播放器显示模式改变
     */
    abstract fun onPlayModeChanged(playerMode: PlayerMode)

    /**
     * 单击屏幕改变UI
     */
    abstract fun onClickUiToggle()

    /**
     * 点播双击暂停
     */
    protected open fun touchDoubleUp() {

    }

    /**
     * 手势左右滑动改变播放位置时，显示控制器中间的播放位置变化视图，
     * 在手势滑动ACTION_MOVE的过程中，会不断调用此方法。
     *
     * @param duration            视频总时长ms
     * @param newPositionProgress 新的位置进度，取值0到100。
     */
    protected open fun showChangePosition(duration: Long, newPositionProgress: Int) {

    }

    /**
     * 手势左右滑动改变播放位置后，手势up或者cancel时，隐藏控制器中间的播放位置变化视图，
     * 在手势ACTION_UP或ACTION_CANCEL时调用。
     */
    protected open fun hideChangePosition() {

    }

    /**
     * 手势在右侧上下滑动改变音量时，显示控制器中间的音量变化视图，
     * 在手势滑动ACTION_MOVE的过程中，会不断调用此方法。
     *
     * @param newVolumeProgress 新的音量进度，取值1到100。
     */
    abstract fun showChangeVolume(newVolumeProgress: Int)

    /**
     * 手势在左侧上下滑动改变音量后，手势up或者cancel时，隐藏控制器中间的音量变化视图，
     * 在手势ACTION_UP或ACTION_CANCEL时调用。
     */
    abstract fun hideChangeVolume()

    /**
     * 手势在左侧上下滑动改变亮度时，显示控制器中间的亮度变化视图，
     * 在手势滑动ACTION_MOVE的过程中，会不断调用此方法。
     *
     * @param newBrightnessProgress 新的亮度进度，取值1到100。
     */
    abstract fun showChangeBrightness(newBrightnessProgress: Int)

    /**
     * 手势在左侧上下滑动改变亮度后，手势up或者cancel时，隐藏控制器中间的亮度变化视图，
     * 在手势ACTION_UP或ACTION_CANCEL时调用。
     */
    abstract fun hideChangeBrightness()

    abstract fun startDismissControllerTimer()

    abstract fun cancelDismissControllerTimer()

    /**
     * 重置
     */
    abstract fun reset()

    /**
     * 手势事件
     */
    private var mGestureDetector = GestureDetector(getContext(), object : GestureDetector.SimpleOnGestureListener() {

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            if (!mChangePosition && !mChangeVolume && !mChangeBrightness) {
                onClickUiToggle()
            }
            return super.onSingleTapConfirmed(e)
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            touchDoubleUp()
            return super.onDoubleTap(e)
        }
    })


    override fun onTouch(v: View?, event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y
        if (player.getCurrentState().state == PlayerState.PLAYING ||
                player.getCurrentState().state == PlayerState.PAUSED) {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    mDownX = x
                    mDownY = y
                    mChangePosition = false
                    mChangeVolume = false
                    mChangeBrightness = false
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = x - mDownX
                    var deltaY = y - mDownY
                    val absDeltaX = Math.abs(deltaX)
                    val absDeltaY = Math.abs(deltaY)
                    if (!isLock()) {
                        if (!mChangePosition && !mChangeVolume && !mChangeBrightness) {
                            if (absDeltaX >= mThreshold) {
                                player.stopTimer()
                                mChangePosition = true
                                mGestureDownPosition = player.getCurrentPosition()
                            } else if (absDeltaY >= mThreshold) {
                                if (mDownX < width * 0.5f) {
                                    mChangeBrightness = true
                                    mGestureDownBrightness = scanForActivity(context)!!.window.attributes.screenBrightness
                                } else {
                                    mChangeVolume = true
                                    mGestureDownVolume = player.getVolume()
                                }
                            }
                        }
                    }
                    if (mChangePosition) {
                        cancelDismissControllerTimer()
                        val duration = player.getDuration()
                        val toPosition = (mGestureDownPosition + duration * deltaX / width).toLong()
                        mNewPosition = Math.max(0, Math.min(duration, toPosition))
                        val newPositionProgress = (100f * mNewPosition / duration).toInt()
                        showChangePosition(duration, newPositionProgress)
                    }
                    if (mChangeBrightness) {
                        deltaY = -deltaY
                        val deltaBrightness = deltaY * 3 / height
                        var newBrightness = mGestureDownBrightness + deltaBrightness
                        newBrightness = Math.max(0f, Math.min(newBrightness, 1f))
                        val newBrightnessPercentage = newBrightness
                        val params = scanForActivity(context)!!.window.attributes
                        params.screenBrightness = newBrightnessPercentage
                        scanForActivity(context)!!.window.attributes = params
                        val newBrightnessProgress = (100f * newBrightnessPercentage).toInt()
                        showChangeBrightness(newBrightnessProgress)
                    }
                    if (mChangeVolume) {
                        deltaY = -deltaY
                        val maxVolume = player.getMaxVolume()
                        val deltaVolume = (maxVolume.toFloat() * deltaY * 3f / height).toInt()
                        var newVolume = mGestureDownVolume + deltaVolume
                        newVolume = Math.max(0, Math.min(maxVolume, newVolume))
                        player.setVolume(newVolume)
                        val newVolumeProgress = (100f * newVolume / maxVolume).toInt()
                        showChangeVolume(newVolumeProgress)
                    }
                }
                MotionEvent.ACTION_CANCEL,
                MotionEvent.ACTION_UP -> {
                    if (mChangePosition) {
                        startDismissControllerTimer()
                        player.seekTo(mNewPosition)
                        hideChangePosition()
                        player.startTimer()
                        return true
                    }
                    if (mChangeBrightness) {
                        hideChangeBrightness()
                        return true
                    }
                    if (mChangeVolume) {
                        hideChangeVolume()
                        return true
                    }
                }
            }
            mGestureDetector.onTouchEvent(event)
            return true
        }
        return false
    }
}