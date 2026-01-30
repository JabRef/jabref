plugins {
    id("java")
}

tasks.javadoc {
    (options as StandardJavadocDocletOptions).apply {
        encoding = "UTF-8"
        // version = false
        // author = false

        addMultilineStringsOption("tag").setValue(listOf("apiNote", "implNote"))

        // We cross-link to (non-visible) tests; therefore: no reference check
        addBooleanOption("Xdoclint:all,-reference", true)

        addMultilineStringsOption("-add-exports").value = listOf(
            "javafx.controls/com.sun.javafx.scene.control=org.jabref",
            "org.controlsfx.controls/impl.org.controlsfx.skin=org.jabref"
        )

    }
}
