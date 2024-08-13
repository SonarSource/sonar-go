class Foo {
  void foo() {
    Integer i = 0;
    while (i < 2) {
//  ^^^^^>
      // Noncompliant@+1 {{Move this DML statement out of the loop or process sObjects by batch.}}
      update task;
//    ^^^^^^^^^^^^
      i++;
    }

    for( Profile p : [ Select Id, PermissionsModifyAllData, Name from Profile limit 100 ] ){ // Compliant
      bar(p);
    }

    for (Integer i = 0; i < 2; i++) {
      for(Integer i = 0, j = 0; i < 10; i++){
//    ^^^>
        // Noncompliant@+1 {{Move this DML statement out of the loop or process sObjects by batch.}}
        delete task;
//      ^^^^^^^^^^^^
      }
    }

    for (Task task: [Select Id, subject from Task]) {
//  ^^^>
       if (task.subject == 'foo') {
          task.subject = 'bar';
          update task; // Noncompliant
//        ^^^^^^^^^^^^
          update task; // Rule only raises one issue on the first query of a loop
       }
    }

    for(Integer i = 0, j = 0; i < 10; i++){
        //Compliant, only DML queries are reported
        List<List<SObject>> searchList = [FIND 'SFDC' IN ALL FIELDS RETURNING Account(Name), Contact(FirstName,LastName)];
    }
  }

  void bar(Profile p) {

  }
}
