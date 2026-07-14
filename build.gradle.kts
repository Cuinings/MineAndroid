// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.kapt) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.parcelize) apply false
    alias(libs.plugins.google.devtools.ksp) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.protobuf) apply false
    alias(libs.plugins.room) apply false
}


// system.jar bootstrap classpath — @hide API 直接编译
gradle.projectsEvaluated {
    tasks.withType(JavaCompile::class.java).configureEach {
        val newClasspath = mutableListOf<File>()
        newClasspath.add(file("libs/system.jar"))
        newClasspath.addAll(options.bootstrapClasspath?.files ?: emptyList())
        options.bootstrapClasspath = files(newClasspath.toTypedArray())
    }
}

tasks {
    val clean by registering(Delete::class) {
        delete("$projectDir\\build")
    }
}

configurations.all {
    resolutionStrategy {
        cacheChangingModulesFor(0, "seconds")
    }
}