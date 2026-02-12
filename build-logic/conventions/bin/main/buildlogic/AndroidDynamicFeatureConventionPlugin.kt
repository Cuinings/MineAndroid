package buildlogic

import com.android.build.api.dsl.DynamicFeatureExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

class AndroidDynamicFeatureConventionPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.pluginManager.apply("com.android.dynamic-feature")
        project.pluginManager.apply("org.jetbrains.kotlin.android")
        project.pluginManager.apply(CodeQualityConventionPlugin::class.java)
        
        // 配置 Android SDK 版本
        val extension = project.extensions.getByType(DynamicFeatureExtension::class.java)
        project.configureAndroidSdkVersions(extension)
    }
}