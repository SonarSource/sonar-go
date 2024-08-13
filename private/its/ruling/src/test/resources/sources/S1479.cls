class A {
  public void foo(Integer i, Object o, Month m) {
    switch on i {
      when 1 { System.debug(' 1'); }
      when 2 { System.debug(' 2'); }
      when -3 { System.debug(' -3'); }
      when 4 { System.debug(' 4'); }
      when 5 { System.debug(' 5'); }
    }

    switch on i {
      when 1 { System.debug(' 1'); }
      when null { System.debug('aouch!'); }
      when 3 { System.debug(' 3'); }
      when 4 { System.debug(' 4'); }
      when 5, 6, 7, 8, 9 { System.debug(' 5 and more'); }
    }

    switch on i {                      // Noncompliant
      when 1 { System.debug(' 1'); }
      when 2 { System.debug(' 2'); }
      when null { System.debug('aouch!'); }
      when 4 { System.debug(' 4'); }
      when 5 { System.debug(' 5'); }
      when 6 { System.debug(' 6'); }
    }

    switch on i {                      // Noncompliant
      when 1 { System.debug(' 1'); }
      when 2 { System.debug(' 2'); }
      when 3 { System.debug(' 3'); }
      when 4 { System.debug(' 4'); }
      when 5 { System.debug(' 5'); }
      when else { System.debug('yolo'); }
    }

    switch on o {                      // Noncompliant
      when Type1 a1 { System.debug( a1 ); }
      when Type2 a2 { System.debug( a2 ); }
      when Type3 a3 { System.debug( a3 ); }
      when Type4 a4 { System.debug( a4 ); }
      when Type5 a5 { System.debug( a5 ); }
      when else { System.debug('yolo'); }
    }

    switch on m {                      // Noncompliant
      when JANUARY { System.debug( 'cold' ); }
      when FEBRUARY { System.debug( 'cold' ); }
      when MARCH, APRIL, MAY { System.debug( 'messed up' ); }
      when JUNE { System.debug( 'pouring rain' ); }
      when JULY { System.debug( '50°C' ); }
      when else { System.debug( 'boring' ); }
    }
  }
}
