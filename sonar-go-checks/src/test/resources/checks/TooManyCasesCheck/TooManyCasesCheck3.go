package samples

func main() {
  day:= 4

  switch day {
  case 1:
    println("Monday")
  case 2:
    println("Tuesday")
  case 3:
    println("Wednesday")
  }

  switch day {
  case 1:
    println("Monday")
  case 2:
    println("Tuesday")
  default:
    println("Wednesday")
  }

  switch day { // Noncompliant {{Reduce the number of switch branches from 4 to at most 3.}}
  case 1:
    println("Monday")
  case 2:
    println("Tuesday")
  case 3:
    println("Wednesday")
  case 4:
    println("Thursday")
  }


    switch day { // Noncompliant
//  ^^^^^^
    case 1:
//  ^^^^^^^<
      println("Monday")
    case 2:
//  ^^^^^^^<
      println("Tuesday")
    case 3:
//  ^^^^^^^<
      println("Wednesday")
    default:
//  ^^^^^^^^<
      println("Thursday")
    }
}
