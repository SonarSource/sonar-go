package samples

import (
	"fmt"
	max "strings" // Noncompliant {{Rename this identifier, "max" shadows a predeclared identifier.}}
	valid "bytes"
)

import recover "os" // Noncompliant
import _ "embed"

const iota = 5 // Noncompliant
//    ^^^^

var nil = 1 // Noncompliant

var print, notShadowing = 1, 2 // Noncompliant

const (
	firstConst = 1
	clear      = 2 // Noncompliant
	real       = 3 // Noncompliant
)

var (
	imag int // Noncompliant
)

type len int // Noncompliant
//   ^^^

type pair[cap any] struct { // Noncompliant
//        ^^^
	value cap
}

func append() {} // Noncompliant

func withParameters(new int, close string, notShadowingEither bool) { // Noncompliant 2
//                  ^^^
	fmt.Println(new, close, notShadowingEither)
}

func withNamedResults() (recover int, err error) { // Noncompliant
//                       ^^^^^^^
	return 0, err
}

func withTypeParameters[min any, T any](value min, other T) {} // Noncompliant

func shortDeclarations() {
	panic := 1 // Noncompliant
	complex, notShadowingAgain := 2, 3 // Noncompliant
	var false = 4                      // Noncompliant
	const println = 5                  // Noncompliant
	fmt.Println(panic, complex, notShadowingAgain, false, println)
}

func inInitializers(value any) {
	if make := 1; make > 0 { // Noncompliant
		fmt.Println(make)
	}
	for cap := 0; cap < 3; cap++ { // Noncompliant
		fmt.Println(cap)
	}
	switch delete := 1; delete { // Noncompliant
	case 1:
	}
	switch nil := value.(type) { // Noncompliant
	default:
		fmt.Println(nil)
	}
	channel := make(chan int)
	select {
	case true := <-channel: // Noncompliant
		fmt.Println(true)
	}
}

func inRangeClauses(items []int) {
	for len, cap := range items { // Noncompliant 2
		fmt.Println(len, cap)
	}
	for max := range items { // Noncompliant
		fmt.Println(max)
	}
	// A "=" range assigns to already declared variables, only their declaration shadows the predeclared identifiers
	var close, complex int // Noncompliant 2
	for close, complex = range items {
		fmt.Println(close, complex)
	}
}

type receiver struct{}

func (new *receiver) method() {} // Noncompliant
//    ^^^

func closures() {
	function := func(imag int) (nil error) { // Noncompliant 2
		return nil
	}
	fmt.Println(function)
	defer func(min int) {}(1)  // Noncompliant
	go func(println int) {}(1) // Noncompliant
}

type (
	firstType  int
	copy       string // Noncompliant
	println    bool   // Noncompliant
	box[cap any] struct { // Noncompliant
		value cap
	}
)

func inFunctionScope() {
	type (
		make int // Noncompliant
	)
	var value make
	fmt.Println(value)
}

type generic[T any] struct {
	value T
}

func (g *generic[recover]) valueOf() recover { // Noncompliant
//               ^^^^^^^
	return g.value
}

func (g generic[imag]) copyOf() imag { // Noncompliant
	return g.value
}

type (
	// A blank name declares nothing, and the type it stands for is a use of an existing name
	_ len
)

type pairOf[K any, V any] struct {
	key   K
	value V
}

func (p *pairOf[recover, imag]) get() (recover, imag) { // Noncompliant 2
	return p.key, p.value
}

// The predeclared types are shadowed by a declaration just like the constants and the functions. The ones declared at
// package scope live in PredeclaredIdentifierShadowedCheckTypes.go, so that this file keeps type-checking.

func withTypeNames(byte int, rune string) (error bool) { // Noncompliant 3
	uintptr := byte // Noncompliant
	for comparable, float64 := range []int{1} { // Noncompliant 2
		fmt.Println(comparable, float64)
	}
	fmt.Println(uintptr, rune)
	return false
}

// The import aliases above are usable under their new names, a use is never reported.
func usesTheImportAliases() {
	fmt.Println(max.ToUpper("a"), valid.MinRead, recover.Getenv("PATH"))
}
