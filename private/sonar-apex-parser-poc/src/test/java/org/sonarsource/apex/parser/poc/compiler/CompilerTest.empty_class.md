## Source Code
```java
class A {}
```
## AST
AST node class          | line | col | start | end
------------------------|------|-----|-------|----
`UserClass` A {         | 1    | 7   | 6     | 7  
. `ModifierNode`        | 1    | 7   | 6     | 7  
. `Method` <clinit> {   | -1   | -1  | 0     | 0  
. . `ModifierNode`      | 1    | 7   | 6     | 7  
. }                     |      |     |       |    
. `Method` clone {      | -1   | -1  | 0     | 0  
. . `ModifierNode`      | -1   | -1  | 0     | 0  
. }                     |      |     |       |    
. `UserClassMethods` {  | -1   | -1  | 0     | 0  
. . `Method` <init> {   | -1   | -1  | 0     | 0  
. . . `ModifierNode`    | 1    | 7   | 6     | 7  
. . }                   |      |     |       |    
. }                     |      |     |       |    
. `BridgeMethodCreator` | -1   | -1  | 0     | 0  
}                       |      |     |       |    
