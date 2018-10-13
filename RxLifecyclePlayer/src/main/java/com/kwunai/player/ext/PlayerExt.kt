package com.kwunai.player.ext

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.support.v7.app.AppCompatActivity
import android.support.v7.view.ContextThemeWrapper
import java.util.*
import android.R.attr.y
import android.os.Build
import android.support.v4.content.ContextCompat.getSystemService
import android.view.WindowManager
import android.R.attr.x
import android.graphics.Point


fun stringForTime(timeMs: Long): String {
    if (timeMs <= 0 || timeMs >= 24 * 60 * 60 * 1000) {
        return "00:00"
    }
    val totalSeconds = timeMs / 1000
    val seconds = totalSeconds % 60
    val minutes = totalSeconds / 60 % 60
    val hours = totalSeconds / 3600
    val stringBuilder = StringBuilder()
    val mFormatter = Formatter(stringBuilder, Locale.getDefault())
    return if (hours > 0) {
        mFormatter.format("%d:%02d:%02d", hours, minutes, seconds).toString()
    } else {
        mFormatter.format("%02d:%02d", minutes, seconds).toString()
    }
}

fun scanForActivity(context: Context?): Activity? {
    if (context == null) return null
    if (context is Activity) {
        return context
    } else if (context is ContextWrapper) {
        return scanForActivity(context.baseContext)
    }

    return null
}

fun getAppCompActivity(context: Context?): AppCompatActivity? {
    if (context == null) return null
    if (context is AppCompatActivity) {
        return context
    } else if (context is ContextThemeWrapper) {
        return getAppCompActivity(context.baseContext)
    }
    return null
}

fun Context.setRequestedOrientation(orientation: Int) {
    getAppCompActivity(this)?.requestedOrientation = orientation
}

fun Context.dp2px(dpValue: Float): Int {
    val scale = resources.displayMetrics.density
    return (dpValue * scale + 0.5f).toInt()
}

fun Context.getScreenWidth(): Int {
    val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    val point = Point()
    wm.defaultDisplay.getRealSize(point)
    return point.x
}

fun Context.getScreenHeight(): Int {
    val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    val point = Point()
    wm.defaultDisplay.getRealSize(point)
    return point.y
}
