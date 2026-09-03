package samples

import (
	"embed"
	"fmt"
)

// Names that live in their own namespace do not shadow anything: "value.len" and "break loop" resolve unambiguously.
type container struct {
	len  int
	copy string
}

type reader interface {
	close() error
	max(new int) (recover int)
}

// The parameters and the results of a function type have no body to shadow anything in.
type handler func(len int) (cap error)

// A method name is looked up in the namespace of its receiver type.
func (c container) delete() {}

var emptyFunction func(true bool)

var _ embed.FS

func usesPredeclaredIdentifiers(items []int) {
	fmt.Println(len(items), cap(items), true, false, nil)
	value := container{len: 1, copy: "a"}
	fmt.Println(value.len, value.copy)
	var _ = new(container)
	var count, size int
	_, _ = count, size

loop:
	for range items {
		break loop
	}

	// A predeclared type used as a type shadows nothing
	var amount int
	var label string
	var failure error
	fmt.Println(amount, label, failure)
}

type (
	firstType  int
	secondType string
	generic[T any] struct {
		value T
	}
)

// A receiver type argument declares a type parameter, the receiver type itself is a use of an existing name.
func (g *generic[T]) valueOf() T {
	return g.value
}

type pairOf[K any, V any] struct {
	key   K
	value V
}

// A receiver declaring several type parameters reuses the names of its own type.
func (p *pairOf[K, V]) get() (K, V) {
	return p.key, p.value
}

// The types of an unnamed result list, of a field and of a parameter are uses of a predeclared type, not declarations.
type measures struct {
	int    int
	string string
}

func unnamedResults(codePoint rune) (int, error) {
	var buffer []byte
	fmt.Println(codePoint, buffer)
	return 0, nil
}

func typeParameterConstraint[T comparable](value T) any {
	return value
}

type number interface {
	int | float64 // Compliant, a type union constrains, it declares nothing
}

func typePositions[T number](values map[string][]byte, callback func(rune) error) (any, error) {
	total := 0
	for key, value := range values {
		total += int(value[0]) + len(string(key))
	}
	if converted, isString := any(total).(string); isString {
		fmt.Println(converted)
	}
	fmt.Println(callback, uint8(total), []complex128{})
	return total, nil
}
