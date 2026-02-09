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
        mavenLocal().apply { url = uri("$rootDir\\maven-repo") }
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
        mavenLocal().apply { url = uri("$rootDir\\maven-repo") }
    }
}

rootProject.name = "MineAndroid"

//include(":Sample:Test")
//include(":Sample:remote_library_test_one")
//include(":Sample:remote_library_test_two")

include(":core:core-resources")
include(":core:core-ui")
include(":core:core-utils")
include(":demo:app-test")
include(":demo:app_test_resources")
include(":demo:other-test")
include(":launcher")
include(":board:meet")
include(":core:core-remote:andlinker")
include(":core:core-remote:msg-router-client")
include(":core:core-remote:msg-router-service")
include(":core:core-remote:msg-subscriber-annotation")
include(":core:core-remote:msg-subscriber-processor")
