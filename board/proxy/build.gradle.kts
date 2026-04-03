plugins {
    id("mineandroid.android.application")
}

android {
    namespace = "com.cn.board.proxy"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.cn.board.proxy"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("boolean", "ENABLE_HOME", "true")
            buildConfigField("boolean", "ENABLE_MEET", "false")
            buildConfigField("boolean", "ENABLE_CONTACTS", "false")
            buildConfigField("boolean", "ENABLE_WALLPAPER", "false")
            buildConfigField("String", "DEFAULT_HOME_MODULE", "\"home\"")
        }
        debug {
            buildConfigField("boolean", "ENABLE_HOME", "true")
            buildConfigField("boolean", "ENABLE_MEET", "false")
            buildConfigField("boolean", "ENABLE_CONTACTS", "false")
            buildConfigField("boolean", "ENABLE_WALLPAPER", "false")
            buildConfigField("String", "DEFAULT_HOME_MODULE", "\"home\"")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    
    // 可选模块依赖
    implementation(project(":board:module:home"))
    implementation(project(":board:module:meet"))
    implementation(project(":board:module:contacts"))
    implementation(project(":board:module:wallpaper"))
    implementation(libs.core.ui)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}