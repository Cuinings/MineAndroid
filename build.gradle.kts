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

// ─── 多语言翻译合并 ───────────────────────────
// 将各模块 translations/{lang}.json 合并到 board/proxy/assets/translations/
// APK仅含中文XML，其他语言从assets JSON运行时加载
val mergeTranslations by tasks.registering {
    group = "translations"
    description = "合并各模块 translations/*.json → proxy/assets/translations/"

    doLast {
        val script = file("tools/merge_translations.py")
        if (!script.exists()) {
            throw GradleException("翻译合并脚本不存在: $script")
        }
        exec {
            workingDir = rootProject.projectDir
            commandLine("python", script.absolutePath)
        }
    }
}

// proxy 模块的 processResources 依赖合并任务
gradle.projectsEvaluated {
    subprojects {
        tasks.matching { it.name == "processResources" || it.name.startsWith("preBuild") }.configureEach {
            dependsOn(mergeTranslations)
        }
    }
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