package com.kwunai.rx.player.core

import com.kwunai.rx.player.modal.StateInfo
import com.kwunai.rx.player.modal.VideoScaleMode
import com.kwunai.rx.player.view.IRenderViewFork
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject

abstract class PlayerStrategy {

    val subject: Subject<PlayerCommandFork> = PublishSubject.create()

    abstract fun setUp(url: String)

    abstract fun setupRenderView(renderView: IRenderViewFork?, videoScaleMode: VideoScaleMode)

    abstract fun onActivityStop()

    abstract fun onActivityResume()

    abstract fun start()

    abstract fun start(position: Long)

    abstract fun restart()

    abstract fun isPlaying(): Boolean

    abstract fun stop()

    abstract fun pause()

    abstract fun seekTo(position: Long)

    abstract fun setPlaybackSpeed(speed: Float)

    abstract fun getCurrentState(): StateInfo

    abstract fun getDuration(): Long

    abstract fun getCurrentPosition(): Long

    abstract fun getCachedPosition(): Long

    abstract fun startVodTimer()

    abstract fun stopVodTimer()
}