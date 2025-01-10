package samples

var notLocal string

func localVariables() {
   var VALIDLOCAL int
   var localVar int // Noncompliant {{Rename this local variable to match the regular expression "^([A-Z]+)$".}}
   var _ int // Noncompliant
}

func parameters(PARAM string, p2 string) { // Noncompliant {{Rename this parameter to match the regular expression "^([A-Z]+)$".}}
//                            ^^
}
