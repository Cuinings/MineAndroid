import java.io.File

// 定义模块类型和对应的插件
val androidApplicationModules = listOf(
    ":Sample:Test",
    ":Sample:remote_library_test_one",
    ":Sample:remote_library_test_two"
)

val androidLibraryModules = listOf(
    ":Library:Remote:Andlinker",
    ":Library:Common:Application",
    ":Library:Common:Activity",
    ":Library:Common:Fragment",
    ":Library:Common:Service",
    ":Library:Common:ViewModel",
    ":Library:Common:Popupwindow",
    ":Library:Common:WebView",
    ":Library:Common:View:Microphone",
    ":Library:Common:View:EditText",
    ":Library:Common:View:Loading",
    ":Library:Common:View:TextView:Page",
    ":Library:Common:View:TextView:Marquee",
    ":Library:Common:View:RecyclerView:Adapter",
    ":Library:Common:View:Icon:Battery",
    ":Library:Common:Dpi",
    ":Library:Common:Drawable:DSL",
    ":Library:Common:Color",
    ":Library:Common:Flow",
    ":Library:Utils:NetWork"
)

val kotlinLibraryModules = listOf(
    ":Library:Remote:Msg:Subscriber:Annotation",
    ":Library:Remote:Msg:Subscriber:Processor",
    ":Library:Remote:Msg:Router:Client",
    ":Library:Remote:Msg:Router:Service",
    ":Library:Utils:Throttle",
    ":Library:Utils:Debounce",
    ":Library:Utils:Email",
    ":Library:Utils:IP",
    ":Library:Utils:Gson",
    ":Library:Utils:Domain"
)

// 更新Android应用模块
task updateAndroidApplicationModules {
    description = "Update Android Application modules to use custom plugin"
    doLast {
        androidApplicationModules.forEach { modulePath ->
            val moduleDir = file(modulePath.replace(":", File.separator).substring(1))
            val buildGradleFile = File(moduleDir, "build.gradle.kts")
            
            if (buildGradleFile.exists()) {
                println("Updating $modulePath...")
                var content = buildGradleFile.readText()
                
                // 替换插件配置
                content = content.replace(
                    Regex("plugins\s*\{[\s\S]*?alias\(libs\.plugins\.android\.application\)[\s\S]*?alias\(libs\.plugins\.kotlin\.android\)[\s\S]*?\}"),
                    "plugins {
    id(\"mineandroid.android.application\")")
                
                buildGradleFile.writeText(content)
                println("Updated $modulePath")
            }
        }
    }
}

// 更新Android库模块
task updateAndroidLibraryModules {
    description = "Update Android Library modules to use custom plugin"
    doLast {
        androidLibraryModules.forEach { modulePath ->
            val moduleDir = file(modulePath.replace(":", File.separator).substring(1))
            val buildGradleFile = File(moduleDir, "build.gradle.kts")
            
            if (buildGradleFile.exists()) {
                println("Updating $modulePath...")
                var content = buildGradleFile.readText()
                
                // 替换插件配置
                content = content.replace(
                    Regex("plugins\s*\{[\s\S]*?alias\(libs\.plugins\.android\.library\)[\s\S]*?alias\(libs\.plugins\.kotlin\.android\)[\s\S]*?\}"),
                    "plugins {
    id(\"mineandroid.android.library\")")
                
                buildGradleFile.writeText(content)
                println("Updated $modulePath")
            }
        }
    }
}

// 更新Kotlin库模块
task updateKotlinLibraryModules {
    description = "Update Kotlin Library modules to use custom plugin"
    doLast {
        kotlinLibraryModules.forEach { modulePath ->
            val moduleDir = file(modulePath.replace(":", File.separator).substring(1))
            val buildGradleFile = File(moduleDir, "build.gradle.kts")
            
            if (buildGradleFile.exists()) {
                println("Updating $modulePath...")
                var content = buildGradleFile.readText()
                
                // 替换插件配置
                content = content.replace(
                    Regex("plugins\s*\{[\s\S]*?id\(\"java-library\"\)[\s\S]*?alias\(libs\.plugins\.jetbrains\.kotlin\.jvm\)[\s\S]*?\}"),
                    "plugins {
    id(\"mineandroid.kotlin.library\")")
                
                buildGradleFile.writeText(content)
                println("Updated $modulePath")
            }
        }
    }
}

// 主任务，更新所有模块
task updateAllModules {
    description = "Update all modules to use custom plugins"
    dependsOn(updateAndroidApplicationModules, updateAndroidLibraryModules, updateKotlinLibraryModules)
}
