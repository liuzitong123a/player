package com.kwunai.rx.player

import android.app.Application
import com.kwunai.neplayer.NEPlayerManager

class VideoApp : Application() {

    override fun onCreate() {
        super.onCreate()
        NEPlayerManager.init(this)
    }
}