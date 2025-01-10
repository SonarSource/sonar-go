package SwitchWithoutDefaultCheck

func main() {
    switch x { // Noncompliant {{Add a default clause to this "switch" statement.}}
//  ^^^^^^
    }

    switch x { // Noncompliant {{Add a default clause to this "switch" statement.}}
//  ^^^^^^
    case 1:
      fmt.Println("1")
    }

    switch x {
    case 1:
      fmt.Println("1")
    default:
      fmt.Println("default")
    }

    switch x {
    default:
      fmt.Println("default")
    case 1:
      fmt.Println("1")
    }

    switch x {
    case 1:
      fmt.Println("1")
    default:
      switch y { // Noncompliant {{Add a default clause to this "switch" statement.}}
   // ^^^^^^
      case 2:
        fmt.Println("2")
      }
    }
}
