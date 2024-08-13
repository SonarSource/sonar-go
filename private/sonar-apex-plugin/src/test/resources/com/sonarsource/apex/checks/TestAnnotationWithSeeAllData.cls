@isTest() // Compliant
class Foo {

    @isTest(SeeAllData=true) // Noncompliant
//          ^^^^^^^^^^^^^^^
    static void myTestMethod() {
    }

    @otherAnnotation()
    @isTest(SeeAllData=true) // Noncompliant
//          ^^^^^^^^^^^^^^^
    static void myTestMethod() {
    }

    @isTest() // Compliant
    static void myTestMethod() {
    }

    @isTest // Compliant
    static void myTestMethod() {
    }

    @isTest(SeeAllData=false) // Compliant
    static void myTestMethod() {
    }

    static void helper() {
    }
}

@isTest() // Compliant
class Foo2 {
}

@isTest(SeeAllData = true) // Noncompliant
//      ^^^^^^^^^^^^^^^^^

class Foo3 {
}

@isTest(SeEaLlDaTa=true) // Noncompliant
//      ^^^^^^^^^^^^^^^
class Foo4 {
}

@isTest(SeeAllData=false) // Compliant
class Foo5 {
}

@isTest(isParallel=true) // Compliant, and isParallel can not be used together with SeeAllData
class Foo6 {
}

@isTest(OnInstall=true SeeAllData=true) // Noncompliant
//                     ^^^^^^^^^^^^^^^
class Foo7 {
}

@isTest(OnInstall=true isParallel=true) // Compliant
class Foo8 {
}

class Foo9 {
}


