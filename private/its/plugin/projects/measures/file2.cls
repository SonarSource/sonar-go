public class Foo {
    public Integer foo;

    static void MyFooMethod() {
        switch on i {
            when 1 {
              System.debug('block 1');
            }
            when else {
              System.debug('block 2');
            }
        }

        if(foo == 42) {
            System.debug('if');
        } else {
            System.debug('else');
        }

        for (Integer i = 0, j = 0; i < 10; i++) {
            System.debug(i+1);
        }
    }
}
