Mine Android
------
插件话架构
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

