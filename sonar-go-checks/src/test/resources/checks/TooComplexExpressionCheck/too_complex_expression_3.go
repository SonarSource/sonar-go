package TooComplexExpressionCheck

func main() {
	a := true
	b := false
	c := true
	d := false
	e := true
	f := false

	g := a && b
	h := a && b || c
	i := a && b || c && d
	j := a && b || c && d || e      // Noncompliant {{Reduce the number of conditional operators (4) used in the expression (maximum allowed 3).}} [[effortToFix=1]]
	k := a && b || c && d || e && f // Noncompliant [[effortToFix=2]]
	//   ^^^^^^^^^^^^^^^^^^^^^^^^^^
	if a && b || c && d || e && f {
	} // Noncompliant
	if a && (b || c) && (d || e && f) {
	} // Noncompliant
	if a && b || c {
	}
	if !(a && b || c && d) {
	}
	if !(a && b || c && d || e) {
	} // Noncompliant
	j := foo(a && b) && foo(a || b) && foo(a && b)

}

func foo(input bool) bool {
	return input
}
