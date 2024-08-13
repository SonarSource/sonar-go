trigger trigger2 on Object1__c (before delete, after insert, after update) {
    Trigger.old[0];
    Trigger.oldmap.get(2);
    System.assertEquals('xxx', Trigger.old.accountNumber());
}
