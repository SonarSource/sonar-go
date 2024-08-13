class Foo {
    public Integer foo(Integer x, Integer y) {
      if (++x == x++) {
        return +x;
      }
      if (--x == x--) {
        return -x;
      }
      return !(x == y);
    }
}
