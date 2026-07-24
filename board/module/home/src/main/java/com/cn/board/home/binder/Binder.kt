package com.cn.board.home.binder

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import androidx.databinding.BindingAdapter
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.cn.board.home.R
import com.cn.board.home.entity.AppInfoEntity
import com.cn.board.home.entity.EmAppType
import com.cn.board.home.entity.SoftEntity
import com.cn.board.home.function.bpModel
import com.cn.board.home.function.commandDispatcherModel
import com.cn.board.home.function.mcModel
import com.cn.board.home.function.modelReturn
import com.cn.board.home.state.State.commandDispatcherModel
import com.cn.board.home.state.State.isInConf
import com.cn.board.home.state.State.isOpenAPS
import com.cn.board.home.state.State.isSendAss
import com.cn.board.home.util.ActivityClassName.APP_CONTACT
import com.cn.board.home.util.ActivityClassName.APP_CREATE_CONF
import com.cn.board.home.util.ActivityClassName.APP_FOR_SCREEN
import com.cn.board.home.util.ActivityClassName.APP_HOME
import com.cn.board.home.util.ActivityClassName.APP_JOIN_CONF
import com.cn.board.home.util.ActivityClassName.APP_VOD
import com.cn.board.home.util.PackageName.PACKAGE_CONFERENCE
import com.cn.board.home.util.PackageName.PACKAGE_DIAGNOSIS
import com.cn.board.home.util.PackageName.PACKAGE_EXPLORER
import com.cn.board.home.util.PackageName.PACKAGE_FILE_EXPLORER
import com.cn.board.home.util.PackageName.PACKAGE_HELP
import com.cn.board.home.util.PackageName.PACKAGE_LAUNCHER
import com.cn.board.home.util.PackageName.PACKAGE_SETTING
import com.cn.board.home.util.PackageName.PACKAGE_TOUCH_DATA
import com.cn.board.home.util.PackageName.PACKAGE_WELCOME_SIGN
import com.cn.core.ui.application.ApplicationContextExt.context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

@BindingAdapter("visibleOrGone")
fun visibilityGone(view: View, visibility: Boolean){
    view.visibility = if (visibility) View.VISIBLE else View.GONE
}

@BindingAdapter("enableSource")
fun enabled(view: View, enableSource: Boolean) {
    view.isEnabled = enableSource
}

@BindingAdapter("selectedSource")
fun selected(view: View, selectedSource: Boolean) {
    view.isSelected = selectedSource
}


@BindingAdapter("remove")
fun loadMainSoftState(imageView: ImageView, entity: SoftEntity) {
    entity.appInfo?.let {
        imageView.setImageResource(if (it.appType == EmAppType.Add || it.appType == EmAppType.NONE) 0 else entity.bpModel({ R.drawable.icon_app_choiced_remove}) {R.drawable.bp_app_remove})
    }
}

@BindingAdapter("main")
fun loadSoftState(imageView: ImageView, entity: SoftEntity) {
    val bLogin = isOpenAPS
    val state = if (bLogin) entity.appInfo?.main == 1 else entity.appInfo?.offlineMain == 1
    imageView.setImageResource(
        if (state) entity.bpModel({ R.drawable.icon_app_choiced }) { R.drawable.choiced_bp }
        else entity.bpModel({ R.drawable.icon_app_choice }) { R.drawable.choice_bp }
    )
}

@SuppressLint("WrongConstant", "InlinedApi")
@BindingAdapter("info", "type")
fun loadTextByAppInfo(textView: TextView, info: AppInfoEntity?, type: Boolean) {
    textView.findViewTreeLifecycleOwner()?.lifecycleScope?.launch {
        val resources = textView.resources
        info?.let {
            flow {
                val isInConf = isInConf
                val name = when (it.appType) {
                    EmAppType.tp -> when(it.packageName?:"") {
                        PACKAGE_EXPLORER -> resources.getString(R.string.str_explorer)
                        PACKAGE_LAUNCHER -> {
                            when(it.clazz?:"") {
                                APP_FOR_SCREEN -> resources.getString(R.string.str_for_screen)
                                APP_HOME -> resources.getString(R.string.str_revert_home)
                                else -> ""
                            }
                        }
                        PACKAGE_CONFERENCE -> {
                            when(info?.clazz?:"") {
                                APP_CREATE_CONF -> {
                                    if (isInConf) resources.getString(R.string.str_revert_conf)
                                    else resources.getString(modelReturn({
                                        R.string.create_meeting
                                    }, {
                                        R.string.create_meeting
                                    }) {
                                        R.string.str_zhdd
                                    })//context.getString(R.string.create_meeting)
                                }
                                APP_JOIN_CONF -> resources.getString(
                                    if (isInConf) if (isSendAss) R.string.str_stop_screen_share else R.string.str_screen_share
                                    else modelReturn({
                                        if (isOpenAPS) R.string.join_meeting
                                        else R.string.call
                                    }, {
                                        R.string.call
                                    }) {
                                        R.string.call
                                    }//mcModel({ R.string.call }) { R.string.join_meeting }
                                )
                                APP_CONTACT -> resources.getString(R.string.str_contact)
                                APP_VOD -> resources.getString(R.string.str_vod)
                                else -> ""
                            }
                        }
                        else -> loadAppName(it)
                    }
                    EmAppType.Add -> if (type) resources.getString(R.string.str_soft_add) else ""
                    else -> loadAppName(it)
                }.toString()
                emit(name)
            }.flowOn(Dispatchers.IO).collect { textView.text = it }
        }
    }
}

@SuppressLint("InlinedApi")
private fun loadAppName(it: AppInfoEntity) = if (isAppInstalled(context, it.packageName)) {
    it.packageName.let {
        if (it.isBlank()) ""
        else context.packageManager.getApplicationInfo(
            it,
            PackageManager.MATCH_UNINSTALLED_PACKAGES
        ).loadLabel(context.packageManager)
    } ?: ""
} else ""

@SuppressLint("UseCompatLoadingForDrawables")
@BindingAdapter("packageName", "clazz", "appType")
fun loadIcon(imageView: ImageView, packageName: String?, clazz: String?, appType: EmAppType?) {
    imageView.findViewTreeLifecycleOwner()?.lifecycleScope?.launch {
        flow {
            when (appType) {
                EmAppType.tp -> when(packageName?:"") {
                    PACKAGE_TOUCH_DATA -> bpModel({ R.drawable.selector_touchdata }) {R.drawable.selector_bp_touchdata}
                    PACKAGE_FILE_EXPLORER -> bpModel({  R.drawable.selector_file_management }) { R.drawable.selector_bp_file_management }
                    PACKAGE_LAUNCHER -> when(clazz?:"") {
                        APP_FOR_SCREEN -> bpModel({ R.drawable.selector_screen_casting }) {R.drawable.selector_bp_screen_casting}
                        else -> 0
                    }
                    PACKAGE_CONFERENCE -> when(clazz?:"") {
                        APP_CREATE_CONF -> {
                            if (isInConf) bpModel({ R.drawable.selector_conf_back }) { R.drawable.selector_bp_conf_back }
                            else bpModel({
                                imageView.commandDispatcherModel({ R.drawable.selector_icon_zhdd }) { R.drawable.selector_conf_start }
                            })  {
                                imageView.commandDispatcherModel({ R.drawable.selector_icon_zhdd_bp }) { R.drawable.selector_bp_create_conf }
                            }
                        }
                        APP_JOIN_CONF ->{
                            if (isInConf)
                                if (isSendAss) bpModel({ R.drawable.selector_ass_send_stop }) { R.drawable.selector_bp_ass_send_stop }
                                else  bpModel({ R.drawable.selector_ass_send_start }) { R.drawable.selector_bp_ass_send_start }
                            else bpModel({ mcModel({ R.drawable.selector_dial_entrance_common}) {
                                if (isOpenAPS) R.drawable.selector_conf_join else R.drawable.selector_dial_entrance_common
                            } }) {
                                mcModel({ R.drawable.selector_bp_icon_dial }) {R.drawable.selector_bp_conf_join } }
                        }
                        APP_CONTACT -> bpModel({ R.drawable.selector_contact }) { R.drawable.selector_bp_contact }
                        APP_VOD -> bpModel({ R.drawable.selector_vod }) { R.drawable.selector_bp_vod }
                        else -> 0
                    }
                    PACKAGE_HELP -> bpModel({ R.drawable.selector_helper }) { R.drawable.selector_bp_helper }
                    PACKAGE_EXPLORER -> bpModel({ R.drawable.selector_explorer }) {R.drawable.selector_bp_explorer}
                    PACKAGE_DIAGNOSIS -> bpModel({ R.drawable.selector_diagnosis }) { R.drawable.selector_bp_diagnosis }
                    PACKAGE_SETTING -> bpModel({ R.drawable.selector_setting }) { R.drawable.selector_bp_setting }
                    PACKAGE_WELCOME_SIGN -> {
                        bpModel({ R.drawable.selector_welcome_sign }) { R.drawable.selector_bp_welcome_sign }
                    }
                    else -> packageName.appIcon(imageView.measuredWidth, imageView.measuredHeight)
                }
                EmAppType.Add -> R.drawable.selector_add
                else -> packageName.appIcon(imageView.measuredWidth, imageView.measuredHeight)
            }.let { it ->
                val resource = imageView.resources
                if (it is Int && it != 0) resource.getDrawable(it, null).let { emit(it) }
                else if (it is Bitmap) RoundedBitmapDrawableFactory.create(resource, it).apply { cornerRadius = context.resources.getDimension(R.dimen.dp10) }.let { emit(it) }
                else {
                    emit(null)
                }
            }
        }.flowOn(Dispatchers.IO).collect { it ->
            it.takeIf { null != it }?.let{
                imageView.apply { adjustViewBounds = true }.run {
                    setBackgroundResource(0)
                    setImageDrawable(it)
                    visibility = View.VISIBLE }
            }
        }
    }
}



private fun String?.appIcon(measuredWidth: Int, measuredHeight: Int): Any {
    val w = if (measuredWidth > 0) measuredWidth else context.resources.getDimension(R.dimen.dp24).toInt()
    val h = if (measuredHeight > 0) measuredHeight else context.resources.getDimension(R.dimen.dp24).toInt()
    return this?.let { it ->
        if (it.isBlank()) 0
        else {
            if (isAppInstalled(context, it)) context.packageManager.getApplicationIcon(it).let {
                createBitmap(
                    w,
                    h,
                    if (it.opacity != PixelFormat.OPAQUE) Bitmap.Config.ARGB_8888 else Bitmap.Config.RGB_565
                ).apply {
                    val canvas = Canvas(this)
                    val paint = Paint().apply { isAntiAlias = true }
                    val rect = Rect(0, 0, w, h)
                    val rectF = RectF(rect)
                    val radius = context.resources.getDimension(R.dimen.dp10)
                    canvas.drawRoundRect(rectF, radius, radius, paint)
                    paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
                    it.setBounds(0, 0, w, h)
                    it.draw(canvas)
                }
            } else 0
        }
    } ?: 0
}

fun isAppInstalled(context: Context, packageName: String): Boolean {
    return try {
        val packageManager = context.packageManager
        packageManager.getPackageInfo(packageName, 0)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        e.printStackTrace()
        false
    }
}

@BindingAdapter("imgBitmap")
fun imgBitmap(imageView: ImageView, imgBitmap: Bitmap?) {
    if (null != imgBitmap) imageView.setImageBitmap(imgBitmap)
    else imageView.setImageResource(0)
}
@BindingAdapter("bitmap", "appType")
fun imgBitmap(imageView: ImageView, bitmap: Bitmap?, appType: EmAppType?) {
    if (appType == EmAppType.Add) {
        imageView.setImageResource(R.drawable.selector_add)
    } else {
        if (null != bitmap) imageView.setImageBitmap(bitmap)
        else imageView.setImageResource(0)

    }
}

@SuppressLint("NewApi")
@BindingAdapter("colorByIntRes")
fun textColorByIntRes(view: TextView, colorByIntRes: Int) {
    view.setTextColor(context.resources.getColorStateList(colorByIntRes, null))
}