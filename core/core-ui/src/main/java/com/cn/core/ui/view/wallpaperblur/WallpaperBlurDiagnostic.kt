package com.cn.core.ui.view.wallpaperblur

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.util.Log
import android.widget.Toast

object WallpaperBlurDiagnostic {
    
    private const val TAG = "WallpaperBlurDiagnostic"
    
    fun runDiagnostic(context: Context): DiagnosticResult {
        val results = mutableListOf<DiagnosticItem>()
        
        results.add(checkWallpaperManager(context))
        results.add(checkWallpaperCapture(context))
        results.add(checkRenderScript(context))
        results.add(checkViewDimensions(context))
        results.add(checkPermissions(context))
        
        return DiagnosticResult(results)
    }
    
    private fun checkWallpaperManager(context: Context): DiagnosticItem {
        return try {
            val wallpaperManager = WallpaperManager.getInstance(context)
            if (wallpaperManager != null) {
                DiagnosticItem(
                    name = "WallpaperManager 初始化",
                    success = true,
                    message = "WallpaperManager 初始化成功"
                )
            } else {
                DiagnosticItem(
                    name = "WallpaperManager 初始化",
                    success = false,
                    message = "WallpaperManager 为 null"
                )
            }
        } catch (e: Exception) {
            DiagnosticItem(
                name = "WallpaperManager 初始化",
                success = false,
                message = "初始化失败: ${e.message}"
            )
        }
    }
    
    private fun checkWallpaperCapture(context: Context): DiagnosticItem {
        return try {
            val wallpaperManager = WallpaperManager.getInstance(context)
            
            val wallpaperDrawable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                wallpaperManager.drawable
            } else {
                @Suppress("DEPRECATION")
                wallpaperManager.fastDrawable
            }
            
            if (wallpaperDrawable == null) {
                return DiagnosticItem(
                    name = "壁纸捕获",
                    success = false,
                    message = "无法获取壁纸 Drawable"
                )
            }
            
            val bitmap = when (wallpaperDrawable) {
                is BitmapDrawable -> {
                    wallpaperDrawable.bitmap?.copy(Bitmap.Config.ARGB_8888, true)
                }
                else -> {
                    val width = wallpaperDrawable.intrinsicWidth
                    val height = wallpaperDrawable.intrinsicHeight
                    
                    if (width <= 0 || height <= 0) {
                        return DiagnosticItem(
                            name = "壁纸捕获",
                            success = false,
                            message = "壁纸尺寸无效: ${width}x${height}"
                        )
                    }
                    
                    val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bmp)
                    wallpaperDrawable.setBounds(0, 0, width, height)
                    wallpaperDrawable.draw(canvas)
                    bmp
                }
            }
            
            if (bitmap != null && !bitmap.isRecycled) {
                val result = DiagnosticItem(
                    name = "壁纸捕获",
                    success = true,
                    message = "壁纸捕获成功，尺寸: ${bitmap.width}x${bitmap.height}"
                )
                bitmap.recycle()
                result
            } else {
                DiagnosticItem(
                    name = "壁纸捕获",
                    success = false,
                    message = "壁纸 Bitmap 为 null 或已回收"
                )
            }
        } catch (e: Exception) {
            DiagnosticItem(
                name = "壁纸捕获",
                success = false,
                message = "壁纸捕获失败: ${e.message}"
            )
        }
    }
    
    private fun checkRenderScript(context: Context): DiagnosticItem {
        return try {
            var renderScript: android.renderscript.RenderScript? = null
            var blurScript: android.renderscript.ScriptIntrinsicBlur? = null
            
            try {
                renderScript = android.renderscript.RenderScript.create(context)
                val element = android.renderscript.Element.U8_4(renderScript)
                blurScript = android.renderscript.ScriptIntrinsicBlur.create(renderScript, element)
                
                DiagnosticItem(
                    name = "RenderScript 初始化",
                    success = true,
                    message = "RenderScript 初始化成功"
                )
            } finally {
                blurScript?.destroy()
                renderScript?.destroy()
            }
        } catch (e: Exception) {
            DiagnosticItem(
                name = "RenderScript 初始化",
                success = false,
                message = "RenderScript 初始化失败: ${e.message}"
            )
        }
    }
    
    private fun checkViewDimensions(context: Context): DiagnosticItem {
        val displayMetrics = context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels
        
        return DiagnosticItem(
            name = "屏幕尺寸",
            success = true,
            message = "屏幕尺寸: ${screenWidth}x${screenHeight}, 密度: ${displayMetrics.density}"
        )
    }
    
    private fun checkPermissions(context: Context): DiagnosticItem {
        val warnings = mutableListOf<String>()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!android.provider.Settings.canDrawOverlays(context)) {
                warnings.add("缺少悬浮窗权限")
            }
        }
        
        return if (warnings.isEmpty()) {
            DiagnosticItem(
                name = "权限检查",
                success = true,
                message = "所有必要权限已授予"
            )
        } else {
            DiagnosticItem(
                name = "权限检查",
                success = true,
                message = "警告: ${warnings.joinToString(", ")}"
            )
        }
    }
    
    fun logDiagnosticResult(result: DiagnosticResult) {
        Log.d(TAG, "========== 壁纸模糊诊断结果 ==========")
        result.items.forEach { item ->
            val status = if (item.success) "✓" else "✗"
            Log.d(TAG, "$status ${item.name}: ${item.message}")
        }
        Log.d(TAG, "=====================================")
    }
    
    fun showDiagnosticResult(context: Context, result: DiagnosticResult) {
        val message = buildString {
            append("诊断结果:\n\n")
            result.items.forEach { item ->
                val status = if (item.success) "✓" else "✗"
                append("$status ${item.name}\n")
                append("  ${item.message}\n\n")
            }
        }
        
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }
    
    data class DiagnosticItem(
        val name: String,
        val success: Boolean,
        val message: String
    )
    
    data class DiagnosticResult(
        val items: List<DiagnosticItem>
    ) {
        val allSuccess: Boolean
            get() = items.all { it.success }
        
        val hasErrors: Boolean
            get() = items.any { !it.success }
    }
}
