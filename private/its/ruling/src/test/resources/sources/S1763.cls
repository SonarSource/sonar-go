/*
    Copyright
*/
class Foo {
    Integer foo(Integer a) {
        Integer i = 10;
        return i + a;
        i++; // dead code
    }

    Integer foo2(Integer a) {
        while(a < 10) {
            if(a == 5) {
                continue;
                a++; // dead code
            }
            if(a == 9) {
                break;
                a++; // dead code
            }
        }
        throw new Exception('Exception');
        a++; // dead code
    }
}
