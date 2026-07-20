package com.cn.board.meet.home.app



object ActivityClassName {

    //设置
    const val APP_SETTING: String = "com.tp.kdsetting.ui.view.main.MainConfigActivity"

    //文件管理
    const val APP_FILE_EXPLORER: String =
        "com.tp.fileexplorer.activities.NewFileExplorerActivity"

    //协作
    const val APP_TOUCH_DATA: String = "com.tp.touchdata.NewTouchDataActivity"

    //创建会议
    const val APP_CREATE_CONF: String = "com.tp.conference.confcontrol.create"

    //加入会议
    const val APP_JOIN_CONF: String = "com.tp.conference.confcontrol.join.JoinActivity"

    //通讯录
    const val APP_CONTACT: String = "com.tp.conference.confcontrol.contract.ContactActivity"

    //录播
    const val APP_VOD: String = "com.tp.conference.confcontrol.vod.VideoDemandActivity"

    //help帮助界面
    const val APP_HELP: String = "com.tp.kdhelpdoc.ui.HelpActivity"

    //投屏
    const val APP_FOR_SCREEN: String = "com.tp.imixlauncher.app.page.forscreen.ForScreenActivity"

    //诊断
    const val APP_DIAGNOSIS: String = "com.tp.diagnosis.activity.DiagnosisActivity"

    //浏览器
    const val APP_CHROME: String = "org.chromium.chrome.browser.ChromeTabbedActivity"
    const val APP_WELCOME_SIGN: String = "com.tp.welcomesign.MainActivity"
    const val APP_WPS_HOME: String = "cn.wps.moffice.main.local.home.PadHomeActivity"

}

object PackageName {
    const val PACKAGE_LAUNCHER: String = "com.tp.imixlauncher"

    //会议
    const val PACKAGE_CONFERENCE: String = "com.tp.conference"

    //系统系统
    const val PACKAGE_SETTING: String = "com.tp.kdsetting"

    //文件管理
    const val PACKAGE_FILE_EXPLORER: String = "com.tp.fileexplorer"

    //浏览器
    const val PACKAGE_EXPLORER: String = "org.chromium.chrome"

    //诊断
    const val PACKAGE_DIAGNOSIS: String = "com.tp.diagnosis"

    //白板
    const val PACKAGE_TOUCH_DATA: String = "com.tp.touchdata"

    //imixappcore
    const val PACKAGE_IMIXAPPCORE: String = "com.tp.imixappcore"

    //屏幕批注
    const val PACKAGE_SCREENDRAWING: String = "com.tp.screendrawing"

    //输入法
    const val PACKAGE_INPUT_METHOD: String = "com.tp.nvinputmethod"
    const val PACKAGE_MEDIA_PLAY: String = "com.tp.mediaplay"

    //airplay
    const val PACKAGE_AIR_PLAY: String = "com.tp.airplayer"

    //帮助
    const val PACKAGE_HELP: String = "com.tp.kdhelpdoc"

    //投屏
    const val PACKAGE_CAST: String = "com.tp.cast"
    const val PACKAGE_NATIVE_DISPLAY: String = "com.tp.nativedisplay"
    const val PACKAGE_WELCOME_SIGN: String = "com.tp.welcomesign"

    const val PACKAGE_SYSTEM_UPGRADE: String = "com.tp.systemupgrade"
    const val PACKAGE_WPS: String = "cn.wps.moffice_eng"
}
