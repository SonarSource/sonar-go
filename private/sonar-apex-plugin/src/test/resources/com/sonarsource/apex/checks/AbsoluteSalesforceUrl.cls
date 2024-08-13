class Foo {
  void foo() {
    // Noncompliant@+1 {{Make the absolute URL 'na4.salesforce.com' relative.}}
    bar('https://na4.salesforce.com');
    //  ^^^^^^^^^^^^^^^^^^^^^^^^^^^^

    // Noncompliant@+1 {{Make the absolute URL 'ap7.salesforce.com' relative.}}
    bar('https://ap7.salesforce.com/');

    // Noncompliant@+1 {{Make the absolute URL 'eu9.salesforce.com' relative.}}
    bar('https://eu9.salesforce.com/one/one.app');

    // Noncompliant@+1 {{Make the absolute URL 'cs12.salesforce.com' relative.}}
    bar('https://cs12.salesforce.com/003i000000im5km');

    // Noncompliant@+1 {{Make the absolute URL 'cs3.visual.force.com' relative.}}
    bar('https://c.cs3.visual.force.com/');

    // Noncompliant@+1 {{Make the absolute URL 'test1.force.com' relative.}}
    bar('https://test1.force.com/');

    // Noncompliant@+1 {{Make the absolute URL 'test2.force.com' relative.}}
    bar('https://test2.force.com/');

    // Noncompliant@+1 {{Make the absolute URL 'login.salesforce.com' relative.}}
    bar('https://login.salesforce.com/');

    // Noncompliant@+1 {{Make the absolute URL 'test.salesforce.com' relative.}}
    bar('https://test.salesforce.com/');

    // Noncompliant@+1 {{Make the absolute URL 'na50.content.force.com' relative.}}
    bar('https://my-domain-dev-ed--c.na50.content.force.com/');

    // Noncompliant@+1 {{Make the absolute URL 'dns03.salesforce.com' relative.}}
    bar('dns03.salesforce.com');

    // Noncompliant@+1 {{Make the absolute URL 'ns2.salesforce.com' relative.}}
    bar('ns2.salesforce.com');

    bar('Xdns03.salesforce.com'); // Compliant
    bar('dns03.salesforce.comX'); // Compliant
    bar('dns03_salesforce_com');  // Compliant

    bar('https://www.salesforce.com');       // Compliant
    bar('https://downloads.salesforce.com'); // Compliant
    bar('https://www1.salesforce.com');      // Compliant
    bar('https://help.salesforce.com/articleView?id=2'); // Compliant
    bar('https://redhat.force.com');         // Compliant
    bar('https://cisco.force.com');          // Compliant
    bar('');                                 // Compliant
  }
  void bar(string id) {
  }
}
