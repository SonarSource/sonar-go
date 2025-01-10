override int fun foo(int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8) {} // OK
private int fun foo(int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8) {} // Noncompliant
native [] {} int fun foo(int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8) {} // Noncompliant

class A {
  fun constructor(int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8) {} // Compliant, constructor
}
