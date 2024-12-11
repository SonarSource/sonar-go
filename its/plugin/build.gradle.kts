dependencies {
    testImplementation(libs.sonar.analyzer.commons)
    testImplementation(libs.sonar.ws)
    testImplementation(libs.assertj.core)
    testImplementation(libs.sonarlint.core)
    testImplementation(libs.sonar.orchestrator)
}

sonarqube.isSkipProject = true

tasks.test {
    onlyIf {
        project.hasProperty("plugin") || project.hasProperty("its")
    }
    filter {
        includeTestsMatching("org.sonarsource.slang.Tests")
        includeTestsMatching("org.sonarsource.slang.SonarLintTest")
    }
    systemProperty("java.awt.headless", "true")
    outputs.upToDateWhen { false }
}
