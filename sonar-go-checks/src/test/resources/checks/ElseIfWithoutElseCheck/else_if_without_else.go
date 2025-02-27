package ElseIfWithoutElseCheck

func doSomething() {
}
func doSomethingElse() {
}

func main() {
    x := 5;

    if x == 0 {
      x = 42;
    }

    if x == 0 {
      x = 42;
    } else {
      x = 43;
    }

    if x == 0 {
      doSomething();
    } else if x == 1 { // Noncompliant {{Add the missing "else" clause.}}
    //^^^^^^^
      doSomethingElse();
    }

    if x == 0 {
      doSomething();
    } else if x == 1 {
      doSomethingElse();
    } else {
      print("Something");
    }

    if x == 0 {
      doSomething();
    } else if x == 1 {
      doSomethingElse();
    } else if x == 2 { // Noncompliant {{Add the missing "else" clause.}}
      print("Something");
    }

    for i := 0; i < 10; i++ {
        if x == 0 {
            break;
        } else if x == 1 {
            return;
        } else if x == 3 {
            panic(1);
        }

        if x == 0 {
            break;
        } else if x == 1 { // Noncompliant {{Add the missing "else" clause.}}
           break;
           doSomething();
        }

        if x == 0 {
            break;
        } else if x == 1 { // Noncompliant {{Add the missing "else" clause.}}
           return;
           doSomething();
        }

        if x == 0 {
            break;
        } else if x == 1 { // Noncompliant {{Add the missing "else" clause.}}
           panic(1);
           doSomething();
        }

        // extra use cases - invalid go code, to ensure we don't crash
        if x == 0 {
            break;
        } else if x == 1 {
            panic;
        } else if x == 2 {
            s := "test";
        } else if x == 3 { // Noncompliant {{Add the missing "else" clause.}}
            panic.other;
        }

        if x == 0 {
            break;
        } else if x == 1 { // Noncompliant {{Add the missing "else" clause.}}
        //^^^^^^^
            doSomething();
        }
    }

    if x == 0 {
    } else if x == 1 { // Noncompliant {{Add the missing "else" clause.}}
        return;
    }

    if x >= 0 {
        if x > 1 {
            return
        } else if x == 1 {
            doSomething()
        } else if x == 0 { // Noncompliant {{Add the missing "else" clause.}}
            doSomethingElse()
        }
    }
}
