class Foo {
    public Integer foo(Integer x, Integer y) {
      if (x == y) {
        return x + y;
      }
      if (x < y) {
        return x * y;
      }
      if (y != 0 || y <> 0) {
        return x / y;
      }
      if (x > y && x !== y) {
        return x - 1;
      }
      if (x & y) {
        return x ^ y;
      }
      if (x = y) {
        x /= y;
        return x;
      }
      return x << y;
    }
}
