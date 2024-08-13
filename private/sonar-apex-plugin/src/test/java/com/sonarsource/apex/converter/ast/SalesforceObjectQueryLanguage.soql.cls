class A {
    public List<Account> accounts(String city) {
        return [SELECT ID FROM Account WHERE city = :city AND DISTANCE(location, GEOLOCATION(10, 10), 'km') < 10 ORDER BY ID];
    }
}
