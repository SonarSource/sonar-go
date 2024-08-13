/*
 * Copyright (C) 2018-2024 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package com.sonarsource.apex.it;
/*
 * SonarQube Java
 * Copyright (C) 2013-2018 SonarSource SA
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

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.OrchestratorBuilder;
import com.sonar.orchestrator.build.SonarScanner;
import com.sonar.orchestrator.container.Edition;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.locator.Location;
import com.sonar.orchestrator.locator.MavenLocation;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.sonarqube.ws.Measures.ComponentWsResponse;
import org.sonarqube.ws.Measures.Measure;
import org.sonarqube.ws.client.HttpConnector;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.WsClientFactories;
import org.sonarqube.ws.client.measures.ComponentRequest;
import org.sonarsource.analyzer.commons.ProfileGenerator;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

public class ApexRulingTest {

  private static final String APEX = "apex";
  private static final String SQ_VERSION_PROPERTY = "sonar.runtimeVersion";
  private static final String DEFAULT_SQ_VERSION = "LATEST_RELEASE";
  private static final String PROJECT_KEY = APEX + "-project";

  private static Orchestrator orchestrator;
  private static boolean keepSonarQubeRunning = "true".equals(System.getProperty("keepSonarqubeRunning"));

  @BeforeClass
  public static void setUp() {
    OrchestratorBuilder builder = Orchestrator.builderEnv()
      .useDefaultAdminCredentialsForBuilds(true)
      .setSonarVersion(System.getProperty(SQ_VERSION_PROPERTY, DEFAULT_SQ_VERSION))
      .addPlugin(MavenLocation.of("org.sonarsource.sonar-lits-plugin", "sonar-lits-plugin", "0.8.0.1209"))
      .setEdition(Edition.ENTERPRISE)
      .activateLicense();

    addApexPlugin(builder);

    orchestrator = builder.build();
    orchestrator.start();

    String propertyKeepSonarQubeRunning = System.getProperty("keepSonarQubeRunning");
    if (!StringUtils.isEmpty(propertyKeepSonarQubeRunning) && Boolean.parseBoolean(propertyKeepSonarQubeRunning)) {
      keepSonarQubeRunning = true;
    }

    ProfileGenerator.RulesConfiguration apexRulesConfiguration = new ProfileGenerator.RulesConfiguration();

    apexRulesConfiguration.add("S1451", "headerFormat", "^(?i).*copyright");
    apexRulesConfiguration.add("S1451", "isRegularExpression", "true");
    apexRulesConfiguration.add("S1479", "maximum", "5");
    apexRulesConfiguration.add("S1151", "max", "5");

    File apexProfile = ProfileGenerator.generateProfile(orchestrator.getServer().getUrl(), APEX, APEX, apexRulesConfiguration, Collections.emptySet());
    orchestrator.getServer().restoreProfile(FileLocation.of(apexProfile));
  }

  private static void addApexPlugin(OrchestratorBuilder builder) {
    String slangVersion = System.getProperty("slangVersion");
    Location pluginLocation;
    if (StringUtils.isEmpty(slangVersion)) {
      // use the plugin that was built on local machine
      pluginLocation = FileLocation.byWildcardMavenFilename(new File("../../sonar-" + APEX + "-plugin/build/libs"), "sonar-" + APEX + "-plugin-*.jar");
    } else {
      // QA environment downloads the plugin built by the CI job
      pluginLocation = MavenLocation.of("com.sonarsource.slang", "sonar-" + APEX + "-plugin", slangVersion);
    }
    builder.addPlugin(pluginLocation);
  }

  @Test
  // @Ignore because it should only be run manually
  @Ignore
  public void test_apex_manual_keep_sonarqube_server_up() throws IOException {
    keepSonarQubeRunning = true;
    test_apex();
  }

  @Test
  public void test_apex() throws IOException {
    Map<String, String> properties = new HashMap<>();
    properties.put("sonar.slang.converter.validation", "throw");
    properties.put("sonar.inclusions",
      "sources/projects/**/*.cls," +
        "sources/projects/**/*.trigger," +
        "ruling/src/test/resources/sources/**/*.cls");

    orchestrator.getServer().provisionProject(PROJECT_KEY, PROJECT_KEY);
    orchestrator.getServer().associateProjectToQualityProfile(PROJECT_KEY, APEX, "rules");

    File actualDirectory = FileLocation.of("build/actual/" + APEX).getFile();
    actualDirectory.mkdirs();

    File litsDifferencesFile = FileLocation.of("build/" + APEX + "-differences").getFile();
    SonarScanner build = SonarScanner.create(FileLocation.of("../").getFile())
      .setProjectKey(PROJECT_KEY)
      .setProjectName(PROJECT_KEY)
      .setProjectVersion("1")
      .setSourceDirs("./")
      .setSourceEncoding("utf-8")
      .setProperties(properties)
      .setProperty("dump.old", FileLocation.of("src/test/resources/expected/" + APEX).getFile().getAbsolutePath())
      .setProperty("dump.new", actualDirectory.getAbsolutePath())
      .setProperty("lits.differences", litsDifferencesFile.getAbsolutePath())
      .setProperty("sonar.cpd.skip", "true")
      .setProperty("sonar.scm.disabled", "true")
      .setProperty("sonar.language", APEX)
      .setEnvironmentVariable("SONAR_RUNNER_OPTS", "-Xmx1024m");

    orchestrator.executeBuild(build);

    String litsDifference = new String(Files.readAllBytes(litsDifferencesFile.toPath()));
    assertThat(litsDifference).isEmpty();

    assertThat(getMeasureAsInt("files")).isEqualTo(1180);
    assertThat(getMeasureAsInt("sources/projects/HEDAP/src/classes/ERR_AsyncErrors.cls", "functions")).isEqualTo(11);
    assertThat(getMeasureAsInt("functions")).isEqualTo(11032);
    assertThat(getMeasureAsInt("sources/projects/HEDAP/src/classes/ERR_AsyncErrors.cls", "statements")).isEqualTo(46);
    assertThat(getMeasureAsInt("statements")).isEqualTo(93053);
  }

  private static Measure getMeasure(@Nullable String componentKey, String metricKey) {
    String component = PROJECT_KEY;
    if (componentKey != null) {
      component += ":" + componentKey;
    }

    ComponentWsResponse response = newWsClient().measures().component(new ComponentRequest()
      .setComponent(component)
      .setMetricKeys(singletonList(metricKey)));
    List<Measure> measures = response.getComponent().getMeasuresList();
    return measures.size() == 1 ? measures.get(0) : null;
  }

  private static Integer getMeasureAsInt(String metricKey) {
    return getMeasureAsInt(null, metricKey);
  }

  private static Integer getMeasureAsInt(@Nullable String componentKey, String metricKey) {
    Measure measure = getMeasure(componentKey, metricKey);
    return (measure == null) ? null : Integer.parseInt(measure.getValue());
  }

  private static WsClient newWsClient() {
    return WsClientFactories.getDefault().newClient(HttpConnector.newBuilder()
      .url(orchestrator.getServer().getUrl())
      .build());
  }

  @AfterClass
  public static void after() {
    if (keepSonarQubeRunning) {
      // keep server running, use CTRL-C to stop it
      new Scanner(System.in).next();
    }
  }

}
