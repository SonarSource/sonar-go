plugins {
    id("org.sonarsource.cloud-native.java-conventions")
}

dependencies {
    testImplementation(libs.sonar.analyzer.commons)
    testImplementation(libs.assertj.core)
    testImplementation(libs.sonar.orchestrator)
    testRuntimeOnly(libs.junit.vintage.engine)
}

sonarqube.isSkipProject = true

tasks.test {
    onlyIf {
        project.hasProperty("its") ||
            project.hasProperty("ruling")
    }

    systemProperty("java.awt.headless", "true")
    outputs.upToDateWhen { false }
}
