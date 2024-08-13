## Source Code
```java
class A{
  // comment 1
  String name = 'abc';
  /* comment 2 */
}
```
## Tokens
text            | type           | line | col | start | stop
----------------|----------------|------|-----|-------|-----
class           | CLASS          | 1    | 1   | 0     | 4   
A               | IDENTIFIER     | 1    | 7   | 6     | 6   
{               | LCURLY         | 1    | 8   | 7     | 7   
// comment 1    | EOL_COMMENT    | 2    | 3   | 11    | 22  
String          | IDENTIFIER     | 3    | 3   | 26    | 31  
name            | IDENTIFIER     | 3    | 10  | 33    | 36  
=               | EQ             | 3    | 15  | 38    | 38  
'abc'           | STRING_LITERAL | 3    | 17  | 40    | 44  
;               | SEMICOLON      | 3    | 22  | 45    | 45  
/* comment 2 */ | BLOCK_COMMENT  | 4    | 3   | 49    | 63  
}               | RCURLY         | 5    | 1   | 65    | 65  
