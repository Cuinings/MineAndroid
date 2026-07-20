package com.cn.board.wallpaper

enum class WallpaperType {
    GPU_VIDEO,
    ;

    companion object {
        fun fromName(name: String?): WallpaperType = GPU_VIDEO
    }
}
