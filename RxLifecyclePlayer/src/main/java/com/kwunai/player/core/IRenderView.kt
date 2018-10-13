package com.kwunai.player.core

import android.view.Surface
import com.kwunai.player.modal.VideoScaleMode

interface IRenderView {

    interface SurfaceCallback {

        fun onSurfaceCreated(surface: Surface?)
    }

    fun setVideoSize(videoWidth: Int, videoHeight: Int, videoSarNum: Int, videoSarDen: Int, scaleMode: VideoScaleMode?)

    fun getSurface(): Surface?

    fun setCallback(callback: SurfaceCallback?)

    fun releaseRender()

}