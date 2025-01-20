package BadFunctionNameCheck

func FOOBAR() {}  // OK
func foo() {} // Noncompliant {{Rename function "foo" to match the regular expression ^[A-Z]*$}}
//   ^^^
