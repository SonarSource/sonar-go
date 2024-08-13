class A {
  void foo() {
    if (true) {
      doSomething();
    }

    if (false) {
      doSomething();
    }

    if ((true)) {
      doSomething();
    }
  }

  void bar() {
    doSomething();
  }
}
