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

import java.util.Arrays;
import java.util.Collection;
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

  private static final String GIN_PACKAGE = "github.com/gin-gonic/gin";
  private static final String NET_HTTP_PACKAGE = "net/http";
  private static final String ECHO_PACKAGE = "github.com/labstack/echo/v4";
  private static final String CHI_PACKAGE = "github.com/go-chi/chi/v5";
  private static final String GORILLA_MUX_PACKAGE = "github.com/gorilla/mux";
  private static final Collection<String> FIBER_PACKAGES = List.of("github.com/gofiber/fiber/v2", "github.com/gofiber/fiber/v3");
  private static final String HTTPROUTER_PACKAGE = "github.com/julienschmidt/httprouter";
  private static final String BEEGO_WEB_PACKAGE = "github.com/beego/beego/v2/server/web";

  /** The route path is the first argument, e.g. {@code router.GET("/users/:id", handler)}. */
  private static final int PATH_AT_FIRST_ARGUMENT = 0;
  /** The route path is preceded by the HTTP method, e.g. {@code router.Handle("GET", "/users/:id", handler)}. */
  private static final int PATH_AT_SECOND_ARGUMENT = 1;

  // Names that several of the frameworks below share, so that each one is spelled out only once
  private static final String HANDLE_FUNCTION = "Handle";
  private static final String GROUP_NAME = "Group";
  private static final String ROUTER_TYPE = "Router";
  private static final List<String> UPPER_CASE_HTTP_METHOD_FUNCTIONS = List.of("GET", "POST", "PUT", "DELETE", "PATCH", "HEAD", "OPTIONS");
  private static final List<String> CAPITALIZED_HTTP_METHOD_FUNCTIONS = List.of("Get", "Post", "Put", "Delete", "Patch", "Head", "Options");
  // Chi and Fiber also register routes for the two HTTP methods the other routers leave out
  private static final List<String> EXTENDED_HTTP_METHOD_FUNCTIONS = union(CAPITALIZED_HTTP_METHOD_FUNCTIONS, List.of("Connect", "Trace"));
  // Register a route matching every HTTP method
  private static final List<String> HANDLE_ROUTE_FUNCTIONS = List.of(HANDLE_FUNCTION, "HandleFunc");
  // Mount a sub-router under a path prefix
  private static final List<String> SUB_ROUTER_FUNCTIONS = List.of("Route", "Mount");

  // Gin and Echo expose the same set of route-defining functions, including "Group" for a sub-router prefix
  private static final List<String> GIN_AND_ECHO_ROUTE_FUNCTIONS = union(UPPER_CASE_HTTP_METHOD_FUNCTIONS, List.of("Any", GROUP_NAME));
  private static final String[] GIN_ROUTER_TYPES = typesIn(GIN_PACKAGE, "Engine", "RouterGroup", "IRouter", "IRoutes");
  private static final List<String> GIN_ROUTE_FUNCTIONS_WITH_METHOD = List.of(HANDLE_FUNCTION, "Match");
  private static final String[] NET_HTTP_ROUTER_TYPES = typesIn(NET_HTTP_PACKAGE, "ServeMux");
  private static final String[] ECHO_ROUTER_TYPES = typesIn(ECHO_PACKAGE, "Echo", GROUP_NAME);
  private static final List<String> ECHO_ROUTE_FUNCTIONS_WITH_METHOD = List.of("Add", "Match");
  private static final String[] CHI_ROUTER_TYPES = typesIn(CHI_PACKAGE, "Mux", ROUTER_TYPE);
  private static final List<String> CHI_ROUTE_FUNCTIONS = List.copyOf(union(EXTENDED_HTTP_METHOD_FUNCTIONS, HANDLE_ROUTE_FUNCTIONS, SUB_ROUTER_FUNCTIONS));
  private static final List<String> CHI_ROUTE_FUNCTIONS_WITH_METHOD = List.of("Method", "MethodFunc");
  // Path and PathPrefix are declared on both types, as in the dominant Gorilla Mux idiom r.Methods("GET").Path("/users/{id}")
  private static final String[] GORILLA_MUX_ROUTER_TYPES = typesIn(GORILLA_MUX_PACKAGE, ROUTER_TYPE, "Route");
  private static final List<String> GORILLA_MUX_ROUTE_FUNCTIONS = union(HANDLE_ROUTE_FUNCTIONS, List.of("Path", "PathPrefix"));
  private static final String[] FIBER_ROUTER_TYPES = typesIn(FIBER_PACKAGES, "App", GROUP_NAME, ROUTER_TYPE);
  private static final List<String> FIBER_ROUTE_FUNCTIONS = union(EXTENDED_HTTP_METHOD_FUNCTIONS, SUB_ROUTER_FUNCTIONS, List.of("All", GROUP_NAME));
  private static final List<String> FIBER_ROUTE_FUNCTIONS_WITH_METHOD = List.of("Add");
  private static final String[] HTTPROUTER_ROUTER_TYPES = typesIn(HTTPROUTER_PACKAGE, ROUTER_TYPE);
  private static final List<String> HTTPROUTER_ROUTE_FUNCTIONS_WITH_METHOD = List.of(HANDLE_FUNCTION, "Handler", "HandlerFunc");
  private static final String[] BEEGO_ROUTER_TYPES = typesIn(BEEGO_WEB_PACKAGE, "ControllerRegister", "HttpServer");
  private static final List<String> BEEGO_ROUTE_FUNCTIONS = union(CAPITALIZED_HTTP_METHOD_FUNCTIONS, List.of("Any", "Router",
    "CtrlGet", "CtrlPost", "CtrlPut", "CtrlDelete", "CtrlPatch", "CtrlHead", "CtrlOptions", "CtrlAny"));

  @RuleProperty(
    key = "threshold",
    description = "Number of times a literal must be duplicated to trigger an issue",
    defaultValue = "" + DEFAULT_THRESHOLD)
  public int threshold = DEFAULT_THRESHOLD;

  private static final List<MethodMatchers> LOG_AND_ERROR_MATCHERS = List.copyOf(buildLogAndErrorMatchers());
  private static final List<RouteMatcher> ROUTE_MATCHERS = List.copyOf(buildRouteMatchers());

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

  private static List<RouteMatcher> buildRouteMatchers() {
    return List.of(
      // Gin: router.GET("/users/:id", ...) and router.Handle("GET", "/users/:id", ...)
      routeMatcherOnVariable(GIN_PACKAGE, GIN_ROUTER_TYPES, PATH_AT_FIRST_ARGUMENT, GIN_AND_ECHO_ROUTE_FUNCTIONS),
      routeMatcherOnVariable(GIN_PACKAGE, GIN_ROUTER_TYPES, PATH_AT_SECOND_ARGUMENT, GIN_ROUTE_FUNCTIONS_WITH_METHOD),
      // Standard library: http.HandleFunc("/users/{id}", ...) and the same functions on a *http.ServeMux
      packageLevelRouteMatcher(NET_HTTP_PACKAGE, PATH_AT_FIRST_ARGUMENT, HANDLE_ROUTE_FUNCTIONS),
      routeMatcherOnVariable(NET_HTTP_PACKAGE, NET_HTTP_ROUTER_TYPES, PATH_AT_FIRST_ARGUMENT, HANDLE_ROUTE_FUNCTIONS),
      // Echo: e.GET("/users/:id", ...) and e.Add("GET", "/users/:id", ...)
      routeMatcherOnVariable(ECHO_PACKAGE, ECHO_ROUTER_TYPES, PATH_AT_FIRST_ARGUMENT, GIN_AND_ECHO_ROUTE_FUNCTIONS),
      routeMatcherOnVariable(ECHO_PACKAGE, ECHO_ROUTER_TYPES, PATH_AT_SECOND_ARGUMENT, ECHO_ROUTE_FUNCTIONS_WITH_METHOD),
      // Chi: r.Get("/users/{id}", ...) and r.Method("GET", "/users/{id}", ...)
      routeMatcherOnVariable(CHI_PACKAGE, CHI_ROUTER_TYPES, PATH_AT_FIRST_ARGUMENT, CHI_ROUTE_FUNCTIONS),
      routeMatcherOnVariable(CHI_PACKAGE, CHI_ROUTER_TYPES, PATH_AT_SECOND_ARGUMENT, CHI_ROUTE_FUNCTIONS_WITH_METHOD),
      // Gorilla Mux: r.HandleFunc("/users/{id}", ...) and r.Methods("GET").Path("/users/{id}")
      routeMatcherOnVariable(GORILLA_MUX_PACKAGE, GORILLA_MUX_ROUTER_TYPES, PATH_AT_FIRST_ARGUMENT, GORILLA_MUX_ROUTE_FUNCTIONS),
      // Fiber: app.Get("/users/:id", ...) and app.Add("GET", "/users/:id", ...)
      routeMatcherOnVariable(FIBER_PACKAGES, FIBER_ROUTER_TYPES, PATH_AT_FIRST_ARGUMENT, FIBER_ROUTE_FUNCTIONS),
      routeMatcherOnVariable(FIBER_PACKAGES, FIBER_ROUTER_TYPES, PATH_AT_SECOND_ARGUMENT, FIBER_ROUTE_FUNCTIONS_WITH_METHOD),
      // httprouter: r.GET("/users/:id", ...) and r.Handle("GET", "/users/:id", ...)
      routeMatcherOnVariable(HTTPROUTER_PACKAGE, HTTPROUTER_ROUTER_TYPES, PATH_AT_FIRST_ARGUMENT, UPPER_CASE_HTTP_METHOD_FUNCTIONS),
      routeMatcherOnVariable(HTTPROUTER_PACKAGE, HTTPROUTER_ROUTER_TYPES, PATH_AT_SECOND_ARGUMENT, HTTPROUTER_ROUTE_FUNCTIONS_WITH_METHOD),
      // Beego: web.Get("/users/:id", ...) as a package-level function and on a *web.ControllerRegister or *web.HttpServer
      packageLevelRouteMatcher(BEEGO_WEB_PACKAGE, PATH_AT_FIRST_ARGUMENT, BEEGO_ROUTE_FUNCTIONS),
      routeMatcherOnVariable(BEEGO_WEB_PACKAGE, BEEGO_ROUTER_TYPES, PATH_AT_FIRST_ARGUMENT, BEEGO_ROUTE_FUNCTIONS));
  }

  @SafeVarargs
  private static List<String> union(List<String>... nameLists) {
    return Arrays.stream(nameLists).flatMap(List::stream).toList();
  }

  private static String[] typesIn(String importPath, String... typeNames) {
    return typesIn(List.of(importPath), typeNames);
  }

  private static String[] typesIn(Collection<String> importPaths, String... typeNames) {
    return importPaths.stream()
      .flatMap(importPath -> Arrays.stream(typeNames).map(typeName -> importPath + "." + typeName))
      .toArray(String[]::new);
  }

  private static RouteMatcher routeMatcherOnVariable(String importPath, String[] variableTypes, int pathArgumentIndex, Collection<String> names) {
    return routeMatcherOnVariable(List.of(importPath), variableTypes, pathArgumentIndex, names);
  }

  private static RouteMatcher routeMatcherOnVariable(Collection<String> importPaths, String[] variableTypes, int pathArgumentIndex,
    Collection<String> names) {
    return new RouteMatcher(MethodMatchers.create()
      .ofTypes(importPaths)
      .withVariableTypeIn(variableTypes)
      .withNames(names)
      .withAnyParameters()
      .build(), pathArgumentIndex);
  }

  private static RouteMatcher packageLevelRouteMatcher(String importPath, int pathArgumentIndex, Collection<String> names) {
    return new RouteMatcher(MethodMatchers.create()
      .ofType(importPath)
      .withNames(names)
      .withAnyParameters()
      .build(), pathArgumentIndex);
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
      ROUTE_MATCHERS.forEach(routeMatcher -> routeMatcher.collectPathLiteral(functionInvocationTree, excludedLiterals));
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

  private record RouteMatcher(MethodMatchers methodMatchers, int pathArgumentIndex) {
    private void collectPathLiteral(FunctionInvocationTree functionInvocation, Set<StringLiteralTree> excludedLiterals) {
      if (methodMatchers.matches(functionInvocation).isPresent()
        && MethodMatchers.getArg(functionInvocation, pathArgumentIndex) instanceof StringLiteralTree pathLiteral) {
        excludedLiterals.add(pathLiteral);
      }
    }
  }
}
