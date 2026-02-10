plugins {
    id("mineandroid.kotlin.library")
    alias(libs.plugins.kotlin.kapt)
}

dependencies {

    implementation(project(":core:core-remote:msg-subscriber-annotation"))

    implementation(libs.squareup.javapoet)
    implementation(libs.squareup.kotlinpoet)

    implementation(libs.google.auto.service)
    kapt(libs.google.auto.service)

}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                groupId = "com.cn.core"
                artifactId = "msg-subscriber-processor"
                version = "1.0.0"

                // 从Java发布组件生成
                from(components["java"])
            }
        }
        repositories {
            maven {
                url = uri("${rootDir}/maven-repo")
            }
        }
    }
}
