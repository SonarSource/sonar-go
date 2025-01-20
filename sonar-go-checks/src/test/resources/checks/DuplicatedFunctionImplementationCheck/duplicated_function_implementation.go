package DuplicatedFunctionImplementationCheck

func foobar() {}
func foo_bar () int {}  // Compliant - has no line

func smallF1() {
  foo = 1;
  bar = foo > 3 || bar
}

func smallF2() { // Noncompliant
  foo = 1;
  bar = foo > 3 || bar
}

func f1() string {
//   ^^>
  foobar = "abc";
  foo = 1;
  bar = foo > 3 || bar
}

func f2() int {
  foobar = "abc";
  foo = 1;
  baz = foo > 3 || bar
}

func f3() bool { // Noncompliant {{Update this function so that its implementation is not identical to "f1" on line 16.}}
//   ^^
  foobar = "abc";
  foo = 1;
  bar = foo > 3 || bar
}

func f4() { // Noncompliant {{Update this function so that its implementation is not identical to "f1" on line 16.}}
//   ^^
  foobar = "abc";
  foo = 1;
  bar = foo > 3 || bar
}

func f5(a) { // Compliant - different parameter list
  foobar = "abc";
  foo = 1;
  bar = foo > 3 || bar
}

func f6() {
  foo = 1;
}

func f7() { // Compliant - only 1 line
  foo = 1;
}

func f8(int a) { // Compliant
  foobar = "abc";
  foo = 1;
  bar = foo > 3 || bar
}

func f9(int a) { // Noncompliant
  foobar = "abc"; foo = 1; bar = foo > 3 || bar
}

func f10(string a) { // Compliant - not same parameter type
  foobar = "abc";
  foo = 1;
  bar = foo > 3 || bar
}

func f11(int a, int b) { // Compliant - not same parameters
  foobar = "abc";
  foo = 1;
  bar = foo > 3 || bar
}

func f12(int a, int b) {
  foobar = "abcdefg";
  foo = 1;
  bar = foo > 3 || bar
}

func f13(int a, int b) {
  foobar = "abc";
  foo = 2;
  bar = foo > 3 || bar
}
