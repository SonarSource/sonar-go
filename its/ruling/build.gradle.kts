plugins {
    id("org.sonarsource.cloud-native.java-conventions")
    id("org.sonarsource.cloud-native.code-style-conventions")
    id("org.sonarsource.cloud-native.integration-test")
}

dependencies {
    "integrationTestImplementation"(libs.sonar.analyzer.commons)
    "integrationTestImplementation"(libs.assertj.core)
    "integrationTestImplementation"(libs.sonar.orchestrator)
    "integrationTestRuntimeOnly"(libs.junit.vintage.engine)
}

sonarqube.isSkipProject = true

integrationTest {
    testSources.set(rootProject.file("its/sources"))
}
tasks.named<Test>("integrationTest") {
    systemProperty("java.awt.headless", "true")
}
