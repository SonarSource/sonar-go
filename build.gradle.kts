/*
 * SonarSource Go
 * Copyright (C) 2018-2024 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
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
    id("org.sonarqube") version "6.0.1.5171"
}

artifactoryConfiguration {
    buildName = "sonar-go"
    artifactsToPublish = "org.sonarsource.slang:sonar-go-plugin:jar"
    artifactsToDownload = ""
    repoKeyEnv = "ARTIFACTORY_DEPLOY_REPO"
    usernameEnv = "ARTIFACTORY_DEPLOY_USERNAME"
    passwordEnv = "ARTIFACTORY_DEPLOY_PASSWORD"
}

spotless {
    java {
        // no Java sources in the root project
        target("")
    }
}

val projectTitle = properties["projectTitle"] as String
sonar {
    properties {
        property("sonar.organization", "sonarsource")
        property("sonar.projectKey", "SonarSource_sonar-go")
        property("sonar.projectName", projectTitle)
        property("sonar.links.ci", "https://cirrus-ci.com/github/SonarSource/sonar-go")
        property("sonar.links.scm", "https://github.com/SonarSource/sonar-go")
        property("sonar.links.issue", "https://jira.sonarsource.com/browse/SONARGO")
        property("sonar.exclusions", "**/build/**/*")
    }
}