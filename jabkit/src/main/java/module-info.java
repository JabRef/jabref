module org.jabref.jabkit {
    requires org.jabref.jablib;

    requires info.picocli;
    opens org.jabref.toolkit.commands;
    opens org.jabref.toolkit.converter;
    opens org.jabref.toolkit;

    requires transitive org.jspecify;
    requires java.prefs;

    requires com.google.common;

    requires org.apache.lucene.queryparser;

    requires javafx.base;
    requires afterburner.fx;

    requires org.slf4j;
    requires jul.to.slf4j;
    requires org.apache.logging.log4j.to.slf4j;
    requires org.tinylog.api;
    requires org.tinylog.api.slf4j;
    requires org.tinylog.impl;

    requires java.xml;
    // region: other libraries (alphabetically)
    requires io.github.adr;
    // endregion
}
