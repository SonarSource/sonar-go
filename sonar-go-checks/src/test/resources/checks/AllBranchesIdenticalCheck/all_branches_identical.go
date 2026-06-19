package AllBranchesIdenticalCheck

import "fmt"

func callMe(args ...any) {}

// the code is formated in another way as callMe()
func callMe2(
    args...any,
) {
    fmt.Printf("Length: %d | Value: %v\n", len(args), args)
}

func main() {
    if x { foo; }
    if x { foo; } else { bar; }
    if x { foo; } else { foo; } // Noncompliant
  //^^^^^^^^^^^^^^^^^^^^^^^^^^^

    if x { foo; } else if (y) { foo; }
    if x { foo; } else if (y) { foo; } else { bar; }
    if x { foo; } else if (y) { foo; } else { foo; } // Noncompliant
  //^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

    switch x { }
    switch x { case 1: a; }
    switch x { case 1: a; default: b; }
    switch x { case 1: a; default: a; } // Noncompliant
  //^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    switch x { case 1: a; case 2: a; default: b; }
    switch x { case 1: a; case 2: a; default: a; } // Noncompliant
    switch x { default: b; } // Compliant: only default case

    args := []any{1, "two", 3.0, true}
    if hasVariadic {
        callMe(args)
    } else {
        callMe(args...) // Compliant: branches differ by variadic spread operator
    }

    if hasVariadic { // Noncompliant
        callMe(args...)
    } else {
        callMe(args...)
    }

    if hasVariadic { // Noncompliant
        callMe(args)
    } else {
        callMe(args)
    }
}

func variadicSpread2(hasVariadic bool) {
    args := []any{1, "two", 3.0, true}
    // Compliant: callMe2(args) and callMe2(args...) have different semantics
    if hasVariadic {
        callMe2(args)
    } else {
        callMe2(
            args...,
        )
    }
}
