class MyClass0 {
  void foo() {
    // Do something without any SOQL, SOSL or DML query
  }
}

public with sharing class MyClass1 {
  void foo() {
    List<Case> lstCases = new List<Case>();
    for(Case c:[SELECT Id FROM Case WHERE Status = 'In Progress']) {
    }
  }
}

public without sharing class MyClass2 {
  void foo() {
    List<List<SObject>> sList = [FIND 'TEST' IN ALL FIELDS RETURNING Case(Name), Contact(FirstName,LastName)];
  }
}

public inherited sharing class MyClass3 {
  void foo() {
    List<Case> lstCases = new List<Case>();
    for(Case c:[SELECT Id, Status FROM Case WHERE Status = 'In Progress']) {
      c.Status = 'Closed';
      lstCasesToBeUpdated.add(c);
    }
    Update lstCasesToBeUpdated;
  }
}

public inherited sharing class MyClass4 {
  void foo(Task task) {
    update task;
  }
}

// Noncompliant@+1 {{Add "with sharing", "without sharing" or "inherited sharing"}}
   public class MyClass5 {
//              ^^^^^^^^
  void foo() {
    List<Case> lstCases = new List<Case>();
    for(Case c:[SELECT Id FROM Case WHERE Status = 'In Progress']) {
//             ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^< {{query}}

    }
  }
}

// Noncompliant@+1 {{Add "with sharing", "without sharing" or "inherited sharing"}}
   public class MyClass6 {
//              ^^^^^^^^
  void foo() {
    List<List<SObject>> sList = [FIND 'TEST' IN ALL FIELDS RETURNING Case(Name), Contact(FirstName,LastName)];
//                              ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^< {{query}}
  }
}

// Noncompliant@+1 {{Add "with sharing", "without sharing" or "inherited sharing"}}
   public class MyClass7 {
//              ^^^^^^^^
  void foo() {
    List<Case> lstCases = new List<Case>();
    for(Case c:[SELECT Id, Status FROM Case WHERE Status = 'In Progress']) {
//             ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^< {{query}}
      c.Status = 'Closed';
      lstCasesToBeUpdated.add(c);
    }
    Update lstCasesToBeUpdated;
  }
}

// Noncompliant@+1 {{Add "with sharing", "without sharing" or "inherited sharing"}}
   public class MyClass8 {
//              ^^^^^^^^
  void foo(Task task) {
    update task;
//  ^^^^^^^^^^^^< {{query}}
  }
}

@isTest(SeeAllData=true)
public class MyClass9 {
  void foo(Task task) {
    update task;
  }
}

@IsTesT
public class MyClass10 {
  void foo(Task task) {
    update task;
  }
}

public class MyClass1Child extends MyClass1 { // Compliant, NyClass1 already define sharing rule
  void foo() {
    List<Case> lstCases = new List<Case>();
    for(Case c:[SELECT Id FROM Case WHERE Status = 'In Progress']) {
    }
  }
}

public class MyClass0Child extends MyClass0 { // FN, MyClass0 does not define sharing rules. See SONARSLANG-515.
  void foo() {
    List<Case> lstCases = new List<Case>();
    for(Case c:[SELECT Id FROM Case WHERE Status = 'In Progress']) {
    }
  }
}
