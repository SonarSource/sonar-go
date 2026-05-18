/*
 * SonarSource Go
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.go.checks;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.go.utils.MethodMatchers;
import org.sonar.plugins.go.api.FunctionInvocationTree;
import org.sonar.plugins.go.api.StringLiteralTree;
import org.sonar.plugins.go.api.checks.CheckContext;
import org.sonar.plugins.go.api.checks.GoCheck;
import org.sonar.plugins.go.api.checks.InitContext;
import org.sonar.plugins.go.api.checks.SecondaryLocation;

@Rule(key = "S1192")
public class StringLiteralDuplicatedCheck implements GoCheck {

  private static final int DEFAULT_THRESHOLD = 3;
  private static final int MINIMAL_LITERAL_LENGTH = 5;
  private static final Pattern NO_SEPARATOR_REGEXP = Pattern.compile("\\w++");
  private static final String[] LOG_FUNCTIONS = {"Print", "Printf", "Println", "Fatal", "Fatalf", "Fatalln", "Panic", "Panicf", "Panicln"};
  private static final String[] FMT_FUNCTIONS = {"Errorf", "Printf", "Fprintf", "Sprintf"};
  private static final String[] SLOG_FUNCTIONS = {"Debug", "Info", "Warn", "Error", "Log", "LogAttrs"};
  private static final String[] LOGRUS_FUNCTIONS = {"Trace", "Tracef", "Traceln",
    "Debug", "Debugf", "Debugln", "Info", "Infof", "Infoln",
    "Warn", "Warnf", "Warnln", "Warning", "Warningf", "Warningln",
    "Error", "Errorf", "Errorln", "Fatal", "Fatalf", "Fatalln",
    "Panic", "Panicf", "Panicln", "Print", "Printf", "Println",
    "WithField", "WithFields", "WithError"};
  private static final String[] ZAP_LOGGER_FUNCTIONS = {"Debug", "Info", "Warn", "Error", "DPanic", "Panic", "Fatal"};
  private static final String[] ZAP_SUGARED_LOGGER_FUNCTIONS = {"Debug", "Debugf", "Debugw", "Debugln", "Info", "Infof", "Infow", "Infoln",
    "Warn", "Warnf", "Warnw", "Warnln", "Error", "Errorf", "Errorw", "Errorln",
    "DPanic", "DPanicf", "DPanicw", "DPanicln", "Panic", "Panicf", "Panicw", "Panicln",
    "Fatal", "Fatalf", "Fatalw", "Fatalln"};
  private static final String[] GLOG_AND_KLOG_FUNCTIONS = {"Info", "Infof", "Infoln", "Warning", "Warningf", "Warningln",
    "Error", "Errorf", "Errorln", "Fatal", "Fatalf", "Fatalln"};
  private static final String[] LOG15_FUNCTIONS = {"Debug", "Info", "Warn", "Error", "Crit"};
  private static final String[] GO_KIT_FUNCTIONS = {"Debug", "Info", "Warn", "Error"};
  private static final String[] APEX_LOG_FUNCTIONS = {"Debug", "Debugf", "Info", "Infof", "Warn", "Warnf", "Error", "Errorf", "Fatal", "Fatalf"};
  private static final String[] XERRORS_FUNCTIONS = {"Errorf", "New"};
  private static final String[] PKG_ERRORS_FUNCTIONS = {"New", "Errorf", "Wrap", "Wrapf", "WithMessage", "WithMessagef"};

  @RuleProperty(
    key = "threshold",
    description = "Number of times a literal must be duplicated to trigger an issue",
    defaultValue = "" + DEFAULT_THRESHOLD)
  public int threshold = DEFAULT_THRESHOLD;

  private static final List<MethodMatchers> LOG_AND_ERROR_MATCHERS = buildLogAndErrorMatchers();

  private final Set<StringLiteralTree> excludedLiterals = new HashSet<>();

  private static List<MethodMatchers> buildLogAndErrorMatchers() {
    return List.of(
      // Standard library: log package
      MethodMatchers.create()
        .ofType("log")
        .withNames(LOG_FUNCTIONS)
        .withAnyParameters()
        .build(),
      // Standard library: fmt package
      MethodMatchers.create()
        .ofType("fmt")
        .withNames(FMT_FUNCTIONS)
        .withAnyParameters()
        .build(),
      // Standard library: errors package
      MethodMatchers.create()
        .ofType("errors")
        .withNames("New")
        .withAnyParameters()
        .build(),
      // Standard library: log/slog package
      MethodMatchers.create()
        .ofType("log/slog")
        .withNames(SLOG_FUNCTIONS)
        .withAnyParameters()
        .build(),
      // Third-party: github.com/sirupsen/logrus
      MethodMatchers.create()
        .ofType("github.com/sirupsen/logrus")
        .withNames(LOGRUS_FUNCTIONS)
        .withAnyParameters()
        .build(),
      // Third-party: go.uber.org/zap - methods on *zap.Logger
      MethodMatchers.create()
        .ofType("go.uber.org/zap")
        .withVariableTypeIn("go.uber.org/zap.Logger")
        .withNames(ZAP_LOGGER_FUNCTIONS)
        .withAnyParameters()
        .build(),
      // Third-party: go.uber.org/zap - methods on *zap.SugaredLogger
      MethodMatchers.create()
        .ofType("go.uber.org/zap")
        .withVariableTypeIn("go.uber.org/zap.SugaredLogger")
        .withNames(ZAP_SUGARED_LOGGER_FUNCTIONS)
        .withAnyParameters()
        .build(),
      // Third-party: github.com/rs/zerolog - Msg/Msgf are chain methods on *zerolog.Event
      MethodMatchers.create()
        .ofType("github.com/rs/zerolog")
        .withVariableTypeIn("github.com/rs/zerolog.Event")
        .withNames("Msg", "Msgf")
        .withAnyParameters()
        .build(),
      // The github.com/golang/glog, k8s.io/klog, and k8s.io/klog/v2 have the same set of functions
      MethodMatchers.create()
        .ofTypes(List.of("github.com/golang/glog", "k8s.io/klog", "k8s.io/klog/v2"))
        .withNames(GLOG_AND_KLOG_FUNCTIONS)
        .withAnyParameters()
        .build(),
      // Third-party: gopkg.in/inconshreveable/log15.v2
      MethodMatchers.create()
        .ofType("gopkg.in/inconshreveable/log15.v2")
        .withNames(LOG15_FUNCTIONS)
        .withAnyParameters()
        .build(),
      // Third-party: github.com/go-kit/log - Log is a method on the log.Logger interface
      MethodMatchers.create()
        .ofType("github.com/go-kit/log")
        .withVariableTypeIn("github.com/go-kit/log.Logger")
        .withNames("Log")
        .withAnyParameters()
        .build(),
      // Third-party: github.com/go-kit/log/level
      MethodMatchers.create()
        .ofType("github.com/go-kit/log/level")
        .withNames(GO_KIT_FUNCTIONS)
        .withAnyParameters()
        .build(),
      // Third-party: github.com/apex/log
      MethodMatchers.create()
        .ofType("github.com/apex/log")
        .withNames(APEX_LOG_FUNCTIONS)
        .withAnyParameters()
        .build(),
      // Third-party: golang.org/x/xerrors
      MethodMatchers.create()
        .ofType("golang.org/x/xerrors")
        .withNames(XERRORS_FUNCTIONS)
        .withAnyParameters()
        .build(),
      // Third-party: github.com/pkg/errors
      MethodMatchers.create()
        .ofType("github.com/pkg/errors")
        .withNames(PKG_ERRORS_FUNCTIONS)
        .withAnyParameters()
        .build());
  }

  @Override
  public void initialize(InitContext init) {
    init.register(FunctionInvocationTree.class, (checkContext, functionInvocationTree) -> {
      if (LOG_AND_ERROR_MATCHERS.stream().anyMatch(matcher -> matcher.matches(functionInvocationTree).isPresent())) {
        functionInvocationTree.arguments().stream()
          .filter(StringLiteralTree.class::isInstance)
          .map(StringLiteralTree.class::cast)
          .forEach(excludedLiterals::add);
      }
    });

    init.registerOnLeave((ctx, tree) -> {
      var occurrences = new HashMap<String, List<StringLiteralTree>>();
      tree.descendants()
        .filter(StringLiteralTree.class::isInstance)
        .map(StringLiteralTree.class::cast)
        .filter(literal -> !excludedLiterals.contains(literal))
        .filter(literal -> literal.content().length() > MINIMAL_LITERAL_LENGTH && !NO_SEPARATOR_REGEXP.matcher(literal.content()).matches())
        .forEach(literal -> occurrences.computeIfAbsent(literal.content(), key -> new LinkedList<>()).add(literal));
      check(ctx, occurrences, threshold);
      excludedLiterals.clear();
    });
  }

  private static void check(CheckContext ctx, Map<String, List<StringLiteralTree>> occurrencesMap, int threshold) {
    for (Map.Entry<String, List<StringLiteralTree>> entry : occurrencesMap.entrySet()) {
      var occurrences = entry.getValue();
      int size = occurrences.size();
      if (size >= threshold) {
        var first = occurrences.get(0);
        var message = "Define a constant instead of duplicating this literal \"%s\" %s times.".formatted(first.content(), size);
        var secondaryLocations = occurrences.stream()
          .skip(1)
          .map(stringLiteral -> new SecondaryLocation(stringLiteral.metaData().textRange(), "Duplication"))
          .toList();
        var gap = size - 1.0;
        ctx.reportIssue(first, message, secondaryLocations, gap);
      }
    }
  }
}
