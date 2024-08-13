/*
 * SonarSource SLang
 * Copyright (C) 2018-2024 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonarsource.slang.plugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.sonarsource.slang.api.Annotation;
import org.sonarsource.slang.api.ClassDeclarationTree;
import org.sonarsource.slang.api.FunctionDeclarationTree;
import org.sonarsource.slang.api.ParameterTree;
import org.sonarsource.slang.api.TextRange;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.api.VariableDeclarationTree;
import org.sonarsource.slang.visitors.TreeVisitor;

public class IssueSuppressionVisitor extends TreeVisitor<InputFileContext> {

  private Map<String, Set<TextRange>> filteredRules;

  private static final List<String> SUPPRESS_ANNOTATION_NAMES = Arrays.asList("Suppress", "SuppressWarnings");

  private static final Pattern LITERAL_PATTERN = Pattern.compile("\"(.*?)\"");

  public IssueSuppressionVisitor() {
    register(FunctionDeclarationTree.class, (ctx, tree) -> checkSuppressAnnotations(tree));
    register(ClassDeclarationTree.class, (ctx, tree) -> checkSuppressAnnotations(tree));
    register(VariableDeclarationTree.class, (ctx, tree) -> checkSuppressAnnotations(tree));
    register(ParameterTree.class, (ctx, tree) -> checkSuppressAnnotations(tree));
  }

  private void checkSuppressAnnotations(Tree tree) {
    List<Annotation> annotations = tree.metaData().annotations();
    TextRange textRange = tree.textRange();

    annotations.forEach(annotation -> {
      if (SUPPRESS_ANNOTATION_NAMES.contains(annotation.shortName())) {
        getSuppressedKeys(annotation.argumentsText()).forEach(ruleKey ->
          filteredRules.computeIfAbsent(ruleKey, key -> new HashSet<>()).add(textRange)
        );
      }
    });
  }

  private static Collection<String> getSuppressedKeys(List<String> argumentsText) {
    List<String> keys = new ArrayList<>();
    for (String s : argumentsText) {
      keys.addAll(getArgumentsValues(s));
    }
    return keys;
  }

  private static Collection<String> getArgumentsValues(String argumentText) {
    List<String> values = new ArrayList<>();
    Matcher m = LITERAL_PATTERN.matcher(argumentText);
    while (m.find()) {
      values.add(m.group(1));
    }
    return values;
  }

  @Override
  protected void before(InputFileContext ctx, Tree root) {
    filteredRules = new HashMap<>();
  }

  @Override
  protected void after(InputFileContext ctx, Tree root) {
    ctx.setFilteredRules(filteredRules);
  }

}
