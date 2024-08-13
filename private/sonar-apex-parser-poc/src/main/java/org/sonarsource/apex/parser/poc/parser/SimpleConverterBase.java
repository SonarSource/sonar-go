package org.sonarsource.apex.parser.poc.parser;

import apex.jorje.data.Identifier;
import apex.jorje.data.Location;
import apex.jorje.data.ast.CompilationUnit;
import java.util.Deque;
import java.util.LinkedList;
import org.sonarsource.apex.parser.poc.lexer.Tokens;

public abstract class SimpleConverterBase implements CompilationUnit.SwitchBlock {

  private final Deque<SimpleAstNode> nodeStack = new LinkedList<>();

  private final Tokens tokens;

  public SimpleConverterBase(Tokens tokens) {
    this.tokens = tokens;
  }

  public SimpleAstNode convert(CompilationUnit node) {
    SimpleAstNode root = addNode("CompilationUnit", null, () -> node._switch(this));
    if (!nodeStack.isEmpty()) {
      throw new IllegalStateException("Unexpected stack size: " + nodeStack.size());
    }
    return root;
  }

  public void _case(Identifier node) {
    // ignore SyntheticIdentifier
    addNode(type(node), node.getLoc(), null);
  }

  public void _case(apex.jorje.data.ast.VersionRef node) {
  }

  public void _case(apex.jorje.data.ast.CompilationUnit.InvalidDeclUnit node) {
  }

  protected SimpleAstNode addNode(String name, Location location, Runnable addContent) {
    SimpleAstNode simpleNode = new SimpleAstNode(name, location);
    if (!nodeStack.isEmpty()) {
      nodeStack.peek().children.add(simpleNode);
    }
    nodeStack.push(simpleNode);
    if (addContent != null) {
      addContent.run();
    }
    nodeStack.pop();
    Location effectiveLocation = simpleNode.location();
    simpleNode.tokens = tokens.lookup(effectiveLocation.getStartIndex(), effectiveLocation.getEndIndex());
    return simpleNode;
  }

  protected static String type(Object object) {
    return object.getClass().getSimpleName();
  }

}
