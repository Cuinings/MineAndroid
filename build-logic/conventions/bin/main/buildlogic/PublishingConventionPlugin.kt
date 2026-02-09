package buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * 发布约定插件，用于统一管理maven-publish配置
 */
class PublishingConventionPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        // 仅应用maven-publish插件，具体发布配置由模块自行管理
        project.pluginManager.apply("maven-publish")
    }
}
