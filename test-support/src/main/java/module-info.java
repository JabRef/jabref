open module org.jabref.testsupport {
    requires org.junit.jupiter.api;
    requires org.mockito;
    requires org.jabref.jablib;
    requires com.tngtech.archunit;
    requires com.tngtech.archunit.junit5.api;
    requires javafx.graphics;

    exports org.jabref.support;
}
