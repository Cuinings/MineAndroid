pluginManagement {
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

include(":Sample:Test")
//include(":Sample:Hilt")
//include(":Sample:Design-Model")

include(":Mine:WanAndroid")

//include(":Mine:Wanandroid-compose")
include(":Library:Common:Debounce")
include(":Library:Common:Throttle")
include(":Library:Common:Dpi")
include(":Library:Common:Application")
include(":Library:Common:Activity")
include(":Library:Common:Color")
include(":Library:Common:Popupwindow")
include(":Library:Common:EditText")
include(":Library:Common:ViewModel")
include(":Library:Common:Drawable:DSL")
include(":Library:Data:WanAndroid")
include(":Library:Common:RecyclerView:Adapter")
