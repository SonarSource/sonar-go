// Noncompliant@0 {{File "TooManyLinesOfCodeFileCheck6.go" has 7 lines, which is greater than 6 authorized. Split it into smaller files.}}
// comment - not a line of code
package samples

func main() {
  if condition {
   a = b + 1
  }


  x := 4


  /*
   * There are 7 lines of code in this file
   */
}


