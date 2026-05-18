package StringLiteralDuplicatedCheck

import (
  "errors"
  "fmt"
  "log"
  "log/slog"
)

func main() {
  x := "string literal1" // Noncompliant {{Define a constant instead of duplicating this literal "string literal1" 3 times.}} [[effortToFix=2]]
  //   ^^^^^^^^^^^^^^^^^
  x += "string literal1" + "other string literal"
  //  <^^^^^^^^^^^^^^^^^
}

func (user *User) literal1() {
  user.name = "string literal1"
}

func literal2() string {
  v := "string literal2" + "string literal2" // Compliant - literal only appears twice
  return v
}

func literal3() {
  a := "string literal3"
  b := "string literal3"
  c := "string literal3${x}" // Compliant - string entries of string templates not considered as string literals
}

func funtcion1(abcde string) {
  if abcde == "string literal4" { // Noncompliant {{Define a constant instead of duplicating this literal "string literal4" 5 times.}} [[effortToFix=4]]
    //        ^^^^^^^^^^^^^^^^^
  }
}

func function2() string {
  switch "string literal4" {
  //    <^^^^^^^^^^^^^^^^^
  case "abc":
    return "string literal4"
//        <^^^^^^^^^^^^^^^^^
  case "string literal4":
    //  <^^^^^^^^^^^^^^^^^
    return "string literal4"
    //    <^^^^^^^^^^^^^^^^^
  }
  return ""
}

func nested_log_calls() {
  // Noncompliant@+1
  log.Printf(foo("literal string"))
  //             ^^^^^^^^^^^^^^^^
  log.Printf(foo("literal string"))
  //            <^^^^^^^^^^^^^^^^
  log.Printf(foo("literal string"))
  //            <^^^^^^^^^^^^^^^^
  log.Printf(foo("literal string"))
  //            <^^^^^^^^^^^^^^^^
}

func foo(text string) string {
  return text
}

func nested_log_calls2() {
  // Complaint as fmt.Sprintf is also ignored
  log.Printf(fmt.Sprintf("literal string"))
  log.Printf(fmt.Sprintf("literal string"))
  log.Printf(fmt.Sprintf("literal string"))
  log.Printf(fmt.Sprintf("literal string"))
}

func concatenation_in_func() {
  // The issues below are raised for "prefix: " and "literal 2" literals
  // Noncompliant@+2
  // Noncompliant@+1
  log.Printf("prefix: " + "literal 2")
  //         ^^^^^^^^^^
  log.Printf("prefix: " + "literal 2")
  //        <^^^^^^^^^^
  log.Printf("prefix: " + "literal 2")
  //        <^^^^^^^^^^
  log.Printf("prefix: " + "literal 2")
  //        <^^^^^^^^^^
}

func function3() {
  "abcd"
  "abcd"
  "abcd"
  "abcd" // Compliant - string length smaller than threshold
}

func function4() {
  "string_literal5"
  "string_literal5"
  "string_literal5"
  "string_literal5" // Compliant - single word
}

type User struct {
  name string
}

func logAndErrorFunctions() {
  log.Printf("log: operation failed")
  log.Printf("log: operation failed")
  log.Printf("log: operation failed") // Compliant - string literal used in log function

  _ = fmt.Errorf("fmt: operation failed")
  _ = fmt.Errorf("fmt: operation failed")
  _ = fmt.Errorf("fmt: operation failed") // Compliant - string literal used in fmt.Errorf

  _ = errors.New("errors: invalid input")
  _ = errors.New("errors: invalid input")
  _ = errors.New("errors: invalid input") // Compliant - string literal used in errors.New

  slog.Info("slog: server started")
  slog.Info("slog: server started")
  slog.Info("slog: server started") // Compliant - string literal used in slog function
}
