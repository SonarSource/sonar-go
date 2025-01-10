package SelfAssignment

func main() {
  x := 1
  x = x + 1
  x += x
  x = x // Noncompliant {{Remove or correct this useless self-assignment.}}
//^^^^^
}

func (user *User) rename1(name string) {
  name = name // Noncompliant
}

func (user *User) rename2(name string) {
  user.name = name
}

type User struct {
  name string
}
