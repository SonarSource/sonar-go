package samples

func foo(p1 string, p2 string, p3 string, p4 string, p5 string, p6 string, p7 string) {
}

func foo(p1 string, p2 string, p3 string, p4 string, p5 string, p6 string, p7 string) int {
  return 0
}

func main(p1 string, p2 string, p3 string, p4 string, p5 string, p6 string, p7 string) {
}


// Noncompliant@+1 {{This function has 8 parameters, which is greater than the 7 authorized.}}
func foo(p1 string, p2 string, p3 string, p4 string, p5 string, p6 string, p7 string, p8 string) {
//   ^^^                                                                              ^^^^^^^^^<
}

// Noncompliant@+1
func foo(p1 string, p2 string, p3 string, p4 string, p5 string, p6 string, p7 string, p8 string) int {
  return 0
}

// Noncompliant@+1
func main(p1 string, p2 string, p3 string, p4 string, p5 string, p6 string, p7 string, p8 string) {
}

// Noncompliant@+1
func Foo(p1 string, p2 string, p3 string, p4 string, p5 string, p6 string, p7 string, p8 string) int {
  return 0
}

type A struct {
  b int
}

func (a *A) add(p1 int) {
  a.b = p1
}

func (a *A) addWithManyParameters(p1 int, p2 int, p3 int, p4 int, p5 int, p6 int, p7 int, p8 int) { // Noncompliant
  a.b = p1
}

// Somehow a FP int, as this is a go style constructor
func NewA(p1 int, p2 int, p3 int, p4 int, p5 int, p6 int, p7 int, p8 int) *A { // Noncompliant
  p := new(A)
  p.b = p1
  return p
}

func anonymousFunctionsInside() {
  // Noncompliant@+1
  func (p1 int, p2 int, p3 int, p4 int, p5 int, p6 int, p7 int, p8 int) {
    println(p1)
  }(1, 2, 3, 4, 5, 6, 7, 8)

  func () {
      println("test") // Compliant
    }()
}

