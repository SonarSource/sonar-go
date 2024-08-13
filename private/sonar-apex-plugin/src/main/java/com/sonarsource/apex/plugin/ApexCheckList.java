/*
 * Copyright (C) 2018-2024 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package com.sonarsource.apex.plugin;

import com.sonarsource.apex.checks.AbsoluteSalesforceUrlCheck;
import com.sonarsource.apex.checks.BusinessLogicInsideTriggerCheck;
import com.sonarsource.apex.checks.DMLStatementInsideLoopCheck;
import com.sonarsource.apex.checks.HardcodedMessageInAddErrorCheck;
import com.sonarsource.apex.checks.HardcodedSalesforceRecordIdCheck;
import com.sonarsource.apex.checks.MissingSharingLevelCheck;
import com.sonarsource.apex.checks.QueriesSharingUserPermissionsCheck;
import com.sonarsource.apex.checks.TestAnnotationWithSeeAllDataCheck;
import com.sonarsource.apex.checks.TestFunctionsContainSystemRunAsCheck;
import com.sonarsource.apex.checks.TestedCodeShouldContainStartAndStopTestCheck;
import com.sonarsource.apex.checks.TriggersProcessRecordsInBulkCheck;
import com.sonarsource.apex.checks.WrongGetRecordTypeInfosMethodUseCheck;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.sonarsource.slang.checks.CheckList;
import org.sonarsource.slang.checks.OctalValuesCheck;

public final class ApexCheckList {

  private ApexCheckList() {
    // utility class
  }

  static final Class[] APEX_CHECK_BLACK_LIST = {
    // Apex does not support Octal values
      OctalValuesCheck.class
  };

  static final List<Class<?>> APEX_LANGUAGE_SPECIFIC_CHECKS = Arrays.asList(
    AbsoluteSalesforceUrlCheck.class,
    BusinessLogicInsideTriggerCheck.class,
    DMLStatementInsideLoopCheck.class,
    HardcodedMessageInAddErrorCheck.class,
    HardcodedSalesforceRecordIdCheck.class,
    MissingSharingLevelCheck.class,
    QueriesSharingUserPermissionsCheck.class,
    TestedCodeShouldContainStartAndStopTestCheck.class,
    TestAnnotationWithSeeAllDataCheck.class,
    TestFunctionsContainSystemRunAsCheck.class,
    TriggersProcessRecordsInBulkCheck.class,
    WrongGetRecordTypeInfosMethodUseCheck.class);

  public static List<Class<?>> checks() {
    List<Class<?>> list = new ArrayList<>(CheckList.excludeChecks(APEX_CHECK_BLACK_LIST));
    list.addAll(APEX_LANGUAGE_SPECIFIC_CHECKS);
    return list;
  }
}
