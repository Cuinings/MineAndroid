package com.cn.core.ui.view.frosted

import android.annotation.SuppressLint
import android.app.Activity
import android.app.WallpaperManager
import android.content.Context
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.view.View
import android.view.WindowManager
import androidx.core.graphics.withClip
import java.util.WeakHashMap

/**
 * 共享蒙版管理器 — 管理 Activity 级 WallpaperMaskDrawable。
 *
 * 多个 View（WallpaperBlurView / FrostedAnimatedGlowView）共享同一个 mask，
 * refCount 管理生命周期，每个 View 独立贡献镂空区域。
 */
object BlurMaskManager {
    private val masks = WeakHashMap<Activity, MaskEntry>()
    private val lock = Any()

    class MaskEntry(
        val drawable: WallpaperMaskDrawable,
        var refCount: Int = 0,
        var maxBlurRadius: Int = 0,
        var originalBackground: Drawable? = null,
    )

    /**
     * 注册 View 到蒙版。首次注册时创建 WallpaperMaskDrawable 并设到 decorView。
     * @return true 如果是首次创建（调用方需设置 Window blur）
     */
    fun register(activity: Activity, blurRadiusPx: Int): Boolean {
        synchronized(lock) {
            val entry = masks[activity]
            if (entry != null) {
                entry.refCount++
                if (blurRadiusPx > entry.maxBlurRadius) {
                    entry.maxBlurRadius = blurRadiusPx
                    return true  // 需要更新 Window blur 到更大值
                }
                return false
            }
            val drawable = WallpaperMaskDrawable(activity)
            val decor = activity.window.decorView
            masks[activity] = MaskEntry(drawable, 1, blurRadiusPx, decor.background)
            decor.background = drawable
            return true
        }
    }

    /** 注销 View，refCount 归零时移除 mask 并还原 decorView background */
    fun unregister(activity: Activity) {
        synchronized(lock) {
            val entry = masks[activity] ?: return
            entry.refCount--
            if (entry.refCount <= 0) {
                activity.window.decorView.background = entry.originalBackground
                masks.remove(activity)
            }
        }
    }

    /** 更新指定 View 的镂空区域 */
    fun updateHole(view: View, activity: Activity, rect: RectF, radii: FloatArray) {
        synchronized(lock) {
            (masks[activity]?.drawable as? WallpaperMaskDrawable)?.updateHole(view, rect, radii)
        }
    }

    /** 移除指定 View 的镂空 */
    fun removeHole(view: View, activity: Activity) {
        synchronized(lock) {
            (masks[activity]?.drawable as? WallpaperMaskDrawable)?.removeHole(view)
        }
    }

    /** 标记壁纸 drawable 需重新加载（壁纸切换时调用，不碰镂空） */
    fun markWallpaperDirty(activity: Activity) {
        synchronized(lock) {
            (masks[activity]?.drawable as? WallpaperMaskDrawable)?.markWallpaperDirty()
        }
    }

    /** 设置/更新 Window blurBehindRadius */
    @SuppressLint("NewApi")
    fun setWindowBlur(activity: Activity, radius: Int) {
        try {
            val p = activity.window.attributes
            p.blurBehindRadius = radius
            p.flags = p.flags or WindowManager.LayoutParams.FLAG_BLUR_BEHIND
            activity.window.attributes = p
        } catch (_: Exception) { }
    }

    /** 检查 OEM 是否支持跨窗口模糊 */
    @SuppressLint("NewApi")
    fun isCrossWindowBlurEnabled(activity: Activity): Boolean {
        val wm = activity.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        return wm.isCrossWindowBlurEnabled
    }

    /** 检测是否为动态壁纸 */
    fun isLiveWallpaper(context: Context): Boolean {
        return try { WallpaperManager.getInstance(context).wallpaperInfo != null } catch (_: Exception) { false }
    }

    // ==================== WallpaperMaskDrawable ====================
    class WallpaperMaskDrawable(private val context: Context) : Drawable() {
        private val holes = mutableMapOf<View, HoleInfo>()
        private val maskPath = Path()
        private var wallpaperDrawable: Drawable? = null
        private var wallpaperDirty = true
        data class HoleInfo(val rect: RectF, val radii: FloatArray)

        fun updateHole(view: View, rect: RectF, radii: FloatArray) {
            holes[view] = HoleInfo(rect, FloatArray(8) { radii[it] })
            invalidateSelf()
        }
        fun removeHole(view: View) { holes.remove(view); invalidateSelf() }
        fun markWallpaperDirty() { wallpaperDrawable = null; wallpaperDirty = true; invalidateSelf() }

        @SuppressLint("MissingPermission")
        private fun ensureWallpaperDrawable() {
            if (!wallpaperDirty && wallpaperDrawable != null) return
            try { wallpaperDrawable = WallpaperManager.getInstance(context).drawable?.mutate() } catch (_: Exception) { }
            if (wallpaperDrawable != null) wallpaperDirty = false  // 仅在成功时清除 dirty
        }

        override fun draw(canvas: Canvas) {
            ensureWallpaperDrawable()
            val wd = wallpaperDrawable ?: return
            if (wd.bounds.isEmpty) wd.setBounds(0, 0, canvas.width, canvas.height)
            if (holes.isEmpty()) { wd.draw(canvas); return }
            maskPath.reset(); maskPath.fillType = Path.FillType.EVEN_ODD
            maskPath.addRect(0f, 0f, canvas.width.toFloat(), canvas.height.toFloat(), Path.Direction.CW)
            for ((_, info) in holes) maskPath.addRoundRect(info.rect, info.radii, Path.Direction.CW)
            canvas.withClip(maskPath) { wd.draw(this) }
        }

        override fun setAlpha(alpha: Int) { wallpaperDrawable?.alpha = alpha }
        override fun setColorFilter(cf: ColorFilter?) { wallpaperDrawable?.colorFilter = cf }
        override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
        fun recycle() { wallpaperDrawable = null }
    }
}
