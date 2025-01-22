package BadFunctionNameCheck

func fooBar() {}  // OK
func foo_bar() {} // Noncompliant {{Rename function "foo_bar" to match the regular expression ^(_|[a-zA-Z0-9]+)$}}
//   ^^^^^^^
func _foo() {} // Noncompliant
func foo2() {}
func Bar() {}

func main() {
    func () {}             // OK
}
