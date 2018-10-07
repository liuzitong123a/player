package com.kwunai.rx.player.view

import android.view.Surface
import com.kwunai.rx.player.modal.VideoScaleMode

interface IRenderViewFork {

    interface SurfaceCallback {

        fun onSurfaceCreated(surface: Surface?)
    }

    fun setVideoSize(videoWidth: Int, videoHeight: Int, videoSarNum: Int, videoSarDen: Int, scaleMode: VideoScaleMode?)

    fun setCallback(callback: SurfaceCallback?)

    fun releaseRender()

}