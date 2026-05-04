module org.jabref.jabkit {
    requires org.jabref.jablib;

    requires info.picocli;
    opens org.jabref.toolkit.commands;
    opens org.jabref.toolkit.converter;
    opens org.jabref.toolkit;

    requires transitive org.jspecify;
    requires java.prefs;

    requires javafx.base;
    requires afterburner.fx;

    requires org.slf4j;
    requires jul.to.slf4j;
    requires /*runtime*/ org.apache.logging.log4j.to.slf4j;
    requires org.tinylog.api;
    requires /*runtime*/ org.tinylog.api.slf4j;
    requires /*runtime*/ org.tinylog.impl;

    requires java.xml;

    // region: other libraries (alphabetically)
    requires static io.github.eadr;
    // endregion
}
