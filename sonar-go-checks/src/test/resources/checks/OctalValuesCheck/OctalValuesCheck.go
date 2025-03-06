package OctalValuesCheck

func main() {
    17000

    a := 02522 // Noncompliant {{Use decimal values instead of octal ones.}}
    //   ^^^^^
    0o2522 // Noncompliant
    0o2522 // Noncompliant

    "0o2522"
    "02522"
    "0O2522"

    a + 022 // Noncompliant
    //  ^^^

    02    // Compliant - part of exceptions
    0077  // Compliant - part of exceptions
    0o077 // Compliant - part of exceptions

    // Numbers with underscores, see: https://go.dev/ref/spec#Integer_literals
    0_123
    0123_456 // Noncompliant
    1_234_567
    0x_0
    0x_1234
    0x_CAFE_f00d
    0o_0
    0o0123_4567 // Noncompliant
    0b_0
    0b_0010_1101
    0_0e0
    1_2_3e0
    0123e1_2_3
    123.e-1_0
    0xdead_cafep+1
    0x_1234p-10
    0x12_34.p1_2_3
    1_234_567i

    // Numbers with dot at the end
    0.
    123.

    // Numbers with dot at the beginning
    .0e-1
    .123e+10
    .0123e123

    // hexadecimal floats
    0x0.p+0
    0xdeadcafe.p-10
    0x1234.p84
    0x.1p-0
    0x.deadcafep4
    0x.1234p+12
    0xdeadcafep+1
    0x0.0p0
}
