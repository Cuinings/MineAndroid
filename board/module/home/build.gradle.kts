import org.gradle.kotlin.dsl.compileOnly

plugins {
    id("mineandroid.android.library")
    alias(libs.plugins.room)
}

android {
    namespace = "com.cn.board.meet.home"
    // compileSdk 用 35 仅为构建期能引用 framework 中的 SurfaceControl.Transaction 部分公开接口；
    // minSdk/targetSdk = 31（运行目标 Android 31，系统应用）。addView / getSurfaceControl /
    // setRelativeLayer / show / View.getHostToken 等仍为 @hide，运行时由系统权限豁免、经反射调用。
    // 注意：最终打包的 launcher app 的 minSdk 也必须 >= 31。
    compileSdk = 35

    defaultConfig {
        // 升到 31：与 compileSdk 对齐，并直接支持 SurfaceControlViewHost 把卡片 UI
        // 抬到 ABOVE_WINDOW 的视频之上（「毛玻璃模糊 + SurfaceView 视频清晰 + 上层 UI 可见」）。
        minSdk = 21

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
        dataBinding = true
    }
}

// Room 配置
room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {

    implementation(project(":core:core-ui"))
    implementation(project(":core:core-utils"))
    implementation(project(":core:core-task"))
    implementation(project(":core:core-resources"))
    // 依赖database模块
    implementation(project(":board:module:database"))
    implementation(project(":board:module:wallpaper"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    
    // Room 依赖
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)

    // Kotlin 协程依赖
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // 内存泄露检测工具
    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.12")

    implementation("com.github.Dimezis:BlurView:version-3.2.0")

}