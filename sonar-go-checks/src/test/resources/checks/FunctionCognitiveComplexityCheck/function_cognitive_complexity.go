package FunctionCognitiveComplexityCheck

func ok() {
    if x {
        if y {
            foo()
        }
    }
    if z {
        foo()
    }
}

func ko() { // Noncompliant {{Refactor this method to reduce its Cognitive Complexity from 5 to the 4 allowed.}} [[effortToFix=1]]
//   ^^
    if x {
//  ^^< {{+1}}
        if y {
//      ^^< {{+2 (incl 1 for nesting)}}
            foo()
        }
        if z {
//      ^^< {{+2 (incl 1 for nesting)}}
            foo()
        }
    }
}

func logical_operators() { // Noncompliant
//   ^^^^^^^^^^^^^^^^^
    if a &&
//  ^^<
//       ^^@-1<
        b && c ||
//             ^^<
        d || e &&
//             ^^<
        f ||
//        ^^<
        g {
        foo()
    }
}

func nesting_anonymous() { // Noncompliant
    func() {
        a && b || c && d || e && f
    }
}
