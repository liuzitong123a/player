package com.kwunai.player.core

import com.kwunai.player.modal.StateInfo
import com.kwunai.player.modal.VideoScaleMode
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject

abstract class PlayerStrategy {

    val subject: Subject<PlayerCommand> = PublishSubject.create()

    abstract fun setUp(url: String)

    abstract fun setupRenderView(renderView: IRenderView?, videoScaleMode: VideoScaleMode)

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

    abstract fun getMaxVolume(): Int

    abstract fun getVolume(): Int

    abstract fun setVolume(volume: Int)

    abstract fun onMobileNetAction()
}