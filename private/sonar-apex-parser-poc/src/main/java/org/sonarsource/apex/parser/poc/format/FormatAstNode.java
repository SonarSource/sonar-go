package org.sonarsource.apex.parser.poc.format;

import apex.jorje.data.Locatable;
import apex.jorje.data.Location;
import apex.jorje.data.Locations;
import apex.jorje.semantic.ast.AstNode;
import apex.jorje.semantic.ast.compilation.UserClass;
import apex.jorje.semantic.ast.member.Method;
import apex.jorje.semantic.ast.modifier.Modifier;
import apex.jorje.semantic.ast.visitor.AdditionalPassScope;
import apex.jorje.semantic.exception.Errors;
import org.sonarsource.apex.parser.poc.compiler.AstNodeVisitor;

public class FormatAstNode {

  public static String table(AstNode node) {
    MarkdownTable table = new MarkdownTable("AST node class", "line", "col", "start", "end");
    node.traverse(new FormatVisitor(table), new AdditionalPassScope(Errors.createErrors()));
    return table.toString();
  }

  private static class FormatVisitor extends AstNodeVisitor {

    private final MarkdownTable table;
    private int indent = 0;

    private Locatable nextNodeToAdd = null;

    public FormatVisitor(MarkdownTable table) {
      this.table = table;
    }

    @Override
    protected boolean enterAstNode(Locatable node, AdditionalPassScope scope) {
      if (nextNodeToAdd != null) {
        indent--;
        addNode(nextNodeToAdd, true);
        indent++;
      }
      indent++;
      nextNodeToAdd = node;
      return true;
    }

    @Override
    protected void exitAstNode(Locatable node, AdditionalPassScope scope) {
      indent--;
      if (nextNodeToAdd != null) {
        addNode(nextNodeToAdd, false);
        nextNodeToAdd = null;
      } else {
        table.add(indentString() + "}", "", "", "", "");
      }
    }

    private void addNode(Locatable node, boolean hasChildren) {
      Location location = location(node);
      String typeInfo = "";
      if (node instanceof UserClass) {
        typeInfo = " " + ((UserClass) node).getDefiningType().getApexName();
      } else if (node instanceof Method) {
        typeInfo = " " + ((Method) node).getMethodInfo().getName();
      }
      table.add(
        indentString() + "`" + node.getClass().getSimpleName() + "`" + typeInfo + (hasChildren ? " {" : ""),
        location.getLine(),
        location.getColumn(),
        location.getStartIndex(),
        location.getEndIndex());
    }

    private String indentString() {
      StringBuilder out = new StringBuilder();
      for (int i = 0; i < indent; i++) {
        out.append(". ");
      }
      return out.toString();
    }

    private static Location location(Locatable node) {
      try {
        return node.getLoc() != null ? node.getLoc() : Locations.NONE;
      } catch (RuntimeException e) {
        return Locations.NONE;
      }
    }

  }

}
