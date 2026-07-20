package com.cn.board.wallpaper

/**
 * 壁纸类型。对应 Wallpaper Engine 的“场景类别”抽象：
 * - [VIDEO]：视频壁纸（MediaCodec 直渲到 TYPE_WALLPAPER 层）
 * - [IMAGE]：图片/多图轮播/视差壁纸（Canvas 直绘）
 *
 * 后续扩展 Web/3D 场景时，只需在此枚举增加一项并实现对应 [WallpaperRenderer]。
 */
enum class WallpaperType {
    VIDEO,
    IMAGE,
    ;

    companion object {
        fun fromName(name: String?): WallpaperType = when (name) {
            "IMAGE" -> IMAGE
            else -> VIDEO
        }
    }
}

/**
 * 壁纸配置模型。由 [WallpaperConfigStore] 持久化并在跨进程间传递。
 *
 * @param type 渲染类型，决定使用哪个 [WallpaperRenderer]
 * @param videoPath 视频壁纸路径（[WallpaperType.VIDEO] 使用，可为 null）
 * @param imagePaths 图片壁纸路径列表（[WallpaperType.IMAGE] 使用，>=1 张时轮播）
 * @param carouselIntervalMs 图片轮播间隔（毫秒），<=0 表示不轮播（仅显示第一张）
 * @param parallaxEnabled 是否启用桌面分页视差
 * @param fpsCap 全局帧率上限（0 表示不限制）。视频渲染器忽略此项（由解码器时序决定）
 */
data class WallpaperConfig(
    val type: WallpaperType,
    val videoPath: String? = null,
    val imagePaths: List<String> = emptyList(),
    val carouselIntervalMs: Long = 5000L,
    val parallaxEnabled: Boolean = true,
    val fpsCap: Int = 0,
) {
    companion object {
        /** 视频壁纸便捷构造（保持与原 BoardWallpaperService 默认路径一致） */
        fun video(path: String?) = WallpaperConfig(
            type = WallpaperType.VIDEO,
            videoPath = path,
        )

        /** 图片壁纸便捷构造 */
        fun image(
            paths: List<String>,
            carouselIntervalMs: Long = 5000L,
            parallaxEnabled: Boolean = true,
        ) = WallpaperConfig(
            type = WallpaperType.IMAGE,
            imagePaths = paths,
            carouselIntervalMs = carouselIntervalMs,
            parallaxEnabled = parallaxEnabled,
        )

        /** 默认配置：沿用原视频壁纸默认路径，保证向后兼容 */
        val DEFAULT = video(DEFAULT_VIDEO_PATH)

        const val DEFAULT_VIDEO_PATH = "/skyconfig/skyui/Pictures/wallpaper_dy_1.mp4"
    }
}
