// Positive cases
fun equalSlices(a, b) { // Noncompliant {{Use slices.Equal() instead of custom slice comparison}}
  if (len(a) != len(b)) {
    return false;
  }
  for i := range a {
    if a[i] != b[i] {
      return false
    }
  }
  return true
}

fun equalSlicesWithDifferentVarNames(slice1, slice2) { // Noncompliant {{Use slices.Equal() instead of custom slice comparison}}
  if (len(slice1) != len(slice2)) {
    return false;
  }
  for i := range slice1 {
    if slice1[i] != slice2[i] {
      return false
    }
  }
  return true
}

// Negative cases - should not be flagged
fun notEqualSlicesWrongLenCheck(a, b) {
  if (len(a) == len(b)) { // Wrong operator
    return false;
  }
  for i := range a {
    if a[i] != b[i] {
      return false
    }
  }
  return true
}

fun notEqualSlicesNoLenCheck(a, b) {
  for i := range a {
    if a[i] != b[i] {
      return false
    }
  }
  return true
}

fun notEqualSlicesNoLoop(a, b) {
  if (len(a) != len(b)) {
    return false;
  }
  return true;
}

fun notEqualSlicesWrongReturn(a, b) {
  if (len(a) != len(b)) {
    return true; // Wrong return value
  }
  for i := range a {
    if a[i] != b[i] {
      return true // Wrong return value
    }
  }
  return false
}

fun notEqualSlicesExtraLogic(a, b) {
  println("Checking slices")
  if (len(a) != len(b)) {
    return false;
  }
  for i := range a {
    if a[i] != b[i] {
      return false
    }
  }
  return true
}

fun differentLogic(a, b) {
  if (len(a) > len(b)) {
    return true
  }
  return false
}

fun tooManyParams(a, b, c) {
  return false
}

fun notBoolReturn(a, b) {
  if (len(a) != len(b)) {
    return 1;
  }
  return 0;
}

fun normalFunction(x, y) {
  return x + y;
}
