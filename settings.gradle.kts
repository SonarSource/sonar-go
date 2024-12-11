pluginManagement {
    includeBuild("build-logic")
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("com.gradle.develocity") version "3.18.2"
}

develocity {
    server = "https://develocity.sonar.build"
}

val isCI = System.getenv("CI") != null
buildCache {
    local {
        isEnabled = !isCI
    }
    remote(develocity.buildCache) {
        isEnabled = true
        isPush = isCI
    }
}

rootProject.name = "sonar-go"

include(":sonar-go-to-slang")
include(":sonar-go-plugin")
include(":its:plugin")
include(":its:ruling")
