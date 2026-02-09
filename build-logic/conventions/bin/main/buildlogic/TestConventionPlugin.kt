package buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
/**
 * @author: cn
 * @time: 2026/1/8 12:25
 * @history
 * @description:
 */
class TestConventionPlugin: Plugin<Project> {
    override fun apply(project: Project) {
        // 只添加测试依赖，不配置复杂的测试任务或 Jacoco
    }
}