/*
 * Copyright (C) 2018-2024 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package com.sonarsource.apex.plugin;

import java.util.List;
import java.util.stream.Collectors;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.sonarsource.slang.testing.PackageScanner;

import static org.assertj.core.api.Java6Assertions.assertThat;

class ApexCheckListTest {

  private static final String APEX_CHECKS_PACKAGE = "com.sonarsource.apex.checks";

  @Test
  void apex_checks_size() {
    Assertions.assertThat(ApexCheckList.checks()).hasSizeGreaterThanOrEqualTo(40);
  }

  @Test
  void apex_specific_checks_are_added_to_check_list() {
    List<String> languageImplementation = PackageScanner.findSlangChecksInPackage(APEX_CHECKS_PACKAGE);

    List<String> checkListNames = ApexCheckList.checks().stream().map(Class::getName).collect(Collectors.toList());
    List<String> languageCheckList = ApexCheckList.APEX_LANGUAGE_SPECIFIC_CHECKS.stream().map(Class::getName).collect(Collectors.toList());

    for (String languageCheck : languageImplementation) {
      assertThat(checkListNames).contains(languageCheck);
      assertThat(languageCheckList).contains(languageCheck);
    }
  }

  @Test
  void apex_excluded_not_present() {
    List<Class<?>> checks = ApexCheckList.checks();
    for (Class excluded : ApexCheckList.APEX_CHECK_BLACK_LIST) {
      assertThat(checks).doesNotContain(excluded);
    }
  }

  @Test
  void apex_specific_are_present() {
    List<Class<?>> checks = ApexCheckList.checks();
    for (Class specificCheck : ApexCheckList.APEX_LANGUAGE_SPECIFIC_CHECKS) {
      assertThat(checks).contains(specificCheck);
    }
  }

}
