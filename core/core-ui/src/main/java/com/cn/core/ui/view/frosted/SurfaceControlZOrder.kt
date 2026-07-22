package com.cn.core.ui.view.frosted

import android.annotation.SuppressLint
import android.util.Log
import android.view.SurfaceControl
import android.view.SurfaceView
import android.view.View

/**
 * 通过 @hide 的 [android.view.SurfaceControl] / [android.view.SurfaceControl.Transaction]
 * 反射调整 SurfaceView 的 Surface 合成层级。
 *
 * ## 使用场景
 * FrostedAnimatedGlowView 走 SYSTEM(跨窗口模糊) 路径时,模糊由 SurfaceFlinger 对
 * "窗口之下的所有图层" 按屏幕区域生效。SurfaceView 的 Surface 默认位于窗口之下,
 * 因此会被一起模糊。
 *
 * 把 SurfaceView 的 Surface 抬到「窗口之上」(reparent 到 ViewRootImpl 的 SurfaceControl
 * 并设正层级),它就不再是"窗口之下"的图层 → 不再被跨窗口模糊影响 → 保持清晰。
 * 这与公开 API `SurfaceView.setZOrderOnTop(true)` 的内部行为等价,本类用于需要
 * 反射/更细粒度控制的场合。
 *
 * ## ⚠️ 风险与坑
 * 1. 全部为 @hide API,不同 Android 版本 / OEM ROM 可能字段或方法签名不同;
 *    初始化与每次调用都做了 try-catch,失败时返回 false,调用方需有降级。
 * 2. SurfaceView 内部 `updateSurface()` 在 layout / 可见性变化时可能会把 Surface 的
 *    父层 / 子层级重置回默认(窗口之下)。因此本类的设置**不一定能跨 relayout 保持**,
 *    建议在 `SurfaceHolder.Callback.surfaceCreated` 之后调用,并在需要时重新 apply。
 *    若可改用公开 API,优先 `surfaceView.setZOrderOnTop(true)` / `setZOrderMediaOverlay(true)`。
 * 3. reparent 到壁纸层(跨进程)通常需要系统权限,普通应用多数机型拿不到壁纸 SC,不推荐。
 *
 * @author cn
 */
@SuppressLint("BlockedPrivateApi", "PrivateApi", "NewApi")
object SurfaceControlZOrder {

    private const val TAG = "SurfaceControlZOrder"
    private const val DEBUG = true
    private inline fun logD(m: () -> String) { if (DEBUG) Log.d(TAG, m()) }
    private inline fun logW(m: () -> String) { Log.w(TAG, m()) }

    // ---- 反射缓存 ----
    private var sInit = false
    private var sOk = false
    private var sInitWindow = false
    private var sOkWindow = false
    private var clsSC: Class<*>? = null          // android.view.SurfaceControl
    private var clsTx: Class<*>? = null          // android.view.SurfaceControl$Transaction
    private var ctorTx: java.lang.reflect.Constructor<*>? = null
    private var mReparent: java.lang.reflect.Method? = null
    private var mSetLayer: java.lang.reflect.Method? = null
    private var mSetRelativeLayer: java.lang.reflect.Method? = null
    private var mApply: java.lang.reflect.Method? = null
    // 以下为 SCVH overlay 收口到反射层所需的 Transaction 操作（SDK 35 部分仍为 @hide / 跨 ROM 签名不同）
    private var mSetPosition: java.lang.reflect.Method? = null          // Transaction.setPosition(SC, float, float)
    private var mSetBufferSize: java.lang.reflect.Method? = null        // Transaction.setBufferSize(SC, int, int)
    private var mSetVisibility: java.lang.reflect.Method? = null        // Transaction.setVisibility(SC, boolean)
    private var mSCGetSurfaceControl: java.lang.reflect.Method? = null  // SurfaceView.getSurfaceControl()
    private var mViewGetVRI: java.lang.reflect.Method? = null          // View.getViewRootImpl()
    private var mVRIGetSurfaceControl: java.lang.reflect.Method? = null // ViewRootImpl.getSurfaceControl()

    @Synchronized
    private fun ensureInit(): Boolean {
        if (sInit) return sOk
        sInit = true
        try {
            clsSC = Class.forName("android.view.SurfaceControl")
            clsTx = Class.forName("android.view.SurfaceControl\$Transaction")
            ctorTx = clsTx!!.getDeclaredConstructor().apply { isAccessible = true }
            mSetLayer = clsTx!!.getDeclaredMethod("setLayer", clsSC, Int::class.javaPrimitiveType).apply { isAccessible = true }
            mSetRelativeLayer = clsTx!!.getDeclaredMethod("setRelativeLayer", clsSC, clsSC, Int::class.javaPrimitiveType).apply { isAccessible = true }
            mApply = clsTx!!.getDeclaredMethod("apply").apply { isAccessible = true }
            mSetPosition = clsTx!!.getDeclaredMethod(
                "setPosition", clsSC, Float::class.javaPrimitiveType, Float::class.javaPrimitiveType
            ).apply { isAccessible = true }
            mSetBufferSize = clsTx!!.getDeclaredMethod(
                "setBufferSize", clsSC, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType
            ).apply { isAccessible = true }
            mSetVisibility = clsTx!!.getDeclaredMethod(
                "setVisibility", clsSC, Boolean::class.javaPrimitiveType
            ).apply { isAccessible = true }
            mSCGetSurfaceControl = SurfaceView::class.java.getDeclaredMethod("getSurfaceControl").apply { isAccessible = true }
            sOk = true
            logD { "init OK (overlay positioning group)" }
        } catch (e: Exception) {
            logW { "init failed: ${e.message}" }
            sOk = false
        }
        return sOk
    }

    /**
     * 窗口模糊组反射初始化：reparent / View.getViewRootImpl() / ViewRootImpl.getSurfaceControl()。
     * 与 [ensureInit] (overlay 定位组) 解耦——定制 ROM 上本组任一方法签名对不上只会影响
     * bringAboveWindow / sendBelowWindow / getWindowSC 等窗口模糊路径，
     * 不会拖垮 SurfaceControlViewHost overlay 的定位（[createTransaction] 仍可用）。
     */
    @Synchronized
    private fun ensureInitWindow(): Boolean {
        if (sInitWindow) return sOkWindow
        sInitWindow = true
        try {
            clsSC = Class.forName("android.view.SurfaceControl")
            clsTx = Class.forName("android.view.SurfaceControl\$Transaction")
            mReparent = clsTx!!.getDeclaredMethod("reparent", clsSC, clsSC).apply { isAccessible = true }
            mViewGetVRI = View::class.java.getDeclaredMethod("getViewRootImpl").apply { isAccessible = true }
            // ViewRootImpl.getSurfaceControl() 返回 SurfaceControl
            val vriCls = Class.forName("android.view.ViewRootImpl")
            mVRIGetSurfaceControl = vriCls.getDeclaredMethod("getSurfaceControl").apply { isAccessible = true }
            sOkWindow = true
            logD { "initWindow OK (window blur group)" }
        } catch (e: Exception) {
            logW { "initWindow failed: ${e.message}" }
            sOkWindow = false
        }
        return sOkWindow
    }

    // ---- 内部工具 ----

    /** 取 SurfaceView 自身的 SurfaceControl(隐藏 API)。 */
    private fun getSurfaceControlInternal(sv: SurfaceView): Any? =
        try { mSCGetSurfaceControl?.invoke(sv) } catch (e: Exception) { logW { "getSurfaceControl: ${e.message}" }; null }

    /**
     * 公开取 SurfaceView 自身的 [SurfaceControl]（API 29+）。
     * 用于上层把其它 surface 用 [setRelativeLayer] 排在这个视频 Surface 之上/之下。
     */
    @androidx.annotation.RequiresApi(android.os.Build.VERSION_CODES.Q)
    fun getSurfaceControl(sv: SurfaceView): SurfaceControl? {
        if (!ensureInit()) return null
        return getSurfaceControlInternal(sv) as? SurfaceControl
    }

    /** 取 SurfaceView 所在窗口的 SurfaceControl(隐藏 API: ViewRootImpl.getSurfaceControl)。 */
    private fun getWindowSurfaceControl(sv: SurfaceView): Any? {
        if (!ensureInitWindow()) return null
        return try {
            val vri = mViewGetVRI?.invoke(sv) ?: return null
            mVRIGetSurfaceControl?.invoke(vri)
        } catch (e: Exception) {
            logW { "getWindowSurfaceControl: ${e.message}" }; null
        }
    }

    private fun newTransaction(): Any? =
        try { ctorTx?.newInstance() } catch (e: Exception) { logW { "newTransaction: ${e.message}" }; null }

    // ---- 公开 API ----

    /**
     * 把 SurfaceView 的 Surface 抬到「窗口之上」:reparent 到窗口 SurfaceControl 并设正层级。
     * 效果 = 不再被跨窗口模糊模糊、显示在窗口 View 内容之上。
     *
     * @param layer 相对窗口根 Surface 的层级,默认用一个较大的正值(InT.MAX_VALUE/2)。
     *              值越大越靠上。若想精确只略高于 View 内容,可传 1。
     * @return 是否成功
     */
    @JvmOverloads
    fun bringAboveWindow(sv: SurfaceView, layer: Int = Int.MAX_VALUE / 2): Boolean {
        if (!ensureInit() || !ensureInitWindow()) return false
        val mySc = getSurfaceControlInternal(sv) ?: return false.also { logW { "bringAboveWindow: surfaceControl null" } }
        val windowSc = getWindowSurfaceControl(sv) ?: return false.also { logW { "bringAboveWindow: window SC null" } }
        val tx = newTransaction() ?: return false
        return try {
            // 等价于 SurfaceView.setZOrderOnTop(true) 的内部行为
            mReparent!!.invoke(tx, mySc, windowSc)
            mSetLayer!!.invoke(tx, mySc, layer)
            mApply!!.invoke(tx)
            logD { "bringAboveWindow OK (layer=$layer)" }
            true
        } catch (e: Exception) {
            logW { "bringAboveWindow: ${e.message}" }; false
        }
    }

    /**
     * 把 SurfaceView 恢复为「窗口之下」(默认位置),使其可被跨窗口模糊模糊。
     * 用 setRelativeLayer 相对窗口 Surface 设为负值;若失败回退到 setLayer 负值。
     */
    fun sendBelowWindow(sv: SurfaceView): Boolean {
        if (!ensureInit() || !ensureInitWindow()) return false
        val mySc = getSurfaceControlInternal(sv) ?: return false.also { logW { "sendBelowWindow: surfaceControl null" } }
        val windowSc = getWindowSurfaceControl(sv) ?: return false.also { logW { "sendBelowWindow: window SC null" } }
        val tx = newTransaction() ?: return false
        return try {
            // 相对窗口 Surface 的子层级 -1 = 在 View 内容之下(默认 behind 行为)
            mSetRelativeLayer!!.invoke(tx, mySc, windowSc, -1)
            mApply!!.invoke(tx)
            logD { "sendBelowWindow OK" }
            true
        } catch (e: Exception) {
            logW { "sendBelowWindow(relative) failed: ${e.message}" }
            try {
                mReparent!!.invoke(tx, mySc, windowSc)
                mSetLayer!!.invoke(tx, mySc, -1)
                mApply!!.invoke(tx)
                logD { "sendBelowWindow fallback setLayer(-1) OK" }
                true
            } catch (e2: Exception) {
                logW { "sendBelowWindow fallback failed: ${e2.message}" }; false
            }
        }
    }

    /**
     * 直接设置 SurfaceView Surface 的绝对层级(相对其当前父层)。
     * 仅当你明确知道当前父层与期望层级时使用。
     */
    fun setSurfaceLayer(sv: SurfaceView, layer: Int): Boolean {
        if (!ensureInit()) return false
        val mySc = getSurfaceControlInternal(sv) ?: return false
        val tx = newTransaction() ?: return false
        return try {
            mSetLayer!!.invoke(tx, mySc, layer)
            mApply!!.invoke(tx)
            logD { "setSurfaceLayer OK (layer=$layer)" }
            true
        } catch (e: Exception) { logW { "setSurfaceLayer: ${e.message}" }; false }
    }

    /**
     * 通用 reparent(对应你贴的片段)。
     * 注意:把 SurfaceView reparent 到壁纸层(窗口之下)会**更**容易被模糊,
     * 通常这不是想要的效果;本方法保留以便你按需控制。
     *
     * @param parentSc 目标父 SurfaceControl(null = 脱离当前父层)
     * @param layer    相对父层的层级
     */
    fun reparent(sv: SurfaceView, parentSc: Any?, layer: Int): Boolean {
        if (!ensureInit() || !ensureInitWindow()) return false
        val mySc = getSurfaceControlInternal(sv) ?: return false
        val tx = newTransaction() ?: return false
        return try {
            mReparent!!.invoke(tx, mySc, parentSc)
            mSetLayer!!.invoke(tx, mySc, layer)
            mApply!!.invoke(tx)
            logD { "reparent OK (layer=$layer)" }
            true
        } catch (e: Exception) { logW { "reparent: ${e.message}" }; false }
    }

    /** 是否反射初始化成功(可用于降级判断)。 */
    fun isAvailable(): Boolean = ensureInit()

    // ================================================================
    //  通用反射操作（供 SurfaceControlViewHost overlay 等上层调用）
    //  统一收口到本类的反射缓存，避免各模块重复写反射、并保留跨 ROM 降级。
    //  所有方法均 try-catch 返回 Boolean，调用方据此走降级。
    // ================================================================

    /**
     * 反射创建一个 [SurfaceControl.Transaction]（SDK 29+）。失败返回 null。
     * 与普通公开构造不同：经由反射拿到真实 framework 的 Transaction 实例，
     * 在系统/特权应用与严格 ROM 上都能稳定取到。
     */
    fun createTransaction(): Any? {
        if (!ensureInit()) return null
        return try { ctorTx?.newInstance() } catch (e: Exception) { logW { "createTransaction: ${e.message}" }; null }
    }

    /** 反射调用 Transaction.apply()。 */
    fun apply(tx: Any?): Boolean = try {
        mApply?.invoke(tx); true
    } catch (e: Exception) { logW { "apply: ${e.message}" }; false }

    /** 反射调用 Transaction.setPosition(SC, x, y)。 */
    fun setPosition(tx: Any?, sc: SurfaceControl?, x: Float, y: Float): Boolean = try {
        mSetPosition?.invoke(tx, sc, x, y); true
    } catch (e: Exception) { logW { "setPosition: ${e.message}" }; false }

    /** 反射调用 Transaction.setBufferSize(SC, w, h)。 */
    fun setBufferSize(tx: Any?, sc: SurfaceControl?, w: Int, h: Int): Boolean = try {
        mSetBufferSize?.invoke(tx, sc, w, h); true
    } catch (e: Exception) { logW { "setBufferSize: ${e.message}" }; false }

    /** 反射调用 Transaction.setVisibility(SC, visible)。 */
    fun setVisibility(tx: Any?, sc: SurfaceControl?, visible: Boolean): Boolean = try {
        mSetVisibility?.invoke(tx, sc, visible); true
    } catch (e: Exception) { logW { "setVisibility: ${e.message}" }; false }

    /** 反射调用 Transaction.setRelativeLayer(SC, relTo, layer)。 */
    fun setRelativeLayer(tx: Any?, sc: SurfaceControl?, relTo: SurfaceControl?, layer: Int): Boolean = try {
        mSetRelativeLayer?.invoke(tx, sc, relTo, layer); true
    } catch (e: Exception) { logW { "setRelativeLayer: ${e.message}" }; false }

    /** 反射调用 Transaction.setLayer(SC, layer)（相对当前父层的绝对层级）。 */
    fun setLayer(tx: Any?, sc: SurfaceControl?, layer: Int): Boolean = try {
        mSetLayer?.invoke(tx, sc, layer); true
    } catch (e: Exception) { logW { "setLayer: ${e.message}" }; false }

    /**
     * 取 SurfaceView 所在窗口的 [SurfaceControl]（公开为 [SurfaceControl] 类型，需 API 29+）。
     * 用于将 UI 的 SurfaceControlViewHost 与视频 Surface 挂到同一父层，
     * 再用 [SurfaceControl.Transaction.setRelativeLayer] 把 UI 排在视频之上。
     */
    @androidx.annotation.RequiresApi(android.os.Build.VERSION_CODES.Q)
    fun getWindowSC(sv: SurfaceView): SurfaceControl? {
        if (!ensureInit() || !ensureInitWindow()) return null
        return getWindowSurfaceControl(sv) as? SurfaceControl
    }

    // ================================================================
    //  SurfacePackage -> SurfaceControl（反射）
    //  SurfaceControlViewHost.SurfacePackage.getSurfaceControl() 在部分
    //  compileSdk / ROM 上为 @hide 或不存在，用反射绕开编译依赖。
    // ================================================================

    /**
     * 反射取 [SurfaceControlViewHost.SurfacePackage] 中的 [SurfaceControl]。
     * 独立于 [ensureInit]——不依赖主 init 组，API 31+ 运行时可取到，
     * 低版本返回 null，调用方自行降级。
     */
    fun getSurfaceControl(pkg: Any?): SurfaceControl? {
        if (pkg == null) return null
        return try {
            val m = pkg.javaClass.getMethod("getSurfaceControl")
            m.isAccessible = true
            m.invoke(pkg) as? SurfaceControl
        } catch (e: Exception) {
            logW { "getSurfaceControl(pkg): ${e.message}" }
            null
        }
    }
}
