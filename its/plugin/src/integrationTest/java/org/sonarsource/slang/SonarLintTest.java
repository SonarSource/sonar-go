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

import java.io.File;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.apache.commons.io.FileUtils;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sonarsource.sonarlint.core.rpc.client.ClientJsonRpcLauncher;
import org.sonarsource.sonarlint.core.rpc.client.SonarLintRpcClientDelegate;
import org.sonarsource.sonarlint.core.rpc.impl.BackendJsonRpcLauncher;
import org.sonarsource.sonarlint.core.rpc.protocol.SonarLintRpcServer;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.analysis.AnalyzeFilesParams;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.analysis.AnalyzeFilesResponse;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.file.DidUpdateFileSystemParams;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.initialize.ClientConstantInfoDto;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.initialize.FeatureFlagsDto;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.initialize.HttpConfigurationDto;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.initialize.InitializeParams;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.initialize.TelemetryClientConstantAttributesDto;
import org.sonarsource.sonarlint.core.rpc.protocol.common.ClientFileDto;
import org.sonarsource.sonarlint.core.rpc.protocol.common.IssueSeverity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonarsource.slang.TestBase.GO_PLUGIN_LOCATION;

public class SonarLintTest {

  private static final ClientConstantInfoDto IT_CLIENT_INFO = new ClientConstantInfoDto("clientName", "integrationTests");
  private static final TelemetryClientConstantAttributesDto IT_TELEMETRY_ATTRIBUTES = new TelemetryClientConstantAttributesDto(
    "SonarLint ITs", "SonarLint ITs", "1.2.3", "4.5.6", Collections.emptyMap());
  private static final String CONFIG_SCOPE_ID = "my-ide-project-name";

  private static final boolean SHOULD_MANAGE_SMART_NOTIFICATIONS = true;
  private static final boolean TAINT_VULNERABILITIES_ENABLED = true;
  private static final boolean SHOULD_SYNCHRONIZE_PROJECTS = true;
  private static final boolean SHOULD_NOT_MANAGE_LOCAL_SERVER = false;
  private static final boolean ENABLE_SECURITY_HOTSPOTS = true;
  private static final boolean SHOULD_MANAGE_SERVER_SENT_EVENTS = true;
  private static final boolean DISABLE_DATAFLOW_BUG_DETECTION = false;
  private static final boolean SHOULD_MANAGE_FULL_SYNCHRONIZATION = true;
  private static final boolean DISABLE_TELEMETRY = false;
  private static final boolean CAN_NOT_OPEN_FIX_SUGGESTION = false;

  @TempDir
  static Path sonarUserHome;

  private static SonarLintRpcServer backend;
  private static SonarLintRpcClientDelegate client;

  @BeforeAll
  public static void prepare() throws Exception {
    var clientToServerOutputStream = new PipedOutputStream();
    var clientToServerInputStream = new PipedInputStream(clientToServerOutputStream);

    var serverToClientOutputStream = new PipedOutputStream();
    var serverToClientInputStream = new PipedInputStream(serverToClientOutputStream);
    client = new MockSonarLintRpcClientDelegate();
    new BackendJsonRpcLauncher(clientToServerInputStream, serverToClientOutputStream);
    var clientLauncher = new ClientJsonRpcLauncher(serverToClientInputStream, clientToServerOutputStream, client);
    backend = clientLauncher.getServerProxy();

    var featureFlags = new FeatureFlagsDto(
      SHOULD_MANAGE_SMART_NOTIFICATIONS,
      TAINT_VULNERABILITIES_ENABLED,
      SHOULD_SYNCHRONIZE_PROJECTS,
      SHOULD_NOT_MANAGE_LOCAL_SERVER,
      ENABLE_SECURITY_HOTSPOTS,
      SHOULD_MANAGE_SERVER_SENT_EVENTS,
      DISABLE_DATAFLOW_BUG_DETECTION,
      SHOULD_MANAGE_FULL_SYNCHRONIZATION,
      DISABLE_TELEMETRY,
      CAN_NOT_OPEN_FIX_SUGGESTION);
    backend.initialize(
      new InitializeParams(IT_CLIENT_INFO, IT_TELEMETRY_ATTRIBUTES, HttpConfigurationDto.defaultConfig(), null, featureFlags,
        sonarUserHome.resolve("storage"),
        sonarUserHome.resolve("work"),
        Set.of(GO_PLUGIN_LOCATION.getFile().toPath()), Collections.emptyMap(),
        Set.of(org.sonarsource.sonarlint.core.rpc.protocol.common.Language.GO), Collections.emptySet(), Collections.emptySet(), Collections.emptyList(), Collections.emptyList(),
        sonarUserHome.toString(), Map.of(),
        false, null, false, null))
      .get();
  }

  @AfterEach
  public void reset() {
    ((MockSonarLintRpcClientDelegate) client).getRaisedIssues().clear();
  }

  @Test
  public void test_go() throws Exception {
    var inputFile = prepareInputFile("""
      package main
      func empty() {
      }
      """);

    var analyzeResponse = analyzeFile(inputFile);
    assertThat(analyzeResponse.getFailedAnalysisFiles()).isEmpty();
    Awaitility.await().atMost(Duration.ofMillis(200)).untilAsserted(() -> assertThat(((MockSonarLintRpcClientDelegate) client).getRaisedIssues(CONFIG_SCOPE_ID)).isNotEmpty());
    var raisedIssues = ((MockSonarLintRpcClientDelegate) client).getRaisedIssues(CONFIG_SCOPE_ID);
    assertThat(raisedIssues).hasSize(1);
    var issue = raisedIssues.get(0);
    assertThat(issue.getRuleKey()).isEqualTo("go:S1186");
    assertThat(issue.getTextRange().getStartLine()).isEqualTo(2);
    assertThat(issue.getFileUri().getPath()).isEqualTo(inputFile.toString());
    assertThat(issue.getSeverity()).isEqualTo(IssueSeverity.CRITICAL);
  }

  @Test
  public void test_go_nosonar() throws Exception {
    var inputFile = prepareInputFile("""
      package main
      func empty() { // NOSONAR
      }
      """);

    var analyzeResponse = analyzeFile(inputFile);
    assertThat(analyzeResponse.getFailedAnalysisFiles()).isEmpty();
    Awaitility.await().atMost(Duration.ofMillis(200));
    var raisedIssues = ((MockSonarLintRpcClientDelegate) client).getRaisedIssues(CONFIG_SCOPE_ID);
    assertThat(raisedIssues).hasSize(0);
  }

  private AnalyzeFilesResponse analyzeFile(Path filePath) {
    var fileUri = filePath.toUri();
    backend.getFileService().didUpdateFileSystem(new DidUpdateFileSystemParams(List.of(),
      List.of(new ClientFileDto(fileUri, filePath, CONFIG_SCOPE_ID, false, null, filePath.toAbsolutePath(), null, null, true)),
      List.of()));

    return backend.getAnalysisService().analyzeFiles(
      new AnalyzeFilesParams(CONFIG_SCOPE_ID, UUID.randomUUID(), List.of(fileUri), new HashMap<>(), System.currentTimeMillis())).join();
  }

  private Path prepareInputFile(String content) throws IOException {
    File file = new File(sonarUserHome.toFile(), "foo.go");
    FileUtils.write(file, content, StandardCharsets.UTF_8);
    return file.toPath();
  }
}
