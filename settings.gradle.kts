pluginManagement {
    includeBuild("build-logic")
}

plugins {
    id("org.sonarsource.cloud-native.common-settings")
}

rootProject.name = "sonar-go"

include(":sonar-go-to-slang")
include(":sonar-go-plugin")
include(":its:plugin")
include(":its:ruling")
