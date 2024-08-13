class A {
    void foo() {
        Booblean x;
        x = !true;
        x = !false;
        x = false && foo;
        x = x || true;
        x = x || ((true));
    }
}
