package DuplicatedFunctionImplementationCheck

func main() {}

func foobar() {}
func foo_bar () {}  // Compliant - has no line

func smallF1() {
  foo := 1;
  bar := foo > 3 || false
  _ = bar
}

func smallF2() { // Noncompliant
  foo := 1;
  bar := foo > 3 || false
  _ = bar
}

func f1() {
  foobar := "abc";
  foo := 1;
  bar := foo > 3 || false
  _ = foobar
  _ = bar
}

func f2() int {
  foobar := "abc";
  foo := 1;
  baz := foo > 3 || false
  _ = foobar
  _ = baz
  return 5
}

func f3() bool {
  foobar := "abc";
  foo := 1;
  bar := foo > 3 || false
  _ = foobar
  _ = bar
  return true
}

func f4() { // Noncompliant {{Update this function so that its implementation is not identical to "f1" on line 20.}}
//   ^^
  foobar := "abc";
  foo := 1;
  bar := foo > 3 || false
  _ = foobar
  _ = bar
}

func f5(a string) { // Compliant - different parameter list
  foobar := "abc";
  foo := 1;
  bar := foo > 3 || false
  _ = foobar
  _ = bar
}

func f6() {
  _ = "foo"
}

func f7() { // Compliant - only 1 line
  _ = "foo"
}

func f8(a int) { // Compliant - not same parameter type
  foobar := "abc";
  foo := 1;
  bar := foo > 3 || false
  _ = foobar
  _ = bar
}

func f9(a int) { // Noncompliant
  foobar := "abc"; foo := 1; bar := foo > 3 || false
  _ = foobar
  _ = bar
}

func f10(a int, b int) { // Compliant - not same parameters
  foobar := "abc";
  foo := 1;
  bar := foo > 3 || false
  _ = foobar
  _ = bar
}

func f11(a int, b int) {
  foobar := "abcdefg";
  foo := 1;
  bar := foo > 3 || false
  _ = foobar
  _ = bar
}

func f12(a int, b int) {
  foobar := "abc";
  foo := 2;
  bar := foo > 3 || false
  _ = foobar
  _ = bar
}
