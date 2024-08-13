class A {
  public void foo(Integer value) {
    switch on value {                  // Noncompliant
      when 1 {
        methodCall1('');
        methodCall2('');
        methodCall3('');
        methodCall4('');
        methodCall5('');
        methodCall6('');
        methodCall7('');
        methodCall8('');
      }
      when 2 { /* ... */ }
    }
  }

  public void bar(Integer value) {
    switch on value {                  // Noncompliant
      when 1 { /* ... */ }
      when else {
        methodCall1('');
        methodCall2('');
        methodCall3('');
        methodCall4('');
        methodCall5('');
        methodCall6('');
        methodCall7('');
        methodCall8('');
      }
    }
  }

  public void qix(Integer value) {
    switch on value {
      when 1 { doSomething(); }
      when 2 { /* ... */ }
    }
  }

  private void doSomething() {
    methodCall1('');
    methodCall2('');
    methodCall3('');
    methodCall4('');
    methodCall5('');
    methodCall6('');
    methodCall7('');
    methodCall8('');
  }
}
