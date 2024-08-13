trigger ComplexTrigger on Account (before insert) { //  Noncompliant {{Move the business logic to a separate trigger handler class}}
//      ^^^^^^^^^^^^^^
  Set<Id> masterIds = getMasterIds();
  Map<Id, Sobject> masterRecordsMap = new Map<Id, Sobject>();
  for (Id mId : masterIds) {
//^^^< {{business logic}}
    masterRecordsMap.put(mId, ctx.master.newSobject(mId));
  }
}
