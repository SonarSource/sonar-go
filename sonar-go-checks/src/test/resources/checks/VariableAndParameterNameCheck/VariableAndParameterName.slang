fun localVariables() {
  var INVALID_LOCAL; // Noncompliant
}

native [] {
  [
    var POSSIBLY_NOT_LOCAL;
  ]
};

// testing corner case where the identifier syntax is not supported
fun method(__) { }
