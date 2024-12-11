pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

rootProject.name = "sonar-go"

include(":sonar-go-to-slang")
include(":sonar-go-plugin")
include(":its:plugin")
include(":its:ruling")
