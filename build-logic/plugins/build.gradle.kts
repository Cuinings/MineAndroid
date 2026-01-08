plugins {
    `kotlin-dsl`
    id("java-gradle-plugin")
}

dependencies {
    implementation(gradleApi())
    implementation(project(":conventions"))
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

// 配置插件发布信息
gradlePlugin {
    plugins {
        create("androidApplication") {
            id = "mineandroid.android.application"
            implementationClass = "buildlogic.AndroidApplicationPlugin"
            displayName = "MineAndroid Android Application Plugin"
            description = "Configures common settings for Android Application modules"
        }
        
        create("androidLibrary") {
            id = "mineandroid.android.library"
            implementationClass = "buildlogic.AndroidLibraryPlugin"
            displayName = "MineAndroid Android Library Plugin"
            description = "Configures common settings for Android Library modules"
        }
        
        create("kotlinLibrary") {
            id = "mineandroid.kotlin.library"
            implementationClass = "buildlogic.KotlinLibraryPlugin"
            displayName = "MineAndroid Kotlin Library Plugin"
            description = "Configures common settings for Kotlin Library modules"
        }
    }
}
