package org.sonarsource.apex.parser.poc.format;

import apex.jorje.data.Location;
import org.sonarsource.apex.parser.poc.parser.SimpleAstNode;

public class FormatCompilationUnit {

  public static String table(SimpleAstNode node) {
    MarkdownTable table = new MarkdownTable("AST node class", "tokens", "line", "col", "start", "end");
    addNode(table, node, 0);
    return table.toString();
  }

  private static void addNode(MarkdownTable table, SimpleAstNode node, int indent) {
    Location location = node.location();
    boolean hasChildren = !node.children.isEmpty();
    String tokens = (node.tokens.isEmpty() ? "" : "`" + node.tokens.get(0).getText() + "`") +
      (node.tokens.size() > 1 ? "…`" + node.tokens.get(node.tokens.size() - 1).getText() + "`" : "");
    table.add(
      indentString(indent) + "`" + node.name + "`" + (hasChildren ? " {" : ""),
      tokens,
      location.getLine(),
      location.getColumn(),
      location.getStartIndex(),
      location.getEndIndex());
    for (SimpleAstNode child : node.children) {
      addNode(table, child, indent + 1);
    }
    if (hasChildren) {
      table.add(indentString(indent) + "}", "", "", "", "", "");
    }
  }

  private static String indentString(int indent) {
    StringBuilder out = new StringBuilder();
    for (int i = 0; i < indent; i++) {
      out.append(". ");
    }
    return out.toString();
  }

}
