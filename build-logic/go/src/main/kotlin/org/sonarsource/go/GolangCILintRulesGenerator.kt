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
package org.sonarsource.go

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.sonar.api.issue.impact.Severity
import org.sonar.api.issue.impact.SoftwareQuality
import org.sonar.api.rules.CleanCodeAttribute

data class Rule(
    val id: String,
    val title: String,
    val attribute: String,
    val softwareQuality: String,
    val qualityImpact: String,
)

/**
 * Convert a rule to a JSON string that is to be included into `rules.json`.
 */
fun Rule.asJson(margin: Int): String =
    """
        {
          "key": "$id",
          "name": "$title",
          "code": {
            "attribute": "$attribute",
            "impacts": {
              "$softwareQuality": "$qualityImpact"
            }
          }
        }
    """.trimIndent()
        .lineSequence()
        .filterNot { it.isBlank() }
        .joinToString(separator = "\n") { "|${" ".repeat(margin)}$it" }

object GolangCILintRulesGenerator {
    private val GOSEC_RULE = Rule(
        id = "gosec",
        title = "Issue raised by gosec",
        attribute = CleanCodeAttribute.TRUSTWORTHY.toString(),
        softwareQuality = SoftwareQuality.SECURITY.toString(),
        qualityImpact = Severity.HIGH.toString()
    )

    private val JSON = Json { ignoreUnknownKeys = true }

    @Serializable
    data class Linter(
        val name: String,
    )

    fun extractRules(jsonLintersInfo: String): List<Rule> {
        val linterNames = extractLinterNames(jsonLintersInfo)
        val allLinterNames = (linterNames + getDeprecatedLinterNames()).distinct()
        val rules = allLinterNames.map { linterName -> createBugRule(linterName) }
        return rules + listOf(GOSEC_RULE)
    }

    private fun extractLinterNames(jsonLintersInfo: String): List<String> {
        val linters = JSON.decodeFromString<List<Linter>>(jsonLintersInfo)
        return linters.map { it.name }
    }

    /**
     * Deprecated linter names that are not used anymore but need to be kept for backward compatibility.
     * They have been deprecated in golangci-lint v2.
     *
     * See:
     * - https://github.com/golangci/golangci-lint/pull/5450
     * - https://github.com/golangci/golangci-lint/pull/5487
     */
    private fun getDeprecatedLinterNames(): List<String> =
        listOf(
            "deadcode",
            "execinquery",
            "exhaustivestruct",
            "exportloopref",
            "golint",
            "gomnd",
            "gosimple",
            "ifshort",
            "interfacer",
            "maligned",
            "megacheck", // Previous name of staticcheck
            "nosnakecase",
            "scopelint",
            "structcheck",
            "stylecheck",
            "tenv",
            "varcheck"
        )

    private fun createBugRule(linterName: String): Rule {
        val id = "$linterName.bug.major" // Bugs are always major severity
        val title = "Issue raised by $linterName" // Should be overridden at issue-level
        val attribute = CleanCodeAttribute.LOGICAL.toString()
        val softwareQuality = SoftwareQuality.RELIABILITY.toString()
        val qualityImpact = Severity.MEDIUM.toString()
        return Rule(id, title, attribute, softwareQuality, qualityImpact)
    }
}
