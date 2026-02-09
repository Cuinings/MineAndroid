package buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import kotlin.jvm.java

class AndroidDynamicFeatureConventionPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.pluginManager.apply("com.android.dynamic-feature")
        project.pluginManager.apply("org.jetbrains.kotlin.android")
        project.pluginManager.apply(CodeQualityConventionPlugin::class.java)
    }
}