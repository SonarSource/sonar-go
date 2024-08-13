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
package org.sonarsource.slang;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.OrchestratorBuilder;
import com.sonar.orchestrator.build.SonarScanner;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.locator.Location;
import com.sonar.orchestrator.locator.MavenLocation;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import org.apache.commons.lang.StringUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.sonarsource.analyzer.commons.ProfileGenerator;

import static org.assertj.core.api.Assertions.assertThat;

public class SlangRulingTest {

  private static final String SQ_VERSION_PROPERTY = "sonar.runtimeVersion";
  private static final String DEFAULT_SQ_VERSION = "LATEST_RELEASE";

  private static Orchestrator orchestrator;
  private static boolean keepSonarqubeRunning = "true".equals(System.getProperty("keepSonarqubeRunning"));


  @BeforeClass
  public static void setUp() {
    OrchestratorBuilder builder = Orchestrator.builderEnv()
      .useDefaultAdminCredentialsForBuilds(true)
      .setSonarVersion(System.getProperty(SQ_VERSION_PROPERTY, DEFAULT_SQ_VERSION))
      .addPlugin(MavenLocation.of("org.sonarsource.sonar-lits-plugin", "sonar-lits-plugin", "0.10.0.2181"));

    addGoPlugin(builder);

    orchestrator = builder.build();
    orchestrator.start();

    ProfileGenerator.RulesConfiguration goRulesConfiguration = new ProfileGenerator.RulesConfiguration();
    goRulesConfiguration.add("S1451", "headerFormat", "^(?i).*copyright");
    goRulesConfiguration.add("S1451", "isRegularExpression", "true");

    File goProfile = ProfileGenerator.generateProfile(SlangRulingTest.orchestrator.getServer().getUrl(), "go", "go", goRulesConfiguration, Collections.emptySet());

    orchestrator.getServer().restoreProfile(FileLocation.of(goProfile));
  }

  static void addGoPlugin(OrchestratorBuilder builder) {
    String plugin = "sonar-go-plugin";
    String goVersion = System.getProperty("goVersion");

    Location pluginLocation;
    if (StringUtils.isEmpty(goVersion)) {
      // use the plugin that was built on local machine
      pluginLocation = FileLocation.byWildcardMavenFilename(new File("../../" + plugin + "/build/libs"), plugin + "-*-all.jar");
    } else {
      // QA environment downloads the plugin built by the CI job
      pluginLocation = MavenLocation.of("org.sonarsource.slang", plugin, goVersion);
    }

    builder.addPlugin(pluginLocation);
  }

  @Test
  // @Ignore because it should only be run manually
  @Ignore
  public void go_manual_keep_sonarqube_server_up() throws IOException {
    keepSonarqubeRunning = true;
    test_go();
  }


  @Test
  public void test_go() throws IOException {
    Map<String, String> properties = new HashMap<>();
    properties.put("sonar.inclusions", "sources/go/**/*.go, ruling/src/test/resources/sources/go/**/*.go");
    properties.put("sonar.exclusions", "**/*generated*.go, **/*.pb.go");
    properties.put("sonar.tests", ".");
    properties.put("sonar.test.inclusions", "**/*_test.go");
    run_ruling_test(properties);
  }

  private void run_ruling_test(Map<String, String> projectProperties) throws IOException {
    Map<String, String> properties = new HashMap<>(projectProperties);
    properties.put("sonar.slang.converter.validation", "log");
    properties.put("sonar.slang.duration.statistics", "true");

    String projectKey = "go".replace("/", "-") + "-project";
    orchestrator.getServer().provisionProject(projectKey, projectKey);
    orchestrator.getServer().associateProjectToQualityProfile(projectKey, "go", "rules");

    File actualDirectory = FileLocation.of("build/tmp/actual/" + "go").getFile();
    actualDirectory.mkdirs();

    File litsDifferencesFile = FileLocation.of("build/" + projectKey + "-differences").getFile();
    SonarScanner build = SonarScanner.create(FileLocation.of("../").getFile())
      .setProjectKey(projectKey)
      .setProjectName(projectKey)
      .setProjectVersion("1")
      .setSourceDirs("./")
      .setSourceEncoding("utf-8")
      .setProperties(properties)
      .setProperty("sonar.lits.dump.old", FileLocation.of("src/test/resources/expected/" + "go").getFile().getAbsolutePath())
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

  @AfterClass
  public static void after() {
    if (keepSonarqubeRunning) {
      // keep server running, use CTRL-C to stop it
      new Scanner(System.in).next();
    }
  }

}
