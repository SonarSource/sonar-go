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
## Tokens
text           | type            | line | col | start | stop
---------------|-----------------|------|-----|-------|-----
global         | GLOBAL          | 1    | 1   | 0     | 5   
class          | CLASS           | 1    | 8   | 7     | 11  
HelloWorld     | IDENTIFIER      | 1    | 14  | 13    | 22  
{              | LCURLY          | 1    | 25  | 24    | 24  
public         | PUBLIC          | 2    | 5   | 30    | 35  
String         | IDENTIFIER      | 2    | 12  | 37    | 42  
hello          | IDENTIFIER      | 2    | 19  | 44    | 48  
(              | LPAREN          | 2    | 24  | 49    | 49  
Integer        | IDENTIFIER      | 2    | 25  | 50    | 56  
x              | IDENTIFIER      | 2    | 33  | 58    | 58  
,              | COMMA           | 2    | 34  | 59    | 59  
String         | IDENTIFIER      | 2    | 36  | 61    | 66  
y              | IDENTIFIER      | 2    | 43  | 68    | 68  
)              | RPAREN          | 2    | 44  | 69    | 69  
{              | LCURLY          | 2    | 46  | 71    | 71  
String         | IDENTIFIER      | 3    | 9   | 81    | 86  
message        | IDENTIFIER      | 3    | 16  | 88    | 94  
=              | EQ              | 3    | 24  | 96    | 96  
(              | LPAREN          | 3    | 26  | 98    | 98  
'Hello world!' | STRING_LITERAL  | 3    | 27  | 99    | 112 
)              | RPAREN          | 3    | 41  | 113   | 113 
;              | SEMICOLON       | 3    | 42  | 114   | 114 
for            | FOR             | 4    | 9   | 124   | 126 
(              | LPAREN          | 4    | 13  | 128   | 128 
Integer        | IDENTIFIER      | 4    | 14  | 129   | 135 
i              | IDENTIFIER      | 4    | 22  | 137   | 137 
=              | EQ              | 4    | 24  | 139   | 139 
0              | INTEGER_LITERAL | 4    | 26  | 141   | 141 
,              | COMMA           | 4    | 27  | 142   | 142 
j              | IDENTIFIER      | 4    | 29  | 144   | 144 
=              | EQ              | 4    | 31  | 146   | 146 
0              | INTEGER_LITERAL | 4    | 33  | 148   | 148 
;              | SEMICOLON       | 4    | 34  | 149   | 149 
i              | IDENTIFIER      | 4    | 36  | 151   | 151 
<              | LT              | 4    | 38  | 153   | 153 
10             | INTEGER_LITERAL | 4    | 40  | 155   | 156 
;              | SEMICOLON       | 4    | 42  | 157   | 157 
i              | IDENTIFIER      | 4    | 44  | 159   | 159 
++             | PLUS_PLUS       | 4    | 45  | 160   | 161 
)              | RPAREN          | 4    | 47  | 162   | 162 
{              | LCURLY          | 4    | 49  | 164   | 164 
System.debug   | IDENTIFIER      | 5    | 11  | 176   | 187 
(              | LPAREN          | 5    | 23  | 188   | 188 
i              | IDENTIFIER      | 5    | 24  | 189   | 189 
+              | PLUS            | 5    | 25  | 190   | 190 
j              | IDENTIFIER      | 5    | 26  | 191   | 191 
)              | RPAREN          | 5    | 27  | 192   | 192 
;              | SEMICOLON       | 5    | 28  | 193   | 193 
}              | RCURLY          | 6    | 9   | 203   | 203 
return         | RETURN          | 7    | 9   | 213   | 218 
message        | IDENTIFIER      | 7    | 16  | 220   | 226 
;              | SEMICOLON       | 7    | 23  | 227   | 227 
}              | RCURLY          | 8    | 5   | 233   | 233 
}              | RCURLY          | 9    | 1   | 235   | 235 
