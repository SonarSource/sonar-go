/*
    Copyright
*/
class A {
  public Integer foo() {
    switch on i { // Compliant - depth = 1
      when 1 {
        for (Integer i = 0, j = 0; i < 10; i++) { // Compliant - depth = 2
          Integer j = 1;
          if(j == 0){// Compliant - depth = 3, not exceeding the limit
            while(j < 10){ // Noncompliant - depth = 4
              if(j == 1){ // Depth = 5, exceeding the limit, but issues are only reported on depth = 4
                return 4;
              }
            }
          }
        }
      }
      when 2 {
        Integer j = 1;
        do{ // Compliant - depth = 2
          try{ // Compliant - depth = 3
            if(j == 0){ // Noncompliant - depth = 4
              return 1;
            }
          } catch(Exception e) {
            System.debug('An unexpected error has occurred: ' + e.getMessage());
          }
        }while(j < 10);
      }
      when else { return 0; }
    }
  }
}
