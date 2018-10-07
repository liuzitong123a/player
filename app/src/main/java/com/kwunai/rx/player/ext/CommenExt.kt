package com.kwunai.rx.player.ext

import android.view.View
import java.lang.Exception


fun tryCatch(func: () -> Unit) {
    try {
        func()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}


fun View.visible(visible: Boolean) {
    visible.yes { visibility = View.VISIBLE }.otherwise { visibility = View.GONE }
}