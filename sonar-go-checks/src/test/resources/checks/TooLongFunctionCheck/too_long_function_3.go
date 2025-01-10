package TooLongfuncctionCheck

import "fmt"

func a() {}

func f1() {
  a(); a()
}

func f2() { // Noncompliant {{This function has 4 lines of code, which is greater than the 3 authorized. Split it into smaller functions.}}
  // ^^
  a()
  a()
}

func f3() {

  a()
}

func f4() {
  // comment
  a()
}

func foo1( // Compliant, no line of code
p1 int,
p2 int,
p3 int,
p4 int) {
}

func foo2(
p1 int,
p2 int,
p3 int,
p4 int) {}

func parent() {  // Noncompliant
  // Anonymous function
  func (
      p1 int,
      p2 int,
      p3 int,
      p4 int) {
    fmt.Println("Hello")
  } (1,2,3,4)
}

func parent2() {  // Noncompliant
  // Anonymous function
  func (          // Noncompliant
      p1 int,
      p2 int,
      p3 int,
      p4 int) {
    fmt.Println(p1)
    fmt.Println(p2)
    fmt.Println(p3)
  } (1,2,3,4)
}

func bar() int { // Noncompliant
  // ^^^
  a()
  a()
  a()
  return 0
}
