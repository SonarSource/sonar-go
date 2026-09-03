package samples

// Shadowing a predeclared type at package scope applies to the whole file, so those cases are kept apart from the
// other fixtures, which use "any", "string" and "int" as the types they are.

type int struct{} // Noncompliant {{Rename this identifier, "int" shadows a predeclared identifier.}}
//   ^^^

var any, notShadowingType = 1, 2 // Noncompliant
//  ^^^

const string = "value" // Noncompliant

type complex128 interface { // Noncompliant
	method() error
}

func (bool *int) methodOnShadowedType() { // Noncompliant
	//   ^^^^
	notShadowingType = 3
}
