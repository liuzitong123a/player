package com.kwunai.rx.player.core

import com.kwunai.rx.player.modal.StateInfo
import com.kwunai.rx.player.modal.VideoScaleMode
import com.kwunai.rx.player.view.IRenderView


abstract class PlayerDispatcher {
    abstract fun setUp(url: String)

    abstract fun start()

    abstract fun setupRenderView(renderView: IRenderView?, videoScaleMode: VideoScaleMode)

    abstract fun setVideoScaleMode(videoScaleMode: VideoScaleMode?)

    abstract fun onActivityStop()

    abstract fun onActivityResume()

    abstract fun isPlaying(): Boolean

    abstract fun stop()

    abstract fun pause()

    abstract fun getCurrentState(): StateInfo

    abstract fun getDuration(): Long

    abstract fun getCurrentPosition(): Long

    abstract fun getCachedPosition(): Long

    abstract fun seekTo(position: Long)

    abstract fun setPlaybackSpeed(speed: Float)

}