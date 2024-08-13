class A extends B {
  public void method1() {}

  private void method2() {}

  override void method3() {}

  protected void method4() {}

  @isTest static void myTest1() {}

  @IsTest(SeeAllData=true) static void myTest2() {}

  static testMethod void myTest3() {}

  @IsTest(SeeAllData=true) private static void myTest4() {}

  private static testMethod void myTest5() {}

  private static TESTMETHOD void myTest6() {}

  @TestVisible private void myTest7() {}

  @TestSetup private static void myTest8() {}

  @AnotherAnnotation private static void myTest9() {}

  private static void privateStaticMethod() {}
}
