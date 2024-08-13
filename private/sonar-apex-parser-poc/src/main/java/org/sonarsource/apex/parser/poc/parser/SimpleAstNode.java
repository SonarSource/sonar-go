package org.sonarsource.apex.parser.poc.parser;

import apex.jorje.data.Location;
import apex.jorje.data.Locations;
import java.util.ArrayList;
import java.util.List;
import org.sonarsource.apex.parser.poc.lexer.Tokens;

public class SimpleAstNode {

  public final String name;

  private final Location location;

  public final List<SimpleAstNode> children = new ArrayList<>();

  public Tokens tokens = new Tokens();

  public SimpleAstNode(String name, Location location) {
    this.name = name;
    this.location = location;
  }

  public Location location() {
    if (location != null) {
      return location;
    }
    if (children.isEmpty()) {
      throw new IllegalStateException("Null location and no child for " + name);
    }
    if (children.size() == 1) {
      return children.get(0).location();
    }
    Location first = children.get(0).location();
    Location last = children.get(children.size() - 1).location();
    return Locations.loc(first.getStartIndex(), last.getEndIndex(), first.getLine(), first.getColumn());
  }

}
