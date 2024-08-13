/*
 * Copyright (C) 2018-2024 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package com.sonarsource.apex.plugin;

import org.junit.jupiter.api.Test;
import org.sonar.api.Plugin;
import org.sonar.api.SonarEdition;
import org.sonar.api.SonarQubeSide;
import org.sonar.api.SonarRuntime;
import org.sonar.api.internal.PluginContextImpl;
import org.sonar.api.internal.SonarRuntimeImpl;
import org.sonar.api.utils.Version;

import static org.assertj.core.api.Assertions.assertThat;

class ApexPluginTest {

  private ApexPlugin apexPlugin = new ApexPlugin();

  @Test
  void sonarqube_7_9_extensions() {
    SonarRuntime runtime = SonarRuntimeImpl.forSonarQube(Version.create(7, 9), SonarQubeSide.SERVER, SonarEdition.ENTERPRISE);
    Plugin.Context context = new Plugin.Context(runtime);
    apexPlugin.define(context);
    assertThat(context.getExtensions()).hasSize(11);
  }

  @Test
  void test_sonarlint() {
    SonarRuntime runtime = SonarRuntimeImpl.forSonarLint(Version.create(3, 9));
    Plugin.Context context = new PluginContextImpl.Builder().setSonarRuntime(runtime).build();
    apexPlugin.define(context);
    assertThat(context.getExtensions()).hasSize(4);
  }

}
