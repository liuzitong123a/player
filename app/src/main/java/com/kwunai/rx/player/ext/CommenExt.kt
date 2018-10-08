package com.kwunai.rx.player.ext

import android.view.View

fun View.visible(visible: Boolean) {
    visible.yes { visibility = View.VISIBLE }.otherwise { visibility = View.GONE }
}