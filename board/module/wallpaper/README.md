# Wallpaper (动态壁纸)

Android Live Wallpaper 动态壁纸服务模块。

## 功能

- 实现 `WallpaperService` 子类 `BoardWallpaperService`
- 壁纸设置页面（`WallpaperSettingsActivity`）
- 需要 `BIND_WALLPAPER` 权限
- 使用系统级权限（`SET_WALLPAPER_COMPONENT`、`SET_SURFACE_FRAME_RATE`、`ACCESS_SURFACE_FLINGER`）

## 模块类型

Android Library（`com.cn.board.wallpaper`）

## 技术要点

- 编译目标 Java 11
- 面向定制 Android 系统
