plugins {
    id("org.sonarsource.cloud-native.java-conventions")
    id("org.sonarsource.cloud-native.code-style-conventions")
    id("org.sonarsource.cloud-native.integration-test")
}

dependencies {
    "integrationTestImplementation"(libs.slf4j.api)
    "integrationTestImplementation"(libs.sonar.analyzer.commons)
    "integrationTestImplementation"(libs.assertj.core)
    "integrationTestImplementation"(libs.sonar.orchestrator.junit5) {
        exclude("ch.qos.logback", "logback-classic")
        exclude("org.slf4j", "slf4j-api")
        exclude("org.slf4j", "jcl-over-slf4j")
        exclude("org.slf4j", "log4j-over-slf4j")
    }
    "integrationTestImplementation"(libs.junit.jupiter.api)

    "integrationTestRuntimeOnly"(project(":sonar-go-plugin", configuration = "shadow"))
    "integrationTestRuntimeOnly"(libs.junit.jupiter.engine)
}

sonar.isSkipProject = true

integrationTest {
    testSources.set(rootProject.file("its/sources"))
}
tasks.named<Test>("integrationTest") {
    systemProperty("java.awt.headless", "true")
}
