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

  a = b != c
  a = b == c
  a =! c // Noncompliant {{Add a space between "=" and "!" to avoid confusion.}}
  a = ! c
  a = !c
  a =
     !c
}

