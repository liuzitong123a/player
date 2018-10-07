package com.kwunai.rx.player.modal


enum class VideoScaleMode {
    // 按比例拉伸，有一边会贴黑边
    FIT,
    // 原始大小
    NONE,
    // 全屏，画面可能会变形
    FILL,
    // 按比例拉伸至全屏，有一边会被裁剪
    FULL,
}