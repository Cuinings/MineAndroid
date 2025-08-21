plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
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
    }
}

dependencies {

    implementation(project(":Library:Common:Activity"))
    implementation(project(":Library:Common:ViewModel"))
    implementation(project(":Library:Common:Application"))
    implementation(project(":Library:Common:Popupwindow"))
    implementation(project(":Library:Common:Dpi"))
    implementation(project(":Library:Common:Color"))
    implementation(project(":Library:Common:EditText"))
    implementation(project(":Library:Common:RecyclerView:Adapter"))
    implementation(project(":Library:Common:MicEnergyView"))
    implementation(project(":Library:Common:Flow"))

    implementation(project(":Library:Utils:Debounce"))
    implementation(project(":Library:Utils:Throttle"))

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