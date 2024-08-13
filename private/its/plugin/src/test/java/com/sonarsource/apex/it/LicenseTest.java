/*
 * Copyright (C) 2018-2024 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package com.sonarsource.apex.it;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarScanner;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.sonarqube.ws.Ce;
import org.sonarqube.ws.Ce.ComponentResponse;
import org.sonarqube.ws.Ce.Task;
import org.sonarqube.ws.client.ce.ComponentRequest;

import static com.sonarsource.apex.it.Tests.createCommunityEditionOrchestratorBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class LicenseTest extends TestBase {

  private static final String BASE_DIRECTORY_MEASURES = "projects/measures/";
  private static final String PROJECT_KEY = "apexLicense";

  @Test
  public void analysis_fails_when_license_is_missing_with_correct_edition() {
    SonarScanner sonarScanner = getSonarScanner(PROJECT_KEY, BASE_DIRECTORY_MEASURES);
    ORCHESTRATOR.clearLicense();
    ORCHESTRATOR.executeBuild(sonarScanner);

    Task currentTask = getCeTaskStatus(PROJECT_KEY);
    assertThat(currentTask.getStatus()).isEqualTo(Ce.TaskStatus.FAILED);
    assertThat(currentTask.getErrorType()).isEqualTo("LICENSING");
  }

  @Test
  public void serverStartupShouldFailInCommunityEdition() throws IOException {
    var orchestrator = createCommunityEditionOrchestratorBuilder().build();

    assertThatExceptionOfType(IllegalStateException.class)
      .isThrownBy(orchestrator::start)
      .withMessage("Server startup failure");

    List<String> webLogs = readWebLogs(orchestrator);
    assertThat(webLogs)
      .anyMatch(line -> line.contains("java.lang.IllegalStateException: Fail to load plugin Apex Code Quality and Security [sonarapex]"))
      .anyMatch(line -> line.contains("Caused by: java.lang.NoClassDefFoundError: com/sonarsource/plugins/license/api/LicensedPluginRegistration"))
      .anyMatch(line -> line.contains("Caused by: java.lang.ClassNotFoundException: com.sonarsource.plugins.license.api.LicensedPluginRegistration"));
  }

  private static List<String> readWebLogs(Orchestrator orchestrator) throws IOException {
    ArrayList<String> result = new ArrayList<>();

    var inputStream = new FileInputStream(orchestrator.getServer().getWebLogs());
    try (var br = new BufferedReader(new InputStreamReader(inputStream))) {
      String line;
      while ((line = br.readLine()) != null) {
        result.add(line);
      }
    }
    return result;
  }

  private Task getCeTaskStatus(String projectKey) {
    ComponentRequest componentRequest = new ComponentRequest();
    componentRequest.setComponent(projectKey);
    ComponentResponse componentResponse = newAdminWsClient().ce().component(componentRequest);
    return componentResponse.getCurrent();
  }

}
