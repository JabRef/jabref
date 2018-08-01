open module org.jabref.logic {

    exports org.jabref.logic;
    exports org.jabref.logic.auxparser;
    exports org.jabref.logic.bibtex;
    exports org.jabref.logic.bibtex.comparator;
    exports org.jabref.logic.bibtexkeypattern;
    exports org.jabref.logic.bst;
    exports org.jabref.logic.citationstyle;
    exports org.jabref.logic.cleanup;
    exports org.jabref.logic.exporter;
    exports org.jabref.logic.formatter;
    exports org.jabref.logic.formatter.bibtexfields;
    exports org.jabref.logic.formatter.casechanger;
    exports org.jabref.logic.formatter.minifier;
    exports org.jabref.logic.groups;
    exports org.jabref.logic.help;
    exports org.jabref.logic.importer;
    exports org.jabref.logic.importer.fetcher;
    exports org.jabref.logic.importer.fileformat;
    exports org.jabref.logic.importer.fileformat.bibtexml;
    exports org.jabref.logic.importer.fileformat.endnote;
    exports org.jabref.logic.importer.fileformat.medline;
    exports org.jabref.logic.importer.fileformat.mods;
    exports org.jabref.logic.importer.util;
    exports org.jabref.logic.integrity;
    exports org.jabref.logic.journals;
    exports org.jabref.logic.l10n;
    exports org.jabref.logic.layout;
    exports org.jabref.logic.layout.format;
    exports org.jabref.logic.logging;
    exports org.jabref.logic.migrations;
    exports org.jabref.logic.msbib;
    exports org.jabref.logic.net;
    exports org.jabref.logic.openoffice;
    exports org.jabref.logic.pdf;
    exports org.jabref.logic.preferences;
    exports org.jabref.logic.protectedterms;
    exports org.jabref.logic.remote;
    exports org.jabref.logic.remote.client;
    exports org.jabref.logic.remote.server;
    exports org.jabref.logic.remote.shared;
    exports org.jabref.logic.search;
    exports org.jabref.logic.shared;
    exports org.jabref.logic.shared.exception;
    exports org.jabref.logic.shared.event;
    exports org.jabref.logic.shared.listener;
    exports org.jabref.logic.shared.prefs;
    exports org.jabref.logic.shared.security;
    exports org.jabref.logic.specialfields;
    exports org.jabref.logic.undo;
    exports org.jabref.logic.util;
    exports org.jabref.logic.util.io;
    exports org.jabref.logic.util.strings;
    exports org.jabref.logic.xmp;

    requires org.jabref.model;

    // automatic modules

    requires de.jensd.fx.glyphs.commons;

    requires org.slf4j;
    requires org.apache.logging.log4j;

    requires com.google.common;

    requires java.desktop;
    requires java.prefs;
    requires java.sql;
    requires java.xml;
    requires java.xml.bind;

    requires javafx.base;
    requires javafx.graphics;

}
