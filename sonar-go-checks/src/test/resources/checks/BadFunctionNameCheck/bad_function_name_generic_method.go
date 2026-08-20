package BadFunctionNameCheck

type container[T any] struct {
	items []T
}

// Since Go 1.27, a method may declare its own type parameters.
// The method name is still subject to the naming convention, and the type parameter list must not be part of the reported range.
func (c container[T]) mapValues[U any](f func(T) U) []U { // OK
	return nil
}

func (c container[T]) map_values[U any](f func(T) U) []U { // Noncompliant {{Rename function "map_values" to match the regular expression ^(_|[a-zA-Z0-9]+)$}}
//                    ^^^^^^^^^^
	return nil
}
