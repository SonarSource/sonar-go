/*
 * Copyright (C) 2018-2024 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package com.sonarsource.apex.it;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.OrchestratorBuilder;
import com.sonar.orchestrator.container.Edition;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.locator.Location;
import com.sonar.orchestrator.locator.MavenLocation;
import java.io.File;
import org.apache.commons.lang.StringUtils;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
  MeasuresTest.class,
  ExternalReportTest.class,
  DuplicationsTest.class,
  LicenseTest.class,
  SonarLintTest.class,
  NoSonarTest.class
})
public class Tests {

  private static final String APEX = "apex";

  static final String SQ_VERSION_PROPERTY = "sonar.runtimeVersion";
  static final String DEFAULT_SQ_VERSION = "LATEST_RELEASE";

  @ClassRule
  public static final Orchestrator ORCHESTRATOR = createOrchestratorBuilder().build();

  static OrchestratorBuilder createOrchestratorBuilder() {
    return createCommunityEditionOrchestratorBuilder()
      .setEdition(Edition.ENTERPRISE)
      .activateLicense();
  }

  static OrchestratorBuilder createCommunityEditionOrchestratorBuilder() {
    OrchestratorBuilder orchestratorBuilder = Orchestrator.builderEnv();
    addApexPlugin(orchestratorBuilder);
    return orchestratorBuilder
      .useDefaultAdminCredentialsForBuilds(true)
      .restoreProfileAtStartup(FileLocation.of("src/test/resources/norule.xml"))
      .restoreProfileAtStartup(FileLocation.of("src/test/resources/nosonar-apex.xml"))
      .setSonarVersion(System.getProperty(SQ_VERSION_PROPERTY, DEFAULT_SQ_VERSION));
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

}
