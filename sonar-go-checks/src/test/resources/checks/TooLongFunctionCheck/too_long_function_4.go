package TooLongfuncctionCheck

func b() {}

func g1() { // Noncompliant {{This function has 5 lines of code, which is greater than the 4 authorized. Split it into smaller functions.}}
  b()
  b()
  b()
}

func g2() {
  b()

  b()
}
