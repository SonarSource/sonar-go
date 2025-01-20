package IdenticalConditionsCheck

func if_statements() {
  if (x) {
  } else if (x) { // Noncompliant {{This condition duplicates the one on line 4.}}
  }

  if (x) {}
  if (x) {} else {}
  if (x) {} else if (y) {}
  if (x) {} else if (x) {} // Noncompliant
  //  ^>             ^
  if (x) {} else if (y) {} else if (z) {}
  if (x) {} else if (y) {} else if (y) {} // Noncompliant
  //                 ^>             ^
  if (x) {} else if (x) {} else if (x) {} // Noncompliant 2
  if (x) {} else if (y) {} else if ((y)) {} // Noncompliant
}

func switch_statements() {
  switch (x) {
  case 1:
    return
  }

  switch (x) {
  case 1:
    return
  default:
    return
  }

  switch x {
  case 1:
    return
  default:
    return
  }

  switch x := 1; x {
  case 1:
    return
  default:
    return
  }

  switch x {
  case 1:
    return
  case 2:
    return
  }

  switch x {
  case 1:
//     ^>
    return
  case 1: // Noncompliant
//     ^
    return
  }

  switch x {
  case 1:
    return
  case (1):
    return
  }
}
