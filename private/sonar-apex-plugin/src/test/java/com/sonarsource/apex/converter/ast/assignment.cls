class A {
    Integer a;
    void setA(Integer a) {
        this.a = a;
        a = a;
        a += a;
        a -= a;

        x.y.z = 1;
        x().y.z = 2;
        x.y().z = 3;
    }
}
