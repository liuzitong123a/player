package com.kwunai.rx.player.view

import android.arch.lifecycle.LifecycleOwner
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.view.Window
import android.widget.FrameLayout
import com.kwunai.rx.player.neplayer.NEPlayerManager
import com.kwunai.rx.player.core.PlayerStrategy
import com.kwunai.rx.player.ext.*
import com.kwunai.rx.player.modal.PlayerMode
import com.kwunai.rx.player.modal.StateInfo
import com.kwunai.rx.player.modal.VideoScaleMode
import io.reactivex.android.schedulers.AndroidSchedulers

/**
 * 视频播放器的容器类，用于绑定生命周期，监听状态变化
 */
class LifeVideoPlayer @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), ILifecyclePlayer {

    // 当前播放器屏幕的状态
    private var currentPlayMode = PlayerMode.MODE_NORMAL

    // 播放核心
    private var playerStrategy: PlayerStrategy? = null

    // 视频所需要的播放组件
    private val surfaceView by lazy {
        TextureRenderView(context)
    }

    // 视频播放器容器
    private val container: FrameLayout by lazy {
        FrameLayout(context)
    }

    // 视频播放器控制器，处理UI
    private lateinit var controller: DefaultVideoController

    init {
        context.transparentBar()
        container.setBackgroundColor(Color.BLACK)
        val params = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT)
        this.addView(container, params)
    }

    /**
     * 设置视频的控制器
     */
    fun setController(controller: DefaultVideoController) {
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
        playerStrategy = NEPlayerManager.configPlayer(context)
        playerStrategy!!.setUp(path)
        playerStrategy!!.setupRenderView(surfaceView, VideoScaleMode.FIT)
        addSurfaceView()
    }

    /**
     * 开始播放
     */
    override fun start() {
        playerStrategy!!.start()
    }

    /**
     * 暂停
     */
    override fun pause() {
        playerStrategy!!.pause()
    }

    /**
     * 是否正在播放
     */
    override fun isPlaying(): Boolean = playerStrategy!!.isPlaying()

    /**
     * 释放VideoPlayer
     */
    private fun releasePlayer() {
        container.removeView(surfaceView)
        playerStrategy?.let {
            it.setupRenderView(null, VideoScaleMode.NONE)
            it.stop()
        }
        playerStrategy = null
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
    override fun seekTo(position: Long) {
        playerStrategy?.seekTo(position)
    }

    /**
     * 开始进度回调
     */
    override fun startTimer() {
        playerStrategy?.startVodTimer()
    }

    /**
     * 结束进度回调
     */
    override fun stopTimer() {
        playerStrategy?.stopVodTimer()
    }

    /**
     * 获取当前视频播放位置
     */
    override fun getCurrentPosition(): Long = playerStrategy?.getCurrentPosition() ?: 0

    /**
     * 获取当前视频总长度
     */
    override fun getDuration(): Long = playerStrategy?.getDuration() ?: 0

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
        currentPlayMode = PlayerMode.MODE_FULL_SCREEN
        controller.onPlayModeChanged(PlayerMode.MODE_FULL_SCREEN)
    }

    /**
     * 退出模式
     */
    override fun exitFullscreenWindow(): Boolean {
        (currentPlayMode == PlayerMode.MODE_FULL_SCREEN).yes {
            context.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
            val contentView = scanForActivity(context)?.findViewById<ViewGroup>(Window.ID_ANDROID_CONTENT)
            contentView?.removeView(container)
            val params = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            )
            this.addView(container, params)
            currentPlayMode = PlayerMode.MODE_NORMAL
            controller.onPlayModeChanged(PlayerMode.MODE_NORMAL)
            return true
        }
        return false
    }

    /**
     * 获取当前屏幕状态
     */
    override fun getPlayMode(): PlayerMode = currentPlayMode

    /**
     * 获取当前播放器状态
     */
    override fun getCurrentState(): StateInfo = playerStrategy!!.getCurrentState()

    /**
     * 获取最大音量
     */
    override fun getMaxVolume(): Int = playerStrategy!!.getMaxVolume()

    /**
     * 获取当前音量
     */
    override fun getVolume(): Int = playerStrategy!!.getVolume()

    /**
     * 改变音量
     */
    override fun setVolume(volume: Int) {
        playerStrategy?.setVolume(volume)
    }

    /**
     * 监听生命周期的onCreate方法
     */
    override fun onCreate(lifecycleOwner: LifecycleOwner) {
        playerStrategy!!.subject
                .observeOn(AndroidSchedulers.mainThread())
                .bindLifecycle(lifecycleOwner)
                .subscribe { controller.onPlayCommandChanged(it) }
    }

    /**
     * 监听生命周期的onResume方法
     */
    override fun onResume(lifecycleOwner: LifecycleOwner) {
        playerStrategy?.onActivityResume()
    }

    /**
     * 监听生命周期的onStop方法
     */
    override fun onStop(lifecycleOwner: LifecycleOwner) {
        playerStrategy?.onActivityStop()
    }

    /**
     * 回退拦截方法
     */
    override fun onBackPressed(): Boolean {
        if (getPlayMode() == PlayerMode.MODE_FULL_SCREEN) {
            return exitFullscreenWindow()
        }
        return false
    }

    /**
     * 监听生命周期的onDestroy方法
     */
    override fun onDestroy(lifecycleOwner: LifecycleOwner) {
        releasePlayer()
    }

}