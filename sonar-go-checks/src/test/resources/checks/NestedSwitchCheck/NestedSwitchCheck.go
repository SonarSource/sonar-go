package NestedSwitchCheck

func main() {
  switch x {
  case 1:
    foo()
  default:
    bar()
  }

  switch x {
  case 1:
    switch y { // Noncompliant {{Refactor the code to eliminate this nested "switch".}}
 // ^^^^^^
    case 2:
      foo()
    default:
      bar()
    }
  default:
    bar()
  }

  switch x {
  case 1:
    foo()
  default:
    switch y { // Noncompliant {{Refactor the code to eliminate this nested "switch".}}
 // ^^^^^^
    case 2:
      foo()
    }
  }

  switch x {
  case 1:
    switch y { // Noncompliant
    case 2:
      foo()
      switch z { // Noncompliant
   // ^^^^^^
      case 3:
        bar()
      }
    }
    switch z { // Noncompliant
    case 4:
      bar()
    }
  default:
    baz()
  }
}
