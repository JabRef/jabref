open module org.jabref {
    // Swing
    requires java.desktop;

    // SQL
    requires java.sql;

    // JavaFX
    requires javafx.base;
    requires javafx.graphics;
    requires javafx.swing;
    requires javafx.controls;
    requires javafx.web;
    requires javafx.fxml;
    requires afterburner.fx;
    requires com.jfoenix;
    requires de.saxsys.mvvmfx;

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
    requires org.tinylog.api;
    requires org.tinylog.api.slf4j;
    requires org.tinylog.impl;

    provides org.tinylog.writers.Writer
    with org.jabref.gui.logging.GuiWriter,
         org.jabref.gui.logging.ApplicationInsightsWriter;

    // Preferences and XML
    requires java.prefs;
    requires jakarta.xml.bind;
    // needs to be loaded here as it's otherwise not found at runtime
    requires org.glassfish.jaxb.runtime;
    requires jdk.xml.dom;

    // Annotations (@PostConstruct)
    requires java.annotation;

    // Microsoft application insights
    requires applicationinsights.core;
    requires applicationinsights.logging.log4j2;

    // Libre Office
    requires org.libreoffice.uno;

    // Other modules
    requires com.google.common;
    requires jakarta.inject;
    requires reactfx;
    requires commons.cli;
    requires com.github.tomtung.latex2unicode;
    requires fastparse;
    requires jbibtex;
    requires citeproc.java;
    requires de.saxsys.mvvmfx.validation;
    requires com.google.gson;
    requires unirest.java;
    requires org.apache.httpcomponents.httpclient;
    requires org.jsoup;
    requires commons.csv;
    requires io.github.javadiffutils;
    requires java.string.similarity;
    requires ojdbc10;
    requires org.postgresql.jdbc;
    requires org.mariadb.jdbc;
    uses org.mariadb.jdbc.credential.CredentialPlugin;
    requires org.apache.commons.lang3;
    requires antlr.runtime;
    requires org.antlr.antlr4.runtime;
    requires org.fxmisc.flowless;
    requires org.apache.tika.core;
    uses org.apache.tika.detect.AutoDetectReader;
    requires pdfbox;
    requires xmpbox;
    requires com.ibm.icu;

    requires flexmark;
    requires flexmark.ext.gfm.strikethrough;
    requires flexmark.ext.gfm.tasklist;
    requires flexmark.util.ast;
    requires flexmark.util.data;
    requires com.h2database.mvstore;

    // fulltext search
    requires org.apache.lucene.core;
    // In case the version is updated, please also adapt SearchFieldConstants#VERSION to the newly used version
    uses org.apache.lucene.codecs.lucene92.Lucene92Codec;

    requires org.apache.lucene.queryparser;
    uses org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
    requires org.apache.lucene.analysis.common;
    requires org.apache.lucene.highlighter;

    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.dataformat.yaml;
    requires com.fasterxml.jackson.datatype.jsr310;
    requires net.harawata.appdirs;
    requires wellbehavedfx;

    requires org.eclipse.jgit;
    uses org.eclipse.jgit.transport.SshSessionFactory;
    uses org.eclipse.jgit.lib.GpgSigner;
}
