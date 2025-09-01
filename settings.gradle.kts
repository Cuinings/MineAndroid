pluginManagement {
    includeBuild("build-logic")
    repositories {
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/central") }
        maven { url = uri("https://maven.aliyun.com/repository/jcenter") }
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        maven { url = uri("https://jitpack.io") }
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
        mavenLocal().apply { url = uri("$rootDir\\local_maven") }
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/central") }
        maven { url = uri("https://maven.aliyun.com/repository/jcenter") }
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        maven { url = uri("https://jitpack.io") }
        google()
        mavenCentral()
        mavenLocal().apply { url = uri("$rootDir\\local_maven") }
    }
}

rootProject.name = "MineAndroid"

include(":TP:Launcher")
include(":TP:Launcher:Model")
include(":TP:Launcher:Event")
include(":TP:Launcher:Repository")

include(":Sample:Test")
//include(":Sample:Hilt")

//include(":Sample:Design-Model")
include(":Mine:WanAndroid:App")
include(":Mine:WanAndroid:Events")
include(":Mine:WanAndroid:Repository")
include(":Mine:WanAndroid:Overlay")

include(":Library:Remote:Andlinker")
include(":Library:Remote:Msg:Subscriber:Annotation")
include(":Library:Remote:Msg:Subscriber:Processor")
include(":Library:Remote:Msg:Router:Client")

include(":Library:Remote:Msg:Router:Service")
include(":Library:Common:Application")
include(":Library:Common:Activity")
include(":Library:Common:Fragment")

include(":Library:Common:Service")
include(":Library:Common:ViewModel")
include(":Library:Common:Popupwindow")

include(":Library:Common:Dpi")

include(":Library:Common:Drawable:DSL")

include(":Library:Common:Color")
include(":Library:Common:EditText")
include(":Library:Common:Flow")
include(":Library:Common:RecyclerView:Adapter")
include(":Library:Utils:Throttle")
include(":Library:Utils:Debounce")
include(":Library:Utils:Email")
include(":Library:Utils:IP")
include(":Library:Utils:Gson")
include(":Library:Utils:NetWork")
include(":Library:Common:WebView")
include(":Library:Common:MicEnergyView")
