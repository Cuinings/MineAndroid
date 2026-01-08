// 统一版本号管理
val libraryVersion = "1.0.0"
val libraryGroupId = "com.cn.core"

// 批量发布core模块任务
tasks.register("publishCoreModules") {
    group = "publishing"
    description = "发布所有core模块到local_maven"
    
    // 添加任务依赖，直接依赖各个模块的发布任务
    dependsOn(
        ":core:core-resources:publishReleasePublicationToMavenRepository",
        ":core:core-ui:publishReleasePublicationToMavenRepository",
        ":core:core-utils:publishReleasePublicationToMavenRepository"
    )
    
    doLast {
        println("\n开始发布所有core模块到local_maven...")
        println("版本: $libraryVersion")
        println("Group ID: $libraryGroupId")
        println("\n所有core模块发布完成！")
        println("发布路径: ${rootDir}/build/maven-repo")
    }
}

// 清理local_maven任务
tasks.register("cleanCoreMaven") {
    group = "publishing"
    description = "清理core模块的maven发布目录"
    
    doLast {
        val localMavenDir = file("${rootDir}/build/maven-repo/com/cn/core")
        if (localMavenDir.exists()) {
            localMavenDir.deleteRecursively()
            println("已清理core模块的maven发布目录")
        } else {
            println("core模块的maven发布目录不存在")
        }
    }
}
