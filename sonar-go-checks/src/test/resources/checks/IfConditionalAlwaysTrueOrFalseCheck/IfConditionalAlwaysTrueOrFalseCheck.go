package IfConditionalAlwaysTrueOrFalseCheck

func main() {
  if true { // Noncompliant {{Remove this useless "if" statement.}}
 //  ^^^^
    return 1
  }

  if (true) { // Noncompliant {{Remove this useless "if" statement.}}
 //  ^^^^^^
    return 1
  }

  if (((true))) { // Noncompliant {{Remove this useless "if" statement.}}
 //  ^^^^^^^^^^
    return 1
  }

  if n := 3; true { // False negative
    return 1
  }

  if false { // Noncompliant
    return 1
  }

  if condition {
    return 1
  } else if true { // Noncompliant
    return 1
  }

  if !true { // Noncompliant
    return 1
  } else if cond && false { // Noncompliant
         // ^^^^^^^^^^^^^
    return 1
  } else if cond || false {
    return 1
  } else if cond1 || cond2 || true { // Noncompliant
    return 1
  } else if cond1 && cond2 && !true && cond3 { // Noncompliant
    return 1
  } else if cond1 && !(cond2 && (!true && cond3)) { // Noncompliant
    return 1
  }
}
