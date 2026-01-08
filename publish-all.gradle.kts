// 统一版本号管理
val libraryVersion = "1.0.0"
val libraryGroupId = "com.cn.library"

// 批量发布任务
tasks.register("publishAllModules") {
    group = "publishing"
    description = "发布所有模块到local_maven"
    
    doLast {
        println("开始发布所有模块到local_maven...")
        println("版本: $libraryVersion")
        println("Group ID: $libraryGroupId")
        
        // 定义需要发布的模块列表
        val modulesToPublish = listOf(
            ":Library:Common:Activity",
            ":Library:Common:Application",
            ":Library:Common:Fragment"
        )
        
        // 执行每个模块的发布命令
        modulesToPublish.forEach { modulePath ->
            println("\n发布模块: $modulePath")
            
            // 使用exec执行gradle命令
            exec {
                commandLine = listOf(
                    "gradlew", 
                    "$modulePath:publishReleasePublicationToMavenLocal",
                    "--no-build-cache"
                )
                workingDir = rootDir
            }
        }
        
        println("\n所有模块发布完成！")
        println("发布路径: ${rootDir}/local_maven")
    }
}

// 清理local_maven任务
tasks.register("cleanLocalMaven") {
    group = "publishing"
    description = "清理local_maven目录"
    
    doLast {
        val localMavenDir = file("${rootDir}/local_maven")
        if (localMavenDir.exists()) {
            localMavenDir.deleteRecursively()
            println("已清理local_maven目录")
        } else {
            println("local_maven目录不存在")
        }
    }
}
