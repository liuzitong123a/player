package com.kwunai.rx.player.view

import android.arch.lifecycle.LifecycleOwner
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.support.annotation.CallSuper
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.view.Window
import android.widget.FrameLayout
import android.widget.SeekBar
import com.kwunai.rx.player.core.PlayerDispatcherImpl
import com.kwunai.rx.player.core.PlayerManager
import com.kwunai.rx.player.ext.bindLifecycle
import com.kwunai.rx.player.ext.scanForActivity
import com.kwunai.rx.player.ext.setRequestedOrientation
import com.kwunai.rx.player.modal.PlayMode
import com.kwunai.rx.player.modal.VideoScaleMode
import io.reactivex.android.schedulers.AndroidSchedulers


class LifeVideoPlayer @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), ILifecyclePlayer {

    private var currentPlayMode = PlayMode.MODE_NORMAL

    private var playerDispatcher: PlayerDispatcherImpl? = null

    private val surfaceView by lazy {
        SurfaceRenderView(context)
    }

    private val container: FrameLayout by lazy {
        FrameLayout(context)
    }

    private lateinit var controller: VideoController

    init {
        container.setBackgroundColor(Color.BLACK)
        val params = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT)
        this.addView(container, params)
    }

    /**
     * 设置视频的控制器
     */
    fun setController(controller: VideoController) {
        //这里必须先移除
        container.removeView(controller)
        this.controller = controller
        controller.setVideoPlayer(this)

        val params = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT)
        container.addView(controller, params)
    }

    /**
     * 外层配置
     */
    fun createPlayerConfig(path: String) {
        playerDispatcher = PlayerManager.configPlayer(context)
        playerDispatcher!!.setUp(path)
        addSurfaceView()
        playerDispatcher!!.setupRenderView(surfaceView, VideoScaleMode.FULL)
    }

    /**
     * 开始播放
     */
    override fun start() {
        playerDispatcher!!.start()
    }

    override fun isPlaying(): Boolean {
        return playerDispatcher!!.isPlaying()
    }

    override fun pause() {
        playerDispatcher!!.pause()
    }

    /**
     * 释放VideoPlayer
     */
    private fun releasePlayer() {
        playerDispatcher?.let {
            it.setupRenderView(null, VideoScaleMode.NONE)
            it.stop()
        }
        playerDispatcher = null
    }

    /**
     * 添加SurfaceView到视图中
     */
    private fun addSurfaceView() {
        container.removeView(surfaceView)
        val params = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER)
        container.addView(surfaceView, 0, params)
    }

    /**
     * 拖动进度
     */
    override fun seekTo(seekBar: SeekBar) {
        playerDispatcher?.let {
            it.seekTo(it.getDuration() * seekBar.progress / 100)
        }
    }

    override fun startTimer() {
        playerDispatcher?.startVodTimer()
    }

    override fun stopTimer() {
        playerDispatcher?.stopVodTimer(false)
    }


    /**
     * 全屏模式
     */
    override fun startFullscreenWindow() {
        context.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
        val contentView = scanForActivity(context)?.findViewById<ViewGroup>(Window.ID_ANDROID_CONTENT)
        this.removeView(container)
        val params = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        )
        contentView?.addView(container, params)
        currentPlayMode = PlayMode.MODE_FULL_SCREEN
        controller.onPlayModeChanged(PlayMode.MODE_FULL_SCREEN)
    }

    /**
     * 退出模式
     */
    override fun exitFullscreenWindow() {
        context.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
        val contentView = scanForActivity(context)?.findViewById<ViewGroup>(Window.ID_ANDROID_CONTENT)
        contentView?.removeView(container)
        val params = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        )
        this.addView(container, params)
        currentPlayMode = PlayMode.MODE_NORMAL
        controller.onPlayModeChanged(PlayMode.MODE_NORMAL)
    }

    override fun getPlayMode(): PlayMode = currentPlayMode

    @CallSuper
    override fun onCreate(lifecycleOwner: LifecycleOwner) {
        Log.e("lzt", "player onCreate register receiver")
        playerDispatcher!!.subject
                .observeOn(AndroidSchedulers.mainThread())
                .bindLifecycle(lifecycleOwner)
                .subscribe { controller.onPlayCommandChanged(it) }
    }


    @CallSuper
    override fun onResume(lifecycleOwner: LifecycleOwner) {
        Log.e("lzt", "player onResume")
        playerDispatcher?.onActivityResume()
    }

    @CallSuper
    override fun onStop(lifecycleOwner: LifecycleOwner) {
        Log.e("lzt", "player onStop")
        playerDispatcher?.onActivityStop()
    }

    @CallSuper
    override fun onDestroy(lifecycleOwner: LifecycleOwner) {
        Log.e("lzt", "player onDestroy")
        releasePlayer()
    }
}