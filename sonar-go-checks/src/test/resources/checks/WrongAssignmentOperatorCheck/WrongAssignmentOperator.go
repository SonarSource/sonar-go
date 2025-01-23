package samples

func main() {
  var target, num = -5, 3

  target -= num
  target =-num // Noncompliant {{Was "-=" meant instead?}}
  //     ^^

  target =
          -num

  target = -num // Compliant intent to assign inverse value of num is clear
  target =- -num // Noncompliant

  target += num
  target =+ num // Noncompliant {{Was "+=" meant instead?}}
  //     ^^
  target =
          + num
  target =
          +num
  target = +num
  target=+num // Compliant - no spaces between variable, operator and expression

  var a, b, c = 1, 2, 3

  a = b != c
  a = b == c
  a =! c // Noncompliant {{Add a space between "=" and "!" to avoid confusion.}}
  a = ! c
  a = !c
  a =
     !c

  a = ^c // Compliant, valid go code
  a =^ c // Could be considered as a FN, as we are currently only targeting "!", "-", "+"
  a ^= c // Compliant, valid go code

  // Other compound assignment, that does not have a unary operator that can be confusing
  a /= c // Compliant
  a %= c // Compliant
  a |= c // Compliant
  a <<= c // Compliant
  a >>= c // Compliant
  a &^= c // Compliant

  var x int = 10
  var p *int
  p = &x // p is a pointer to x
  p =& x // Could be considered as a FN
  x &= x // Compliant

  x = *p // Compliant
  x =* p // Could be considered as a FN
  x *= x // Compliant

  target = num +num // Compliant
  target = num+ num // Compliant
  target = num+num  // Compliant
  target2 :=- num

  // In Go, increment and decrement are statements, not expressions.
  num++
  num--
}

