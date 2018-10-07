package com.kwunai.rx.player.view

import android.view.Surface
import com.kwunai.rx.player.modal.VideoScaleMode

interface IRenderView {

    interface SurfaceCallback {
        fun onSurfaceCreated(surface: Surface?)

        fun onSurfaceDestroyed(surface: Surface?)

        fun onSurfaceSizeChanged(surface: Surface?, format: Int, width: Int, height: Int)
    }

    fun onSetupRenderView()

    fun setVideoSize(videoWidth: Int, videoHeight: Int, videoSarNum: Int, videoSarDen: Int, scaleMode: VideoScaleMode?)

    fun getSurface(): Surface?

    fun setCallback(callback: SurfaceCallback?)

    fun showView(show: Boolean)
}