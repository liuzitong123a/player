package com.kwunai.rx.player.view

import android.content.Context
import android.graphics.SurfaceTexture
import android.util.AttributeSet
import android.view.Surface
import android.view.TextureView
import com.kwunai.rx.player.ext.otherwise
import com.kwunai.rx.player.ext.yes
import com.kwunai.rx.player.modal.VideoScaleMode

class TextureRenderView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : TextureView(context, attrs, defStyleAttr), IRenderViewFork, TextureView.SurfaceTextureListener {


    private var mCallback: IRenderViewFork.SurfaceCallback? = null

    private var mSurfaceTexture: SurfaceTexture? = null

    private var mSurface: Surface? = null

    private var mMeasureHelper: MeasureHelper = MeasureHelper()

    init {
        surfaceTextureListener = this
    }

    override fun setVideoSize(videoWidth: Int, videoHeight: Int, videoSarNum: Int, videoSarDen: Int, scaleMode: VideoScaleMode?) {
        var changed = false

        if (videoWidth > 0 && videoHeight > 0 && mMeasureHelper.setVideoSize(videoWidth, videoHeight)) {
            changed = true
        }

        if (videoSarNum > 0 && videoSarDen > 0 && mMeasureHelper.setVideoSampleAspectRatio(videoSarNum, videoSarDen)) {
            changed = true
        }

        if (scaleMode != null && mMeasureHelper.setVideoScaleMode(scaleMode)) {
            changed = true
        }

        if (changed) {
            requestLayout()
        }
    }

    override fun setCallback(callback: IRenderViewFork.SurfaceCallback?) {
        if (mCallback != null || callback == null) {
            return
        }
        mCallback = callback
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture?, width: Int, height: Int) {

    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {

    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?): Boolean {
        return mSurfaceTexture == null
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture?, width: Int, height: Int) {
        if (mSurfaceTexture == null) {
            mSurfaceTexture = surface
            if (mSurface == null) {
                mSurface = Surface(surfaceTexture)
            }
            mCallback?.onSurfaceCreated(surface = mSurface)
        } else {
            surfaceTexture = mSurfaceTexture
        }
    }

    override fun releaseRender() {
        if (mSurface != null) {
            mSurface!!.release()
            mSurface = null
        }
        if (mSurfaceTexture != null) {
            mSurfaceTexture!!.release()
            mSurfaceTexture = null
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        mMeasureHelper.doMeasure(widthMeasureSpec, heightMeasureSpec).yes {
            setMeasuredDimension(mMeasureHelper.getMeasuredWidth(), mMeasureHelper.getMeasuredHeight())
        }.otherwise {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        }
    }
}