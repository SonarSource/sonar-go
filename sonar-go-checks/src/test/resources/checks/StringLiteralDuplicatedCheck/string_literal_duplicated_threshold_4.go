package StringLiteralDuplicatedCheck

func duplicates() {
	a := "string literal1"
	b := "string literal1"
	c := "string literal1" // Compliant - only appears 3 times which is less than the custom threshold of 4

	d := "string literal2" // Noncompliant {{Define a constant instead of duplicating this literal "string literal2" 4 times.}}
	//   ^^^^^^^^^^^^^^^^^
	e := "string literal2"
	//  <^^^^^^^^^^^^^^^^^
	f := "string literal2"
	//  <^^^^^^^^^^^^^^^^^
	g := "string literal2"
	//  <^^^^^^^^^^^^^^^^^
}
