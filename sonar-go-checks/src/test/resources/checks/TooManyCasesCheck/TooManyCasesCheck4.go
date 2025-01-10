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
  case 4:
      println("Thursday")
  }


  switch day {
  case 1:
    println("Monday")
  case 2:
    println("Tuesday")
  case 3:
    println("Wednesday")
  default:
    println("Thursday")
  }

  switch day { // Noncompliant {{Reduce the number of switch branches from 5 to at most 4.}}
  case 1:
    println("Monday")
  case 2:
    println("Tuesday")
  case 3:
    println("Wednesday")
  case 4:
    println("Thursday")
  case 5:
    println("Friday")
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
    case 4:
//  ^^^^^^^<
      println("Thursday")
    default:
//  ^^^^^^^^<
      println("Friday")
    }
}
