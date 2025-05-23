plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt)
    alias(libs.plugins.room)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.google.devtools.ksp)
}

android {
    namespace = "com.cn.mine.wan.android"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.cn.mine.wan.android"
        minSdk = 24
        targetSdk = 34
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
        buildConfig = true
    }
    room {
        schemaDirectory("$projectDir/schemas")
    }
}

dependencies {

    implementation(project(":Mine:WanAndroid:Data"))

    implementation(project(":Library:Common:Activity"))
    implementation(project(":Library:Common:ViewModel"))
    implementation(project(":Library:Common:Application"))
    implementation(project(":Library:Common:Popupwindow"))
    implementation(project(":Library:Common:Dpi"))
    implementation(project(":Library:Common:Color"))
    implementation(project(":Library:Common:RecyclerView:Adapter"))

    implementation(project(":Library:Utils:Debounce"))
    implementation(project(":Library:Utils:Throttle"))

    implementation(project(":Library:Remote:Msg:Router:Client"))

    implementation(project(":Library:Remote:Msg:Subscriber:Annotation"))
    kapt(project(":Library:Remote:Msg:Subscriber:Processor"))

    implementation(libs.tencent.tbs.sdk)

    implementation(libs.squareup.retrofit)
    implementation(libs.squareup.retrofit.converter.gson)
    implementation(libs.squareup.okhttp3)
    implementation(libs.squareup.okhttp3.logging.interceptor)

    implementation(libs.androidx.swiperefreshlayout)

    implementation(libs.androidx.lifecycle.viewmodel)

    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)

    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)

    implementation(libs.androidx.cardview)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}