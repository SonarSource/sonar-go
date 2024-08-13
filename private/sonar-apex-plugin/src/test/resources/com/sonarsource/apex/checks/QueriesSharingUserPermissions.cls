class MyClass0 {
  void foo() {
    for(Case c:[SELECT Id FROM Case WHERE Status = 'In Progress']){
      // ...
    }
  }
}

public with sharing class MyClass1 {
  void foo() {
    List<Case> lstCases = new List<Case>();
    for(Case c:[SELECT Id FROM Case WHERE Status = 'In Progress']){
      // ...
    }
  }
}

public without sharing  class MyClass2 {
//     ^^^^^^^^^^^^^^^> {{permissions modifier}}
  void foo() {
// Noncompliant@+1 {{Make sure that executing SOQL, SOSL or DML queries without sharing is safe here}}
    List<List<SObject>> sList = [FIND 'TEST' IN ALL FIELDS RETURNING Case(Name), Contact(FirstName,LastName)];
//                              ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
  }
}

public inherited sharing class MyClass3 {
//     ^^^^^^^^^^^^^^^^^> {{permissions modifier}}
  void foo() {
    List<Case> lstCases = new List<Case>();
// Noncompliant@+1 {{Make sure that executing SOQL, SOSL or DML queries with inherited sharing is safe here}}
    for(Case c:[SELECT Id, Status FROM Case WHERE Status = 'In Progress']){
//             ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
      c.Status = 'Closed';
      lstCasesToBeUpdated.add(c);
    }
// Noncompliant@+1 {{Make sure that executing SOQL, SOSL or DML queries with inherited sharing is safe here}}
    Update lstCasesToBeUpdated;
//  ^^^^^^^^^^^^^^^^^^^^^^^^^^^
  }
}

public inherited sharing class MyClass4 {
//     ^^^^^^^^^^^^^^^^^>
  void foo(Task task) {
    update task; // Noncompliant
//  ^^^^^^^^^^^^
  }
}

public inherited sharing class MyClass5 {
//     ^^^^^^^^^^^^^^^^^>
  void foo(Task task) {
    // Only one issue reported
    delete [select id from Error__c where Context_Type__c = 'RLLP']; // Noncompliant
//  ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

    merge m1 [select id from Error__c where Context_Type__c = 'RLLP']; // Noncompliant
//  ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

    upsert [select id from Error__c where Context_Type__c = 'RLLP'] Account.Fields.MyExternalId__c; // Noncompliant
//  ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
  }
}
