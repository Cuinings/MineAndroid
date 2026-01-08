plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    id("maven-publish")
}

android {
    namespace = "com.cn.library.common.fragment"
    compileSdk = 35

    defaultConfig {
        minSdk = 24

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
    buildFeatures {
        dataBinding = true
        viewBinding = true
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    // 添加到 android 块
    publishing {
        singleVariant("release") {}
    }
}

dependencies {
    implementation(project(":Library:Common:ViewModel"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
//    testImplementation(libs.androidx.fragment)
    testImplementation(libs.androidx.fragment.ktx)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = "com.cn.library.common"
            artifactId = "fragment"
            version = "1.0.0-SNAPSHOT"
            afterEvaluate{ from(components["release"]) }
        }
    }
    repositories {
        maven(url = uri("$rootDir\\local_maven"))
    }
}