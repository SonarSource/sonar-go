public with sharing class ClassB { public ClassB() {}
    public ClassB(ApexPages.StandardController controller) {
        String lastName = ApexPages.currentPage().getParameters().get('lastName');
        if (lastName != null) {
            Contact theContact = [SELECT ID FROM Contact WHERE LastName = :lastName LIMIT 1];
            ApexPages.currentPage().getParameters().put('id', theContact.Id);
        }
    }

    public String getAppVersion() {
        return '1.0.0';
    }

    public String doSomething(Integer a) {
        if (a == 1) {
           return '1';
        }
        return '1.0.0';
    }
}
