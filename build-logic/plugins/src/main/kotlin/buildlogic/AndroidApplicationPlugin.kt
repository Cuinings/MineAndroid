package buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply

/**
 * Android应用插件，用于配置Android应用模块的通用设置
 */
class AndroidApplicationPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.pluginManager.apply(AndroidApplicationConventionPlugin::class.java)
    }
}
