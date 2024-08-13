## Source Code
```java
class A {}
```
## AST
AST node class             | tokens      | line | col | start | end
---------------------------|-------------|------|-----|-------|----
`CompilationUnit` {        | `class`…`}` | 1    | 1   | 0     | 10 
. `ClassDeclUnit` {        | `class`…`}` | 1    | 1   | 0     | 10 
. . `ClassDecl` {          | `class`…`}` | 1    | 1   | 0     | 10 
. . . `LocationIdentifier` | `A`         | 1    | 7   | 6     | 7  
. . }                      |             |      |     |       |    
. }                        |             |      |     |       |    
}                          |             |      |     |       |    
## Comments
