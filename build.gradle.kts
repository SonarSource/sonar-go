/*
 * SonarSource Go
 * Copyright (C) 2018-2026 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
plugins {
    id("org.sonarsource.cloud-native.code-style-conventions")
    id("org.sonarsource.cloud-native.artifactory-configuration")
    id("org.sonarsource.cloud-native.rule-api")
    id("org.sonarqube") version "7.2.1.6560"
}

artifactoryConfiguration {
    buildName = providers.environmentVariable("PROJECT").orElse("sonar-go")
    artifactsToPublish = "org.sonarsource.go:sonar-go-plugin:jar"
    artifactsToDownload = ""
    repoKeyEnv = "ARTIFACTORY_DEPLOY_REPO"
    usernameEnv = "ARTIFACTORY_DEPLOY_USERNAME"
    passwordEnv = "ARTIFACTORY_DEPLOY_ACCESS_TOKEN"
}

ruleApi {
    languageToSonarpediaDirectory = mapOf(
        "Go" to "sonar-go-plugin"
    )
}

spotless {
    java {
        // no Java sources in the root project
        target("")
    }
    kotlin {
        target("build-logic/go/src/**/*.kt")
        ktlint().setEditorConfigPath("$rootDir/build-logic/common/.editorconfig")
        licenseHeaderFile(rootProject.file("LICENSE_HEADER")).updateYearWithLatest(true)
    }
    kotlinGradle {
        target("build-logic/go/src/**/*.gradle.kts", "build-logic/go/*.gradle.kts", "*.gradle.kts")
    }
}

sonar {
    properties {
        property("sonar.organization", "sonarsource")
        property("sonar.projectKey", System.getenv("SONAR_PROJECT_KEY"))
        property("sonar.projectName", "SonarGo Enterprise")
        property("sonar.links.ci", "https://github.com/SonarSource/sonar-go-enterprise/actions")
        property("sonar.links.scm", "https://github.com/SonarSource/sonar-go-enterprise")
        property("sonar.links.issue", "https://jira.sonarsource.com/browse/SONARGO")
        property("sonar.exclusions", "**/build/**/*")
        property(
            "sonar.sca.exclusions",
            "private/its/sources/**," +
                "private/its/ruling/src/integrationTest/resources/sources/**," +
                "private/its/plugin/projects/**," +
                "go/**," +
                "private/go-custom-rules-plugin/**," +
                "private/go-package-data-exporter/**"
        )
    }
}
