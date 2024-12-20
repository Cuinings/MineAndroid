pluginManagement {
    repositories {
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
        google()
        mavenCentral()
        mavenLocal().apply { url = uri("$rootDir\\local_maven") }
    }
}

rootProject.name = "MineAndroid"
include(":Library:Common:Debounce")
include(":Library:Common:Throttle")
include(":Library:Common:Dpi")
include(":Library:Common:Application")
include(":Library:Common:Activity")
include(":Library:Common:Color")
include(":Library:Common:Popupwindow")
//include(":Library:Processor:Bind:Msg:Annotation")
//include(":Library:Processor:Bind:Msg:compiler")
include(":Mine:Sample")
//include(":Mine:Compose-Sample")
//include(":Mine:Launcher")
include(":Mine:Hilt-Sample")
include(":Mine:WanAndroid")
