package com.kwunai.rx.player.view

import android.content.Context
import android.util.AttributeSet
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RelativeLayout
import com.kwunai.rx.player.modal.VideoScaleMode
import com.orhanobut.logger.Logger


class SurfaceRenderView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : SurfaceView(context, attrs, defStyleAttr), IRenderView, SurfaceHolder.Callback {

    /// callback
    private var mCallback: IRenderView.SurfaceCallback? = null

    /// surface holder state
    private var mSurfaceHolder: SurfaceHolder? = null
    private var mSizeChanged: Boolean = false
    private var mFormat: Int = 0
    private var mWidth: Int = 0
    private var mHeight: Int = 0

    /// measure
    private var mMeasureHelper: MeasureHelper = MeasureHelper()

    /// show/hide
    private var showLayoutParams: ViewGroup.LayoutParams? = null
    private var hideLayoutParams: ViewGroup.LayoutParams? = null

    init {
        holder.addCallback(this)
    }


    override fun onSetupRenderView() {
        showLayoutParams = layoutParams
    }

    override fun setCallback(callback: IRenderView.SurfaceCallback?) {
        if (mCallback != null || callback == null) {
            return  // 已经注册过的或者null注册的，直接返回
        }

        mCallback = callback
        if (mSurfaceHolder != null) {
            mCallback!!.onSurfaceCreated(getSurface())
        }

        if (mSizeChanged) {
            mCallback!!.onSurfaceSizeChanged(getSurface(), mFormat, mWidth, mHeight)
        }
    }

    override fun showView(show: Boolean) {
        if (show) {
            layoutParams = showLayoutParams
            Logger.d("show view")
        } else {
            if (hideLayoutParams == null) {
                when (showLayoutParams) {
                    is FrameLayout.LayoutParams -> hideLayoutParams = FrameLayout.LayoutParams(0, 0)
                    is RelativeLayout.LayoutParams -> hideLayoutParams = RelativeLayout.LayoutParams(0, 0)
                    is LinearLayout.LayoutParams -> hideLayoutParams = LinearLayout.LayoutParams(0, 0)
                }
            }

            if (hideLayoutParams != null) {
                layoutParams = hideLayoutParams
                Logger.d("hide view")
            } else {
                Logger.d("unsupported layout for hide view!!!")
            }
        }
    }

    override fun setVideoSize(videoWidth: Int, videoHeight: Int, videoSarNum: Int, videoSarDen: Int, scaleMode: VideoScaleMode?) {
        var changed = false

        if (videoWidth > 0 && videoHeight > 0 && mMeasureHelper.setVideoSize(videoWidth, videoHeight)) {
            holder.setFixedSize(videoWidth, videoHeight)
            changed = true
        }

        if (videoSarNum > 0 && videoSarDen > 0 && mMeasureHelper.setVideoSampleAspectRatio(videoSarNum, videoSarDen)) {
            changed = true
        }

        if (scaleMode != null && mMeasureHelper.setVideoScaleMode(scaleMode)) {
            changed = true
        }

        if (changed) {
            Logger.d("set video size to render view done, request layout...")
            requestLayout()
        }
    }

    override fun getSurface(): Surface? {
        return if (mSurfaceHolder != null) mSurfaceHolder!!.surface else null
    }

    override fun surfaceCreated(holder: SurfaceHolder?) {
        mSurfaceHolder = holder
        mSizeChanged = false
        mFormat = 0
        mWidth = 0
        mHeight = 0

        if (mCallback != null) {
            mCallback!!.onSurfaceCreated(holder?.surface)
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder?) {
        mSurfaceHolder = null
        mSizeChanged = false
        mFormat = 0
        mWidth = 0
        mHeight = 0

        if (mCallback != null) {
            mCallback!!.onSurfaceDestroyed(holder?.surface)
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
        mSurfaceHolder = holder
        mSizeChanged = true
        mFormat = format
        mWidth = width
        mHeight = height

        if (mCallback != null) {
            mCallback!!.onSurfaceSizeChanged(holder?.surface, format, width, height)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        mMeasureHelper.doMeasure(widthMeasureSpec, heightMeasureSpec)
        setMeasuredDimension(mMeasureHelper.getMeasuredWidth(), mMeasureHelper.getMeasuredHeight())
    }
}