/*
 * Copyright (C) 2018-2024 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package com.sonarsource.apex.converter.impl;

import java.util.Locale;
import org.sonarsource.slang.api.TreeMetaData;
import org.sonarsource.slang.impl.IdentifierTreeImpl;

public class ApexIdentifierTreeImpl extends IdentifierTreeImpl {
  private String identifier;

  public ApexIdentifierTreeImpl(TreeMetaData metaData, String name) {
    super(metaData, name);
    identifier = name.toLowerCase(Locale.ENGLISH);
  }

  @Override
  public String identifier() {
    return identifier;
  }
}
