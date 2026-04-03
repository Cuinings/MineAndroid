# Home 模块动态壁纸设置功能

## 功能概述

Home 模块提供了设置指定路径 MP4 文件为动态壁纸的功能。通过简单的 API 调用，可以将本地视频文件设置为系统动态壁纸。

## 核心类

### 1. HomeWallpaperManager
- **路径**: `com/cn/board/meet/home/HomeWallpaperManager.kt`
- **功能**: 管理动态壁纸设置，提供核心功能实现
- **主要方法**:
  - `setVideoWallpaper(videoPath: String)` - 设置视频壁纸
  - `isVideoWallpaperSupported()` - 检查是否支持视频壁纸
  - `validateVideoFile(videoPath: String)` - 验证视频文件
  - `setVideoWallpaperWithResult(videoPath: String, callback: (Boolean, String?) -> Unit)` - 带回调的设置方法
  - `getWallpaperInfo()` - 获取壁纸相关信息

### 2. HomeWallpaperUtil
- **路径**: `com/cn/board/meet/home/HomeWallpaperUtil.kt`
- **功能**: 提供静态方法简化调用，方便其他模块使用
- **主要方法**:
  - `setVideoWallpaper(context: Context, videoPath: String)`
  - `setVideoWallpaperWithCallback(context: Context, videoPath: String, callback: (Boolean, String?) -> Unit)`
  - `isVideoWallpaperSupported(context: Context)`
  - `validateVideoFile(context: Context, videoPath: String)`
  - `getWallpaperInfo(context: Context)`
  - `getSupportedVideoFormats()`

### 3. HomeActivity
- **路径**: `com/cn/board/meet/home/HomeActivity.kt`
- **功能**: 提供 Activity 级别的壁纸设置方法，集成权限管理

## 使用方法

### 方法 1: 通过 HomeActivity 调用

```kotlin
val homeActivity = HomeActivity()
homeActivity.setVideoWallpaper("/storage/emulated/0/Download/video.mp4")
```

### 方法 2: 通过 HomeWallpaperUtil 调用

```kotlin
// 简单调用
HomeWallpaperUtil.setVideoWallpaper(context, "/storage/emulated/0/Download/video.mp4")

// 带回调调用
HomeWallpaperUtil.setVideoWallpaperWithCallback(
    context,
    "/storage/emulated/0/Download/video.mp4"
) { success, message ->
    if (success) {
        Log.d("Wallpaper", "设置成功: $message")
    } else {
        Log.e("Wallpaper", "设置失败: $message")
    }
}
```

### 方法 3: 直接使用 HomeWallpaperManager

```kotlin
val wallpaperManager = HomeWallpaperManager.getInstance(context)
wallpaperManager.setVideoWallpaper("/storage/emulated/0/Download/video.mp4")
```

## 权限要求

- `android.permission.READ_EXTERNAL_STORAGE`
- `android.permission.WRITE_EXTERNAL_STORAGE`
- `android.permission.MANAGE_EXTERNAL_STORAGE` (Android 10+)

## 支持的视频格式

- `.mp4`
- `.3gp`
- `.webm`

## 系统要求

- Android 7.0 (API 24) 及以上
- 支持 Live Wallpaper 功能的系统

## 注意事项

1. **文件路径必须是绝对路径**
2. **文件必须存在且可读取**
3. **需要存储权限**
4. **部分系统可能需要额外设置**
5. **某些定制 ROM 可能不支持动态壁纸**
6. **设置过程会启动系统壁纸选择器**

## 示例代码

### 基本使用

```kotlin
// 检查是否支持视频壁纸
if (HomeWallpaperUtil.isVideoWallpaperSupported(context)) {
    // 验证视频文件
    val videoPath = "/storage/emulated/0/Download/wallpaper.mp4"
    if (HomeWallpaperUtil.validateVideoFile(context, videoPath)) {
        // 设置视频壁纸
        val success = HomeWallpaperUtil.setVideoWallpaper(context, videoPath)
        if (success) {
            Toast.makeText(context, "正在设置动态壁纸...", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "设置失败，请检查文件路径", Toast.LENGTH_SHORT).show()
        }
    } else {
        Toast.makeText(context, "无效的视频文件", Toast.LENGTH_SHORT).show()
    }
} else {
    Toast.makeText(context, "当前设备不支持动态壁纸", Toast.LENGTH_SHORT).show()
}
```

### 带回调使用

```kotlin
val videoPath = "/storage/emulated/0/Movies/wallpaper.mp4"

HomeWallpaperUtil.setVideoWallpaperWithCallback(
    context,
    videoPath
) { success, message ->
    runOnUiThread {
        if (success) {
            Toast.makeText(context, "动态壁纸设置成功", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "设置失败: $message", Toast.LENGTH_SHORT).show()
        }
    }
}
```

### 检查设备支持

```kotlin
val wallpaperInfo = HomeWallpaperUtil.getWallpaperInfo(context)
Log.d("WallpaperInfo", wallpaperInfo)

val isSupported = HomeWallpaperUtil.isVideoWallpaperSupported(context)
Log.d("WallpaperSupport", "Video wallpaper supported: $isSupported")

val formats = HomeWallpaperUtil.getSupportedVideoFormats()
Log.d("WallpaperFormats", "Supported formats: ${formats.joinToString(", ")}")
```

## 实现原理

1. **权限检查**: 自动请求存储权限
2. **文件验证**: 检查文件是否存在、可读、格式正确
3. **系统兼容性检查**: 检查 Android 版本是否支持
4. **Intent 启动**: 启动系统 Live Wallpaper 选择器
5. **文件提供**: 使用 FileProvider 提供文件访问

## 常见问题

### 1. 权限被拒绝
- **原因**: 用户拒绝了存储权限
- **解决**: 引导用户在设置中开启存储权限

### 2. 文件不存在
- **原因**: 路径错误或文件已删除
- **解决**: 检查文件路径是否正确

### 3. 格式不支持
- **原因**: 视频格式不在支持列表中
- **解决**: 使用 .mp4、.3gp 或 .webm 格式

### 4. 系统不支持
- **原因**: Android 版本低于 7.0 或系统不支持 Live Wallpaper
- **解决**: 升级系统或使用其他壁纸方式

### 5. 启动失败
- **原因**: 系统 Live Wallpaper 组件不存在
- **解决**: 检查系统是否完整，可能是定制 ROM 的问题

## 代码示例

### 完整示例

```kotlin
import android.content.Context
import android.widget.Toast
import com.cn.board.meet.home.HomeWallpaperUtil

class WallpaperSetter(private val context: Context) {
    
    fun setVideoWallpaper(videoPath: String) {
        // 检查设备支持
        if (!HomeWallpaperUtil.isVideoWallpaperSupported(context)) {
            showToast("当前设备不支持动态壁纸")
            return
        }
        
        // 验证视频文件
        if (!HomeWallpaperUtil.validateVideoFile(context, videoPath)) {
            showToast("无效的视频文件")
            return
        }
        
        // 设置壁纸
        HomeWallpaperUtil.setVideoWallpaperWithCallback(
            context,
            videoPath
        ) { success, message ->
            if (success) {
                showToast("动态壁纸设置成功")
            } else {
                showToast("设置失败: $message")
            }
        }
    }
    
    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}

// 使用示例
// val setter = WallpaperSetter(context)
// setter.setVideoWallpaper("/storage/emulated/0/Download/video.mp4")
```