plugins {
    id("mineandroid.android.library")
}

android {
    namespace = "com.cn.board.home"
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

dependencies {

    // 子模块聚合（对外暴露统一入口）
    api(project(":board:module:home-data"))
    api(project(":board:module:home-domain"))
    api(project(":board:module:home-presentation"))

    implementation(project(":core:core-ui"))
    implementation(project(":core:core-utils"))
    implementation(project(":core:core-task"))
    implementation(project(":core:core-resources"))
    implementation(project(":board:module:wallpaper"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.constraintlayout)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.12")
    implementation("com.github.Dimezis:BlurView:version-3.2.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

}
