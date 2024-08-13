class A {
  void foo(Boolean b) {
    doSomething();
    doSomethingElse();
    if (b) {
      doSomethingFun();
    }
  }

  // Noncompliant
  void bar(Boolean b) {
    doSomething();
    doSomethingElse();
    if (b) {
      doSomethingFun();
    }
  }

  // Noncompliant - identifiers are not case sensitive
  void qix(Boolean b) {
    dosoMething();
    dosOmeThingeLse();
    iF (B) {
      dosomethingfun();
    }
  }
}
