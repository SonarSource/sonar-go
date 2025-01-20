package SwitchCaseTooBigCheck

func main() {
  switch x {
    case "8 lines": // Noncompliant {{Reduce this case clause number of lines from 8 to at most 6, for example by extracting code into methods.}}
//  ^^^^^^^^^^^^^^^
      a := 1
      foo()
      bar()
      if a == 1 {
        foo()
        fmt.Printf(1)
      }
    case "7 lines": // Noncompliant {{Reduce this case clause number of lines from 7 to at most 6, for example by extracting code into methods.}}
      a := 1
      foo()
      if a == 1 {
        foo()
        fmt.Printf(1)
      }
    case "6 lines": // Compliant, equals to the max lines limit
      a := 1
      if a == 1 {
        foo()
        fmt.Printf(1)
      }
    case "4 lines": // Compliant, below the max lines limit
      a := 1
      foo()
      bar()
    default:
      b := 2
      fmt.Printf(1);
  }

  switch x {
    case "4 lines":
      a := 1
      foo()
      bar()
    default: // Noncompliant {{Reduce this case clause number of lines from 7 to at most 6, for example by extracting code into methods.}}
//  ^^^^^^^^
      a := 1
      foo()
      bar()
      baz()
      b := 2
      fmt.Printf(1);
  }
}
