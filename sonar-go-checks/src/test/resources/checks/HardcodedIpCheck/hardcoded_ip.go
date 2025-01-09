package HardcodedIpCheck

const x = 120
func foo() { return "120" }
var ip = "1.2.3.4" // Noncompliant {{Make sure using this hardcoded IP address is safe here.}}
//       ^^^^^^^^^
func foo() { return "1.2.3.4" } // Noncompliant
func foo() { return "1.2.3.4:80" } // Noncompliant
func foo() { return "1.2.3.4:8080" } // Noncompliant
func foo() { return "1.2.3.4:a" }
func foo() { return "1.2.3.4.5" }

// Noncompliant@+1 {{Make sure using this hardcoded IP address is safe here.}}
const url = "http://192.168.0.1/admin.html"
// Noncompliant@+1
const url = "http://192.168.0.1:8181/admin.html"
const url2 = "http://www.example.org"

var notAnIp1 = "0.0.0.1234"
var notAnIp2 = "1234.0.0.0"
var notAnIp3 = "1234.0.0.0.0.1234"
var notAnIp4 = ".0.0.0.0"
var notAnIp5 = "0.256.0.0"

var ip = "0.00.0.0" // Compliant
var ip = "1.2.03.4" // Compliant

var fileName = "v0.0.1.200__do_something.sql" // Compliant - suffixed and prefixed
var version = "1.0.0.0-1" // Compliant - suffixed

const ip = "1080:0:0:0:8:800:200C:417A" // Noncompliant {{Make sure using this hardcoded IP address is safe here.}}
const ip = "[1080::8:800:200C:417A]" // Noncompliant
const ip = "::800:200C:417A" // Noncompliant
const ip = "1080:800:200C::" // Noncompliant
const ip = "::FFFF:129.144.52.38" // Noncompliant
const ip = "::129.144.52.38" // Noncompliant
const ip = "::FFFF:38" // Noncompliant
const ip = "::100" // Noncompliant
const ip = "1080:0:0:0:8:200C:131.107.129.8" // Noncompliant
const ip = "1080:0:0::8:200C:131.107.129.8" // Noncompliant

const ip = "1080:0:0:0:8:800:200C:417G" // Compliant - not valid IPv6
const ip = "1080:0:0:0:8::800:200C:417A" // Compliant - not valid IPv6
const ip = "1080:0:0:0:8:::200C:417A" // Compliant - not valid IPv6
const ip = "1080:0:0:0:8" // Compliant - not valid IPv6
const ip = "1080:0::0:0:8::200C:417A" // Compliant - not valid IPv6
const ip = "1080:0:0:0:8::200C:417A:" // Compliant - not valid IPv6
const ip = "1080:0:0:0:8::200C:131.107.129.8" // Compliant - not valid IPv6
const ip = "1080:0:0:0:8::200C:256.256.129.8" // Compliant - not valid IPv6
const ip = "1080:0:0:0:8:200C:200C:131.107.129.8" // Compliant - not valid IPv6
const ip = "1080:0:0:0:8:131.107.129.8" // Compliant - not valid IPv6
const ip = "1080:0::0::8:200C:131.107.129.8" // Compliant - not valid IPv6
const ip = "1080:0:0:0:8:200C:131.107.129" // Compliant - not valid IPv6
const ip = "1080:0:0:0:8:200C:417A:131.107" // Compliant - not valid IPv6

// Noncompliant@+1 {{Make sure using this hardcoded IP address is safe here.}}
const ip = "http://[2002:db8:1f70::999:de8:7648:6e8]"
// Noncompliant@+1
const ip = "http://[2002:db8:1f70::999:de8:7648:6e8]:100/"
// Noncompliant@+1
const ip = "https://[3FFE:1A05:510:1111:0:5EFE:131.107.129.8]:8080/"
//         ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

// Noncompliant@+1
const ip = "https://[3FFE::1111:0:5EFE:131.107.129.8]:8080/"

const ip = "ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff" // Noncompliant

// Exceptions
const ip = "0.0.0.0"
const ip = "::1"
const ip = "000:00::1"
const ip = "255.255.255.255"
const ip = "255.255.255.255:80"
const ip = "2.5.255.255"
const ip = "127.5.255.255"
const ip = "http://[::0]:100/"
const ip = "0000:0000:0000:0000:0000:0000:0000:0000"
const ip = "192.0.2.0"
const ip = "198.51.100.0"
const ip = "203.0.113.0"
const ip = "2001:db8:3:4:5:6:7:8"
const ip = "::ffff:0:127.0.0.1"
const ip = "::ffff:0:127.100.150.200"
const ip = "::ffff:0:127.255.255.255"
const ip = "::ffff:127.0.0.1"
const ip = "::ffff:127.100.150.200"
const ip = "::ffff:127.255.255.255"
