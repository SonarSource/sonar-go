package org.sonar.go.checks.utils;

import java.util.function.Predicate;
import org.sonarsource.slang.api.NativeTree;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.persistence.conversion.StringNativeKind;

public class TreeUtils {
  private TreeUtils() {
  }

  public static Predicate<Tree> IS_NOT_SEMICOLON = Predicate.not(tree -> tree instanceof NativeTree nativeTree
    && nativeTree.nativeKind() instanceof StringNativeKind stringNativeKind
    && stringNativeKind.toString().equals("Semicolon"));
}
