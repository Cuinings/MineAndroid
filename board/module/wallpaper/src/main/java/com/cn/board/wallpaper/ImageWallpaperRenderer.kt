package com.cn.board.wallpaper

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.SurfaceHolder
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 图片壁纸渲染器（Canvas 直绘）。
 *
 * 实现 [WallpaperRenderer]，支持三类 Wallpaper Engine 风格的图片壁纸：
 * 1. **静态单图**：一张图居中覆盖（center-crop）绘制。
 * 2. **多图轮播**：`imagePaths.size > 1` 且 `carouselIntervalMs > 0` 时按间隔切换。
 * 3. **桌面视差**：`parallaxEnabled` 且系统提供分页偏移（`xStep > 0`）时，
 *    随桌面左右滑动做水平视差位移。
 *
 * 渲染方式：直接 `SurfaceHolder.lockCanvas()` 绘制到壁纸 Surface（TYPE_WALLPAPER 层），
 * 不走 ImageView/TextureView。所有“解码 + 绘制 + 轮播”都跑在独立 [HandlerThread]，
 * 共享字段用 `@Volatile` 保证可见性，避免阻塞壁纸主线程。
 *
 * 省电策略：未充电且电量低于 [BatteryStateMonitor.LOW_BATTERY_THRESHOLD] 时，轮播间隔放大
 * [BATTERY_SAVE_INTERVAL_MULTIPLIER] 倍；静态图不受影响。
 */
class ImageWallpaperRenderer(
    initialConfig: WallpaperConfig = WallpaperConfig.DEFAULT,
) : WallpaperRenderer {

    companion object {
        private const val TAG = "ImageWallpaperRenderer"
        private const val BATTERY_SAVE_INTERVAL_MULTIPLIER = 4L
    }

    @Volatile private var config: WallpaperConfig = initialConfig

    private val running = AtomicBoolean(false)
    private var isVisible = false

    @Volatile private var surfaceWidth = 0
    @Volatile private var surfaceHeight = 0

    // 视差状态（主线程写，渲染线程读）
    @Volatile private var lastXOffset = 0.5f
    @Volatile private var lastXStep = 0f

    // 电量状态（用于节流）
    private var isCharging = false
    private var batteryLevel = 100

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    // 仅由渲染线程访问
    private val bitmaps = mutableListOf<Bitmap>()
    private var currentIndex = 0

    @Volatile private var currentHolder: SurfaceHolder? = null

    private var renderThread: HandlerThread? = null
    private var renderHandler: Handler? = null
    private val carouselTask = Runnable { onCarouselTick() }

    // ==================== WallpaperRenderer 生命周期 ====================

    override fun attach(holder: SurfaceHolder) {
        currentHolder = holder
        ensureThread()
        val frame = holder.surfaceFrame
        surfaceWidth = frame.width()
        surfaceHeight = frame.height()
        refresh()
    }

    override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        currentHolder = holder
        if (width > 0 && height > 0) {
            surfaceWidth = width
            surfaceHeight = height
        }
        requestDraw()
    }

    override fun onVisibilityChanged(visible: Boolean) {
        isVisible = visible
        if (visible) {
            scheduleCarousel()
            requestDraw()
        } else {
            cancelCarousel()
        }
    }

    override fun onOffsetsChanged(
        xOffset: Float,
        yOffset: Float,
        xStep: Float,
        yStep: Float,
        xPixels: Int,
        yPixels: Int,
    ) {
        lastXOffset = xOffset
        lastXStep = xStep
        requestDraw()
    }

    override fun onConfigChanged(newConfig: WallpaperConfig) {
        config = newConfig
        ensureThread()
        refresh()
    }

    override fun onBatteryStateChanged(charging: Boolean, levelPercent: Int) {
        isCharging = charging
        batteryLevel = levelPercent
        // 电量改善后重新排程，使轮播恢复正常间隔
        scheduleCarousel()
    }

    override fun release() {
        running.set(false)
        cancelCarousel()
        // 让渲染线程把残留 Bitmap 回收后再退出
        renderHandler?.post { recycleBitmaps() }
        val t = renderThread
        renderThread = null
        renderHandler = null
        t?.quitSafely()
    }

    override fun isRunning(): Boolean = running.get()

    // ==================== 线程与绘制调度 ====================

    private fun ensureThread() {
        if (renderThread == null) {
            val t = HandlerThread("WallpaperImageRender").also {
                it.start()
                renderHandler = Handler(it.looper)
                running.set(true)
            }
            renderThread = t
        }
    }

    /** 重新解码并绘制当前配置（投递到渲染线程，保证 bitmaps 仅被渲染线程访问） */
    private fun refresh() {
        renderHandler?.post {
            prepareBitmaps()
            currentIndex = 0
            drawCurrent()
            scheduleCarousel()
        }
    }

    /** 请求一次重绘 */
    private fun requestDraw() {
        if (!running.get()) return
        renderHandler?.post { drawCurrent() }
    }

    private fun scheduleCarousel() {
        cancelCarousel()
        if (!isVisible || !running.get()) return
        if (config.type != WallpaperType.IMAGE) return
        if (config.imagePaths.size <= 1) return
        if (config.carouselIntervalMs <= 0) return
        renderHandler?.postDelayed(carouselTask, effectiveInterval())
    }

    private fun cancelCarousel() {
        renderHandler?.removeCallbacks(carouselTask)
    }

    private fun effectiveInterval(): Long {
        val base = config.carouselIntervalMs
        val lowBattery = !isCharging && batteryLevel <= BatteryStateMonitor.LOW_BATTERY_THRESHOLD
        return if (lowBattery) base * BATTERY_SAVE_INTERVAL_MULTIPLIER else base
    }

    private fun onCarouselTick() {
        if (!isVisible || !running.get()) return
        if (bitmaps.isNotEmpty()) {
            currentIndex = (currentIndex + 1) % bitmaps.size
            requestDraw()
        }
        scheduleCarousel()
    }

    // ==================== 资源准备（仅渲染线程调用） ====================

    private fun prepareBitmaps() {
        recycleBitmaps()
        if (config.type != WallpaperType.IMAGE) return
        for (path in config.imagePaths) {
            if (path.isEmpty()) continue
            val file = File(path)
            if (!file.exists()) {
                Log.w(TAG, "image not found: $path")
                continue
            }
            decodeScaled(file)?.let { bitmaps.add(it) }
        }
        Log.d(TAG, "prepared ${bitmaps.size} bitmap(s)")
    }

    /** 按屏幕尺寸下采样解码，控制内存占用 */
    private fun decodeScaled(file: File): Bitmap? {
        return try {
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, opts)
            val (bw, bh) = opts.outWidth to opts.outHeight
            if (bw <= 0 || bh <= 0) return null
            val targetW = if (surfaceWidth > 0) surfaceWidth else bw
            val targetH = if (surfaceHeight > 0) surfaceHeight else bh
            opts.inSampleSize = computeSampleSize(bw, bh, targetW, targetH)
            opts.inJustDecodeBounds = false
            opts.inPreferredConfig = Bitmap.Config.RGB_565 // 壁纸无需 alpha，省一半内存
            BitmapFactory.decodeFile(file.absolutePath, opts)
        } catch (e: Exception) {
            Log.e(TAG, "decode failed: $file", e)
            null
        }
    }

    private fun computeSampleSize(srcW: Int, srcH: Int, targetW: Int, targetH: Int): Int {
        var sample = 1
        while (srcW / (sample + 1) >= targetW && srcH / (sample + 1) >= targetH) {
            sample += 1
        }
        return sample
    }

    private fun recycleBitmaps() {
        bitmaps.forEach { if (!it.isRecycled) it.recycle() }
        bitmaps.clear()
        currentIndex = 0
    }

    // ==================== 绘制（仅渲染线程调用） ====================

    private fun drawCurrent() {
        if (bitmaps.isEmpty() || surfaceWidth <= 0 || surfaceHeight <= 0) return
        val holder = currentHolder ?: return
        val canvas = try {
            holder.lockCanvas() ?: return
        } catch (e: Exception) {
            Log.w(TAG, "lockCanvas failed: $e")
            return
        }
        try {
            canvas.drawColor(Color.BLACK)
            val bmp = bitmaps.getOrNull(currentIndex) ?: return
            drawCenterCropParallax(canvas, bmp)
        } finally {
            try { holder.unlockCanvasAndPost(canvas) } catch (_: Exception) {}
        }
    }

    private fun drawCenterCropParallax(canvas: android.graphics.Canvas, bmp: Bitmap) {
        val cw = surfaceWidth.toFloat()
        val ch = surfaceHeight.toFloat()
        val bw = bmp.width.toFloat()
        val bh = bmp.height.toFloat()
        val scale = if (cw / bw > ch / bh) cw / bw else ch / bh
        val dw = bw * scale
        val dh = bh * scale
        var left = (cw - dw) / 2f
        val top = (ch - dh) / 2f

        // 视差：仅在有多桌面分页时位移
        val overflowX = dw - cw
        if (config.parallaxEnabled && overflowX > 0f && lastXStep > 0f) {
            val shift = (lastXOffset - 0.5f) * overflowX
            left -= shift
        }
        canvas.drawBitmap(bmp, null, RectF(left, top, left + dw, top + dh), paint)
    }
}
