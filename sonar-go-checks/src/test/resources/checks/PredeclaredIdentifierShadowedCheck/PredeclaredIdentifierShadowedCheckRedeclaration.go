package samples

import "fmt"

// A short variable declaration only declares the names that are new in its scope, so only the declaring usage of a
// name is reported. Valid Go is needed here for the names to resolve to symbols.

func repeatedShortDeclaration() {
	max, err := parse(1) // Noncompliant {{Rename this identifier, "max" shadows a predeclared identifier.}}
	max, other := parse(2) // Compliant, "max" is assigned here, "other" is the only name it declares
	fmt.Println(max, err, other)
}

func parameterThenShortDeclaration(min int) { // Noncompliant
//                                 ^^^
	min, err := parse(min) // Compliant, the parameter already declared "min"
	fmt.Println(min, err)
}

func separateScopes() {
	if len, err := parse(1); err == nil { // Noncompliant
		fmt.Println(len)
	}
	if len, err := parse(2); err == nil { // Noncompliant
		fmt.Println(len)
	}
}

func parse(value int) (int, error) {
	return value, nil
}

type target struct{}

func namedResultThenShortDeclaration() (max int, err error) { // Noncompliant
//                                      ^^^
	max, count := parse(1) // Compliant, the named result already declared "max"
	fmt.Println(max, count)
	return max, err
}

func (min *target) receiverThenShortDeclaration() { // Noncompliant
//    ^^^
	min, err := newTarget() // Compliant, the receiver already declared "min"
	fmt.Println(min, err)
}

func newTarget() (*target, error) {
	return &target{}, nil
}
