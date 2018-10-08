package com.kwunai.rx.player.view

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.OnLifecycleEvent
import com.kwunai.rx.player.modal.PlayMode
import com.kwunai.rx.player.modal.StateInfo
import org.jetbrains.annotations.NotNull

interface ILifecyclePlayerFork : LifecycleObserver {

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    fun onCreate(@NotNull lifecycleOwner: LifecycleOwner)

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun onResume(@NotNull lifecycleOwner: LifecycleOwner)

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onStop(@NotNull lifecycleOwner: LifecycleOwner)

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onDestroy(@NotNull lifecycleOwner: LifecycleOwner)

    fun start()

    fun pause()

    fun isPlaying(): Boolean

    fun seekTo(position: Long)

    fun stopTimer()

    fun startTimer()

    fun startFullscreenWindow()

    fun exitFullscreenWindow()

    fun getPlayMode(): PlayMode

    fun getCurrentState(): StateInfo

    fun getDuration(): Long
}