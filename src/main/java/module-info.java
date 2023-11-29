open module org.jabref {
    // Swing
    requires java.desktop;

    // SQL
    requires java.sql;
    requires java.sql.rowset;

    // JavaFX
    requires javafx.base;
    requires javafx.graphics;
    requires javafx.controls;
    requires javafx.web;
    requires javafx.fxml;
    requires afterburner.fx;
    requires com.dlsc.gemsfx;
    uses com.dlsc.gemsfx.TagsField;
    requires de.saxsys.mvvmfx;
    requires reactfx;
    requires org.fxmisc.flowless;

    requires org.kordamp.ikonli.core;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.materialdesign2;
    uses org.kordamp.ikonli.IkonHandler;
    uses org.kordamp.ikonli.IkonProvider;

    provides org.kordamp.ikonli.IkonHandler
            with org.jabref.gui.icon.JabRefIkonHandler;
    provides org.kordamp.ikonli.IkonProvider
            with org.jabref.gui.icon.JabrefIconProvider;

    requires org.controlsfx.controls;
    requires org.fxmisc.richtext;
    requires com.tobiasdiez.easybind;

    provides com.airhacks.afterburner.views.ResourceLocator
            with org.jabref.gui.util.JabRefResourceLocator;
    provides com.airhacks.afterburner.injection.PresenterFactory
            with org.jabref.gui.DefaultInjector;

    // Logging
    requires org.slf4j;
    requires jul.to.slf4j;
    requires org.apache.logging.log4j.to.slf4j;
    requires org.tinylog.api;
    requires org.tinylog.api.slf4j;
    requires org.tinylog.impl;

    provides org.tinylog.writers.Writer
    with org.jabref.gui.logging.GuiWriter;

    // Preferences and XML
    requires java.prefs;
    requires com.fasterxml.aalto;

    // Annotations (@PostConstruct)
    requires jakarta.annotation;
    requires jakarta.inject;

    // http server and client exchange
    requires java.net.http;
    requires jakarta.ws.rs;
    requires grizzly.framework;

    // data mapping
    requires jakarta.xml.bind;
    requires jdk.xml.dom;
    requires com.google.gson;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.dataformat.yaml;
    requires com.fasterxml.jackson.datatype.jsr310;
    // needs to be loaded here as it's otherwise not found at runtime
    requires org.glassfish.jaxb.runtime;

    // dependency injection using HK2
    requires org.glassfish.hk2.api;

    // http clients
    requires unirest.java;
    requires org.apache.httpcomponents.httpclient;
    requires org.jsoup;

    // SQL databases
    requires ojdbc10;
    requires org.postgresql.jdbc;
    requires org.mariadb.jdbc;
    uses org.mariadb.jdbc.credential.CredentialPlugin;

    // Apache Commons and other (similar) helper libraries
    requires org.apache.commons.cli;
    requires org.apache.commons.csv;
    requires org.apache.commons.lang3;
    requires com.google.common;
    requires io.github.javadiffutils;
    requires java.string.similarity;

    requires com.github.tomtung.latex2unicode;
    requires fastparse;

    requires jbibtex;
    requires citeproc.java;

    requires snuggletex.core;

    requires org.apache.pdfbox;
    requires org.apache.xmpbox;
    requires com.ibm.icu;

    requires flexmark;
    requires flexmark.util.ast;
    requires flexmark.util.data;

    requires com.h2database.mvstore;

    requires java.keyring;
    requires org.freedesktop.dbus;

    requires org.jooq.jool;

    // fulltext search
    requires org.apache.lucene.core;
    // In case the version is updated, please also adapt SearchFieldConstants#VERSION to the newly used version
    uses org.apache.lucene.codecs.lucene95.Lucene95Codec;

    requires org.apache.lucene.queryparser;
    uses org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
    requires org.apache.lucene.analysis.common;
    requires org.apache.lucene.highlighter;

    requires net.harawata.appdirs;
    requires com.sun.jna;
    requires com.sun.jna.platform;

    requires org.eclipse.jgit;
    uses org.eclipse.jgit.transport.SshSessionFactory;
    uses org.eclipse.jgit.lib.GpgSigner;

    // other libraries
    requires org.antlr.antlr4.runtime;
    requires org.libreoffice.uno;
    requires de.saxsys.mvvmfx.validation;
    requires com.jthemedetector;
}
