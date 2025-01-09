package StringLiteralDuplicatedCheck

func main() {
	x := "string literal1" // Noncompliant {{Define a constant instead of duplicating this literal "string literal1" 3 times.}} [[effortToFix=2]]
	//   ^^^^^^^^^^^^^^^^^
	x += "string literal1" + "other string literal"
	//  <^^^^^^^^^^^^^^^^^
}

func (user *User) literal1() {
	user.name = "string literal1"
}

func literal2() string {
	v := "string literal2" + "string literal2" // Compliant - literal only appears twice
	return v
}

func literal3() {
	a := "string literal3"
	b := "string literal3"
	c := "string literal3${x}" // Compliant - string entries of string templates not considered as string literals
}

func funtcion1(abcde string) {
	if abcde == "string literal4" { // Noncompliant {{Define a constant instead of duplicating this literal "string literal4" 5 times.}} [[effortToFix=4]]
		//        ^^^^^^^^^^^^^^^^^
	}
}

func function2() string {
	switch "string literal4" {
	//    <^^^^^^^^^^^^^^^^^
	case "abc":
		return "string literal4"
	//         <^^^^^^^^^^^^^^^^^
	case "string literal4":
		//  <^^^^^^^^^^^^^^^^^
		return "string literal4"
		//    <^^^^^^^^^^^^^^^^^
	}
	return ""
}

func function3() {
	"abcd"
	"abcd"
	"abcd"
	"abcd" // Compliant - string length smaller than threshold
}

func function4() {
	"string_literal5"
	"string_literal5"
	"string_literal5"
	"string_literal5" // Compliant - single word
}

type User struct {
	name string
}
