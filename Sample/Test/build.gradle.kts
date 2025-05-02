plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
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
    implementation(project(":Library:Common:Application"))
    implementation(project(":Library:Common:Popupwindow"))
    implementation(project(":Library:Common:Dpi"))
    implementation(project(":Library:Common:Color"))
    implementation(project(":Library:Common:Throttle"))
    implementation(project(":Library:Common:Debounce"))
    implementation(project(":Library:Common:EditText"))
    implementation(project(":Library:Common:RecyclerView:Adapter"))

    implementation(libs.google.code.gson)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}