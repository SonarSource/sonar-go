package BooleanLiteralCheck

func main() {
    x := true;
    y := true;
    z := true;
    foo := true;

    if x == true {}                                   // OK - as for now without semantic we do not know if x is nullable or a primitive
    if x == false {}                                  // OK - as for now without semantic we do not know if x is nullable or a primitive
    if x != true {}                                   // OK - as for now without semantic we do not know if x is nullable or a primitive
    if x != false {}                                  // OK - as for now without semantic we do not know if x is nullable or a primitive
    if true == x {}                                   // OK - as for now without semantic we do not know if x is nullable or a primitive
    if false == x {}                                  // OK - as for now without semantic we do not know if x is nullable or a primitive
    if true != x {}                                   // OK - as for now without semantic we do not know if x is nullable or a primitive
    if false != x {}                                  // OK - as for now without semantic we do not know if x is nullable or a primitive
    if !true {}                                       // Noncompliant {{Remove the unnecessary Boolean literal.}}
    if !false {}                                      // Noncompliant {{Remove the unnecessary Boolean literal.}}
    if false && foo {}                                // Noncompliant {{Remove the unnecessary Boolean literal.}}
    if x || true {}                                   // Noncompliant {{Remove the unnecessary Boolean literal.}}
    if x || ((true)) {}                               // Noncompliant {{Remove the unnecessary Boolean literal.}}

    if !x  {}                                         // OK
    if x || foo {}                                    // OK
    if x == y {}                                      // OK
    if z != x {}                                      // OK
}
