/*
 * Copyright (C) 2018-2024 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package com.sonarsource.apex.converter;

import org.sonarsource.slang.api.CodeVerifier;
import org.sonarsource.slang.api.ParseException;

public class ApexCodeVerifier implements CodeVerifier {

  @Override
  public boolean containsCode(String content) {
    int words = content.trim().split("\\w+").length;
    if (words < 2) {
      return false;
    }
    try {
      Parser.parseFreeCode(content);
      return true;
    } catch (ParseException e) {
      // do nothing
    }
    return false;
  }
}
