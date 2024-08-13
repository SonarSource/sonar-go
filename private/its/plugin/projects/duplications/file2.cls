public class Bar {
  public void duplicatedFunction(List<String> args) {
    System.debug('hello world1');
    Integer a = 0;
    a = a + 1;
    System.debug('hello world2');
    a = 0;
    a = a + 1;
    System.debug('hello world3');
    a = 0;
    a = a + 1;
    System.debug('hello world4');
    a = 0;
    a = a + 1;
    System.debug('hello world2');
    a = 0;
    a = a + 1;
    System.debug('hello world3');
    a = 0;
    a = a + 1;
    System.debug('hello world4');
    a = 0;
    a = a + 1;
  }

  public void bodyDuplication(List<Integer> args) {
    System.debug('bonjour1');
    Integer a = 0;
    a = a + 1;
    System.debug('bonjour2');
    a = 0;
    a = a + 1;
    System.debug('bonjour3');
    a = 0;
    a = a + 1;
    System.debug('bonjour4');
    a = 0;
    a = a + 1;
    System.debug('bonjour2');
    a = 0;
    a = a + 1;
    System.debug('bonjour3');
    a = 0;
    a = a + 1;
    System.debug('bonjour4');
    a = 0;
    a = a + 1;
  }

  public void nonDuplicated(List<String> args) {
    System.debug('');
    Integer a = 0;
    a = a + 2;
    System.debug('');
    a = 0;
    a = a + 3;
    System.debug('');
    a = 0;
    a = a + 4;
    System.debug('');
    a = 0;
    a = a + 5;
    System.debug('');
    a = 0;
    a = a + 6;
    System.debug('ciao');
    a = 0;
    a = a + 7;
    System.debug('tchuss');
    a = 0;
    a = a + 8;
  }

}
