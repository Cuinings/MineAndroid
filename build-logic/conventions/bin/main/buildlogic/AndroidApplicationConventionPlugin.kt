package buildlogic

import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.pluginManager.apply("com.android.application")
        project.pluginManager.apply("org.jetbrains.kotlin.android")
        project.pluginManager.apply(CodeQualityConventionPlugin::class.java)
        // 应用发布插件
        project.pluginManager.apply(PublishingConventionPlugin::class.java)
        
        // 配置 Android SDK 版本
        val extension = project.extensions.getByType(ApplicationExtension::class.java)
        project.configureAndroidSdkVersions(extension)
    }
}
