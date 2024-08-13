trigger CaseTrigger1 on Case (before insert, before update) {
    Trigger.old[0]; // Noncompliant
    Trigger.new[1]; // Noncompliant
    Trigger.oldmap.get(2); // Noncompliant
    Trigger.newmap.get(3); // Noncompliant

    Trigger.old[x]; // Ok - not an integer literal
    Trigger.new[x]; // Ok - not an integer literal
    Trigger.oldmap.get(x); // Ok - not an integer literal
    Trigger.newmap.get(x); // Ok - not an integer literal
}
