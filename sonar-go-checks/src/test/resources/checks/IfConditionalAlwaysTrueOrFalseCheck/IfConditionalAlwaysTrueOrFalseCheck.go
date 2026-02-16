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

  if n := 3; true { // Noncompliant {{Remove this useless "if" statement.}}
 //          ^^^^
    return 1
  }

  if n := 3; false { // Noncompliant
    return 1
  }

  if n := 3; true || someCondition { // Noncompliant
    return 1
  }

  if n := 3; false && someCondition { // Noncompliant
    return 1
  }

  if false { // Noncompliant
    return 1
  }

  if !false { // Noncompliant
  // ^^^^^^
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
    return 1
  } else if isFoo() && false { // Compliant, isFoo can have a side-effect
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
