package com.kwunai.rx.player.modal

import com.netease.neliveplayer.sdk.NEMediaInfo


data class MediaInfo(val mInner: NEMediaInfo, val mDuration: Long)