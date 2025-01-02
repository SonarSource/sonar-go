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
package org.sonarsource.slang;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarScanner;
import com.sonar.orchestrator.junit5.OrchestratorExtension;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.locator.MavenLocation;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.sonarsource.analyzer.commons.ProfileGenerator;

import static org.assertj.core.api.Assertions.assertThat;

public class GoRulingTest {

  private static final String SQ_VERSION_PROPERTY = "sonar.runtimeVersion";
  private static final String DEFAULT_SQ_VERSION = "LATEST_RELEASE";

  private static Orchestrator orchestrator;
  private static boolean keepSonarqubeRunning = "true".equals(System.getProperty("keepSonarqubeRunning"));
  public static final FileLocation GO_PLUGIN_LOCATION = FileLocation.byWildcardFilename(new File("../../sonar-go-plugin/build/libs"), "sonar-go-plugin-*-all.jar");

  @BeforeAll
  public static void setUp() {
    var builder = OrchestratorExtension.builderEnv()
      .useDefaultAdminCredentialsForBuilds(true)
      .setSonarVersion(System.getProperty(SQ_VERSION_PROPERTY, DEFAULT_SQ_VERSION))
      .addPlugin(GO_PLUGIN_LOCATION)
      .addPlugin(MavenLocation.of("org.sonarsource.sonar-lits-plugin", "sonar-lits-plugin", "0.10.0.2181"));

    orchestrator = builder.build();
    orchestrator.start();

    ProfileGenerator.RulesConfiguration goRulesConfiguration = new ProfileGenerator.RulesConfiguration();
    goRulesConfiguration.add("S1451", "headerFormat", "^(?i).*copyright");
    goRulesConfiguration.add("S1451", "isRegularExpression", "true");

    File goProfile = ProfileGenerator.generateProfile(GoRulingTest.orchestrator.getServer().getUrl(), "go", "go", goRulesConfiguration, Collections.emptySet());

    orchestrator.getServer().restoreProfile(FileLocation.of(goProfile));
  }

  @Test
  @Disabled("This test should only be run manually")
  public void go_manual_keep_sonarqube_server_up() throws IOException {
    keepSonarqubeRunning = true;
    test_go();
  }

  @Test
  public void test_go() throws IOException {
    Map<String, String> properties = new HashMap<>();
    properties.put("sonar.inclusions", "sources/**/*.go, ruling/src/integrationTest/resources/sources/**/*.go");
    properties.put("sonar.exclusions", "**/*generated*.go, **/*.pb.go");
    properties.put("sonar.tests", ".");
    properties.put("sonar.test.inclusions", "**/*_test.go");
    run_ruling_test(properties);
  }

  private void run_ruling_test(Map<String, String> projectProperties) throws IOException {
    Map<String, String> properties = new HashMap<>(projectProperties);
    properties.put("sonar.slang.converter.validation", "log");
    properties.put("sonar.slang.duration.statistics", "true");

    var projectKey = "go-project";
    orchestrator.getServer().provisionProject(projectKey, projectKey);
    orchestrator.getServer().associateProjectToQualityProfile(projectKey, "go", "rules");

    var actualDirectory = new File("build/reports/lits");
    actualDirectory.mkdirs();

    var litsDifferencesFile = new File("build/" + projectKey + "-differences");
    SonarScanner build = SonarScanner.create(FileLocation.of("../").getFile())
      .setProjectKey(projectKey)
      .setProjectName(projectKey)
      .setProjectVersion("1")
      .setSourceDirs("./")
      .setSourceEncoding("utf-8")
      .setProperties(properties)
      .setProperty("sonar.lits.dump.old", Path.of("src/integrationTest/resources/expected").toAbsolutePath().toString())
      .setProperty("sonar.lits.dump.new", actualDirectory.getAbsolutePath())
      .setProperty("sonar.lits.differences", litsDifferencesFile.getAbsolutePath())
      .setProperty("sonar.cpd.exclusions", "**/*")
      .setProperty("sonar.scm.disabled", "true")
      .setProperty("sonar.project", "go")
      .setEnvironmentVariable("SONAR_RUNNER_OPTS", "-Xmx1024m");

    orchestrator.executeBuild(build);

    String litsDifference = new String(Files.readAllBytes(litsDifferencesFile.toPath()));
    assertThat(litsDifference).isEmpty();
  }

  @AfterAll
  public static void after() {
    if (keepSonarqubeRunning) {
      // keep server running, use CTRL-C to stop it
      new Scanner(System.in).next();
    }
  }
}
