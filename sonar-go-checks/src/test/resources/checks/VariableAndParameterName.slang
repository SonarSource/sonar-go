var NOT_LOCAL;

fun localVariables() {
  var localVar;
  var INVALID_LOCAL; // Noncompliant {{Rename this local variable to match the regular expression "^(_|[a-zA-Z0-9]+)$".}}
//    ^^^^^^^^^^^^^
  var invalid_local; // Noncompliant
}

fun parameters(param1, _PARAM2, param3) { // Noncompliant {{Rename this parameter to match the regular expression "^(_|[a-zA-Z0-9]+)$".}}
//                     ^^^^^^^
}

native [] {
  [
    var POSSIBLY_NOT_LOCAL;
  ]
};

class A {
  fun constructor(param1, PARAM_2) { // Noncompliant
//                        ^^^^^^^
  }
  fun method(param1, PARAM2_) { // Noncompliant
//                   ^^^^^^^
  }
}

fun method(_) { }

// testing corner case where the identifier syntax is not supported
fun method(__) { }
