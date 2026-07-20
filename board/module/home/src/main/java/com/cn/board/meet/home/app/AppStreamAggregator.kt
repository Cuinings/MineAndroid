package com.cn.board.meet.home.app

import android.annotation.SuppressLint
import android.content.Context
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
import com.cn.core.utils.AppUtil.isAppInstalled

/**
 * App 流聚合器（AppStreamAggregator）
 *
 * 同时承接原 AppInfoData 的职责：持有默认应用列表、可展示 App 列表等数据状态，
 * 并负责扫描设备上已安装应用、与本地 App 信息表对齐（写入默认应用 / 新安装应用），
 * 聚合出可展示的 App 列表数据。
 *
 * 注意：本类只负责“产出”聚合结果（通过 [init] 返回），不持有结果流；结果流的持有与
 * 分发由 HomeModel 负责，避免 producer / consumer 共享可变状态，职责更清晰。
 */
class AppStreamAggregator(val context: Context) {

    private val repository by lazy { AppRepository(context) }

    private val defaultAppList by lazy {
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
                                mainIndex = 1
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

    private val filterAppPkg by lazy {
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
            add("com.he.ardc")
        }
    } //需要过滤的app包名5r

    suspend fun init(): List<SoftEntity> {
        Log.d(AppStreamAggregator::class.simpleName, "init")
        checkDefaultApp()
        checkByPackageInfo()
        val aggregated = ArrayList<SoftEntity>()
        repository.query().takeIf { it.isNotEmpty() }?.let { list ->
            Log.d(AppStreamAggregator::class.simpleName, "init: ${list.size}")
            // 1→2 迁移后存量数据 orderIndex 默认 -1：把未初始化的行「追加」到当前最大 orderIndex
            // 之后回填，保持它们在表中原有的 id 相对顺序；已设定（用户拖拽过）的 orderIndex
            // 不被覆盖，避免重置已有顺序。回填后全表无 -1，后续 init 不再重复执行。
            if (list.any { it.orderIndex < 0 }) {
                var next = (list.maxOfOrNull { it.orderIndex } ?: -1) + 1
                list.filter { it.orderIndex < 0 }.sortedBy { it.id }.forEach { app ->
                    app.orderIndex = next
                    repository.update(app)
                    next++
                }
            }
            initSoftEntity(list, aggregated)
        } ?: Log.e(AppStreamAggregator::class.simpleName, "init empty")
        // 展示顺序由 orderIndex 决定：按 orderIndex 升序还原（未设置 -1 排最后，再用 id 兜底）。
        aggregated.sortWith(
            compareBy(
                { it.appInfo?.orderIndex ?: Int.MAX_VALUE },
                { it.appInfo?.id }
            )
        )
        // 只产出聚合结果，由 HomeModel 负责写入 appListFlow 并分发给消费者
        Log.d(AppStreamAggregator::class.simpleName, "init done, aggregated=${aggregated.size}")
        return aggregated
    }

    suspend fun getMainApp(online: Boolean): MutableList<SoftEntity> {
        val aggregated = ArrayList<SoftEntity>()
        val list = if (online) repository.queryByMain(1) else repository.queryOfflineMain(1)
        list.forEach {
            Log.d(AppStreamAggregator::class.simpleName, "getMainApp: $it")
        }
        initSoftEntity(list, aggregated)
        return aggregated
    }

    /**
     * 仅读取并聚合当前已存 App（不执行 [checkDefaultApp]/[checkByPackageInfo] 对齐），
     * 专门用于「标记（main/offlineMain）变更后」刷新列表。若复用 [init] 会因默认对齐把刚改的
     * 标记覆盖回默认值，导致列表选择样式与编辑列表都看似「没有变化」。
     */
    suspend fun queryAllApps(): List<SoftEntity> {
        val aggregated = ArrayList<SoftEntity>()
        repository.query().takeIf { it.isNotEmpty() }?.let { list ->
            initSoftEntity(list, aggregated)
        } ?: Log.e(AppStreamAggregator::class.simpleName, "queryAllApps empty")
        // 展示顺序由 orderIndex 决定：按 orderIndex 升序还原（未设置 -1 排最后，再用 id 兜底）。
        aggregated.sortWith(
            compareBy(
                { it.appInfo?.orderIndex ?: Int.MAX_VALUE },
                { it.appInfo?.id }
            )
        )
        return aggregated
    }

    /** 持久化单个 AppInfo 的标记变更（main / offlineMain 等） */
    suspend fun updateApp(app: AppInfo) = repository.update(app)

    /** 当前在线主页应用的最大 mainIndex（无则为 -1），供新增主应用时追加到末尾、保持排序稳定 */
    suspend fun nextMainIndex(): Int {
        val list = repository.queryByMain(1)
        return (list.maxOfOrNull { it.mainIndex } ?: -1) + 1
    }

    /** 当前离线主页应用的最大 offlineMainIndex，供新增离线主应用时追加到末尾、保持排序稳定 */
    suspend fun nextOfflineMainIndex(): Int {
        val list = repository.queryOfflineMain(1)
        return (list.maxOfOrNull { it.offlineMainIndex } ?: -1) + 1
    }

    private fun initSoftEntity(
        list: MutableList<AppInfo>,
        aggregated: ArrayList<SoftEntity>,
    ) {
        list.forEach { app: AppInfo ->
            if (isAppInstalled(context, app.packageName)) {
                if (app.clazz != APP_VOD) {
                    val soft = SoftEntity(app).apply {
                        appInfo?.packageName?.let { pkg ->
                            // 应用名称：通过 PackageManager 取已安装应用的显示名（label）
                            try {
                                appInfo?.name = context.packageManager
                                    .getApplicationInfo(pkg, 0)
                                    .loadLabel(context.packageManager)
                                    .toString()
                            } catch (e: NameNotFoundException) {
                            }
                            // 第三方应用额外异步加载图标为 Bitmap
                            if (appInfo?.appType == EmAppType.Third) {
                                try {
                                    context.packageManager.getApplicationIcon(pkg).let { drawable ->
                                        bitmap =
                                            createBitmap(
                                                drawable.intrinsicWidth,
                                                drawable.intrinsicHeight,
                                                if (drawable.opacity != PixelFormat.OPAQUE) Bitmap.Config.ARGB_8888 else Bitmap.Config.RGB_565,
                                            ).apply {
                                                val canvas = Canvas(this)
                                                drawable.setBounds(
                                                    0,
                                                    0,
                                                    drawable.intrinsicWidth,
                                                    drawable.intrinsicHeight
                                                )
                                                drawable.draw(canvas)
                                            }
                                    }
                                } catch (e: NameNotFoundException) {
                                }
                            }
                        }
                    }
                    aggregated.add(soft)
                    Log.d(AppStreamAggregator::class.simpleName, "init: ${soft.appInfo}")
                }
            }
        }
    }

    /**
     * 拖拽后持久化新的展示顺序：只更新各 AppInfo 的 orderIndex 列（= 在列表中的下标），
     * 主键 id 保持不变，因此不会和任何隐藏行撞键、也不存在 id 无界增长的问题。
     * 下次 [init] 会按 orderIndex 升序还原该顺序（见 init 内的 sortWith）。
     */
    suspend fun persistOrder(order: List<SoftEntity>) {
        order.forEachIndexed { index, entity ->
            entity.appInfo?.let { app ->
                repository.updateOrderIndex(app.id, index)
                app.orderIndex = index
            }
        }
        Log.d(AppStreamAggregator::class.simpleName, "persistOrder done, size=${order.size}")
    }

    private suspend fun checkDefaultApp() {
        defaultAppList.forEach { defaultApp ->
            if (isAppInstalled(context, defaultApp.packageName)) {
                val matched = repository.queryByPackageName(defaultApp.packageName)
                    .firstOrNull { it.packageName == defaultApp.packageName && it.clazz == defaultApp.clazz }
                if (matched == null) {
                    // 不存在：直接插入默认应用（mainIndex/offlineMainIndex 等随默认值写入）
                    repository.insert(defaultApp)
                } else {
                    // 已存在（多为升级场景，旧行 mainIndex/offlineMainIndex 仍为 -1）：
                    // 把默认应用自带的静态字段同步回库，保证首页/离线排序正确。
                    // 不覆盖 id、orderIndex（用户拖拽顺序）、name（动态名称）等可变字段。
                    val dirty = matched.main != defaultApp.main
                        || matched.mainIndex != defaultApp.mainIndex
                        || matched.offlineMain != defaultApp.offlineMain
                        || matched.offlineMainIndex != defaultApp.offlineMainIndex
                        || matched.clazz != defaultApp.clazz
                        || matched.appType != defaultApp.appType
                    if (dirty) {
                        matched.main = defaultApp.main
                        matched.mainIndex = defaultApp.mainIndex
                        matched.offlineMain = defaultApp.offlineMain
                        matched.offlineMainIndex = defaultApp.offlineMainIndex
                        matched.clazz = defaultApp.clazz
                        matched.appType = defaultApp.appType
                        repository.update(matched)
                    }
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
