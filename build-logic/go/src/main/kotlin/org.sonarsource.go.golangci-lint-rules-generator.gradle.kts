/*
 * SonarSource Go
 * Copyright (C) 2018-2025 SonarSource SA
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
import de.undercouch.gradle.tasks.download.Download
import org.gradle.api.Task
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.registering
import org.sonarsource.go.GolangCILintRulesGenerator.extractRules
import org.sonarsource.go.asJson

plugins {
    id("de.undercouch.download")
}

val generateGolangCILintRules by tasks.registering(Task::class) {
    group = "build"
    description = "Generate the list of rules from the golangci-lint meta-linter"
    dependsOn(downloadGolangCILintInfo)

    doFirst {
        val rules = extractRules(downloadGolangCILintInfo.get().dest.readText())
        val rulesFile = file("src/main/resources/org/sonar/l10n/go/rules/golangci-lint/rules.json")
        rulesFile.writeText(
            """
                |[
                ${rules.joinToString(separator = ",\n") { it.asJson(2) }}
                |]
            """.trimMargin().plus("\n")
        )
    }
}

val downloadGolangCILintInfo by tasks.registering(Download::class) {
    group = "build"
    description = "Download linters info from the golangci-lint meta-linter"

    src("https://raw.githubusercontent.com/golangci/golangci-lint/refs/heads/main/assets/linters-info.json")
    dest(layout.buildDirectory.file("linters-info.json"))
}
