package buildlogic

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.DynamicFeatureExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Project
import org.gradle.api.JavaVersion
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

/**
 * Configures Android SDK versions for application projects
 */
internal fun Project.configureAndroidSdkVersions(extension: ApplicationExtension) {
    extension.apply {
        compileSdk = 35
        
        defaultConfig {
            minSdk = 21
            targetSdk = 35
        }
        
        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }
    }
    
    configureKotlinJvmTarget()
}

/**
 * Configures Android SDK versions for library projects
 */
internal fun Project.configureAndroidSdkVersions(extension: LibraryExtension) {
    extension.apply {
        compileSdk = 35
        
        defaultConfig {
            minSdk = 21
            targetSdk = 35
        }
        
        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }
    }
    
    configureKotlinJvmTarget()
}

/**
 * Configures Android SDK versions for dynamic feature projects
 */
internal fun Project.configureAndroidSdkVersions(extension: DynamicFeatureExtension) {
    extension.apply {
        compileSdk = 35
        
        defaultConfig {
            minSdk = 21
        }
        
        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }
    }
    
    configureKotlinJvmTarget()
}

/**
 * Configures Kotlin JVM target for all Kotlin compile tasks
 */
private fun Project.configureKotlinJvmTarget() {
    tasks.withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = "17"
        }
    }
}