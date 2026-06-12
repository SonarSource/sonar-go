// Only //nolint lists that name `gosec` (or `all`) are honored as blanket per-line
// suppression for now; other linter names are out of scope and tracked in SONARGO-815.
// Trigger: x == x raises S1764.
package nolint

func recognisedForms() {
	x := 1
	_ = x == x //nolint:gosec                          // Compliant (S1764)
	_ = x == x //nolint:gosec,errcheck                 // Compliant (S1764)
	_ = x == x //nolint:errcheck,gosec                 // Compliant (S1764)
	_ = x == x //nolint:staticcheck,gosec,unused       // Compliant (S1764)
	_ = x == x //nolint:gosec // explanation tolerated // Compliant (S1764)
	_ = x == x // nolint:gosec                         // Compliant (S1764)
}

func unrecognisedForms() {
	x := 1
	_ = x == x //nolint:errcheck                                             // Noncompliant (S1764) - FP - not yet honored, only gosec/all are (SONARGO-815)
	_ = x == x //nolint:staticcheck                                          // Noncompliant (S1764) - FP - not yet honored, only gosec/all are (SONARGO-815)
	_ = x == x //NoLint:gosec                                                // Noncompliant (S1764)
	_ = x == x //nolint:GoSec                                                // Noncompliant (S1764)
	_ = x == x //nolintgosec                                                 // Noncompliant (S1764)
	_ = x == x // this is not a directive, just a comment about nolint:gosec // Noncompliant (S1764)
}

func scopePrecedingLine() {
	x := 1
	//nolint:gosec
	_ = x == x // Noncompliant (S1764) // FP - preceding-line directive not yet honoured (SONARGO-815)
}

//nolint:gosec
func scopeFunction() {
	x := 1
	_ = x == x // Noncompliant (S1764) // FP - function-level directive not yet honoured (SONARGO-815)
}
