/// Test utilities shared by the test source sets of all modules: architecture tests
/// (ArchUnit), CI-conditional JUnit extensions and test data builders.
///
/// Entry points: [org.jabref.support.CommonArchitectureTest],
/// [org.jabref.support.BibEntryAssert], [org.jabref.support.DisabledOnCIServer].
///
/// @see <a href="https://devdocs.jabref.org/code-howtos/testing.html">Testing code howto</a>
open module org.jabref.testsupport {
    requires transitive com.tngtech.archunit.junit5.api;
    requires transitive com.tngtech.archunit;
    requires transitive org.jabref.jablib;
    requires transitive org.junit.jupiter.api;

    requires org.mockito;
    requires javafx.base;
    requires org.junit.platform.commons;

    requires static com.fasterxml.jackson.annotation;

    exports org.jabref.support;
}
