package com.cn.board.wallpaper

data class WallpaperConfig(
    val videoPath: String? = DEFAULT_VIDEO_PATH,
) {
    companion object {
        val DEFAULT = WallpaperConfig()
        const val DEFAULT_VIDEO_PATH = "/storage/emulated/0/Download/wallpaper_dy_1.mp4"
    }
}
