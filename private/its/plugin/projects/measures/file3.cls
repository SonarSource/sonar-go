class A {
  Integer foo() {
    if(1 != 1) {  // +1 for if
      return 1;
    }
    return 0;
  }

  Integer foo() {
    if (1 == 1) {     // +1 for if
      if (1 <> 2) {   // +2 for nested if
        return 1;
      }
    }
    return 0;
  }

  Integer bar(Integer i) {
    switch on i {            // +1 for switch
      when 1 { foo(); }
      when 2 { foo(); }
      when else {
        if (i == 3) foo();   // +2 for nested if
      }
    }
  }
}
