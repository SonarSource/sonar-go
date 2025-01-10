package SwitchCaseTooBigCheck

func main() {
  switch x {
    case "4 lines": // Noncompliant {{Reduce this case clause number of lines from 4 to at most 3, for example by extracting code into methods.}}
//  ^^^^^^^^^^^^^^^
      a := 1
      foo()
      bar()
    case "3 lines": // Compliant, equals to the max lines limit
      foo()
      bar()
    case "2 lines": // Compliant, below the max lines limit
      foo()
    default:
      b := 2
      fmt.Printf(1);
  }

  switch x {
    case "3 lines":
      foo()
      bar()
    default: // Noncompliant {{Reduce this case clause number of lines from 4 to at most 3, for example by extracting code into methods.}}
//  ^^^^^^^^
      a := 1
      foo()
      bar()
  }
}
