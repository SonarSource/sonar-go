class A {
  Integer size;
  Integer i = 0, j, k = 1;
  String name = 'abc';
  String a, b, c;
  Integer y, z = 12;
  final String finalVariable;
  static final Double staticFinalVariables1 = 1.0, staticFinalVariables2;

  void doSomething(String param) {
    Integer size;
    Integer i = 0, j, k = 1;
    String name = 'abc';
    String a, b, c;
    Integer y, z = 12;
    final String finalVariable;
    static final Double staticFinalVariables1 = 1.0, staticFinalVariables2;
  }

  void doSomethingElse() {
    Integer orgAcctId = 123;
    Integer countAccts = [SELECT Count() FROM Account WHERE Id != :orgAcctId];
  }

}
