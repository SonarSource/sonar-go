package HardcodedCredentialsCheck

const x = "pass"
func foo() string { return "pass" }
const x = "password"
func foo () string { return "password" }
const x = "login=a&password="
func foo() string { return "login=a&password=" }
func foo(value string) string { return "login=a&password= " + value }
func foo() string { return "login=a&password=a" } // Noncompliant
const x = "login=a&password=xxx" // Noncompliant {{"password" detected here, make sure this is not a hard-coded credential.}}
//        ^^^^^^^^^^^^^^^^^^^^^^
func foo() string { return "login=a&password=xxx" } // Noncompliant
func foo() string { return "login=a&passwd=xxx" } // Noncompliant {{"passwd" detected here, make sure this is not a hard-coded credential.}}
func foo() string { return "login=a&pwd=xxx" } // Noncompliant {{"pwd" detected here, make sure this is not a hard-coded credential.}}
func foo() string { return "login=a&passphrase=xxx" } // Noncompliant {{"passphrase" detected here, make sure this is not a hard-coded credential.}}
const variableNameWithPasswordInIt = "xxx" // Noncompliant {{"Password" detected here, make sure this is not a hard-coded credential.}}
//    ^^^^^^^^^^^^^^^^^^^^^^^^^^^^
const variableNameWithPasswdInIt = "xxx" // Noncompliant
//variableNameWithPasswdInIt += "xxx" // Noncompliant
const variableNameWithPwdInIt = "xxx"  // Noncompliant {{"Pwd" detected here, make sure this is not a hard-coded credential.}}
func foo(A A) {
	A.variableNameWithPwdInIt = "xxx" // False negative
	A.B.variableNameWithPwdInIt = "xxx" // False negative
	A.B.variableNameWithPwdInIt = ""
	var variableNameWithPasswdInIt string
	variableNameWithPasswdInIt = ""
}
const otherVariableNameWithPasswordInIt
const constValue = "login=a&password=xxx" // Noncompliant
var passwd = "xxxx" // Noncompliant
var passphrase = "xxx" // Noncompliant
var okVariable = "xxxx"
var passwd = ""
var passwd = 2

// No issue is raised when the matched wordlist item is present in both symbol name and literal string value.
var password = "password" // Compliant
var myPassword = "users/connection.secretPassword" // Compliant
const myPassword = "users/connection.secretPassword" // Compliant
var myPassword = "secretPasswd" // Noncompliant {{"Password" detected here, make sure this is not a hard-coded credential.}}
const myPassword = "secretPasswd" // Noncompliant {{"Password" detected here, make sure this is not a hard-coded credential.}}
var params = "user=admin&password=Password123" // Noncompliant {{"password" detected here, make sure this is not a hard-coded credential.}}

// Database queries are compliant
var query = "password=?"
var query = "password=:password"
var query = "password=:param"
var query = "password='" + password + "'"
var query = "password=%s"
var query = "password=%v"

// String format is compliant
var query = "password={0}"

// Support URI
var uri = "http://user:azer:ty123@domain.com" // Noncompliant
var uri = "https://:azerty123@domain.com/path" // Noncompliant {{Review this hard-coded URL, which may contain a credential.}}
var uri = "http://anonymous:anonymous@domain.com" // Compliant, user and password are the same
var uri = "http://user:@domain.com"
var uri = "http://user@domain.com:80"
var uri = "http://domain.com/user:azerty123"
var uri = "too-long-url-scheme://user:123456@server.com"
var uri = "https:// invalid::url::format"
