public class Foo {

    static void MyFooMethod() {
        if (true) {      // NOSONAR
        }
        if (true) {      // raise an issue S1145
        }
    }
}
