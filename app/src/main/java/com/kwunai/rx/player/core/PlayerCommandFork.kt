package com.kwunai.rx.player.core

/**
 * 播放视频触发的一系列行为
 */
sealed class PlayerCommandFork {

    object Preparing : PlayerCommandFork()

    object Prepared : PlayerCommandFork()

    object BufferingStart : PlayerCommandFork()

    object BufferingEnd : PlayerCommandFork()

    object Completion : PlayerCommandFork()

    object NetStateBad : PlayerCommandFork()

    class Error(val code: Int, val extra: Int) : PlayerCommandFork()

    class CurrentProgress(val currentPosition: Long,
                          val duration: Long,
                          val percent: Float,
                          val cachedPosition: Long) : PlayerCommandFork()
}