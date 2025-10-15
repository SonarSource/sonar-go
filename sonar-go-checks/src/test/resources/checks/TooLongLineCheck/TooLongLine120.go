package samples

import (
	"net/http"
	"unicode/utf8"

	"github.com/golang/repo-with-long-name/path-with-long-name/subdir-with-long-name/package-with-long-name/package/v1"
)
import (
	"github.com/golang/another-repo-with-long-name/path-with-long-name/subdir-with-long-name/package-with-long-name/package/v1"
)

func foo(p1 string, p2 int) int {
	return 0
}

// short comment

// Noncompliant@+1 {{Split this 122 characters long line (which is greater than 120 authorized).}}
func fooVeryLongName(fooVeryLongName1 string, fooVeryLongName2 string, fooVeryLongName3 string, fooVeryLongName4 string) {
}

// Noncompliant@+1 {{Split this 130 characters long line (which is greater than 120 authorized).}}
// this is a very long comment that should raise an issue when its length is greater that 120 characters that is the default value

// Compliant, long URL without additional text should be ignored
// http://very-long-url.com/this/is/a/very/long/url/that/should/not/raise/an/issue/when/its/length/is/greater/that/120/characters

// Noncompliant@+1 {{Split this 127 characters long line (which is greater than 120 authorized).}}
// http://short-url.com/this/is/a/shorter/url/than/120/characters/but/overall/line/length/isgreter/than/120 and some text after

// Noncompliant@+1 {{Split this 122 characters long line (which is greater than 120 authorized).}}
// http://short-url.com/this/is/a/shorter/url/than/120/characters/but/overall/line/length/isgreter/than/120	and		tab after

func getMethod(r *http.Request) string {
	switch r.Method {
	// Noncompliant@+1
	case "HEAD", "GET", "POST", "PUT", "DELETE", "CONNECT", "OPTIONS", "TRACE": // https://some-url.org/path/to/the/url_/121
		return r.Method
	}
	return ""
}
