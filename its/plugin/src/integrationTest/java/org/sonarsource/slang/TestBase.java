/*
 * SonarSource Go
 * Copyright (C) 2018-2024 SonarSource SA
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
import com.sonar.orchestrator.build.SonarScannerInstaller;
import com.sonar.orchestrator.config.Configuration;
import com.sonar.orchestrator.junit5.OrchestratorExtension;
import com.sonar.orchestrator.junit5.OrchestratorExtensionBuilder;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.version.Version;
import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.sonarqube.ws.Issues;
import org.sonarqube.ws.Measures.ComponentWsResponse;
import org.sonarqube.ws.Measures.Measure;
import org.sonarqube.ws.client.HttpConnector;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.WsClientFactories;
import org.sonarqube.ws.client.issues.SearchRequest;
import org.sonarqube.ws.client.measures.ComponentRequest;

import static java.util.Collections.singletonList;

public abstract class TestBase {

  public static final String KEEP_ORCHESTRATOR_RUNNING_ENV = "KEEP_ORCHESTRATOR_RUNNING";
  public static final boolean KEEP_ORCHESTRATOR_RUNNING = "true".equals(System.getenv(KEEP_ORCHESTRATOR_RUNNING_ENV));
  public static final String SQ_VERSION_PROPERTY = "sonar.runtimeVersion";
  public static final String DEFAULT_SQ_VERSION = "LATEST_RELEASE";
  public static final FileLocation GO_PLUGIN_LOCATION = FileLocation.byWildcardFilename(new File("../../sonar-go-plugin/build/libs"), "sonar-go-plugin-*-all.jar");
  public static final Configuration CONFIGURATION = Configuration.createEnv();

  private static final AtomicInteger REQUESTED_ORCHESTRATORS_KEY = new AtomicInteger();
  private static final CountDownLatch IS_ORCHESTRATOR_READY = new CountDownLatch(1);
  private static final String SCANNER_VERSION = "6.2.1.4610";

  private static final OrchestratorExtensionBuilder orchestratorBuilder = OrchestratorExtension.builder(CONFIGURATION);
  public static final Orchestrator ORCHESTRATOR = orchestratorBuilder
    .addPlugin(GO_PLUGIN_LOCATION)
    .useDefaultAdminCredentialsForBuilds(true)
    .setSonarVersion(System.getProperty(SQ_VERSION_PROPERTY, DEFAULT_SQ_VERSION))
    .restoreProfileAtStartup(FileLocation.of("src/integrationTest/resources/nosonar-go.xml"))
    .build();

  @BeforeAll
  public static void startOrchestrator() {
    // This is to avoid multiple starts when using nested tests
    // See https://github.com/junit-team/junit5/issues/2421
    if (REQUESTED_ORCHESTRATORS_KEY.getAndIncrement() == 0) {
      ORCHESTRATOR.start();
      // installed scanner will be shared by all tests
      new SonarScannerInstaller(CONFIGURATION.locators()).install(Version.create(SCANNER_VERSION), CONFIGURATION.fileSystem().workspace());
      IS_ORCHESTRATOR_READY.countDown();
    } else {
      try {
        IS_ORCHESTRATOR_READY.await();
      } catch (InterruptedException e) {
        throw new IllegalStateException(e);
      }
    }
  }

  @AfterAll
  public static void stopOrchestrator() {
    if (!KEEP_ORCHESTRATOR_RUNNING && REQUESTED_ORCHESTRATORS_KEY.decrementAndGet() == 0) {
      ORCHESTRATOR.stop();
    }
  }

  protected SonarScanner getSonarScanner(String projectKey, String directoryToScan, String languageKey) {
    return getSonarScanner(projectKey, directoryToScan, languageKey, null);
  }

  protected SonarScanner getSonarScanner(String projectKey, String directoryToScan, String languageKey, @Nullable String profileName) {
    ORCHESTRATOR.getServer().provisionProject(projectKey, projectKey);
    if (profileName != null) {
      ORCHESTRATOR.getServer().associateProjectToQualityProfile(projectKey, languageKey, profileName);
    }
    return SonarScanner.create()
      .setProjectDir(new File(directoryToScan, languageKey))
      .setProjectKey(projectKey)
      .setProjectName(projectKey)
      .setProjectVersion("1")
      .setSourceDirs(".");
  }

  protected Measure getMeasure(String projectKey, String metricKey) {
    return getMeasure(projectKey, null, metricKey);
  }

  protected Measure getMeasure(String projectKey, @Nullable String componentKey, String metricKey) {
    String component;
    if (componentKey != null) {
      component = projectKey + ":" + componentKey;
    } else {
      component = projectKey;
    }
    ComponentWsResponse response = newWsClient().measures().component(new ComponentRequest()
      .setComponent(component)
      .setMetricKeys(singletonList(metricKey)));
    List<Measure> measures = response.getComponent().getMeasuresList();
    return measures.size() == 1 ? measures.get(0) : null;
  }

  protected List<Issues.Issue> getIssuesForRule(String componentKey, String rule) {
    return newWsClient().issues().search(new SearchRequest()
      .setRules(Collections.singletonList(rule))
      .setComponentKeys(Collections.singletonList(componentKey))).getIssuesList();
  }

  protected Integer getMeasureAsInt(String componentKey, String metricKey) {
    Measure measure = getMeasure(componentKey, metricKey);
    return (measure == null) ? null : Integer.parseInt(measure.getValue());
  }

  protected static WsClient newWsClient() {
    return WsClientFactories.getDefault().newClient(HttpConnector.newBuilder()
      .url(ORCHESTRATOR.getServer().getUrl())
      .build());
  }

}
