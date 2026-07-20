package com.cn.board.wallpaper

import android.annotation.SuppressLint
import android.content.Context
import android.content.IntentFilter
import android.os.Build
import android.service.wallpaper.WallpaperService
import android.util.Log
import android.view.SurfaceHolder

class BoardWallpaperService : WallpaperService() {

    companion object { private const val TAG = "BoardWallpaperService" }

    override fun onCreateEngine(): Engine = BoardWallpaperEngine()

    private inner class BoardWallpaperEngine : Engine(), WallpaperConfigStore.Listener {

        private var renderer: GpuVideoRenderer? = null
        private var currentConfig: WallpaperConfig = WallpaperConfig.DEFAULT
        private var currentHolder: SurfaceHolder? = null
        private var isVisibleNow = false
        private val configReceiver = WallpaperConfigStore.createReceiver()

        override fun onCreate(holder: SurfaceHolder) {
            super.onCreate(holder)
            setTouchEventsEnabled(false)
            val ctx = this@BoardWallpaperService
            currentConfig = WallpaperConfigStore.load(ctx)
            renderer = GpuVideoRenderer(ctx, currentConfig)
            WallpaperConfigStore.subscribe(this)
            registerConfigReceiver(ctx)
            Log.i(TAG, "engine created: videoPath=${currentConfig.videoPath}")
        }

        override fun onConfigChanged(config: WallpaperConfig) {
            currentConfig = config
            renderer?.onConfigChanged(config)
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder); currentHolder = holder; renderer?.attach(holder)
        }
        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height); currentHolder = holder
        }
        override fun onVisibilityChanged(visible: Boolean) { isVisibleNow = visible; renderer?.onVisibilityChanged(visible) }
        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder); isVisibleNow = false; renderer?.release(); currentHolder = null
        }
        override fun onDestroy() { WallpaperConfigStore.unsubscribe(this); unregisterConfigReceiver(); renderer?.release(); renderer = null; super.onDestroy() }

        @SuppressLint("WrongConstant")
        private fun registerConfigReceiver(ctx: Context) {
            val filter = IntentFilter(WallpaperConfigStore.ACTION_CONFIG_CHANGED)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ctx.registerReceiver(configReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                @Suppress("UnspecifiedRegisterReceiverFlag")
                ctx.registerReceiver(configReceiver, filter)
            }
        }
        private fun unregisterConfigReceiver() { try { unregisterReceiver(configReceiver) } catch (_: Exception) {} }
    }
}
