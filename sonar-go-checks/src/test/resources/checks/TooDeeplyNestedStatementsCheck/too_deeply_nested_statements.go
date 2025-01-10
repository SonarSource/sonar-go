package TooDeeplyNestedStatementsCheck

import "fmt"

func main() {

  if false { // Compliant
  }

  if false { // Compliant
  }

  if true {
//^^>
  } else if true {
  } else if true {
    if true { // Compliant
//  ^^>
      if true {
//    ^^>
        if true {
//      ^^>
          if true { // Noncompliant {{Refactor this code to not nest more than 4 control flow statements.}}
//        ^^
          }
        }
      }
    }
  }

  if false { // Compliant
    if true { // Compliant
    } else {
      if false { // Compliant
        if true { // Compliant
          if false { // Noncompliant {{Refactor this code to not nest more than 4 control flow statements.}}
            if false { // Compliant
            }
          } else if true { // Compliant
          } else {
            if false { // Compliant
            }
          }
        }
      }
    }
  }

  if false { // Compliant
  } else if false { // Compliant
  } else if false { // Compliant
  } else if false { // Compliant
  } else if false { // Compliant
  }

  if false { // Compliant
    if false { // Compliant
      if false { // Compliant
        if false { // Compliant
          if true { // Noncompliant
            fmt.Println()
          }
        }
      }
    }
  }

  primes := [6]int{2, 3, 5, 7, 11, 13}

  for _, x := range primes { // Compliant
//^^^>
    for i := 0; i < 10; i++ { // Compliant
//  ^^^>
      for i < 0 { // Compliant
//    ^^^>
        for i < 0 { // Compliant
//      ^^^>
          for j := 0; j < 10; j++ { // Noncompliant
//        ^^^
            fmt.Println(x)
          }

          for false { // Noncompliant
          }

          // it simulates do..while
          for ok := true; ok; ok = false { } // Noncompliant

          if false { // Noncompliant
          }

          switch i { // Noncompliant
//        ^^^^^^
          }

          if false { // Noncompliant
            foo()
          } else {
            bar()
          }
        }
      }

      sum := 0
      for sum < 100 {
        condition := true
        if condition { if x == 1 { foo() } else { bar() } // Noncompliant
        }
      }
    }
  }

  for {
    if true {
    } else if true {
    } else {
      if true {
        if true {
          if true { // Noncompliant
          }
        }
      }
    }
  }
}

func foo() {}
func bar() {}
