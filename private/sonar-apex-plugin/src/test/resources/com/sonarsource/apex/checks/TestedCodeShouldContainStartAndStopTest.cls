@isTest()
class Bar2 {
    @isTest()
    static void myTestMethod() { // Compliant, one helper method contains start and stop, code might have been refactored
        myHelperStartStop();
    }

    static void myHelperStartStop() { // Compliant
        Test.StartTest();
        Test.StopTest();
    }
}


@isTest()
class Foo {

    @isTest()
    static void myTestMethod() { // Noncompliant
    }

    @IsTest()
    static void myTestMethod() { // Noncompliant
       Test.StartTest();
    }

    @ISTEST()
    static void myTestMethod() { // Noncompliant
       Test.StopTest();
    }

    @isTest()
    static void myTestMethod() { // Noncompliant
       Test.StopTest();
       Test.StopTest();
    }

    static testmethod void myTestMethod2() { // Noncompliant
    }

    @isNotTest()
    static void myTestMethod() { // Compliant
    }

    @isTest()
    static void myTestMethod() { // Compliant
        Test.StartTest();
        Test.StopTest();
    }


    @isTest()
    @otherAnnotation()
    static void myTestMethod() { // Compliant
        Test.StartTest();
        Test.StopTest();
    }

    @isTest()
    static void myTestMethod() { // Compliant
        Test.sTaRtTeSt();
        Test.stOPTest();
    }

    static testmethod void myTestMethod2() { // Compliant
        Test.StartTest();
        Test.StopTest();
    }

    public static testmethod void myTestMethod2() { // Noncompliant
    }

    abstract testmethod void abtractTestMethod(); // Compliant

    static void myHelper() { // Compliant, and does not contains start and stop
    }
}

class Bar {
    @isTest()
    static void myTestMethod() { // Compliant, not belonging to test class
    }
}

@isTest()
@otherAnnotation()
class Bar2 {
    @isTest()
    static void myTestMethod() { // Noncompliant
    }
}


