package buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply

/**
 * Kotlin库插件，用于配置Kotlin库模块的通用设置
 */
class KotlinLibraryPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.pluginManager.apply(KotlinLibraryConventionPlugin::class.java)
    }
}
