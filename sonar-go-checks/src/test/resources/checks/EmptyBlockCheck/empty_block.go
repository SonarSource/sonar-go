package EmptyBlockCheck

func a() {}

func b() {
  // comment
}

func c() {
  // comment
}

func d() {
  // Noncompliant@+1
  if x > 0 { }
}

func e(int x) {
  // Noncompliant@+1
  if x > 0 { }

  if x > 1 {
    // comment
  }

  switch (x) {
  // comment
  }

  // Noncompliant@+1
  switch (x) {

  }

  switch (x) {
    case 1:
    // Noncompliant@+1
        { }
    case 2:
        x;
    default:
        { x; }
  };

  for (cond) {} // Compliant - exception to the rule
}
