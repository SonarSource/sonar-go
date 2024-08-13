@isTest()
class Foo {

    @isTest()
    static void myTestMethod() { // Compliant
       System.runAs(u) {
           Test.startTest();
           Test.stopTest();
       }
    }

    @isTest
    static void myTestMethod() { // Noncompliant
    }

    @isTest()
    static void myTestMethod() { // Noncompliant
    }

    static testmethod void myTestMethod2() { // Noncompliant
    }

    @isTest()
    static void myTestMethod() { // Noncompliant
       Test.startTest();
       Test.StopTest();
    }

    @isTest()
    static void myTestMethod() { // Compliant
       System.runAs(u) {
           Test.startTest();
           Test.stopTest();
       }
    }

    static testmethod void myTestMethod() { // Compliant
       System.runAs(u) {
           Test.startTest();
           Test.stopTest();
       }
    }

    @isNotTest()
    static void myTestMethod() { // Compliant
    }

    @isTest()
    @otherAnnotation()
    static void myTestMethod() { // Noncompliant
    }

    @isTest()
    static void myTestMethod() { // Compliant
       System.RuNaS(u) {
           Test.startTest();
           Test.stopTest();
       }
    }

    public static testmethod void myTestMethod2() { // Noncompliant
    }

    abstract testmethod void abtractTestMethod(); // Compliant

    static void myHelper() { // Compliant, helper method
    }
}

@isTest()
class Bar2 {
    @isTest()
    static void myTestMethod() { // Compliant one helper method contains start and stop, code might have been refactored
        myHelperRunAs();
    }

    static void myHelperRunAs() { // Compliant
        System.runAs(u) {
            Test.startTest();
            Test.stopTest();
        }
    }
}

class Bar3 {
    @isTest()
    static void myTestMethod() { // Compliant, not belonging to test class
    }
}

@isTest()
@otherAnnotation()
class Bar4 {
    @isTest()
    static void myTestMethod() { // Noncompliant
    }
}
