Mine Android
------
资源包案例
------
# 生成AAB文件
./gradlew bundleDebug

# 使用bundletool生成APK集（需要下载bundletool.jar）
# 下载bundletool.jar（如果没有）
# https://github.com/google/bundletool/releases
java -jar bundletool.jar build-apks --bundle=demo/app-test/build/outputs/bundle/debug/app-test-debug.aab --output=ap

# 安装APK集（自动安装主APK和所有动态功能模块）
java -jar tools/bundletool-all-1.18.3.jar build-apks --bundle=demo/app-test/build/outputs/bundle/debug/app-test-debug.aab --output=app-test.apks --local-testing
java -jar tools/bundletool-all-1.18.3.jar install-apks --apks=app-test.apks --adb=C:\Users\Work\AppData\Local\Android\Sdk\platform-tools\adb.exe

# 或手动分别安装（适用于测试单个模块）
# 1. 先安装主APK
adb install demo/app-test/build/outputs/apk/debug/app-test-debug.apk
# 2. 再安装资源包APK（路径根据实际生成的文件调整）
adb install demo/app_test_resources/build/outputs/apk/debug/app_test_resources-debug.apk

# 启动应用 查看结果

插件化架构
------
1、目录结构
build-logic/
├── settings.gradle.kts        # build-logic项目配置
├── conventions/               # 约定插件、定义模块通用配置
│   ├── build.gradle.kts
│   └── src/main/kotlin/buildlogic/
│       ├── AndroidApplicationConventionPlugin.kt
│       ├── AndroidLibraryConventionPlugin.kt
│       └── KotlinLibraryConventionPlugin.kt
└── plugins/                   # 插件实现 用户直接使用 提供简洁ID
├── build.gradle.kts
└── src/main/kotlin/buildlogic/
├── AndroidApplicationPlugin.kt
├── AndroidLibraryPlugin.kt
└── KotlinLibraryPlugin.kt
2、核心配置
settings.gradle.kts(根目录)
pluginManagement {
includeBuild("build-logic")  // 关键：引入build-logic
repositories { ... }
}
settings.gradle.kts(build-logic)
rootProject.name = "build-logic"
include(":conventions")
include(":plugins")
3、插件注册
gradlePlugin {
plugins {
create("androidApplication") {
id = "mineandroid.android.application"
implementationClass = "buildlogic.AndroidApplicationPlugin"
}
}
}
4、使用方式
// 之前
plugins {
alias(libs.plugins.android.application)
alias(libs.plugins.kotlin.android)
}
// 之后
plugins {
id("mineandroid.android.application")
}

