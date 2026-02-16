package IdenticalConditionsCheck

func test() {
	// Test that conditions are correctly extracted from if with short statements
	if x := 5; x > 3 {
		return 1
	} else if y := 10; x > 3 { // Noncompliant {{This condition duplicates the one on line 5.}}
		//		                  ^^^^^
		return 2
	}

	// Test with multiple identical conditions
	if a := 1; a == 1 {
		return 3
	} else if b := 2; a == 1 { // Noncompliant {{This condition duplicates the one on line 13.}}
	//                ^^^^^^
		return 4
	} else if c := 3; a == 1 { // Noncompliant {{This condition duplicates the one on line 13.}}
	//                ^^^^^^
		return 5
	}

	// Test with different conditions (compliant)
	if d := 4; d > 0 {
		return 6
	} else if e := 5; e < 10 {
		return 7
	} else if f := 6; f != 0 {
		return 8
	}

	// Test with parentheses around condition
	if x := 5; x > 3 {
		return 9
	} else if y := 10; x > 3 { // Noncompliant {{This condition duplicates the one on line 33.}}
	//                 ^^^^^
		return 10
	}

	// Test variable reassignment (not declaration)
	var x int
	if x = 5; x > 3 {
		return 11
	} else if x = 10; x > 3 { // Should NOT be reported (x reassigned in both)
		return 12
	}

	// Test multiple variables in init
	if x, y := foo(); x > 3 {
		return 13
	} else if x, y := bar(); x > 3 { // Should NOT be reported (x shadowed)
		return 14
	}

	// Test multiple variables in init with a single boolean variable used in condition
	if v, ok := foo(); ok {
	} else if v, ok := bar(); ok {
	}

	// Test outer scope variable in condition
	var z int = 10
	if x := 1; z > 3 {
		return 15
	} else if y := 2; z > 3 { // Noncompliant {{This condition duplicates the one on line 62.}}
	//                ^^^^^
		return 16
	}

	// Test function calls in init that might have side effects
	if err := doSomething(); err == nil {
		return 17
	} else if err := doSomethingElse(); err == nil { // Should NOT be reported (different functions)
		return 18
	}

	// Test variables used in condition but NOT modified in init
	var w int = 5
	if y := 5; w > 3 {
		return 19
	} else if x := 10; w > 3 { // Noncompliant {{This condition duplicates the one on line 78.}}
	//                 ^^^^^
		return 20
	}

	// Test variables declared outside and modified in init
	var w int = 5
	if w > 3 {
		return 19
	} else if w = 10; w > 3 {
		return 20
	}
}

func foo() (int, int) {
	return 1, 2
}

func bar() (int, int) {
	return 3, 4
}

func doSomething() error {
	return nil
}

func doSomethingElse() error {
	return nil
}
