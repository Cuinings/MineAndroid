package com.cn.sample.test.wallpaper.viewmodel

import android.R
import android.annotation.SuppressLint
import android.app.WallpaperManager
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.annotation.ColorInt
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.drawable.toDrawable
import androidx.lifecycle.viewModelScope
import androidx.palette.graphics.Palette
import com.cn.library.commom.viewmodel.BasicViewModel
import com.cn.library.commom.viewmodel.UIEvent
import com.cn.library.commom.viewmodel.UIState
import com.cn.sample.test.MainActivity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.log

/**
 * @Author: CuiNing
 * @Time: 2025/8/20 9:51
 * @Description:
 */
sealed class WallpaperState: UIState {
    object Init: WallpaperState()
    data class WallpaperColor(@ColorInt val color: Int): WallpaperState()
}
sealed class WallpaperEvent: UIEvent {
    data class WallpaperUpdate(val context: Context): WallpaperEvent()
}
@HiltViewModel
class WallpaperViewModel @Inject constructor(): BasicViewModel<WallpaperState, WallpaperEvent>() {

    override fun initUIState(): WallpaperState = WallpaperState.Init

    @SuppressLint("UseKtx")
    override fun handleEvent(event: WallpaperEvent) {
        Log.i(TAG, "handleEvent: ${event.javaClass.simpleName}")
        when(event) {
            is WallpaperEvent.WallpaperUpdate -> updateThemeColor(event)
        }
    }

    /**
     * 获取壁纸颜色并更新
     */
    @SuppressLint("MissingPermission")
    private fun updateThemeColor(event: WallpaperEvent.WallpaperUpdate) {
        viewModelScope.launch {
            try {
                // 获取壁纸Drawable（简化实现，实际需用WallpaperManager）
                WallpaperManager.getInstance(event.context).drawable?.toBitmap()?.let { bitmap ->
                    // 使用Palette分析壁纸颜色
                    Palette.from(bitmap).generate { palette ->
                        val dominantColor = palette?.dominantSwatch?.rgb ?: Color.GRAY
                        Log.i(TAG, "handleEvent: $dominantColor")
                        sendUiState { WallpaperState.WallpaperColor(dominantColor) }
                    }
                } ?: Log.e(TAG, "Error processing wallpaper")
            } catch (e: Exception) {
                Log.e(TAG, "Error processing wallpaper", e)
            }
        }
    }
}