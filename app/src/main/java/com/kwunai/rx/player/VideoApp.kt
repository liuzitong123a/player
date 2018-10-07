package com.kwunai.rx.player

import android.app.Application
import com.kwunai.rx.player.core.PlayerManager
import com.orhanobut.logger.PrettyFormatStrategy
import com.orhanobut.logger.AndroidLogAdapter
import com.orhanobut.logger.Logger


class VideoApp : Application() {

    override fun onCreate() {
        super.onCreate()
        PlayerManager.init(this)
        val formatStrategy = PrettyFormatStrategy.newBuilder()
                .showThreadInfo(false)
                .methodCount(0)
                .methodOffset(7)
                .tag("lzt")
                .build()
        Logger.addLogAdapter(object : AndroidLogAdapter(formatStrategy) {
            override fun isLoggable(priority: Int, tag: String?): Boolean {
                return BuildConfig.DEBUG
            }
        })
    }
}