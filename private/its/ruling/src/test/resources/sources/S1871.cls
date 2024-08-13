class A {
    void foo() {
        switch on i {
            when 1 {
                doFirstThing();
                doSomething();
            }
            when 2 {
                doSomethingDifferent();
            }
            when 3 {  // Noncompliant; duplicates when 1's implementation
                doFirstThing();
                doSomething();
            }
            when else {
                doTheRest();
            }
        }
    }
}
