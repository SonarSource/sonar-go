package IdenticalBinaryOperandCheck

func main() {
  x == 1
   1 == 1 // Noncompliant {{Correct one of the identical sub-expressions on both sides this operator}}
// ^>   ^
  1 == (1)           // Noncompliant {{Correct one of the identical sub-expressions on both sides this operator}}
  (1 + 2) == 1+2     // Noncompliant
  (1 + 2) == (1 + 2) // Noncompliant
  (1 + 2) == (1 + 2 + 3)
  (x ==
// ^>
    x) // Noncompliant
 // ^
  1 == 2
  x = x
  x + x
  x * x
  x <= x   // Noncompliant
  _x <= _x // Noncompliant
  x_ <= x
  _x <= x
  x <= _
  _ <= y
}
