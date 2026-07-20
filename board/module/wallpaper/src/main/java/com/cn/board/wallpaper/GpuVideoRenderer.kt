package com.cn.board.wallpaper

import android.content.Context
import android.util.Log
import android.view.SurfaceHolder

/**
 * [GpuVideoWallpaperView] (SurfaceTexture + MediaPlayer) 生命周期适配器。
 * 融入 BoardWallpaperService 引擎。
 *
 * GPU 路径零 CPU 拷贝：MediaPlayer 直接输出到 SurfaceTexture，
 * GL 线程通过 GL_TEXTURE_EXTERNAL_OES 采样渲染。
 */
class GpuVideoRenderer(
    private val ctx: Context,
    initialConfig: WallpaperConfig,
) {

    private companion object {
        private const val TAG = "GpuVideoRenderer"
    }

    @Volatile private var config = initialConfig
    @Volatile private var view: GpuVideoWallpaperView? = null
    private var attached = false
    private var visible = false
    private var loadedPath: String? = null

    fun attach(holder: SurfaceHolder) {
        if (attached) return
        val v = GpuVideoWallpaperView.create(ctx, holder)
        view = v
        attached = true
        v.setWallpaperSurfaceAvailable(true)
        if (visible) v.setPlaybackEnabled(true)
        val path = config.videoPath
        if (!path.isNullOrEmpty()) {
            v.loadVideo(path)
            loadedPath = path
        }
    }

    fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        // GpuVideoWallpaperView 通过 GLSurfaceView.onSurfaceChanged 自行处理
    }

    fun onVisibilityChanged(v: Boolean) {
        visible = v
        view?.setPlaybackEnabled(v)
    }

    fun onConfigChanged(newConfig: WallpaperConfig) {
        config = newConfig
        val path = newConfig.videoPath
        if (!path.isNullOrEmpty() && path != loadedPath) {
            view?.loadVideo(path)
            loadedPath = path
        }
    }

    fun release() {
        val v = view
        view = null
        attached = false
        visible = false
        loadedPath = null
        v?.setPlaybackEnabled(false)
        v?.destroy()
    }

    fun isRunning(): Boolean = view?.isPlayerCreated() == true
}
