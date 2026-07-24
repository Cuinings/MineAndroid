package com.cn.board.home.data

import android.annotation.SuppressLint
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PixelFormat
import android.util.Log
import androidx.core.graphics.createBitmap
import com.cn.board.home.entity.AppInfoEntity
import com.cn.board.home.entity.EmAppType
import com.cn.board.home.entity.SoftEntity
import com.cn.board.home.function.bpModel
import com.cn.board.home.function.ifConf
import com.cn.board.home.function.mcModel
import com.cn.board.home.HomeModel
import com.cn.board.home.HomeModelIntent
import com.cn.board.home.room.repsoitory.repository
import com.cn.board.home.state.State.chromeEnable
import com.cn.board.home.util.ActivityClassName.APP_CHROME
import com.cn.board.home.util.ActivityClassName.APP_CONTACT
import com.cn.board.home.util.ActivityClassName.APP_CREATE_CONF
import com.cn.board.home.util.ActivityClassName.APP_DIAGNOSIS
import com.cn.board.home.util.ActivityClassName.APP_FILE_EXPLORER
import com.cn.board.home.util.ActivityClassName.APP_FOR_SCREEN
import com.cn.board.home.util.ActivityClassName.APP_HELP
import com.cn.board.home.util.ActivityClassName.APP_JOIN_CONF
import com.cn.board.home.util.ActivityClassName.APP_SETTING
import com.cn.board.home.util.ActivityClassName.APP_TOUCH_DATA
import com.cn.board.home.util.ActivityClassName.APP_VOD
import com.cn.board.home.util.ActivityClassName.APP_WELCOME_SIGN
import com.cn.board.home.util.ActivityClassName.APP_WPS_HOME
import com.cn.board.home.util.PackageName.PACKAGE_AIR_PLAY
import com.cn.board.home.util.PackageName.PACKAGE_CAST
import com.cn.board.home.util.PackageName.PACKAGE_CONFERENCE
import com.cn.board.home.util.PackageName.PACKAGE_DIAGNOSIS
import com.cn.board.home.util.PackageName.PACKAGE_EXPLORER
import com.cn.board.home.util.PackageName.PACKAGE_FILE_EXPLORER
import com.cn.board.home.util.PackageName.PACKAGE_HELP
import com.cn.board.home.util.PackageName.PACKAGE_IMIXAPPCORE
import com.cn.board.home.util.PackageName.PACKAGE_INPUT_METHOD
import com.cn.board.home.util.PackageName.PACKAGE_LAUNCHER
import com.cn.board.home.util.PackageName.PACKAGE_MEDIA_PLAY
import com.cn.board.home.util.PackageName.PACKAGE_NATIVE_DISPLAY
import com.cn.board.home.util.PackageName.PACKAGE_SCREENDRAWING
import com.cn.board.home.util.PackageName.PACKAGE_SETTING
import com.cn.board.home.util.PackageName.PACKAGE_SYSTEM_UPGRADE
import com.cn.board.home.util.PackageName.PACKAGE_TOUCH_DATA
import com.cn.board.home.util.PackageName.PACKAGE_WELCOME_SIGN
import com.cn.board.home.util.PackageName.PACKAGE_WPS
import com.cn.core.ui.application.ApplicationContextExt.context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.util.Locale

val appInfoData by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
    AppInfoData()
}
class AppInfoData {

    val defaultAppList = ArrayList<AppInfoEntity>()

    val appData = ArrayList<SoftEntity>()
    val mainAppData = ArrayList<SoftEntity>()

    val mainAppListSize: Int get() = mainAppData.size

    val filterAppPkg by lazy { ArrayList<String>().apply {
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
    } }//需要过滤的app包名5r

    suspend fun updateApp(appInfo: AppInfoEntity) {
        repository.update(appInfo)
        repository.queryById(appInfo.id).let {
            if (it.isNotEmpty()) Log.d(TAG, "updateApp: ${it[0]}")
        }
    }

    suspend fun addPackage(pkg: String): Any? = withContext(Dispatchers.IO) {
        if (filterAppPkg.contains(pkg)) return@withContext null
        packageInfoList().takeIf { it.isNotEmpty() }?.let{
            it.forEach { it ->
                if (it.packageName.equals(pkg)) {
                    return@withContext SoftEntity(getAppInfoEntity(it)).apply { appInfo?.let { it ->
                        context.packageManager.getApplicationIcon(appInfo?.packageName!!).let {
                            bpModel({
                                bitmap = createBitmap(
                                    it.intrinsicWidth,
                                    it.intrinsicHeight,
                                    if (it.opacity != PixelFormat.OPAQUE) Bitmap.Config.ARGB_8888 else Bitmap.Config.RGB_565
                                ).apply {
                                    val canvas = Canvas(this)
                                    it.setBounds(0, 0, it.intrinsicWidth, it.intrinsicHeight)
                                    it.draw(canvas)
                                }
                            }) {}
                        }
                        if (repository.queryByPackageName(pkg).isEmpty()) {
                            repository.insert(it)
                            delay(300)
                            it.id = repository.queryByPackageName(it.packageName).let { it[0].id } ?:0
                        } else {
                            repository.update(it)
                        }
                        appData.add(this)
                    } }
                }
            }
        }
    }

    suspend fun replacedPackage(pkg: String): Any? {
        if (filterAppPkg.contains(pkg)) return null
        return withContext(Dispatchers.IO) {
            appData.forEach { entity ->
                if (entity.appInfo?.packageName == pkg) {
                    context.packageManager.getApplicationIcon(entity.appInfo?.packageName!!).let {
                        entity.bitmap = createBitmap(
                            it.intrinsicWidth,
                            it.intrinsicHeight,
                            if (it.opacity != PixelFormat.OPAQUE) Bitmap.Config.ARGB_8888 else Bitmap.Config.RGB_565
                        ).apply {
                            val canvas = Canvas(this)
                            it.setBounds(0, 0, it.intrinsicWidth, it.intrinsicHeight)
                            it.draw(canvas)
                        }
                    }
                    repository.update(entity.appInfo!!)
                    return@withContext entity
                }
            }
        }
    }

    fun AppInfoEntity.allowShow(action: () -> Unit) {
        Locale.getDefault().let {
            if (packageName == PACKAGE_EXPLORER) { if (chromeEnable) action.invoke()
            } else if (packageName ==  PACKAGE_CONFERENCE && clazz == APP_VOD) mcModel({}) { action.invoke() }
            else if (packageName == PACKAGE_HELP) bpModel({ /*if (it.isChina() || it.isEn() )*/ action.invoke() }) {}
            else action.invoke()
        }
    }

    suspend fun removedPackage(pkg: String): Any? {
        if (filterAppPkg.contains(pkg)) return null
        return withContext(Dispatchers.IO) {
            val mainAPPIterator = mainAppData.iterator()
            while (mainAPPIterator.hasNext()) {
                val entity = mainAPPIterator.next()
                if (entity.appInfo?.packageName == pkg) {
                    repository.deleteByPackageName(entity.appInfo?.packageName!!)
                    mainAPPIterator.remove()
                }
            }
            HomeModel.processIntent(HomeModelIntent.InitAppStream)
            val iterator = appData.iterator()
            while (iterator.hasNext()) {
                val entity = iterator.next()
                if (entity.appInfo?.packageName == pkg) {
                    repository.deleteByPackageName(entity.appInfo?.packageName!!)
                    iterator.remove()
                    return@withContext entity
                }
            }
        }
    }

    suspend fun deleteAll() {
        withContext(Dispatchers.IO) {
            repository.delete()
        }
    }

    private fun getAppInfoEntity(it: PackageInfo): AppInfoEntity {
        return AppInfoEntity().apply {
            packageName = it.packageName
            allowDelete = it.packageName.takeIf { it == PACKAGE_WPS }?.let { false }?:!it.applicationInfo?.sourceDir?.contains("/skyuser/respack/android/app/")!!
        }
    }

    @SuppressLint("QueryPermissionsNeeded")
    private fun packageInfoList(): MutableList<PackageInfo> = context.packageManager.getInstalledPackages(PackageManager.GET_ACTIVITIES)

    suspend fun update(entity: SoftEntity?) {
        entity?.appInfo?.let {
            repository.update(it)
        }
    }

    suspend fun mainApp() = flow {

        delay(500)

        val mainAppList = ArrayList<SoftEntity>()

        ifConf({
            SoftEntity(appInfo = AppInfoEntity().apply {
                appType = EmAppType.tp
                clazz = APP_CREATE_CONF
                packageName = PACKAGE_CONFERENCE
            }).apply {
                span = 1
            }.let { mainAppList.add(it) }

            SoftEntity(appInfo = AppInfoEntity().apply {
                appType = EmAppType.tp
                APP_JOIN_CONF.also { clazz = it }
                packageName = PACKAGE_CONFERENCE
            }).apply {
                span = 1
            }.let { mainAppList.add(it) }
        }) {  }

        emit(mainAppList)
    }

    init {
        for (index in 0 until 12) {////默认 11 个app
            defaultAppList.add(AppInfoEntity().apply {
                appType = EmAppType.tp
                allowDelete = false
                when(index) {
                    0 -> {
                        main = 1
                        mainIndex = 0
                        clazz = APP_CREATE_CONF
                        packageName = PACKAGE_CONFERENCE
                    }
                    1 -> {
                        main = 1
                        mainIndex = bpModel({ 2 }) { 1 }
                        offlineMain = 1
                        offlineMainIndex = 0
                        clazz = APP_JOIN_CONF
                        packageName = PACKAGE_CONFERENCE
                    }
                    2 -> {
                        main = 1
                        mainIndex = bpModel({ 1 }) { 3 }
                        clazz = APP_TOUCH_DATA
                        packageName = PACKAGE_TOUCH_DATA
                        offlineMain = 1
                        offlineMainIndex = bpModel({ 2 }) { 1 }
                    }
                    3 -> {
                        main = 1
                        mainIndex = bpModel({ 3 }) { 2 }
                        clazz = APP_FILE_EXPLORER
                        packageName = PACKAGE_FILE_EXPLORER
                        offlineMain = 1
                        offlineMainIndex = 3
                    }
                    4 -> {
                        clazz = APP_CONTACT
                        packageName = PACKAGE_CONFERENCE
                        offlineMain = 1
                        offlineMainIndex = bpModel({ 1 }) { 2 }
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
            })
        }
    }

    companion object {

        val TAG = AppInfoData::class.simpleName
        val addEntity = SoftEntity(AppInfoEntity(appType = EmAppType.Add).apply {
            mainIndex = Int.MAX_VALUE
        })
        val noneEntity = SoftEntity(AppInfoEntity(appType = EmAppType.NONE).apply {
            mainIndex = Int.MAX_VALUE
        })
    }
}
