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
import com.sonar.orchestrator.locator.Locators;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.assertj.core.groups.Tuple;
import org.jetbrains.annotations.NotNull;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonarsource.sonarlint.core.StandaloneSonarLintEngineImpl;
import org.sonarsource.sonarlint.core.analysis.api.ClientInputFile;
import org.sonarsource.sonarlint.core.client.api.common.analysis.Issue;
import org.sonarsource.sonarlint.core.client.api.standalone.StandaloneAnalysisConfiguration;
import org.sonarsource.sonarlint.core.client.api.standalone.StandaloneGlobalConfiguration;
import org.sonarsource.sonarlint.core.client.api.standalone.StandaloneSonarLintEngine;
import org.sonarsource.sonarlint.core.commons.IssueSeverity;
import org.sonarsource.sonarlint.core.commons.Language;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class SonarLintTest {

  @ClassRule
  public static TemporaryFolder temp = new TemporaryFolder();

  private static StandaloneSonarLintEngine sonarlintEngine;

  private static File baseDir;

  @BeforeClass
  public static void prepare() throws Exception {
    // Orchestrator is used only to retrieve plugin artifacts from filesystem or maven
    OrchestratorBuilder orchestratorBuilder = Orchestrator.builderEnv();
    Tests.addGoPlugin(orchestratorBuilder);
    Orchestrator orchestrator = orchestratorBuilder
      .useDefaultAdminCredentialsForBuilds(true)
      .setSonarVersion(System.getProperty(Tests.SQ_VERSION_PROPERTY, Tests.DEFAULT_SQ_VERSION))
      .build();

    Locators locators = orchestrator.getConfiguration().locators();
    StandaloneGlobalConfiguration.Builder sonarLintConfigBuilder = StandaloneGlobalConfiguration.builder();
    orchestrator.getDistribution().getPluginLocations().stream()
      .filter(location -> !location.toString().contains("sonar-reset-data-plugin"))
      .map(plugin -> locators.locate(plugin).toPath())
      .forEach(sonarLintConfigBuilder::addPlugin);

    sonarLintConfigBuilder
      .setSonarLintUserHome(temp.newFolder().toPath())
      .setLogOutput((formattedMessage, level) -> {
        /* Don't pollute logs */
      });
    StandaloneGlobalConfiguration configuration = sonarLintConfigBuilder
      .addEnabledLanguage(Language.GO)
      .build();
    sonarlintEngine = new StandaloneSonarLintEngineImpl(configuration);
    baseDir = temp.newFolder();
  }

  @AfterClass
  public static void stop() {
    sonarlintEngine.stop();
  }

  @Test
  public void test_go() throws Exception {
    ClientInputFile inputFile = prepareInputFile(
      "package main\n"
        + "func empty() {\n"        // go:S1186 (empty function)
        + "}\n"
    );

    assertIssues(analyzeWithSonarLint(inputFile),
      tuple("go:S1186", 2, inputFile.getPath(), IssueSeverity.CRITICAL));
  }

  @Test
  public void test_go_nosonar() throws Exception {
    ClientInputFile goInputFile = prepareInputFile(
      "package main\n"
        + "func empty() { // NOSONAR\n"        //  skipped go:S1186 (empty function)
        + "}\n"
    );
    assertThat(analyzeWithSonarLint(goInputFile)).isEmpty();
  }

  private List<Issue> analyzeWithSonarLint(ClientInputFile inputFile) {
    List<Issue> issues = new ArrayList<>();
    StandaloneAnalysisConfiguration analysisConfiguration = StandaloneAnalysisConfiguration.builder()
      .setBaseDir(baseDir.toPath())
      .addInputFiles(Collections.singletonList(inputFile))
      .build();

    sonarlintEngine.analyze(analysisConfiguration, issues::add, null, null);

    return issues;
  }

  private void assertIssues(List<Issue> issues, Tuple... expectedIssues) {
    assertThat(issues)
      .extracting(Issue::getRuleKey, Issue::getStartLine, issue -> issue.getInputFile().getPath(), Issue::getSeverity)
      .containsExactlyInAnyOrder(expectedIssues);
  }

  private ClientInputFile prepareInputFile(String content) throws IOException {
    File file = new File(baseDir, "foo.go");
    FileUtils.write(file, content, StandardCharsets.UTF_8);
    return createInputFile(file.toPath());
  }

  private ClientInputFile createInputFile(final Path path) {
    return new ClientInputFile() {

      @Override
      public URI uri() {
        return path.toUri();
      }

      @Override
      public String getPath() {
        return path.toString();
      }

      @Override
      public boolean isTest() {
        return false;
      }

      @Override
      public Charset getCharset() {
        return StandardCharsets.UTF_8;
      }


      @Override
      public <G> G getClientObject() {
        return null;
      }

      @Override
      public String contents() throws IOException {
        return Files.readString(path);
      }

      @Override
      public String relativePath() {
        return path.toString();
      }

      @Override
      public InputStream inputStream() throws IOException {
        return Files.newInputStream(path);
      }

      @NotNull
      @Override
      public Language language() {
        return Language.forKey("go").orElse(Language.APEX);
      }
    };
  }

}
