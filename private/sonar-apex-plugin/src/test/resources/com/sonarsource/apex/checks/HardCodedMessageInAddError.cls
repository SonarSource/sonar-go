class Foo {
  void foo(Case mycase) {
    sObject.addError('This is a hardcoded error'); // Noncompliant {{Replace this hardcoded message with a Label.}}
    //               ^^^^^^^^^^^^^^^^^^^^^^^^^^^
    mycase.addError('This is a hardcoded error');  // Noncompliant
    //              ^^^^^^^^^^^^^^^^^^^^^^^^^^^
    mycase.AddError('This is a hardcoded error');  // Noncompliant
    //              ^^^^^^^^^^^^^^^^^^^^^^^^^^^
    mycase.addError('This is a hardcoded error', true);  // Noncompliant
    //              ^^^^^^^^^^^^^^^^^^^^^^^^^^^

    mycase.addError(true, 'This is a hardcoded error');  // Compliant, not SObject method signature
    mycase.addError('This is a hardcoded error', true, 'other message');  // Compliant, not SObject method signature
    mycase.addError(System.Label.caseErrorMessage); // Compliant
    mycase.addError(i); // Compliant
    addError('This is a hardcoded error');  // Compliant

    Bar b = new Bar();
    b.getCase().addError('This is a hardcoded error'); // Noncompliant
    //                   ^^^^^^^^^^^^^^^^^^^^^^^^^^^
    b.case.addError('This is a hardcoded error'); // Noncompliant
    //              ^^^^^^^^^^^^^^^^^^^^^^^^^^^

    String s = 'This is a hardcoded error';
    mycase.addError(s); // Compliant
  }

  void addError(String s) {
  }
}

class Bar {
    Case case;

    Case getCase() {
        return case;
    }
}
