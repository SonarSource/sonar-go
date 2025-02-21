package main

import "fmt"

func main() {
    var a int = 1 // Noncompliant{{Symbol found!}}
    //  ^
    fmt.Println(a) // Noncompliant{{Symbol found!}}
    //          ^
}
