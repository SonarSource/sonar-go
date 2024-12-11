sonarqube {
    properties {
        property("sonar.sources", ".")
        property("sonar.inclusions", "**/*.go")
        property("sonar.exclusions", "**/render.go,**/generate_source.go,**/*_generated.go,**/build/**,**/vendor/**,**/.gogradle/**")
        property("sonar.tests", ".")
        property("sonar.test.inclusions", "**/*_test.go")
        property("sonar.test.exclusions", "**/build/**,**/vendor/**,**/.gogradle/**")
        property("sonar.go.tests.reportPaths", "${project.projectDir}/.gogradle/reports/test-report.out")
        property("sonar.go.coverage.reportPaths", "${project.projectDir}/.gogradle/reports/coverage/profiles/github.com%2FSonarSource%2Fslang%2Fsonar-go-to-slang.out")
    }
}

val generateParserAndBuild = tasks.register<Exec>("generateParserAndBuild") {
    group = "build"
    description = "Generate Go parser and build the Go executable"

    commandLine("./make.sh")
    args("build")
}

val generateTestReport = tasks.register<Exec>("generateTestReport") {
    group = "verification"
    description = "Generate Go test report"

    commandLine("./make.sh")
    args("generate-test-report")
}

val cleanTask = tasks.register<Exec>("cleanTask") {
    group = "build"
    description = "Clean the Go build"

    commandLine("./make.sh")
    args("clean")
}

tasks.clean {
    dependsOn(cleanTask)
}
generateTestReport {
    dependsOn(generateParserAndBuild)
}
tasks.build {
    dependsOn(generateTestReport)
}
