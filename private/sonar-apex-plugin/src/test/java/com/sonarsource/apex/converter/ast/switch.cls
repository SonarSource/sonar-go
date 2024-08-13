class A {
  public void foo(Integer i) {
    switch on i {
      when 2, 3, 4 {
        System.debug('block 2');
      }
      when -3 {
        System.debug('block -3');
      }
      when null {
        System.debug('null');
      }
      when else {
        System.debug('default');
      }
    }
  }

  public void bar(sObject sobject) {
    switch on sobject {
      when Account a {
        System.debug('account ' + a);
      }
      when Contact c {
        System.debug('contact ' + c);
      }
      when null {
        System.debug('null');
      }
      when else {
        System.debug('default');
      }
    }
  }
}
