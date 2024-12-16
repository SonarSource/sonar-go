plugins {
    id("org.sonarsource.cloud-native.java-conventions")
    id("org.sonarsource.cloud-native.code-style-conventions")
    id("org.sonarsource.cloud-native.integration-test")
}

dependencies {
    "integrationTestImplementation"(libs.sonar.analyzer.commons)
    "integrationTestImplementation"(libs.sonar.ws)
    "integrationTestImplementation"(libs.assertj.core)
    "integrationTestImplementation"(libs.sonarlint.core)
    "integrationTestImplementation"(libs.sonar.orchestrator)
    "integrationTestRuntimeOnly"(libs.junit.vintage.engine)
}

sonarqube.isSkipProject = true

integrationTest {
    testSources.set(file("projects"))
}
tasks.named<Test>("integrationTest") {
    filter {
        includeTestsMatching("org.sonarsource.slang.Tests")
        includeTestsMatching("org.sonarsource.slang.SonarLintTest")
    }
    systemProperty("java.awt.headless", "true")
}
