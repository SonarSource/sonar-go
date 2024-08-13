/*
 * Copyright (C) 2018-2024 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package com.sonarsource.apex.checks;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.sonar.check.Rule;
import org.sonarsource.slang.api.StringLiteralTree;
import org.sonarsource.slang.checks.api.InitContext;
import org.sonarsource.slang.checks.api.SlangCheck;

@Rule(key = "S5389")
public class AbsoluteSalesforceUrlCheck implements SlangCheck {

  private static final Pattern ABSOLUTE_URL_PATTERN = Pattern.compile(
    "(?<!\\w)(login|test|(dns|test|[a-z]{1,2})\\d++)\\.(salesforce|force|visual\\.force|content\\.force)\\.com(?!\\w)");

  @Override
  public void initialize(InitContext init) {
    init.register(StringLiteralTree.class, (ctx, tree) -> findAbsoluteUrl(tree.content())
      .ifPresent(url -> ctx.reportIssue(tree, "Make the absolute URL '" + url + "' relative.")));
  }

  private static Optional<String> findAbsoluteUrl(String text) {
    Matcher matcher = ABSOLUTE_URL_PATTERN.matcher(text);
    if (matcher.find()) {
      return Optional.of(matcher.group());
    }
    return Optional.empty();
  }

}
