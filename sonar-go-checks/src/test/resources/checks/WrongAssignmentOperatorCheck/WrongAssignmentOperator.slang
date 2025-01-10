target =--num;
target =++num;

a = b =! c; // Noncompliant {{Was "!=" meant instead?}} [[sc=11;ec=13]]
a = b =!! c; // Noncompliant
a = b = !c;
