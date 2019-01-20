package com.kwunai.player.ext

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.View
import android.graphics.Color
import android.os.Build
import android.view.WindowManager
import android.util.DisplayMetrics

/**
 * 获取statusBar的高度
 */
@SuppressLint("PrivateApi")
fun Context.getStatusHeight(): Int {
    var height = -1
    try {
        val clazz = Class.forName("com.android.internal.R\$dimen")
        val `object` = clazz.newInstance()
        val heightStr = clazz.getField("status_bar_height").get(`object`).toString()
        height = Integer.parseInt(heightStr)
        height = resources.getDimensionPixelSize(height)
    } catch (e: ClassNotFoundException) {
        e.printStackTrace()
    } catch (e: InstantiationException) {
        e.printStackTrace()
    } catch (e: IllegalAccessException) {
        e.printStackTrace()
    } catch (e: NoSuchFieldException) {
        e.printStackTrace()
    }
    return height
}

/**
 * 获取NavigationBar的高度
 */
@SuppressLint("PrivateApi")
fun Context.getNavBarHeight(): Int {
    var height = -1
    try {
        val clazz = Class.forName("com.android.internal.R\$dimen")
        val `object` = clazz.newInstance()
        val heightStr = clazz.getField("navigation_bar_height").get(`object`).toString()
        height = Integer.parseInt(heightStr)
        height = resources.getDimensionPixelSize(height)
    } catch (e: ClassNotFoundException) {
        e.printStackTrace()
    } catch (e: InstantiationException) {
        e.printStackTrace()
    } catch (e: IllegalAccessException) {
        e.printStackTrace()
    } catch (e: NoSuchFieldException) {
        e.printStackTrace()
    }
    return height
}

fun Context.hasNavBar(): Boolean {
    val windowManager = scanForActivity(this)?.windowManager
    windowManager?.let {
        val display = it.defaultDisplay
        val displayMetrics = DisplayMetrics()
        display.getRealMetrics(displayMetrics)
        val heightDisplay = displayMetrics.heightPixels
        val widthDisplay = displayMetrics.widthPixels
        val contentDisplayMetrics = DisplayMetrics()
        display.getMetrics(contentDisplayMetrics)
        val contentDisplay = contentDisplayMetrics.heightPixels
        val contentDisplayWidth = contentDisplayMetrics.widthPixels
        val w = widthDisplay - contentDisplayWidth
        val h = heightDisplay - contentDisplay
        return w > 0 || h > 0
    }
    return false
}


/**
 * 检测华为手机是否有刘海屏
 */
fun Context.hasNotchAtHuawei(): Boolean {
    var ret = false
    try {
        val classLoader = classLoader
        val hwNotchSizeUtil = classLoader.loadClass("com.huawei.android.util.HwNotchSizeUtil")
        val method = hwNotchSizeUtil.getMethod("hasNotchInScreen")
        ret = method.invoke(hwNotchSizeUtil) as Boolean
    } catch (e: ClassNotFoundException) {
        Log.e("Notch", "hasNotchAtHuawei ClassNotFoundException")
    } catch (e: NoSuchMethodException) {
        Log.e("Notch", "hasNotchAtHuawei NoSuchMethodException")
    } catch (e: Exception) {
        Log.e("Notch", "hasNotchAtHuawei Exception")
    } finally {
        return ret
    }
}

@SuppressLint("PrivateApi")
fun Context.hasNotchAtVivo(): Boolean {
    var ret = false
    try {
        val classLoader = classLoader
        val ftFeature = classLoader.loadClass("android.util.FtFeature")
        val method = ftFeature.getMethod("isFeatureSupport", Int::class.java)
        ret = method.invoke(ftFeature, 0x00000020) as Boolean
    } catch (e: ClassNotFoundException) {
        Log.e("Notch", "hasNotchAtVivo ClassNotFoundException")
    } catch (e: NoSuchMethodException) {
        Log.e("Notch", "hasNotchAtVivo NoSuchMethodException")
    } catch (e: Exception) {
        Log.e("Notch", "hasNotchAtVivo Exception")
    } finally {
        return ret
    }
}

/**
 * OPPO是否有刘海屏
 */
fun Context.hasNotchAtOPPO(): Boolean {
    return packageManager.hasSystemFeature("com.oppo.feature.screen.heteromorphism")
}

/**
 * 小米是否有刘海屏
 */
@SuppressLint("PrivateApi")
fun Context.hasNotchAtXiaoMi(): Boolean {
    var ret = 0
    try {
        val classLoader = classLoader
        val systemProperties = classLoader.loadClass("android.os.SystemProperties")
        val paramTypes = arrayOfNulls<Class<*>>(2)
        paramTypes[0] = String::class.java
        paramTypes[1] = Int::class.java
        val getInt = systemProperties.getMethod("getInt", *paramTypes)
        val params = arrayOfNulls<Any>(2)
        params[0] = "ro.miui.notch"
        params[1] = 0
        ret = getInt.invoke(systemProperties, params) as Int
    } catch (e: IllegalArgumentException) {
        e.printStackTrace()
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return ret == 1
}

/**
 * 是否有刘海屏
 */
fun Context.hasNotch(): Boolean {
    return hasNotchAtHuawei() || hasNotchAtOPPO() || hasNotchAtVivo() || hasNotchAtXiaoMi()
}

fun Context.transparentBar() {
    val activity = scanForActivity(context = this)
    activity?.let {
        it.window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        val flag = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            it.window.statusBarColor = Color.TRANSPARENT
        }
        it.window.decorView.systemUiVisibility = flag
    }

}

fun Context.hideBar() {
    val activity = scanForActivity(context = this)
    activity?.let {
        it.window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        val flag = (View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            it.window.statusBarColor = Color.TRANSPARENT
        }
        it.window.decorView.systemUiVisibility = flag
    }
}

fun Context.showBar(flag: Int) {
    val activity = scanForActivity(context = this)
    activity?.let {
        it.window.decorView.systemUiVisibility = flag
    }

}


