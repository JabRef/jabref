module org.jabref.jabkit {
    requires org.jabref.jablib;

    requires org.apache.commons.cli;

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
}
