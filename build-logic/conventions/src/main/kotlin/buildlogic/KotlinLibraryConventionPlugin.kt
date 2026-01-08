package buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project

class KotlinLibraryConventionPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.pluginManager.apply("java-library")
        project.pluginManager.apply("org.jetbrains.kotlin.jvm")
        project.pluginManager.apply(CodeQualityConventionPlugin::class.java)
        // 应用发布插件
        project.pluginManager.apply(PublishingConventionPlugin::class.java)
    }
}
