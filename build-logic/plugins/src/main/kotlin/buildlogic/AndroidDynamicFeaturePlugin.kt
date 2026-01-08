package buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import kotlin.jvm.java

class AndroidDynamicFeaturePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.pluginManager.apply(AndroidDynamicFeatureConventionPlugin::class.java)
    }
}