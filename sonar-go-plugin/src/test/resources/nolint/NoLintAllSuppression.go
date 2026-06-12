// Every recognised //nolint form is a blanket per-line suppression; wider scopes are tracked
// in SONARGO-815. Trigger: x == x raises S1764.
package nolint

func recognisedForms() {
	x := 1
	_ = x == x //nolint                                           // Compliant (S1764)
	_ = x == x //nolint:all                                       // Compliant (S1764)
	_ = x == x //nolint:all // explanation about why this is fine // Compliant (S1764)
	_ = x == x //nolint:errcheck,all                              // Compliant (S1764)
	_ = x == x // nolint                                          // Compliant (S1764)
	_ = x == x //   nolint:all                                    // Compliant (S1764)
}

func unrecognisedForms() {
	x := 1
	_ = x == x //NoLint                            // Noncompliant (S1764)
	_ = x == x //nolint:All                        // Noncompliant (S1764)
	_ = x == x //NOLINT                            // Noncompliant (S1764)
	_ = x == x //nolintall                         // Noncompliant (S1764)
	_ = x == x //no-lint:all                       // Noncompliant (S1764)
	_ = x == x // this comment mentions nolint:all // Noncompliant (S1764)
}

func scopePrecedingLine() {
	x := 1
	//nolint:all
	_ = x == x // Noncompliant (S1764) // FP - preceding-line directive not yet honoured (SONARGO-815)
}

//nolint:all
func scopeFunction() {
	x := 1
	_ = x == x // Noncompliant (S1764) // FP - function-level directive not yet honoured (SONARGO-815)
}
