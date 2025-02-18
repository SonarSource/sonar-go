package IdenticalBinaryOperandCheck

func main() {
  x == 1
   1 == 1 // Noncompliant {{Correct one of the identical sub-expressions on both sides of this operator.}}
// ^>   ^
  1 == (1)           // Noncompliant {{Correct one of the identical sub-expressions on both sides of this operator.}}
  (1 + 2) == 1+2     // Noncompliant
  (1 + 2) == (1 + 2) // Noncompliant
  (1 + 2) == (1 + 2 + 3)
  (x ==
// ^>
    x) // Noncompliant
 // ^
  1 == 2
  x <= x   // Noncompliant
  _x <= _x // Noncompliant
  x_ <= x
  _x <= x
  x <= _
  _ <= y
  x >> x // Noncompliant
  // Exceptions:
  1 << 1
  x << x
  x + x
  x * x
  // The following one is not a binary expression, but an assignment expression.
  x = x
}
