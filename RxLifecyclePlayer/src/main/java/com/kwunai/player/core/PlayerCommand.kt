package com.kwunai.player.core

import com.kwunai.player.modal.StateInfo

/**
 * 播放视频触发的一系列行为
 */
sealed class PlayerCommand {

    object Preparing : PlayerCommand()

    object Prepared : PlayerCommand()

    object BufferingStart : PlayerCommand()

    object BufferingEnd : PlayerCommand()

    object Completion : PlayerCommand()

    object NetStateBad : PlayerCommand()

    object MobileNet : PlayerCommand()

    object WifiNet : PlayerCommand()

    class Error(val code: Int, val extra: Int) : PlayerCommand()

    class StateChanged(val stateInfo: StateInfo) : PlayerCommand()

    class CurrentProgress(val currentPosition: Long,
                          val duration: Long,
                          val percent: Float,
                          val cachedPosition: Long) : PlayerCommand()
}