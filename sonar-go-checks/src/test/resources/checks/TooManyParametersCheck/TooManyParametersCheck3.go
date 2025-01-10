package samples

func foo(p1 string, p2 string, p3 string) {
}

// Noncompliant@+1 {{This function has 8 parameters, which is greater than the 3 authorized.}}
func foo(p1 string, p2 string, p3 string, p4 string, p5 string, p6 string, p7 string, p8 int) {
//   ^^^                                  ^^^^^^^^^< ^^^^^^^^^< ^^^^^^^^^< ^^^^^^^^^< ^^^^^^<
}
