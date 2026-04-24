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
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "borshchevyk-android"
include(":app")
include(":core:model")
include(":core:domain:auth")
include(":core:data:auth")
include(":core:domain:user")
include(":core:data:user")
include(":core:domain:chat")
include(":core:domain:message")
include(":core:data:chat")
include(":core:data:message")
include(":core:network:client")
include(":core:network:auth")
include(":core:network:chat")
include(":core:network:message")
include(":core:network:user")
include(":core:network:media")
include(":core:network:websocket")
include(":core:security")
include(":core:ui")
include(":feature:auth")
include(":feature:chat")
include(":feature:profile")
include(":feature:search")
 