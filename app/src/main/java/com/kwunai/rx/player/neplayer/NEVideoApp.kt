package com.kwunai.rx.player.neplayer

import android.app.Application
import com.kwunai.neplayer.NEPlayerManager

class NEVideoApp : Application() {

    override fun onCreate() {
        super.onCreate()
        NEPlayerManager.init(this)
    }
}