/*
    Copyright
*/
class A {
    String passwd = 'abc'; // Noncompliant
    public String passwd = 'abc'; // Noncompliant

    void foo() {
        String password = 'abc'; // Noncompliant

        String pwd; // Compliant
        pwd = 'abc'; // Noncompliant

        String s = 'pwd=abc'; // Noncompliant

        this.passwd = 'abc'; // Noncompliant

        String params = 'user=admin&password=Password123'; // Sensitive
        String sqlserver = 'pgsql:host=localhost port=5432 dbname=test user=postgres password=postgres'; // Sensitive
    }

    void f1() {
        String password = 'Password'; // Compliant
    }

    void f2() {
        String password = '[id=\'password\']'; // Compliant
    }

    void f3() {
        String password = 'custom.password'; // Compliant
    }

    void f4() {
        String password = 'trustStorePassword'; // Compliant
    }

    void f5() {
        String password = 'connection.password'; // Compliant
    }

    void f6() {
        String password = '/users/resetUserPassword'; // Compliant
    }

    void databaseQuery(String password) {
      String query1 = 'password=?'; // Compliant
      String query2 = 'password=:password'; // Compliant
      String query3 = 'password=:param'; // Compliant
      String query4 = 'password=' + password ; // Compliant
      String query5 = 'password=%s'; // Compliant
      String query6 = 'password=%v'; // Compliant
    }

    void uriUserInfo() {
      String url1 = 'scheme://user:azerty123@domain.com'; // Sensitive
      String url2 = 'scheme://user:@domain.com'; // Compliant
      String url3 = 'scheme://user@domain.com:80'; // Compliant
      String url4 = 'scheme://user@domain.com'; // Compliant
      String url5 = 'scheme://domain.com/user:azerty123'; // Compliant
    }

    void constantPropagation() {
      String uname = getString();
      String password = getString();
      String conn = 'jdbc:mysql://localhost/test?' +
       'user=' + uname + '&password=' + password; // Compliant

      // in Java, constant propagation is tracked. In Apex, we don't.
      String uname = 'steve';
      String hardcoded = 'blue';
      String conn = 'jdbc:mysql://localhost/test?' +
        'user=' + uname + '&password=' + hardcoded; // FN
    }

    void stringFormat() {
      String a = getString();
      String b = getString();
      String value1 = String.format('user={0}&password={1}', a, b); // Compliant
      String value2 = String.format('user={0}&password={1}', 'a', 'b'); // FN
   }

   String getString() {
      return '';
   }
}
