trigger ComplexTrigger on Account (before insert) { //  Noncompliant {{Move the business logic to a separate trigger handler class}}
//      ^^^^^^^^^^^^^^
  try {
//^^^< {{business logic}}
  } catch(DmlException e) {
    System.debug('The following exception has occurred: ' + e.getMessage());
  }
}
