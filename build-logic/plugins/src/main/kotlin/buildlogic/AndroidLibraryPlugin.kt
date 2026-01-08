package buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply

/**
 * Android库插件，用于配置Android库模块的通用设置
 */
class AndroidLibraryPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.pluginManager.apply(AndroidLibraryConventionPlugin::class.java)
    }
}
