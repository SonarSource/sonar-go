package samples

var NOT_LOCAL string
const CONSTANT string = "1"
const CONSTANT_2 = "1"

var (
    NOT_LOCAL_2 = "test"
)

func localVariables() {
   const CONSTANT string = "1"
   var localVar int
   var _ int
   var __ int // Noncompliant
   var stringVar = "test"
   var INVALID_LOCAL int // Noncompliant {{Rename this local variable to match the regular expression "^(_|[a-zA-Z0-9]+)$".}}
 //    ^^^^^^^^^^^^^
   var invalid_local = "test" // Noncompliant
   invalid_local_2 := "test" // Noncompliant
   var (
       invalid_local_3 = "test" // Noncompliant
   )
}

func parameters(p1 string, _PARAM2 string, p3 string) { // Noncompliant {{Rename this parameter to match the regular expression "^(_|[a-zA-Z0-9]+)$".}}
//                         ^^^^^^^
}

type A struct {
  b int
}

func (a *A) add(p1 int, PARAM_2 int) { // Noncompliant
//                      ^^^^^^^
  a.b = p1
}

func NewA(p1 int, PARAM_2 int) *A { // Noncompliant
  p := new(A)
  p.b = p1
  return p
}

func parameters(_ string) {
}

func parameters(__ string) { // Noncompliant
}

func unnamed(*A) {
}
