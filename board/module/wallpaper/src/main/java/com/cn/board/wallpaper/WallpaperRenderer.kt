package com.cn.board.wallpaper

import android.view.SurfaceHolder

/**
 * 壁纸渲染器统一接口。
 *
 * 这是“类 Wallpaper Engine”架构的核心抽象：每一种壁纸类型（视频 / 图片 / 未来的 Web / 3D 场景）
 * 都实现本接口，由 [BoardWallpaperService.BoardWallpaperEngine] 统一驱动生命周期。
 *
 * 生命周期与调用顺序（由 Engine 转发）：
 * ```
 * attach(holder)                         // 拿到壁纸 Surface，准备资源
 *   └─ onSurfaceChanged(...)             // 尺寸/格式变化（可选）
 *   └─ onVisibilityChanged(true)         // 桌面可见 → 开始渲染
 *   └─ onOffsetsChanged(...)             // 桌面分页/视差（可选，图片壁纸用）
 *   └─ onConfigChanged(...)              // 运行时切换壁纸/参数（可选）
 *   └─ onBatteryStateChanged(...)        // 电量/充电状态（可选，用于节流）
 *   └─ onVisibilityChanged(false)        // 桌面隐藏 → 暂停（省电）
 * release()                              // Surface 销毁，释放全部资源
 * ```
 *
 * 约定：
 * - `attach` 与 `onVisibilityChanged(true)` 的先后顺序不固定，实现需对“surface 已就绪但不可见”
 *   或“可见但 surface 未就绪”两种情况都做幂等处理。
 * - 所有实现必须保证 `release()` 可重入且线程安全。
 */
interface WallpaperRenderer {

    /** 绑定到壁纸 Surface。此时 [SurfaceHolder.getSurface] 已有效。 */
    fun attach(holder: SurfaceHolder)

    /** Surface 尺寸或像素格式变化。 */
    fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int)

    /** 可见性变化：true = 桌面可见，应开始/恢复渲染；false = 隐藏，应暂停。 */
    fun onVisibilityChanged(visible: Boolean)

    /**
     * 桌面分页偏移（用于视差壁纸）。默认空实现。
     * @param xOffset 横向偏移归一化值 [0,1)，多桌面时为当前桌面在整排中的位置
     * @param yOffset 纵向偏移归一化值 [0,1)
     * @param xStep   相邻桌面间 xOffset 步进（0 表示无分页视差）
     * @param yStep   相邻桌面间 yOffset 步进
     * @param xPixels 前景（桌面图标层）已平移的像素数
     * @param yPixels 前景已平移的像素数
     */
    fun onOffsetsChanged(
        xOffset: Float,
        yOffset: Float,
        xStep: Float,
        yStep: Float,
        xPixels: Int,
        yPixels: Int,
    ) {
        // 默认不处理
    }

    /** 运行时配置变化（切换壁纸/改参数）。实现需平滑切换或重建资源。 */
    fun onConfigChanged(config: WallpaperConfig)

    /** 电量/充电状态变化（用于节流省电）。默认空实现。 */
    fun onBatteryStateChanged(isCharging: Boolean, levelPercent: Int) {
        // 默认不处理
    }

    /** 释放全部资源。可重入。 */
    fun release()

    /** 当前是否处于渲染中（用于调试/状态查询）。 */
    fun isRunning(): Boolean
}
