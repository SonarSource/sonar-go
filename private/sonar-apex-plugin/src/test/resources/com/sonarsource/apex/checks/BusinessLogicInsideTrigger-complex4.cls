trigger ComplexTrigger on Account (before insert) { //  Noncompliant {{Move the business logic to a separate trigger handler class}}
//      ^^^^^^^^^^^^^^
  Merchandise__c m = new Merchandise__c();
  insert m;
//^^^^^^< {{business logic}}
}
