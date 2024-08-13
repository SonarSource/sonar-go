class TriggerHandlerClass {

    static void triggerHandlerMethod1() {
       TRIGGER.old[0]; // Noncompliant
       Trigger.NEW[1]; // Noncompliant
       Trigger.olDmap.geT(2); // Noncompliant
       TriggeR.newMap.Get(3); // Noncompliant

       Trigger.OlD[x]; // Ok - not an integer literal
       TRIGGER.New[x]; // Ok - not an integer literal
       TriGGer.oLdmap.gEt(x); // Ok - not an integer literal
       TrigGer.nEwmap.geT(x); // Ok - not an integer literal
    }

    static void triggerHandlerMethod2() {
        for(MyClass m : Trigger.new) {
	        MyClass oldStuff = Trigger.oldMap.get(m.ID);
	        MyClass newStuff = Trigger.newMap.get(m.ID);
	        string oldValue = oldStuff.MyValue;
	        string newValue = newStuff.MyValue;
	    }
    }

    static void triggerHandlerMethod3() {
        Trigger.old[2]; // Noncompliant
        abc(2);
        Trigger.other[1];
        Trigger.other[1][2];
        Trigger.get(1);
        Trigger.map.get(1);
        Trigger.oldmap.get(1, 2);
        Trigger.oldmap.gett(2);
        Trigger.2(2);
        Trigger.oldmap(2);
        Trigger.newmap.myFunc(3);
    }

}
