class Foo {
  void foo() {
    bar(''); // Compliant
    bar('Hello World'); // Compliant
    bar('000000000000000'); // Noncompliant
    bar('HelloSuperWorld'); // Compliant (15 digits)
    bar('009000000000000'); // Compliant
    bar('0JT000000000000'); // Noncompliant
    bar('0jt000000000000'); // Noncompliant
    bar('0JT000000000000123'); // Noncompliant
    bar('0JT00000000000-123'); // Compliant
    bar('0JT000000000000123 '); // Compliant

    bar('012500000009tuy'); // Noncompliant
    bar('0123000000095Kmwer'); // Noncompliant
    bar('a12000000000000'); // Noncompliant

    // Three digits pattern
    bar('a11000000000000'); // Noncompliant
    bar('e22000000000000'); // Noncompliant
    bar('m33000000000000'); // Noncompliant
    bar('z44000000000000'); // Noncompliant
    bar('a55000000000000123'); // Noncompliant

    // Special non 3 digits prefixes
    bar('kA8000000000000'); // Noncompliant
    bar('ka8000000000000'); // Noncompliant
    bar('CF00N0000000000'); // Noncompliant
    bar('0.00E0000000000'); // Noncompliant
    bar('0.00E0000000000123'); // Noncompliant
  }
  void bar(string id) {
  }
}

@IsTest()
class Bar {
  void bar() {
    bar('000000000000000'); // Compliant, in a test class
  }
}

@IsTest()
class Bar {
    class InsideBar {
      void bar() {
        bar('000000000000000'); // Compliant, in a test class
      }
    }
}
