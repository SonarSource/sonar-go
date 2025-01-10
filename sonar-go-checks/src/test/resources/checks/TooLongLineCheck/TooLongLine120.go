package samples

func foo(p1 string, p2 int) int {
    return 0
}

// short comment

// Noncompliant@+1 {{Split this 122 characters long line (which is greater than 120 authorized).}}
func fooVeryLongName(fooVeryLongName1 string, fooVeryLongName2 string, fooVeryLongName3 string, fooVeryLongName4 string) {
}

// Noncompliant@+1 {{Split this 130 characters long line (which is greater than 120 authorized).}}
// this is a very long comment that should raise an issue when its length is greater that 120 characters that is the default value
