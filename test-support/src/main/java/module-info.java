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
