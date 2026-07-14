package com.cn.board.meet.home.app

import android.annotation.SuppressLint
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.NameNotFoundException
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PixelFormat
import android.util.Log
import androidx.core.graphics.createBitmap
import com.cn.board.database.AppInfo
import com.cn.board.database.EmAppType
import com.cn.board.meet.home.app.ActivityClassName.APP_CHROME
import com.cn.board.meet.home.app.ActivityClassName.APP_CONTACT
import com.cn.board.meet.home.app.ActivityClassName.APP_CREATE_CONF
import com.cn.board.meet.home.app.ActivityClassName.APP_DIAGNOSIS
import com.cn.board.meet.home.app.ActivityClassName.APP_FILE_EXPLORER
import com.cn.board.meet.home.app.ActivityClassName.APP_FOR_SCREEN
import com.cn.board.meet.home.app.ActivityClassName.APP_HELP
import com.cn.board.meet.home.app.ActivityClassName.APP_JOIN_CONF
import com.cn.board.meet.home.app.ActivityClassName.APP_SETTING
import com.cn.board.meet.home.app.ActivityClassName.APP_TOUCH_DATA
import com.cn.board.meet.home.app.ActivityClassName.APP_VOD
import com.cn.board.meet.home.app.ActivityClassName.APP_WELCOME_SIGN
import com.cn.board.meet.home.app.ActivityClassName.APP_WPS_HOME
import com.cn.board.meet.home.app.PackageName.PACKAGE_AIR_PLAY
import com.cn.board.meet.home.app.PackageName.PACKAGE_CAST
import com.cn.board.meet.home.app.PackageName.PACKAGE_CONFERENCE
import com.cn.board.meet.home.app.PackageName.PACKAGE_DIAGNOSIS
import com.cn.board.meet.home.app.PackageName.PACKAGE_EXPLORER
import com.cn.board.meet.home.app.PackageName.PACKAGE_FILE_EXPLORER
import com.cn.board.meet.home.app.PackageName.PACKAGE_HELP
import com.cn.board.meet.home.app.PackageName.PACKAGE_IMIXAPPCORE
import com.cn.board.meet.home.app.PackageName.PACKAGE_INPUT_METHOD
import com.cn.board.meet.home.app.PackageName.PACKAGE_LAUNCHER
import com.cn.board.meet.home.app.PackageName.PACKAGE_MEDIA_PLAY
import com.cn.board.meet.home.app.PackageName.PACKAGE_NATIVE_DISPLAY
import com.cn.board.meet.home.app.PackageName.PACKAGE_SCREENDRAWING
import com.cn.board.meet.home.app.PackageName.PACKAGE_SETTING
import com.cn.board.meet.home.app.PackageName.PACKAGE_SYSTEM_UPGRADE
import com.cn.board.meet.home.app.PackageName.PACKAGE_TOUCH_DATA
import com.cn.board.meet.home.app.PackageName.PACKAGE_WELCOME_SIGN
import com.cn.board.meet.home.app.PackageName.PACKAGE_WPS
import com.cn.core.ui.application.ApplicationContextExt.context
import com.cn.core.utils.AppUtil.isAppInstalled

/**
 * App 流聚合器（AppStreamAggregator）
 *
 * 同时承接原 AppInfoData 的职责：持有默认应用列表、可展示 App 列表等数据状态，
 * 并负责扫描设备上已安装应用、与本地 App 信息表对齐（写入默认应用 / 新安装应用），
 * 聚合出可展示的 App 列表数据。
 *
 * 以 object 单例形式存在，承接原 appInfoData 全局单例的语义（供 SoftEntity 等访问
 * mainAppListSize 等共享状态）。
 */
class AppStreamAggregator {

    private val repository by lazy { AppRepository(context) }

    val defaultAppList by lazy {
        ArrayList<AppInfo>().apply {
            for (index in 0 until 12) { ////默认 11 个app
                add(
                    AppInfo().apply {
                        appType = EmAppType.tp
                        when (index) {
                            0 -> {
                                main = 1
                                mainIndex = 0
                                clazz = APP_CREATE_CONF
                                packageName = PACKAGE_CONFERENCE
                            }
                            1 -> {
                                main = 1
                                mainIndex = 2
                                offlineMain = 1
                                offlineMainIndex = 0
                                clazz = APP_JOIN_CONF
                                packageName = PACKAGE_CONFERENCE
                            }
                            2 -> {
                                main = 1
                                mainIndex = 2
                                clazz = APP_TOUCH_DATA
                                packageName = PACKAGE_TOUCH_DATA
                                offlineMain = 1
                                offlineMainIndex = 2
                            }
                            3 -> {
                                main = 1
                                mainIndex = 3
                                clazz = APP_FILE_EXPLORER
                                packageName = PACKAGE_FILE_EXPLORER
                                offlineMain = 1
                                offlineMainIndex = 3
                            }
                            4 -> {
                                clazz = APP_CONTACT
                                packageName = PACKAGE_CONFERENCE
                                offlineMain = 1
                                offlineMainIndex = 1
                            }
                            5 -> {
                                clazz = APP_FOR_SCREEN
                                packageName = context.packageName
                            }
                            6 -> {
                                clazz = APP_VOD
                                packageName = PACKAGE_CONFERENCE
                            }
                            7 -> {
                                clazz = APP_SETTING
                                packageName = PACKAGE_SETTING
                            }
                            8 -> {
                                clazz = APP_CHROME
                                packageName = PACKAGE_EXPLORER
                            }
                            9 -> {
                                clazz = APP_DIAGNOSIS
                                packageName = PACKAGE_DIAGNOSIS
                            }
                            10 -> {
                                clazz = APP_HELP
                                packageName = PACKAGE_HELP
                            }
                            11 -> {
                                clazz = APP_WELCOME_SIGN
                                packageName = PACKAGE_WELCOME_SIGN
                            }
                            12 -> {
                                clazz = APP_WPS_HOME
                                packageName = PACKAGE_WPS
                            }
                        }
                    },
                )
            }
        }
    }

    val filterAppPkg by lazy {
        ArrayList<String>().apply {
            add(PACKAGE_DIAGNOSIS)
            add(PACKAGE_CONFERENCE)
            add(PACKAGE_SETTING)
            add(PACKAGE_HELP)
            add(PACKAGE_EXPLORER)
            add(PACKAGE_LAUNCHER)
            add(PACKAGE_FILE_EXPLORER)
            add(PACKAGE_TOUCH_DATA)
            add(PACKAGE_AIR_PLAY)
            add(PACKAGE_IMIXAPPCORE)
            add(PACKAGE_SCREENDRAWING)
            add(PACKAGE_INPUT_METHOD)
            add(PACKAGE_MEDIA_PLAY)
            add(PACKAGE_NATIVE_DISPLAY)
            add(PACKAGE_CAST)
            add(PACKAGE_SYSTEM_UPGRADE)
            add(PACKAGE_WELCOME_SIGN)
            add("com.yzzd.dm")
//            add(PACKAGE_WPS)
            add("com.he.ardc")
        }
    } //需要过滤的app包名5r

    suspend fun init() {
        checkDefaultApp()
        checkByPackageInfo()
        repository.query().takeIf { it.isNotEmpty() }?.let { list ->
            list.forEach { it: AppInfo ->
                if (isAppInstalled(context, it.packageName)) {
                    if (it.clazz != APP_VOD) {
                        SoftEntity(it).apply {
                            if (appInfo?.appType == EmAppType.Third) {
                                appInfo?.packageName?.let { it ->
                                    try {
                                        context.packageManager.getApplicationIcon(it).let {
                                            bitmap =
                                                createBitmap(
                                                    it.intrinsicWidth,
                                                    it.intrinsicHeight,
                                                    if (it.opacity != PixelFormat.OPAQUE) Bitmap.Config.ARGB_8888 else Bitmap.Config.RGB_565,
                                                ).apply {
                                                    val canvas = Canvas(this)
                                                    it.setBounds(0, 0, it.intrinsicWidth, it.intrinsicHeight)
                                                    it.draw(canvas)
                                                }
                                        }
                                    } catch (e: NameNotFoundException) {
                                    }
                                }
                            }
                        }.let {
                            Log.d(AppStreamAggregator::class.simpleName, "init: ${it.appInfo}")
                        }
                    }
                }
            }
        }
    }

    private suspend fun checkDefaultApp() {
        defaultAppList.forEach { defaultApp ->
            var hasDefaultApp = false
            if (isAppInstalled(context, defaultApp.packageName)) {
                repository.queryByPackageName(defaultApp.packageName).onEach {
                    it.takeIf { defaultApp.packageName == it.packageName && defaultApp.clazz == it.clazz }
                        ?.let { hasDefaultApp = true }
                }
                hasDefaultApp.takeIf { !it }?.let {
                    repository.insert(defaultApp)
                }
            }
        }
    }

    private suspend fun checkByPackageInfo() {
        packageInfoList().forEach { it ->
            it.takeIf { !isSystemApp(it) && !filterAppPkg.contains(it.packageName) }?.let { it ->
                getAppInfo(it).let { entity ->
                    repository.run {
                        queryByPackageName(entity.packageName).takeIf { it.size == 0 }?.let {
                            insert(entity)
                        }
                    }
                }
            }
        }
    }

    private fun getAppInfo(it: PackageInfo): AppInfo {
        return AppInfo().apply {
            packageName = it.packageName
        }
    }

    @SuppressLint("QueryPermissionsNeeded")
    private fun packageInfoList(): MutableList<PackageInfo> = context.packageManager.getInstalledPackages(PackageManager.GET_ACTIVITIES)

    private fun isSystemApp(it: PackageInfo): Boolean {
        if (it.packageName.equals(PACKAGE_WPS) || it.packageName == PACKAGE_WELCOME_SIGN) {
            return false
        }
        if (it.applicationInfo?.sourceDir?.contains("/skyuser/respack/android/app/") == true) {
            return false
        }
        return it.applicationInfo?.flags!! and ApplicationInfo.FLAG_SYSTEM > 0
    }
}
