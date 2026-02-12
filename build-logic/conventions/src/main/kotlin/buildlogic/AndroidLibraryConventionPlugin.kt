package buildlogic

import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.pluginManager.apply("com.android.library")
        project.pluginManager.apply("org.jetbrains.kotlin.android")
        project.pluginManager.apply("org.jetbrains.kotlin.kapt")
        project.pluginManager.apply(CodeQualityConventionPlugin::class.java)
        // 应用发布插件
        project.pluginManager.apply(PublishingConventionPlugin::class.java)
        
        // 配置 Android SDK 版本
        val extension = project.extensions.getByType(LibraryExtension::class.java)
        project.configureAndroidSdkVersions(extension)
    }
}
