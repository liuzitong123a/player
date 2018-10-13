package com.kwunai.player.modal


object CauseCode {

    /**
     * 视频解析出错
     */
    const val CODE_VIDEO_PARSER_ERROR = -10001

    /**
     * 视频被用户手动暂停
     */
    const val CODE_VIDEO_PAUSED_BY_MANUAL = -10101

    /**
     * 视频被用户手动停止(销毁)
     */
    const val CODE_VIDEO_STOPPED_BY_MANUAL = -10102

    /**
     * 系统网络断开导致视频停止播放(不销毁仅重置播放器)
     */
    const val CODE_VIDEO_STOPPED_AS_NET_UNAVAILABLE = -10103

    /**
     * 视频因为播放完成而停止
     */
    const val CODE_VIDEO_STOPPED_AS_ON_COMPLETION = -10104

    /**
     * 视频因为到后台而暂停
     */
    const val CODE_VIDEO_PAUSED_BY_BACKGROUND = -10105
}