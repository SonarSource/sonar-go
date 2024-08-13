class A {
    void run(String msg) {
        System.debug('string literal'); // Noncompliant - 'string literal' is duplicated 3 times
        System.assertEquals(msg, 'string literal');
        msg = 'string literal';
    }

    void method(String msg) {
        System.debug('a'); // Compliant - literal 'a' has less than 5 characters and is excluded
        System.assertEquals(msg, 'a');
        msg = 'a';
    }
}