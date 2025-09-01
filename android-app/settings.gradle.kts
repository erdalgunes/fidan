pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Fidan"
include(":app")
include(":core:data")
include(":core:domain")
include(":core:ui")
include(":features:home")
include(":features:timer")
include(":features:forest")
include(":features:settings")