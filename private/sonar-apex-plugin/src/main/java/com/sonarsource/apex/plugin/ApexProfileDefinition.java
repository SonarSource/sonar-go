/*
 * Copyright (C) 2018-2024 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package com.sonarsource.apex.plugin;

import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition;
import org.sonarsource.analyzer.commons.BuiltInQualityProfileJsonLoader;

public class ApexProfileDefinition implements BuiltInQualityProfilesDefinition {

  static final String PATH_TO_JSON = "org/sonar/l10n/apex/rules/apex/Sonar_way_profile.json";

  @Override
  public void define(BuiltInQualityProfilesDefinition.Context context) {
    NewBuiltInQualityProfile profile = context.createBuiltInQualityProfile(ApexPlugin.PROFILE_NAME, ApexPlugin.APEX_LANGUAGE_KEY);
    BuiltInQualityProfileJsonLoader.load(profile, ApexPlugin.APEX_REPOSITORY_KEY, PATH_TO_JSON);
    profile.done();
  }
}
