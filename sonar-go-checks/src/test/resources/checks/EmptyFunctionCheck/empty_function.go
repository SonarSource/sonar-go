package EmptyFunctionCheck

func noBody()

func notEmpty() {
	bar()
}

// Noncompliant@+1
func empty() {}
//           ^^

func containingOnlyAComment() { /* comment */ }

func emptyWithEndOfLineComment1() {} // end of line comment

func emptyWithEndOfLineComment2() {} /* comment */

func emptyWithEndOfLineCommentOnMultipleLine() {} /* comment
 */

func emptyOnSeveralLine() {
} // comment

// Noncompliant@+1
func empty() {
}
