trigger ComplexTrigger on Account (before insert) { //  Noncompliant {{Move the business logic to a separate trigger handler class}}
//      ^^^^^^^^^^^^^^
  switch on getCode() {
//^^^^^^< {{business logic}}
      when 1 {
          // code block 1
      }
      when 2 {
          // code block 2
      }
      when else {
          // code block else
      }
  }
}
