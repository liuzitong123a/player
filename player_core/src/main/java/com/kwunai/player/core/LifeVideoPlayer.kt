package com.kwunai.player.core

import android.arch.lifecycle.LifecycleOwner
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.util.AttributeSet
import android.view.Gravity
import android.view.ViewGroup
import android.view.Window
import android.widget.FrameLayout
import com.kwunai.player.ext.*
import com.kwunai.player.modal.PlayerMode
import com.kwunai.player.modal.StateInfo
import com.kwunai.player.modal.VideoScaleMode
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable

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

    // 进度监听器
    private var playerObservable: Disposable? = null

    // 视频所需要的播放组件
    private val renderView by lazy {
        RenderView(context)
    }

    // 视频播放器容器
    private val container: FrameLayout by lazy {
        FrameLayout(context)
    }

    // 视频播放器控制器，处理UI
    private lateinit var controller: VideoController

    init {
        context.transparentBar()
        container.setBackgroundColor(Color.BLACK)
        val params = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT)
        this.addView(container, params)
    }


    /**
     * 初始化视频播放器
     */
    fun init(playerStrategy: PlayerStrategy) {
        this.playerStrategy = playerStrategy
    }

    /**
     * 外层配置
     */
    fun createPlayerConfig(path: String) {
        playerStrategy!!.setUp(path)
        playerStrategy!!.setupRenderView(renderView, VideoScaleMode.FIT)
        addSurfaceView()
    }

    /**
     * 设置视频的控制器
     */
    fun setController(controller: VideoController) {
        container.removeView(controller)
        this.controller = controller
        controller.setVideoPlayer(this)
        val params = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT)
        container.addView(controller, params)
    }


    /**
     * 开始播放
     */
    override fun start() {
        playerStrategy!!.start()
    }

    /**
     * 从指定位置开始播放
     */
    override fun start(position: Long) {
        playerStrategy!!.start(position)
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
        container.removeView(renderView)
        playerStrategy?.let {
            it.setupRenderView(null, VideoScaleMode.NONE)
            it.stop()
        }
        playerStrategy = null
        controller.reset()
    }


    /**
     * 添加SurfaceView到视图中
     */
    private fun addSurfaceView() {
        container.removeView(renderView)
        val params = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER)
        container.addView(renderView, 0, params)
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
     * 移动网络下的操作
     */
    override fun onNetMobileEvent() {
        playerStrategy?.onMobileNetAction()
    }


    override fun dispatchReceiverEvent() {
        playerObservable = playerStrategy!!.emit.subject()
                .observeOn(AndroidSchedulers.mainThread())
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
        playerObservable?.let {
            it.isDisposed.no {
                it.dispose()
            }
        }
    }
}