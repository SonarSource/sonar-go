global class HelloWorld {
    public void hello() {
        try {
            System.debug('Try block');
            throw new Exception('Exception');
        } catch(DmlException e) {
            System.debug('An unexpected error has occurred: ' + e.getMessage());
        } catch(Exception e) {
            System.debug('An unexpected error has occurred: ' + e.getMessage());
        } finally {
            System.debug('Finally block');
        }
    }
}
