class A {
    public List<List<SObject>> find(String pattern, Integer accountLimit, String divisionName, Integer returningLimit) {
        return [FIND '1234' IN ALL FIELDS
                RETURNING
                Account (Id, Name WHERE Name LIKE :pattern LIMIT :accountLimit),
                Contact,
                Opportunity,
                Lead
                WITH DIVISION =:divisionName LIMIT :returningLimit];
    }
}
