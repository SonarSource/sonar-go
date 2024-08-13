/*
 * Copyright (C) 2018-2024 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package com.sonarsource.apex.converter;

import org.sonarsource.slang.api.NativeKind;

class ClassNativeKind implements NativeKind {
  private Class<?> kind;

  public ClassNativeKind(Class<?> kind) {
    this.kind = kind;
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (other == null || getClass() != other.getClass()) {
      return false;
    }
    return kind.equals(((ClassNativeKind) other).kind);
  }

  @Override
  public int hashCode() {
    return kind.hashCode();
  }

  @Override
  public String toString() {
    return kind.getSimpleName();
  }

}
