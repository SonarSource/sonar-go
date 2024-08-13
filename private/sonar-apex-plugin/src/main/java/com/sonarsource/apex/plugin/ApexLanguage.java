/*
 * Copyright (C) 2018-2024 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package com.sonarsource.apex.plugin;

import org.sonar.api.config.Configuration;
import org.sonar.api.resources.AbstractLanguage;

public class ApexLanguage extends AbstractLanguage {

  private Configuration configuration;

  public ApexLanguage(Configuration configuration) {
    super(ApexPlugin.APEX_LANGUAGE_KEY, ApexPlugin.APEX_LANGUAGE_NAME);
    this.configuration = configuration;
  }


  @Override
  public String[] getFileSuffixes() {
    String[] suffixes = configuration.getStringArray(ApexPlugin.APEX_FILE_SUFFIXES_KEY);
    if (suffixes.length == 0) {
      suffixes = ApexPlugin.APEX_FILE_SUFFIXES_DEFAULT_VALUE.split(",");
    }
    return suffixes;
  }
}
