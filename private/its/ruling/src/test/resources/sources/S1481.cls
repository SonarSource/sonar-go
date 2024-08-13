class A {
  void foo() {
    Integer a = 0; // Compliant - as identifiers are case insensitive, a is used bellow
    Integer b;
    System.debug(A);
  }

  void bar() {
    Integer orgAcctId = 100; // FP - orgAcctId is actually used by Dynamic SOQL query bellow
    String query = 'SELECT Count() FROM Account WHERE Id != :orgAcctId';
    List<List<SObject>>searchList = Database.query(query);
  }

  void foo() {
    Integer a, b; // Noncompliant, b is never used
    System.debug(a);
  }
}

class Foo {
    void foo() {
        String s1 = ''; // Noncompliant
        String s2; // Noncompliant
    }

    void bar() {
        String first = '';
        List<Lead> drop0 = [SELECT Id FROM Lead WHERE CreatedDate > :first]; // Noncompliant
                // ^^^^^
    }

    void bar2() {
        String second;
        String third;
        Integer fourth;
        Integer fifth;
        Integer sixth;
        List<List<SObject>> drop2 = [FIND :second IN ALL FIELDS // Noncompliant
                         // ^^^^^
                                   RETURNING
                                      Account (Id, Name WHERE Name LIKE :third
                                               LIMIT :fourth),
                                      Contact,
                                      Opportunity,
                                      Lead
                                   WITH DIVISION =:fifth
                                   LIMIT :sixth];
    }

    private void bar3() {
        String a;
        String unused; // Noncompliant
        List<Account> accounts = [SELECT id, name FROM Account WHERE Name =: a]; // Noncompliant
                 //   ^^^^^^^^
    }

    String qix() {
        String s = '';
        return s;
    }
}