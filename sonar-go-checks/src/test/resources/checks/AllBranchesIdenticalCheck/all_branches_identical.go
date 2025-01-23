package AllBranchesIdenticalCheck

func main() {
    if x { foo; }
    if x { foo; } else { bar; }
    if x { foo; } else { foo; } // Noncompliant
  //^^^^^^^^^^^^^^^^^^^^^^^^^^^

    if x { foo; } else if (y) { foo; }
    if x { foo; } else if (y) { foo; } else { bar; }
    if x { foo; } else if (y) { foo; } else { foo; } // Noncompliant
  //^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

    switch x { }
    switch x { case 1: a; }
    switch x { case 1: a; default: b; }
    switch x { case 1: a; default: a; } // Noncompliant
  //^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    switch x { case 1: a; case 2: a; default: b; }
    switch x { case 1: a; case 2: a; default: a; } // Noncompliant
    switch x { default: b; } // Compliant: only default case
}
