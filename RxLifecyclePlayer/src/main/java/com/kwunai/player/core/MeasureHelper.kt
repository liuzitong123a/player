package com.kwunai.player.core

import android.util.Log
import android.view.View
import com.kwunai.player.modal.VideoScaleMode

class MeasureHelper {

    private var mVideoWidth: Int = 0 // 视频帧宽度
    private var mVideoHeight: Int = 0 // 视频帧高度
    private var mVideoSarNum: Int = 0 // 视频帧像素宽高比的宽，计算机产生的像素宽高比都是1:1
    private var mVideoSarDen: Int = 0 // 视频帧像素宽高比的高

    private var mMeasuredWidth: Int = 0 // 测量结果宽spec
    private var mMeasuredHeight: Int = 0 // 测量结果高spec

    private var mVideoScaleMode = VideoScaleMode.FULL

    fun setVideoSize(videoWidth: Int, videoHeight: Int): Boolean {
        if (mVideoWidth == videoWidth && mVideoHeight == videoHeight) {
            return false // the same
        }

        mVideoWidth = videoWidth
        mVideoHeight = videoHeight
        return true // changed
    }

    fun setVideoSampleAspectRatio(videoSarNum: Int, videoSarDen: Int): Boolean {
        if (mVideoSarNum == videoSarNum && mVideoSarDen == videoSarDen) {
            return false // the same
        }

        mVideoSarNum = videoSarNum
        mVideoSarDen = videoSarDen
        return true // changed
    }

    fun setVideoScaleMode(mode: VideoScaleMode): Boolean {
        if (mVideoScaleMode === mode) {
            return false // the same
        }

        mVideoScaleMode = mode
        return true
    }

    fun doMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int): Boolean {
        if (mVideoWidth <= 0 || mVideoHeight <= 0) {
            mMeasuredWidth = 0
            mMeasuredHeight = 0
            return false // 还没有收到画面,先不显示
        }

        // 输出在父容器的约束下，根据视频帧的分辨率，默认的渲染宽高
        var width = View.getDefaultSize(mVideoWidth, widthMeasureSpec)
        var height = View.getDefaultSize(mVideoHeight, heightMeasureSpec)

        if (width == 0 && height == 0) {
            mMeasuredWidth = 0
            mMeasuredHeight = 0
            return true // 主动隐藏
        }

        // 输入宽高spec
        Log.e("lzt", "on measure, input widthMeasureSpec=" + View.MeasureSpec.toString(widthMeasureSpec)
                + ", heightMeasureSpec=" + View.MeasureSpec.toString(heightMeasureSpec) + ", video scale mode=" + mVideoScaleMode)

        /*
         * 父容器约束下的View最大宽、高最大尺寸
         * 自定义View，无论选择WRAP_CONTENT还是MATCH_PARENT，他的尺寸都是size, 即父亲的尺寸;
         * 当然模式会不一样，MATCH_PARENT对应EXACTLY，WRAP_CONTENT对应AT_MOST。
         */
        val widthSpecMode = View.MeasureSpec.getMode(widthMeasureSpec)
        val heightSpecMode = View.MeasureSpec.getMode(heightMeasureSpec)
        val widthSpecSize = View.MeasureSpec.getSize(widthMeasureSpec) // 父容器的宽度
        val heightSpecSize = View.MeasureSpec.getSize(heightMeasureSpec) // 父容器的高度

        // 父容器的宽高比
        val specAspectRatio = widthSpecSize.toFloat() / heightSpecSize.toFloat()

        // 视频帧的宽高比
        var displayAspectRatio = mVideoWidth.toFloat() / mVideoHeight.toFloat()
        if (mVideoSarNum > 0 && mVideoSarDen > 0) {
            displayAspectRatio = displayAspectRatio * mVideoSarNum / mVideoSarDen
        }

        // 是否视频帧的宽高比更大
        val shouldBeWider = displayAspectRatio > specAspectRatio

        // 根据用户指定的缩放模式来调整最终渲染的宽高
        if (mVideoScaleMode === VideoScaleMode.FILL) {
            // 全屏，拉伸到控件允许的最大宽高(即父容器宽高)
            width = widthSpecSize
            height = heightSpecSize
        } else if (mVideoWidth > 0 && mVideoHeight > 0) {
            if (widthSpecMode == View.MeasureSpec.AT_MOST && heightSpecMode == View.MeasureSpec.AT_MOST) {
                // WRAP_CONTENT:根据模式来确定最终绘制的宽高
                if (mVideoScaleMode === VideoScaleMode.FIT) {
                    // 按比例拉伸，有一边会贴黑边
                    if (shouldBeWider) {
                        // too wide, fix width
                        width = widthSpecSize
                        height = (width / displayAspectRatio).toInt()
                    } else {
                        // too high, fix height
                        height = heightSpecSize
                        width = (height * displayAspectRatio).toInt()
                    }
                } else if (mVideoScaleMode === VideoScaleMode.FULL) {
                    // 按比例拉伸至全屏，有一边会被裁剪
                    if (shouldBeWider) {
                        // not high enough, fix height
                        height = heightSpecSize
                        width = (height * displayAspectRatio).toInt()
                    } else {
                        // not wide enough, fix width
                        width = widthSpecSize
                        height = (width / displayAspectRatio).toInt()
                    }
                } else if (mVideoScaleMode === VideoScaleMode.NONE) {
                    // 原始大小
                    if (shouldBeWider) {
                        // too wide, fix width
                        width = Math.min(mVideoWidth, widthSpecSize)
                        height = (width / displayAspectRatio).toInt()
                    } else {
                        // too high, fix height
                        height = Math.min(mVideoHeight, heightSpecSize)
                        width = (height * displayAspectRatio).toInt()
                    }
                } else {
                    Log.e("lzt", "on measure, unsupported scale mode!!!")
                }
            } else if (widthSpecMode == View.MeasureSpec.EXACTLY && heightSpecMode == View.MeasureSpec.EXACTLY) {
                // MATCH_PARENT: 控件大小已经确定即填满父容器，或者已经制定宽度高度具体dip了。
                // 这里只做等比例拉伸，有一边会贴黑边。即 VideoScaleMode.FIT
                // 这样会改变用户预期的控件大小，不推荐！
                if (shouldBeWider) {
                    width = widthSpecSize
                    height = (width / displayAspectRatio).toInt()
                } else {
                    height = heightSpecSize
                    width = (height * displayAspectRatio).toInt()
                }
            } else {
                Log.e("lzt", "on measure, unsupported spec mode!!!")
            }
        } else {
            Log.e("lzt", "on measure, unsupported spec mode!!!")
        }

        // 最后输出的结果
        mMeasuredWidth = width
        mMeasuredHeight = height

        Log.e("lzt", "on measure done, set measure width=$mMeasuredWidth, height=$mMeasuredHeight")
        return true
    }

    fun getMeasuredWidth(): Int {
        return mMeasuredWidth
    }

    fun getMeasuredHeight(): Int {
        return mMeasuredHeight
    }
}