package BadFunctionNameCheck

func fooBar() {}      // OK
func foo_bar() {}     // OK - underscores allowed in test files
func Test_fooBar() {} // OK
func _foo() {}        // OK
func _() {}           // OK
func foo2() {}
func Bar() {}

func main() {
    func () {}             // OK
}

func fooąćźüöä() {}   // Noncompliant {{Rename function "fooąćźüöä" to match the regular expression ^(_|[a-zA-Z0-9_]+)$}}
