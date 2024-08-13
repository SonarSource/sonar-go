/*
    Copyright
*/
class A {
  public void foo(Integer param, Integer i) {
    if (param == 1) {
      openWindow();
    } else if (param == 2) {
      closeWindow();
    } else if (param == 1) { // Noncompliant
      moveWindowToTheBackground();
    } else {
      // ...
    }
  }

  public void foo2(Integer param, Integer i) {
    switch on i {
      when 1 {
        openWindow();
      }
      when 2 {
        closeWindow();
      }
      when 1 { // Noncompliant
        closeAgainWindow();
      }
      when 1, 3 { // Compliant
        compliantClose();
      }
      when else {
        // ...
      }
    }
  }

  public void foo3(Integer param, Integer i) {
    if(param == i) {
      System.debug(' 1');
    } else if(param === i) { // Compliant
      System.debug(' 2');
    } else {
      System.debug(' 3');
    }

    if(param == i || param == i + 1 && param - 1 < 10) {
      System.debug(' 1');
    } else if(param == i || param == i + 1  && param - 1 < 10) { // Noncompliant
      System.debug(' 2');
    } else if((param == i || param == i + 1)  && param - 1 < 10) { // Compliant
      System.debug(' 3');
    } else {
      System.debug(' 4');
    }
  }
}
