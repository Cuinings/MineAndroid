package com.cn.board.wallpaper

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.util.Log
import android.view.SurfaceHolder
import java.io.File

class BoardWallpaperService : WallpaperService() {

    companion object {
        private const val TAG = "BoardWallpaperService"
        var wallpaperPath: String? = "/storage/emulated/0/Download/wallpaper_dy_1.mp4"
            set(value) { value.takeIf { it != field }?.let {
                field = it
                // 通知所有活动的引擎更新壁纸
                activeEngines.apply {
                    Log.d(TAG, "activeEngines size: ${this.size}")
                }.forEach { it.updateWallpaperPath(field) }
            } }
        private val activeEngines = mutableListOf<BoardWallpaperEngine>()
    }

    override fun onCreateEngine(): Engine {
        return BoardWallpaperEngine()
    }

    private inner class BoardWallpaperEngine : Engine() {

        init {
            // 将引擎实例添加到活动列表
            activeEngines.add(this)
        }

        private val handler = Handler(Looper.getMainLooper())
        private var isVisible = false
        private var currentDrawable: Drawable? = null
        private var mediaPlayer: MediaPlayer? = null

        // 模糊配置
        private val blurRadius = 25f // 模糊半径

        private val drawRunnable = Runnable {
            drawFrame()
        }
        
        private val blurRunnable = Runnable {
            drawBlur()
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            Log.d(TAG, "onSurfaceCreated")
            
            loadWallpaper()
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            Log.d(TAG, "onSurfaceChanged: width=$width, height=$height")
            
            if (mediaPlayer != null) {
                // 视频壁纸需要重新设置surface
                mediaPlayer?.setSurface(holder.surface)
            } else {
                drawFrame()
            }
        }

        override fun onVisibilityChanged(visible: Boolean) {
            isVisible = visible
            if (visible) {
                if (mediaPlayer != null) {
                    mediaPlayer?.start()
                    handler.post(blurRunnable)
                } else {
                    handler.post(drawRunnable)
                }
            } else {
                if (mediaPlayer != null) {
                    mediaPlayer?.pause()
                    handler.removeCallbacks(blurRunnable)
                } else {
                    handler.removeCallbacks(drawRunnable)
                }
            }
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            isVisible = false
            handler.removeCallbacks(drawRunnable)
            handler.removeCallbacks(blurRunnable)
            releaseMediaPlayer()
            // 从活动列表中移除引擎实例
            activeEngines.remove(this)
        }

        private fun loadWallpaper() {
            val path = wallpaperPath
            if (path != null && File(path).exists()) {
                try {
                    // 释放之前的资源
                    releaseMediaPlayer()
                    currentDrawable = null

                    // 检查文件类型
                    if (isVideoFile(path)) {
                        // 加载视频
                        loadVideoWallpaper(path)
                    } else {
                        // 加载图片
                        val drawable = Drawable.createFromPath(path)
                        currentDrawable = drawable
                        Log.d(TAG, "Image wallpaper loaded from: $path")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to load wallpaper: $e")
                    currentDrawable = null
                    releaseMediaPlayer()
                }
            } else {
                Log.w(TAG, "Wallpaper path not set or file does not exist")
                currentDrawable = null
                releaseMediaPlayer()
            }
        }

        private fun isVideoFile(path: String): Boolean {
            val videoExtensions = listOf(".mp4", ".3gp", ".mov", ".avi", ".wmv", ".mkv")
            return videoExtensions.any { path.lowercase().endsWith(it) }
        }

        private fun loadVideoWallpaper(path: String) {
            try {
                mediaPlayer = MediaPlayer()
                mediaPlayer?.setDataSource(path)
                mediaPlayer?.setSurface(surfaceHolder.surface)
                mediaPlayer?.isLooping = true
                mediaPlayer?.prepare()
                if (isVisible) {
                    mediaPlayer?.start()
                }
                Log.d(TAG, "Video wallpaper loaded from: $path")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load video wallpaper: $e")
                // 检查是否是权限错误
                if (e.message?.contains("Permission denied") == true) {
                    Log.e(TAG, "Storage permission is required to load video wallpaper")
                }
                releaseMediaPlayer()
            }
        }

        private fun releaseMediaPlayer() {
            mediaPlayer?.let {
                try {
                    it.stop()
                    it.release()
                } catch (e: Exception) {
                    Log.e(TAG, "Error releasing media player: $e")
                }
                mediaPlayer = null
            }
        }

        private fun drawFrame() {
            // 双重检查：确保holder和surface都有效
            val holder = surfaceHolder ?: return
            if (!isVisible) {
                return
            }
            
            var canvas: Canvas? = null

            try {
                // 检查Surface是否有效
                val surface = holder.surface
                if (surface == null || !surface.isValid) {
                    Log.w(TAG, "Surface is not valid, skipping draw")
                    return
                }
                
                // 尝试锁定画布
                try {
                    canvas = holder.lockCanvas()
                } catch (e: IllegalArgumentException) {
                    Log.e(TAG, "Error locking canvas: $e")
                    // Surface可能已经被销毁，跳过本次绘制
                    return
                }
                
                canvas?.let {
                    try {
                        // 清空画布
                        canvas.drawColor(0xFF000000.toInt())

                        // 绘制壁纸
                        currentDrawable?.let {drawable ->
                            val bounds = holder.surfaceFrame
                            drawable.setBounds(0, 0, bounds.width().toInt(), bounds.height().toInt())
                            drawable.draw(canvas)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error drawing on canvas: $e")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in drawFrame: $e")
            } finally {
                if (canvas != null) {
                    try {
                        holder.unlockCanvasAndPost(canvas)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error unlocking canvas: $e")
                    }
                }
            }

            // 继续绘制下一帧
            if (isVisible && mediaPlayer == null) {
                // 延迟一段时间再绘制下一帧，避免过于频繁的绘制
                handler.postDelayed(drawRunnable, 32) // 约30fps，降低绘制频率
            }
        }

        private fun drawBlur() {
            // 双重检查：确保holder和surface都有效
            val holder = surfaceHolder ?: return
            if (!isVisible) {
                return
            }
            
            var canvas: Canvas? = null

            try {
                // 检查Surface是否有效
                val surface = holder.surface
                if (surface == null || !surface.isValid) {
                    Log.w(TAG, "Surface is not valid, skipping draw")
                    return
                }
                
                // 尝试锁定画布
                try {
                    canvas = holder.lockCanvas()
                } catch (e: IllegalArgumentException) {
                    Log.e(TAG, "Error locking canvas: $e")
                    // Surface可能已经被销毁，跳过本次绘制
                    return
                }
                
                canvas?.let {
                    try {
                        // 绘制半透明黑色覆盖层作为模糊效果的基础
                        val paint = Paint()
                        paint.isAntiAlias = true
                        paint.style = Paint.Style.FILL
                        paint.alpha = 180 // 设置透明度，使模糊效果更自然
                        canvas.drawRect(0f, 0f, canvas.width.toFloat(), canvas.height.toFloat(), paint)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error drawing on canvas: $e")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in drawBlur: $e")
            } finally {
                if (canvas != null) {
                    try {
                        holder.unlockCanvasAndPost(canvas)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error unlocking canvas: $e")
                    }
                }
            }

            // 继续绘制下一帧
            if (isVisible && mediaPlayer != null) {
                // 延迟一段时间再绘制下一帧，避免过于频繁的绘制
                handler.postDelayed(blurRunnable, 32) // 约30fps，降低绘制频率
            }
        }





        // 当壁纸路径改变时调用
        fun updateWallpaperPath(path: String?) {
            loadWallpaper()
            if (isVisible && mediaPlayer == null) {
                handler.post(drawRunnable)
            } else if (isVisible && mediaPlayer != null) {
                handler.post(blurRunnable)
            }
        }
    }
}
