class Foo {
    void publicFoo(String s) { // only private methods are checked
    }

    private void foo(String s) { // Noncompliant
    }

    private void bar(String first) {
        List<Lead> drop0 = [SELECT Id FROM Lead WHERE CreatedDate > :first];
    }

    private void bar2(String second, String third, Integer fourth, Integer fifth, Integer sixth) { // Compliant
        List<List<SObject>> drop2 = [FIND :second IN ALL FIELDS
                                   RETURNING
                                      Account (Id, Name WHERE Name LIKE :third
                                               LIMIT :fourth),
                                      Contact,
                                      Opportunity,
                                      Lead
                                   WITH DIVISION =:fifth
                                   LIMIT :sixth];
    }

    private void bar3(String a, String unused) { // Noncompliant
                                //     ^^^^^^
        List<Account> accounts = [SELECT id, name FROM Account WHERE Name =: a];
    }

    private String qix(String s) { // Compliant
        return s;
    }
}
