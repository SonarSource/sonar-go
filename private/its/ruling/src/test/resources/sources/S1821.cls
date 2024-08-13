class A {
  public void foo(Integer i, Integer j) {
    switch on i {
      when 1 {System.debug(' 1'); }
      when 2 { System.debug(' 2'); }
      when -3 {
        switch on j {
          when 1 {System.debug(' 3');}
          when else {System.debug(' 4');}
        }
      }
    }
  }
}
