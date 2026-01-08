plugins {
    id("mineandroid.android.application")
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.google.devtools.ksp)
}

android {
    namespace = "com.cn.sample.test"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.cn.sample.test"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        kapt {
            arguments {
                arg("appId", this@defaultConfig.applicationId!!)
            }
        }
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
        dataBinding = true
        viewBinding = true
    }
}

dependencies {

    implementation(project(":Library:Common:Activity"))
    implementation(project(":Library:Common:ViewModel"))
    implementation(project(":Library:Common:Application"))
    implementation(project(":Library:Common:Popupwindow"))
    implementation(project(":Library:Common:Dpi"))
    implementation(project(":Library:Common:Color"))
    implementation(project(":Library:Common:Flow"))

    implementation(project(":Library:Common:View:EditText"))
    implementation(project(":Library:Common:View:RecyclerView:Adapter"))
    implementation(project(":Library:Common:View:Microphone"))
    implementation(project(":Library:Common:View:TextView:Marquee"))
    implementation(project(":Library:Common:View:TextView:Page"))
    implementation(project(":Library:Common:View:Loading"))

    implementation(project(":Library:Utils:Domain"))
    implementation(project(":Library:Utils:Debounce"))
    implementation(project(":Library:Utils:Throttle"))

//    implementation(project(":Sample:nativelib"))

    implementation(libs.android.svg)
    implementation(libs.androidx.vectordrawable)
    implementation(libs.androidx.vectordrawable.animated)
    implementation(libs.androidx.palette)

    implementation(libs.google.code.gson)

    implementation(libs.androidx.lifecycle.viewmodel)

    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}