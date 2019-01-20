package com.kwunai.player.core

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.OnLifecycleEvent
import com.kwunai.player.modal.PlayerMode
import com.kwunai.player.modal.StateInfo
import org.jetbrains.annotations.NotNull

interface ILifecyclePlayer : LifecycleObserver {

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun onResume(@NotNull lifecycleOwner: LifecycleOwner)

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onStop(@NotNull lifecycleOwner: LifecycleOwner)

    fun onBackPressed(): Boolean

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onDestroy(@NotNull lifecycleOwner: LifecycleOwner)

    fun dispatchReceiverEvent()

    fun start()

    fun start(position: Long)

    fun pause()

    fun isPlaying(): Boolean

    fun seekTo(position: Long)

    fun stopTimer()

    fun startTimer()

    fun startFullscreenWindow()

    fun exitFullscreenWindow(): Boolean

    fun getPlayMode(): PlayerMode

    fun getCurrentState(): StateInfo

    fun getCurrentPosition(): Long

    fun getDuration(): Long

    fun getMaxVolume(): Int

    fun getVolume(): Int

    fun setVolume(volume: Int)

    fun onNetMobileEvent()
}