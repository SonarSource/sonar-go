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
AST node class                               | line | col | start | end
---------------------------------------------|------|-----|-------|----
`UserClass` HelloWorld {                     | 1    | 14  | 13    | 23 
. `ModifierNode`                             | 1    | 14  | 13    | 23 
. `Method` hello {                           | 2    | 19  | 44    | 49 
. . `ModifierNode`                           | 2    | 19  | 44    | 49 
. . `Parameter` {                            | 2    | 33  | 58    | 59 
. . . `ModifierNode`                         | 2    | 33  | 58    | 59 
. . }                                        |      |     |       |    
. . `Parameter` {                            | 2    | 43  | 68    | 69 
. . . `ModifierNode`                         | 2    | 43  | 68    | 69 
. . }                                        |      |     |       |    
. . `BlockStatement` {                       | 2    | 46  | 71    | 234
. . . `VariableDeclarationStatements` {      | 3    | 9   | 81    | 95 
. . . . `ModifierNode`                       | -1   | -1  | 0     | 0  
. . . . `VariableDeclaration` {              | 3    | 16  | 88    | 95 
. . . . . `LiteralExpression`                | 3    | 27  | 99    | 113
. . . . . `VariableExpression` {             | 3    | 16  | 88    | 95 
. . . . . . `EmptyReferenceExpression`       | -1   | -1  | 0     | 0  
. . . . . }                                  |      |     |       |    
. . . . }                                    |      |     |       |    
. . . }                                      |      |     |       |    
. . . `ForLoopStatement` {                   | 4    | 9   | 124   | 127
. . . . `VariableDeclarationStatements` {    | 4    | 14  | 129   | 145
. . . . . `ModifierNode`                     | -1   | -1  | 0     | 0  
. . . . . `VariableDeclaration` {            | 4    | 22  | 137   | 138
. . . . . . `LiteralExpression`              | 4    | 26  | 141   | 142
. . . . . . `VariableExpression` {           | 4    | 22  | 137   | 138
. . . . . . . `EmptyReferenceExpression`     | -1   | -1  | 0     | 0  
. . . . . . }                                |      |     |       |    
. . . . . }                                  |      |     |       |    
. . . . . `VariableDeclaration` {            | 4    | 29  | 144   | 145
. . . . . . `LiteralExpression`              | 4    | 33  | 148   | 149
. . . . . . `VariableExpression` {           | 4    | 29  | 144   | 145
. . . . . . . `EmptyReferenceExpression`     | -1   | -1  | 0     | 0  
. . . . . . }                                |      |     |       |    
. . . . . }                                  |      |     |       |    
. . . . }                                    |      |     |       |    
. . . . `StandardCondition` {                | 4    | 36  | 151   | 152
. . . . . `BooleanExpression` {              | 4    | 36  | 151   | 152
. . . . . . `VariableExpression` {           | 4    | 36  | 151   | 152
. . . . . . . `EmptyReferenceExpression`     | -1   | -1  | 0     | 0  
. . . . . . }                                |      |     |       |    
. . . . . . `LiteralExpression`              | 4    | 40  | 155   | 157
. . . . . }                                  |      |     |       |    
. . . . }                                    |      |     |       |    
. . . . `PostfixExpression` {                | 4    | 45  | 160   | 162
. . . . . `VariableExpression` {             | 4    | 44  | 159   | 160
. . . . . . `EmptyReferenceExpression`       | -1   | -1  | 0     | 0  
. . . . . }                                  |      |     |       |    
. . . . }                                    |      |     |       |    
. . . . `BlockStatement` {                   | 4    | 49  | 164   | 204
. . . . . `ExpressionStatement` {            | 5    | 18  | 183   | 194
. . . . . . `MethodCallExpression` {         | 5    | 18  | 183   | 188
. . . . . . . `ReferenceExpression`          | 5    | 11  | 176   | 182
. . . . . . . `BinaryExpression` {           | 5    | 24  | 189   | 190
. . . . . . . . `VariableExpression` {       | 5    | 24  | 189   | 190
. . . . . . . . . `EmptyReferenceExpression` | -1   | -1  | 0     | 0  
. . . . . . . . }                            |      |     |       |    
. . . . . . . . `VariableExpression` {       | 5    | 26  | 191   | 192
. . . . . . . . . `EmptyReferenceExpression` | -1   | -1  | 0     | 0  
. . . . . . . . }                            |      |     |       |    
. . . . . . . }                              |      |     |       |    
. . . . . . }                                |      |     |       |    
. . . . . }                                  |      |     |       |    
. . . . }                                    |      |     |       |    
. . . }                                      |      |     |       |    
. . . `ReturnStatement` {                    | 7    | 9   | 213   | 228
. . . . `VariableExpression` {               | 7    | 16  | 220   | 227
. . . . . `EmptyReferenceExpression`         | -1   | -1  | 0     | 0  
. . . . }                                    |      |     |       |    
. . . }                                      |      |     |       |    
. . }                                        |      |     |       |    
. }                                          |      |     |       |    
. `Method` <clinit> {                        | -1   | -1  | 0     | 0  
. . `ModifierNode`                           | 1    | 14  | 13    | 23 
. }                                          |      |     |       |    
. `Method` clone {                           | -1   | -1  | 0     | 0  
. . `ModifierNode`                           | -1   | -1  | 0     | 0  
. }                                          |      |     |       |    
. `UserClassMethods` {                       | -1   | -1  | 0     | 0  
. . `Method` <init> {                        | -1   | -1  | 0     | 0  
. . . `ModifierNode`                         | 1    | 14  | 13    | 23 
. . }                                        |      |     |       |    
. }                                          |      |     |       |    
. `BridgeMethodCreator`                      | -1   | -1  | 0     | 0  
}                                            |      |     |       |    
