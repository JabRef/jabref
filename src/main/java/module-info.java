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
    requires de.jensd.fx.fontawesomefx.commons;
    requires de.jensd.fx.fontawesomefx.materialdesignicons;
    requires org.controlsfx.controls;
    requires org.fxmisc.richtext;
    requires com.tobiasdiez.easybind;

    provides com.airhacks.afterburner.views.ResourceLocator
            with org.jabref.gui.util.JabRefResourceLocator;
    provides com.airhacks.afterburner.injection.PresenterFactory
            with org.jabref.gui.DefaultInjector;

    // Logging
    requires org.slf4j;
    requires org.apache.logging.log4j;
    requires org.apache.logging.log4j.core;
    requires org.apache.logging.log4j.plugins;
    requires applicationinsights.logging.log4j2;
    provides org.apache.logging.log4j.plugins.processor.PluginService
            with org.jabref.gui.logging.plugins.Log4jPlugins;

    // Preferences and XML
    requires java.prefs;
    requires java.xml.bind;
    requires jdk.xml.dom;

    // Annotations (@PostConstruct)
    requires java.annotation;

    // Microsoft application insights
    requires applicationinsights.core;

    // Libre Office
    requires org.jabref.thirdparty.libreoffice;

    // Other modules
    requires commons.logging;
    requires com.google.common;
    requires jakarta.inject;
    requires org.apache.pdfbox;
    requires reactfx;
    requires commons.cli;
    requires com.github.tomtung.latex2unicode;
    requires jbibtex;
    requires citeproc.java;
    requires antlr.runtime;
    requires org.graalvm.truffle;
    requires org.graalvm.sdk;
    requires transitive org.graalvm.js;
    requires java.scripting;
    requires jdk.internal.vm.compiler;
    requires org.apache.xmpbox;
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
    requires org.antlr.antlr4.runtime;
    requires flowless;
    requires org.apache.tika.core;

    requires flexmark;
    requires flexmark.ext.gfm.strikethrough;
    requires flexmark.ext.gfm.tasklist;
    requires flexmark.util.ast;
    requires flexmark.util.data;
    requires com.h2database.mvstore;
}
