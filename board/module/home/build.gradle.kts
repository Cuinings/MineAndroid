import org.gradle.kotlin.dsl.compileOnly

plugins {
    id("mineandroid.android.library")
    alias(libs.plugins.room)
}

android {
    namespace = "com.cn.board.meet.home"
    compileSdk = 35

    defaultConfig {
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