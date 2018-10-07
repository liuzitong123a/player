package com.kwunai.rx.player.core

import com.kwunai.rx.player.modal.MediaInfo
import com.kwunai.rx.player.modal.StateInfo

/**
 * 播放视频触发的一系列行为
 */
sealed class PlayerCommand {

    object Preparing : PlayerCommand()

    class Prepared(val mediaInfo: MediaInfo) : PlayerCommand()

    object BufferingStart : PlayerCommand()

    object BufferingEnd : PlayerCommand()

    object Completion : PlayerCommand()

    object NetStateBad : PlayerCommand()

    class Error(val code: Int, val extra: Int) : PlayerCommand()

    class StateChanged(val stateInfo: StateInfo) : PlayerCommand()

    class CurrentProgress(val currentPosition: Long,
                          val duration: Long,
                          val percent: Float,
                          val cachedPosition: Long) : PlayerCommand()
}