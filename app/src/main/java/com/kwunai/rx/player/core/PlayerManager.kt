package com.kwunai.rx.player.core

import android.content.Context
import com.netease.neliveplayer.sdk.NEDynamicLoadingConfig
import com.netease.neliveplayer.sdk.NELivePlayer
import com.netease.neliveplayer.sdk.NESDKConfig


object PlayerManager {

    /**
     * 初始化SDK,使用播放器时必须先进行初始化才能进行后续操作。
     */
    fun init(context: Context) {
        val sdkConfig = NESDKConfig()
        sdkConfig.refreshPreLoadDuration = 30 * 60 * 1000
        sdkConfig.isCloseTimeOutProtect = false
        val config = NEDynamicLoadingConfig()
        config.isArmeabiv7a = true
        sdkConfig.dynamicLoadingConfig = config
        sdkConfig.isCloseTimeOutProtect = false
        NELivePlayer.init(context, sdkConfig)
    }


    /**
     * 构造播放器实例对象
     *
     * @param context   上下文
     * @return 播放器实例对象
     */
    fun configPlayer(context: Context): PlayerDispatcherImpl {
        return PlayerDispatcherImpl(context)
    }

    fun configPlayerTest(context: Context): PlayerStrategy {
        return NEPlayerStrategy(context)
    }
}