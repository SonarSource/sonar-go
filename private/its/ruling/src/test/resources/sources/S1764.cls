class Foo {
    public Boolean foo(Integer x, Integer y) {
      if (x == y && x <= y) {
        return x == y || x === y;
      }
      if (x != y || x !== y) {
        return x < y && x > y;
      }
      if (y != 0 || y <> 0) { // Noncompliant
        return true;
      }
      if (x > y && x > y + 1) {
        return true;
      }
      if (x == x) { // Noncompliant
        return true;
      }
      if (x === x) { // Noncompliant, FN for S1764
        return true;
      }
      if (x === x && x == x) { // Noncompliant (can be FP for SObjects) (1 FN for S1764)
        return true;
      }
      Integer a = x + x;
      a = x - x; // Noncompliant
      a = x * x;
      a = x / x; // Noncompliant
      a = x ^ x;
      a = x & x;
      a = x | x;
      a = x << x;
      a = x >> x;
      a = x >>> x;
      x = x;
      return x < y && x < y; // Noncompliant
    }
}
