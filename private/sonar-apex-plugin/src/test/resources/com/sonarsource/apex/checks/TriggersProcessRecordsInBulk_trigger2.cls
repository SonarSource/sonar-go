trigger CaseTrigger2 on Case (before insert, before update) {
    for(Case c: Trigger.new) {
        // do something
    }

    for (Integer i = 0; i < Trigger.old.size(); i++) {
        Case oldVal = Trigger.old[i];
        Case newVal = Trigger.new[i];
    }
 }
