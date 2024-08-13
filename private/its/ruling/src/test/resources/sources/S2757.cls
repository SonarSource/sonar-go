/*
    Copyright
*/
class A {
  void foo() {
    Integer x = 1;
    Integer y = -1;

    x += 1; // Compliant
    x = +y; // Compliant
    x = + y; // Compliant
    x =+1; // Noncompliant
    x =+ 1; // Noncompliant
    x =+ y; // Noncompliant
    x =+            y; // Noncompliant
    x =+ (y + 1); // Noncompliant

    x -= 1; // Compliant
    x = -y; // Compliant
    x =- 1; // Noncompliant

    x != 1; // Compliant
    x = !y; // Compliant
    x =! 1; // Noncompliant
  }
}
