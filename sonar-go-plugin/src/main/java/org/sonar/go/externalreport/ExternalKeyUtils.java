/*
 * SonarSource Go
 * Copyright (C) 2018-2025 SonarSource Sàrl
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
package org.sonar.go.externalreport;

import java.util.List;
import java.util.function.Predicate;

public class ExternalKeyUtils {

  /**
   * Messages for individual checks can be looked up in the Go source, e.g., for the `appends` checker at
   * <a href="https://cs.opensource.google/go/x/tools/+/refs/tags/v0.33.0:go/analysis/passes/appends/appends.go">appends.go</a>.
   * <p>
   * Versions of x/tools used in Go distributions:
   * <ul>
   *   <li>1.20.0     -> 0.3.1</li>
   *   <li>1.21.0     -> 0.11.1</li>
   *   <li>1.22.0     -> 0.16.2</li>
   *   <li>1.23.0     -> 0.22.1</li>
   *   <li>1.24.0     -> 0.28.1</li>
   * </ul>
   */
  public static final List<ExternalKey> GO_VET_KEYS = List.of(
    new ExternalKey("appends", msg -> msg.contains("append with no values")),
    new ExternalKey("asmdecl", msg -> msg.contains("(FP)") ||
      msg.contains("wrong argument size") ||
      msg.endsWith("points beyond argument frame")),
    new ExternalKey("assign", msg -> msg.startsWith("self-assignment of")),
    new ExternalKey("atomic", "direct assignment to atomic value"::equals),
    new ExternalKey("bools", msg -> msg.startsWith("redundant") || msg.startsWith("suspect")),
    new ExternalKey("buildtag", msg -> msg.contains("//go:build") || msg.contains("build comment") || msg.contains("build constraint")),
    new ExternalKey("cgocall", "possibly passing Go type with embedded pointer to C"::equals),
    new ExternalKey("composites", msg -> msg.endsWith("composite literal uses unkeyed fields")),
    new ExternalKey("copylocks", msg -> msg.contains("passes lock by value:") || msg.contains("copies lock")),
    new ExternalKey("defers", msg -> msg.contains("call to time.Since is not deferred")),
    new ExternalKey("directive", msg -> msg.contains("//go:debug")),
    new ExternalKey("errorsas", msg -> msg.contains("second argument to errors.As")),
    new ExternalKey("framepointer", msg -> msg.contains("frame pointer")),
    new ExternalKey("httpresponse", msg -> msg.endsWith("before checking for errors")),
    new ExternalKey("ifaceassert", msg -> msg.contains("impossible type assertion")),
    new ExternalKey("loopclosure", msg -> msg.contains("loop variable")),
    new ExternalKey("lostcancel", msg -> msg.matches("the cancel\\d? function .*") || msg.contains("without using the cancel")),
    new ExternalKey("nilfunc", msg -> msg.contains("comparison of function")),
    new ExternalKey("printf", msg -> msg.matches("(Printf|Println|Sprintf|Sprintln|Logf|Log) .*") || msg.contains("formatting directive")),
    new ExternalKey("shift", msg -> msg.contains("too small for shift")),
    new ExternalKey("sigchanyzer", msg -> msg.contains("misuse of unbuffered os.Signal")),
    new ExternalKey("slog", msg -> msg.contains("or a slog.Attr") ||
      msg.contains("missing a final value") ||
      msg.contains("has a missing or misplaced value")),
    new ExternalKey("stdmethods", msg -> msg.contains("should have signature")),
    new ExternalKey("stdversion", msg -> msg.contains("requires go1.")),
    new ExternalKey("stringintconv", msg -> msg.contains("yields a string of one rune")),
    new ExternalKey("structtag", msg -> msg.contains("struct field") && msg.contains("tag")),
    new ExternalKey("testinggoroutine", msg -> msg.contains("from a non-test goroutine") || msg.contains("defined outside of the subtest")),
    new ExternalKey("tests", msg -> msg.contains("has malformed") ||
      msg.contains("refers to unknown") ||
      msg.endsWith("should return nothing") ||
      msg.endsWith("should be niladic")),
    new ExternalKey("timeformat", msg -> msg.contains(" should be ")),
    new ExternalKey("unmarshal", msg -> msg.contains("passes non-pointer")),
    new ExternalKey("unreachable", "unreachable code"::equals),
    new ExternalKey("unsafeptr", "possible misuse of unsafe.Pointer"::equals),
    new ExternalKey("unusedresult", msg -> msg.endsWith("call not used")));

  public static final List<ExternalKey> GO_LINT_KEYS = List.of(
    new ExternalKey("PackageComment", msg -> msg.startsWith("package comment should be of the form") ||
      msg.startsWith("package comment should not have leading space") ||
      msg.equals("package comment is detached; there should be no blank lines between it and the package statement") ||
      msg.equals("should have a package comment, unless it's in another file for this package")),
    new ExternalKey("BlankImports", msg -> msg.equals("a blank import should be only in a main or test package, or have a comment justifying it")),
    new ExternalKey("Imports", msg -> msg.equals("should not use dot imports")),
    new ExternalKey("Exported", msg -> (msg.startsWith("exported") && msg.endsWith("or be unexported")) ||
      msg.startsWith("comment on exported") ||
      msg.endsWith("should have its own declaration") ||
      msg.contains("by other packages, and that stutters; consider calling this")),
    new ExternalKey("VarDecls", msg -> msg.contains("from declaration of var")),
    new ExternalKey("Elses", msg -> msg.startsWith("if block ends with a return statement, so drop this else and outdent its block")),
    new ExternalKey("Ranges", msg -> msg.contains("from range; this loop is equivalent to")),
    new ExternalKey("Errorf", msg -> msg.contains("(fmt.Sprintf(...)) with") && msg.contains(".Errorf(...)")),
    new ExternalKey("Errors", msg -> msg.startsWith("error var ") && msg.contains("should have name of the form ")),
    new ExternalKey("ErrorStrings", msg -> msg.equals("error strings should not be capitalized or end with punctuation or a newline")),
    new ExternalKey("ReceiverNames", msg -> msg.contains("should be consistent with previous receiver name") ||
      msg.startsWith("receiver name should not be an underscore") ||
      msg.equals("receiver name should be a reflection of its identity; don't use generic names such as \"this\" or \"self\"")),
    new ExternalKey("IncDec", msg -> msg.startsWith("should replace") && !msg.contains("(fmt.Sprintf(...)) with")),
    new ExternalKey("ErrorReturn", msg -> msg.startsWith("error should be the last type when returning multiple items")),
    new ExternalKey("UnexportedReturn", msg -> msg.contains("returns unexported type") && msg.endsWith("which can be annoying to use")),
    new ExternalKey("TimeNames", msg -> msg.contains("don't use unit-specific suffix")),
    new ExternalKey("ContextKeyTypes", msg -> msg.startsWith("should not use basic type") && msg.endsWith("as key in context.WithValue")),
    new ExternalKey("ContextArgs", msg -> msg.equals("context.Context should be the first parameter of a function")),
    new ExternalKey("Names", msg -> msg.startsWith("don't use an underscore in package name") ||
      msg.startsWith("don't use ALL_CAPS in Go names; use CamelCase") ||
      msg.startsWith("don't use leading k in Go names;") ||
      msg.startsWith("don't use underscores in Go names;") ||
      msg.matches("(range var|struct field|[\\w]+) [\\w_]+ should be [\\w_]+") ||
      msg.startsWith("don't use MixedCaps in package name;")));

  private ExternalKeyUtils() {
    // utility class, forbidden constructor
  }

  public static String lookup(String message, String linter) {
    if (linter.equals(GoVetReportSensor.LINTER_ID) || linter.equals(GoLintReportSensor.LINTER_ID)) {
      List<ExternalKey> keys = linter.equals(GoVetReportSensor.LINTER_ID) ? GO_VET_KEYS : GO_LINT_KEYS;
      return keys.stream()
        .filter(externalKey -> externalKey.matches.test(message))
        .map(ExternalKey::key)
        .findFirst()
        .orElse(null);
    }
    return null;
  }

  public record ExternalKey(String key, Predicate<String> matches) {
  }
}
