## Source Code
```java
class A{
  // comment 1
  String name = 'abc';
  /* comment 2 */
}
```
## AST
AST node class                     | line | col | start | end
-----------------------------------|------|-----|-------|----
`UserClass` A {                    | 1    | 7   | 6     | 7  
. `ModifierNode`                   | 1    | 7   | 6     | 7  
. `Field` {                        | 3    | 10  | 33    | 37 
. . `ModifierNode`                 | 3    | 10  | 33    | 37 
. }                                |      |     |       |    
. `FieldDeclarationStatements` {   | 3    | 3   | 26    | 37 
. . `ModifierNode`                 | -1   | -1  | 0     | 0  
. . `FieldDeclaration` {           | -1   | -1  | 0     | 0  
. . . `LiteralExpression`          | 3    | 17  | 40    | 45 
. . . `VariableExpression` {       | 3    | 10  | 33    | 37 
. . . . `EmptyReferenceExpression` | -1   | -1  | 0     | 0  
. . . }                            |      |     |       |    
. . }                              |      |     |       |    
. }                                |      |     |       |    
. `Method` <clinit> {              | -1   | -1  | 0     | 0  
. . `ModifierNode`                 | 1    | 7   | 6     | 7  
. }                                |      |     |       |    
. `Method` clone {                 | -1   | -1  | 0     | 0  
. . `ModifierNode`                 | -1   | -1  | 0     | 0  
. }                                |      |     |       |    
. `UserClassMethods` {             | -1   | -1  | 0     | 0  
. . `Method` <init> {              | -1   | -1  | 0     | 0  
. . . `ModifierNode`               | 1    | 7   | 6     | 7  
. . }                              |      |     |       |    
. }                                |      |     |       |    
. `BridgeMethodCreator`            | -1   | -1  | 0     | 0  
}                                  |      |     |       |    
