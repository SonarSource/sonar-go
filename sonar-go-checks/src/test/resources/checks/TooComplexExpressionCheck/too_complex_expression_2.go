package TooComplexExpressionCheck

func main() {
	a := true
	b := false
	c := true
	d := false

	g := a && b
	h := a && b || c
	i := a && b || c && d // Noncompliant {{Reduce the number of conditional operators (3) used in the expression (maximum allowed 2).}}
}
