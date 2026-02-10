plugins {
    id("mineandroid.kotlin.library")
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                groupId = "com.cn.core"
                artifactId = "msg-subscriber-annotation"
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
