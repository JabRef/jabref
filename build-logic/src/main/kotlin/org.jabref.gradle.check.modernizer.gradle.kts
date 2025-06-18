plugins {
    id("com.github.andygoossens.modernizer")
}

modernizer {
    failOnViolations = true
    includeTestClasses = true
    exclusions = setOf(
        "java/util/Optional.get:()Ljava/lang/Object;"
    )
}
