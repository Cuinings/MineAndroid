package buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * 代码质量检测约定插件，用于配置detekt
 */
class CodeQualityConventionPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        // 暂时移除detekt插件，避免jvm-target配置问题
        // project.pluginManager.apply("io.gitlab.arturbosch.detekt")
    }
}
