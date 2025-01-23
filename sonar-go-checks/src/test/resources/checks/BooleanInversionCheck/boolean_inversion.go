package BooleanInversionCheck

func main() {
    a := 2;
    i := 5

    if !(a == 2) { };  // Noncompliant {{Use the opposite operator ("!=") instead.}}
    // ^^^^^^^^^
    if !(i < 10) { };  // Noncompliant {{Use the opposite operator (">=") instead.}}
    if !(i > 10) { };  // Noncompliant {{Use the opposite operator ("<=") instead.}}
    if !(i != 10) { };  // Noncompliant {{Use the opposite operator ("==") instead.}}
    if !(i <= 10) { };  // Noncompliant {{Use the opposite operator (">") instead.}}
    if !(i >= 10) { };  // Noncompliant {{Use the opposite operator ("<") instead.}}

    if a != 2 { };
    if (i >= 10) { };
}
