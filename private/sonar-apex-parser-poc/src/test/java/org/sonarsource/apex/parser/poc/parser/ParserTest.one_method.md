## Source Code
```java
global class HelloWorld {
    public String hello(Integer x, String y) {
        String message = ('Hello world!');
        for (Integer i = 0, j = 0; i < 10; i++) {
          System.debug(i+j);
        }
        return message;
    }
}
```
## AST
AST node class                               | tokens                     | line | col | start | end
---------------------------------------------|----------------------------|------|-----|-------|----
`CompilationUnit` {                          | `global`…`}`               | 1    | 8   | 7     | 236
. `ClassDeclUnit` {                          | `class`…`}`                | 1    | 8   | 7     | 236
. . `ClassDecl` {                            | `class`…`}`                | 1    | 8   | 7     | 236
. . . `GlobalModifier`                       | `global`                   | 1    | 1   | 0     | 6  
. . . `LocationIdentifier`                   | `HelloWorld`               | 1    | 14  | 13    | 23 
. . . `MethodMember` {                       | `public`…`}`               | 2    | 5   | 30    | 234
. . . . `MethodDecl` {                       | `public`…`}`               | 2    | 5   | 30    | 234
. . . . . `PublicModifier`                   | `public`                   | 2    | 5   | 30    | 36 
. . . . . `ClassTypeRef` {                   | `String`                   | 2    | 12  | 37    | 43 
. . . . . . `LocationIdentifier`             | `String`                   | 2    | 12  | 37    | 43 
. . . . . }                                  |                            |      |     |       |    
. . . . . `LocationIdentifier`               | `hello`                    | 2    | 19  | 44    | 49 
. . . . . `EmptyModifierParameterRef` {      |                            | 2    | 33  | 58    | 57 
. . . . . . `LocationIdentifier`             | `x`                        | 2    | 33  | 58    | 59 
. . . . . . `ClassTypeRef` {                 | `Integer`                  | 2    | 25  | 50    | 57 
. . . . . . . `LocationIdentifier`           | `Integer`                  | 2    | 25  | 50    | 57 
. . . . . . }                                |                            |      |     |       |    
. . . . . }                                  |                            |      |     |       |    
. . . . . `EmptyModifierParameterRef` {      |                            | 2    | 43  | 68    | 67 
. . . . . . `LocationIdentifier`             | `y`                        | 2    | 43  | 68    | 69 
. . . . . . `ClassTypeRef` {                 | `String`                   | 2    | 36  | 61    | 67 
. . . . . . . `LocationIdentifier`           | `String`                   | 2    | 36  | 61    | 67 
. . . . . . }                                |                            |      |     |       |    
. . . . . }                                  |                            |      |     |       |    
. . . . . `BlockStmnt` {                     | `{`…`}`                    | 2    | 46  | 71    | 234
. . . . . . `VariableDeclStmnt` {            | `String`…`'Hello world!'`  | 3    | 9   | 81    | 113
. . . . . . . `VariableDecls` {              | `String`…`'Hello world!'`  | 3    | 9   | 81    | 113
. . . . . . . . `ClassTypeRef` {             | `String`                   | 3    | 9   | 81    | 87 
. . . . . . . . . `LocationIdentifier`       | `String`                   | 3    | 9   | 81    | 87 
. . . . . . . . }                            |                            |      |     |       |    
. . . . . . . . `VariableDecl` {             | `message`…`'Hello world!'` | 3    | 16  | 88    | 113
. . . . . . . . . `LocationIdentifier`       | `message`                  | 3    | 16  | 88    | 95 
. . . . . . . . . `NestedExpr` {             | `'Hello world!'`           | 3    | 27  | 99    | 113
. . . . . . . . . . `LiteralExpr`            | `'Hello world!'`           | 3    | 27  | 99    | 113
. . . . . . . . . }                          |                            |      |     |       |    
. . . . . . . . }                            |                            |      |     |       |    
. . . . . . . }                              |                            |      |     |       |    
. . . . . . }                                |                            |      |     |       |    
. . . . . . `ForLoop` {                      | `for`                      | 4    | 9   | 124   | 127
. . . . . . . `CStyleForControl` {           | `Integer`…`++`             | 4    | 14  | 129   | 162
. . . . . . . . `ForInits` {                 | `Integer`…`0`              | 4    | 14  | 129   | 149
. . . . . . . . . `ClassTypeRef` {           | `Integer`                  | 4    | 14  | 129   | 136
. . . . . . . . . . `LocationIdentifier`     | `Integer`                  | 4    | 14  | 129   | 136
. . . . . . . . . }                          |                            |      |     |       |    
. . . . . . . . . `ForInit` {                | `i`…`0`                    | 4    | 22  | 137   | 142
. . . . . . . . . . `LocationIdentifier`     | `i`                        | 4    | 22  | 137   | 138
. . . . . . . . . . `LiteralExpr`            | `0`                        | 4    | 26  | 141   | 142
. . . . . . . . . }                          |                            |      |     |       |    
. . . . . . . . . `ForInit` {                | `j`…`0`                    | 4    | 29  | 144   | 149
. . . . . . . . . . `LocationIdentifier`     | `j`                        | 4    | 29  | 144   | 145
. . . . . . . . . . `LiteralExpr`            | `0`                        | 4    | 33  | 148   | 149
. . . . . . . . . }                          |                            |      |     |       |    
. . . . . . . . }                            |                            |      |     |       |    
. . . . . . . . `BooleanExpr` {              | `i`…`10`                   | 4    | 36  | 151   | 157
. . . . . . . . . `VariableExpr` {           | `i`                        | 4    | 36  | 151   | 152
. . . . . . . . . . `LocationIdentifier`     | `i`                        | 4    | 36  | 151   | 152
. . . . . . . . . }                          |                            |      |     |       |    
. . . . . . . . . `LiteralExpr`              | `10`                       | 4    | 40  | 155   | 157
. . . . . . . . }                            |                            |      |     |       |    
. . . . . . . . `PostfixExpr` {              | `++`                       | 4    | 45  | 160   | 162
. . . . . . . . . `VariableExpr` {           | `i`                        | 4    | 44  | 159   | 160
. . . . . . . . . . `LocationIdentifier`     | `i`                        | 4    | 44  | 159   | 160
. . . . . . . . . }                          |                            |      |     |       |    
. . . . . . . . }                            |                            |      |     |       |    
. . . . . . . }                              |                            |      |     |       |    
. . . . . . . `BlockStmnt` {                 | `{`…`}`                    | 4    | 49  | 164   | 204
. . . . . . . . `ExpressionStmnt` {          | `(`…`;`                    | 5    | 18  | 183   | 194
. . . . . . . . . `MethodCallExpr` {         | `System.debug`…`j`         | 5    | 11  | 176   | 192
. . . . . . . . . . `LocationIdentifier`     |                            | 5    | 11  | 176   | 182
. . . . . . . . . . `LocationIdentifier`     |                            | 5    | 18  | 183   | 188
. . . . . . . . . . `BinaryExpr` {           | `i`…`j`                    | 5    | 24  | 189   | 192
. . . . . . . . . . . `VariableExpr` {       | `i`                        | 5    | 24  | 189   | 190
. . . . . . . . . . . . `LocationIdentifier` | `i`                        | 5    | 24  | 189   | 190
. . . . . . . . . . . }                      |                            |      |     |       |    
. . . . . . . . . . . `VariableExpr` {       | `j`                        | 5    | 26  | 191   | 192
. . . . . . . . . . . . `LocationIdentifier` | `j`                        | 5    | 26  | 191   | 192
. . . . . . . . . . . }                      |                            |      |     |       |    
. . . . . . . . . . }                        |                            |      |     |       |    
. . . . . . . . . }                          |                            |      |     |       |    
. . . . . . . . }                            |                            |      |     |       |    
. . . . . . . }                              |                            |      |     |       |    
. . . . . . }                                |                            |      |     |       |    
. . . . . . `ReturnStmnt` {                  | `return`…`;`               | 7    | 9   | 213   | 228
. . . . . . . `VariableExpr` {               | `message`                  | 7    | 16  | 220   | 227
. . . . . . . . `LocationIdentifier`         | `message`                  | 7    | 16  | 220   | 227
. . . . . . . }                              |                            |      |     |       |    
. . . . . . }                                |                            |      |     |       |    
. . . . . }                                  |                            |      |     |       |    
. . . . }                                    |                            |      |     |       |    
. . . }                                      |                            |      |     |       |    
. . }                                        |                            |      |     |       |    
. }                                          |                            |      |     |       |    
}                                            |                            |      |     |       |    
## Comments
