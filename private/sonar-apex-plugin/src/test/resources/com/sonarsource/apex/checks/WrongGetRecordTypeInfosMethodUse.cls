class Foo {
  void foo() { // Compliant
    getRecordTypeInfosByName(); // Noncompliant
    getRecordTypeInfosByDeveloperName(); // Compliant

    A.getRecordTypeInfosByName(); // Noncompliant
    A.B.getRecordTypeInfosByName(); // Noncompliant
    A.getRecordTypeInfosByName().get('arg'); // Noncompliant
    A.get('arg').getRecordTypeInfosByName(); // Noncompliant
    A.getRecordTypeInfosByName('arg'); // Compliant

    Id RTId = Schema.SObjectType.Case.getRecordTypeInfosByName().get('Asia Service').getRecordTypeId(); // Noncompliant
  }

  void getRecordTypeInfosByName() { // Compliant
  }

  void getRecordTypeInfosByName(String s) { // Compliant
  }
}
