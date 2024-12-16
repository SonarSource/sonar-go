pluginManagement {
    includeBuild("build-logic")
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("com.gradle.develocity") version "3.18.2"
    id("com.diffplug.blowdryerSetup") version "1.7.1"
}

develocity {
    server = "https://develocity.sonar.build"
    buildScan {
        tag(if (System.getenv("CI").isNullOrEmpty()) "local" else "CI")
        tag(System.getProperty("os.name"))
        if (System.getenv("CIRRUS_BRANCH") == "master") {
            tag("master")
        }
        if (System.getenv("CIRRUS_PR")?.isBlank() == false) {
            tag("PR")
        }
        value("Build Number", System.getenv("BUILD_NUMBER"))
        value("Branch", System.getenv("CIRRUS_BRANCH"))
        value("PR", System.getenv("CIRRUS_PR"))
    }
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
