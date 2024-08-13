global class HelloWorld {
    public void hello() {
        for (Integer i = 0, j = 0; i < 10; i++) {
          System.debug(i);
        }

        Integer j = 0;
        do {
            System.debug(j);
            j++;
            break;
        } while(j < 10);

        Integer k = 0;
        while(k < 10) {
            System.debug(k);
            k ++;
            continue;
        }

        while(k < 10);

        for (Integer i = 0, j = 0; i < 10; i++);
    }
}
