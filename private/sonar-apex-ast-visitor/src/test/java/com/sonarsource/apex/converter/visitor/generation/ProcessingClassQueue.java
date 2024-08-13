/*
 * Copyright (C) 2018-2024 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package com.sonarsource.apex.converter.visitor.generation;

import java.util.Deque;
import java.util.LinkedList;
import java.util.Set;

class ProcessingClassQueue {

  private final Deque<Class<?>> queue = new LinkedList<>();
  private final Set<Class<?>> knownClass;

  ProcessingClassQueue(Set<Class<?>> allNativeAstClasses) {
    this.knownClass = allNativeAstClasses;
  }

  void pushIfNotExists(Class<?> cls) {
    if (knownClass.add(cls)) {
      queue.push(cls);
    }
  }

  Class<?> pop() {
    return queue.pop();
  }

  boolean isEmpty() {
    return queue.isEmpty();
  }

}
