## Source Code
```java
class A{
  // comment 1
  String name = 'abc';
  /* comment 2 */
}
```
## AST
AST node class                   | tokens           | line | col | start | end
---------------------------------|------------------|------|-----|-------|----
`CompilationUnit` {              | `class`…`}`      | 1    | 1   | 0     | 66 
. `ClassDeclUnit` {              | `class`…`}`      | 1    | 1   | 0     | 66 
. . `ClassDecl` {                | `class`…`}`      | 1    | 1   | 0     | 66 
. . . `LocationIdentifier`       | `A`              | 1    | 7   | 6     | 7  
. . . `FieldMember` {            | `String`…`'abc'` | 3    | 3   | 26    | 45 
. . . . `VariableDecls` {        | `String`…`'abc'` | 3    | 3   | 26    | 45 
. . . . . `ClassTypeRef` {       | `String`         | 3    | 3   | 26    | 32 
. . . . . . `LocationIdentifier` | `String`         | 3    | 3   | 26    | 32 
. . . . . }                      |                  |      |     |       |    
. . . . . `VariableDecl` {       | `name`…`'abc'`   | 3    | 10  | 33    | 45 
. . . . . . `LocationIdentifier` | `name`           | 3    | 10  | 33    | 37 
. . . . . . `LiteralExpr`        | `'abc'`          | 3    | 17  | 40    | 45 
. . . . . }                      |                  |      |     |       |    
. . . . }                        |                  |      |     |       |    
. . . }                          |                  |      |     |       |    
. . }                            |                  |      |     |       |    
. }                              |                  |      |     |       |    
}                                |                  |      |     |       |    
## Comments
text            | type          | line | col | start | stop
----------------|---------------|------|-----|-------|-----
// comment 1    | EOL_COMMENT   | 2    | 3   | 11    | 22  
/* comment 2 */ | BLOCK_COMMENT | 4    | 3   | 49    | 63  
