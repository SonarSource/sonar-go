package samples

import "package/with/long/name/longer/than/40"

func foo(p1 string) {
}

// Noncompliant@+1
func funWithLongName(p1 string, p2 string, p3 string) {

	// Noncompliant@+1
	// long comment with url http://example.com
	// ^[sc=1;ec=44]
	println(10)
}

// http://very-long-url.com/this/is/a/41

// Noncompliant@+1
// FixMe: http://very-long-url.com/this/is/a/48
