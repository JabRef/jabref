open module org.jabref.jablib {
    exports org.jabref.model;
    exports org.jabref.logic;

    exports org.jabref.search;
    exports org.jabref.logic.search;
    exports org.jabref.logic.search.query;
    exports org.jabref.model.entry.field;
    exports org.jabref.model.search;
    exports org.jabref.model.search.query;
    exports org.jabref.model.util;
    exports org.jabref.logic.preferences;
    exports org.jabref.logic.importer;
    exports org.jabref.logic.bibtex;
    exports org.jabref.logic.citationkeypattern;
    exports org.jabref.logic.exporter;
    exports org.jabref.logic.importer.fileformat;
    exports org.jabref.logic.journals;
    exports org.jabref.logic.l10n;
    exports org.jabref.logic.net;
    exports org.jabref.logic.os;
    exports org.jabref.logic.quality.consistency;
    exports org.jabref.logic.shared.prefs;
    exports org.jabref.logic.util;
    exports org.jabref.logic.util.io;
    exports org.jabref.logic.xmp;
    exports org.jabref.model.database;
    exports org.jabref.model.entry;
    exports org.jabref.model.strings;
    exports org.jabref.logic.protectedterms;
    exports org.jabref.logic.remote;
    exports org.jabref.logic.remote.client;
    exports org.jabref.logic.net.ssl;
    exports org.jabref.logic.citationstyle;
    exports org.jabref.architecture;
    exports org.jabref.logic.journals.ltwa;
    exports org.jabref.logic.shared;
    exports org.jabref.model.groups;
    exports org.jabref.model.groups.event;
    exports org.jabref.logic.preview;
    exports org.jabref.logic.ai;
    exports org.jabref.logic.pdf;
    exports org.jabref.model.database.event;
    exports org.jabref.model.entry.event;
    exports org.jabref.logic.push;
    exports org.jabref.model.search.event;
    exports org.jabref.model.search.matchers;
    exports org.jabref.model.entry.identifier;
    exports org.jabref.model.entry.types;
    exports org.jabref.logic.importer.util;
    exports org.jabref.logic.database;
    exports org.jabref.logic.externalfiles;
    exports org.jabref.logic.help;
    exports org.jabref.logic.bibtex.comparator;
    exports org.jabref.logic.groups;
    exports org.jabref.logic.layout;
    exports org.jabref.logic.openoffice.style;
    exports org.jabref.model.metadata;
    exports org.jabref.model.metadata.event;
    exports org.jabref.logic.ai.chatting;
    exports org.jabref.logic.ai.util;
    exports org.jabref.logic.ai.ingestion;
    exports org.jabref.logic.ai.ingestion.model;
    exports org.jabref.model.ai;
    exports org.jabref.logic.ai.processingstatus;
    exports org.jabref.logic.ai.summarization;
    exports org.jabref.logic.layout.format;
    exports org.jabref.logic.auxparser;
    exports org.jabref.logic.cleanup;
    exports org.jabref.logic.formatter;
    exports org.jabref.logic.importer.fetcher.citation.crossref;
    exports org.jabref.logic.importer.fetcher.citation.semanticscholar;
    exports org.jabref.logic.formatter.bibtexfields;
    exports org.jabref.model.pdf;
    exports org.jabref.logic.texparser;
    exports org.jabref.model.texparser;
    exports org.jabref.logic.importer.fetcher;
    exports org.jabref.logic.importer.fetcher.citation;
    exports org.jabref.logic.importer.fileformat.pdf;
    exports org.jabref.logic.integrity;
    exports org.jabref.logic.formatter.casechanger;
    exports org.jabref.logic.shared.exception;
    exports org.jabref.logic.importer.fetcher.isbntobibtex;
    exports org.jabref.logic.importer.fetcher.transformers;
    exports org.jabref.logic.biblog;
    exports org.jabref.model.biblog;
    exports org.jabref.model.http;
    exports org.jabref.logic.remote.server;
    exports org.jabref.logic.util.strings;
    exports org.jabref.model.openoffice;
    exports org.jabref.logic.openoffice;
    exports org.jabref.logic.openoffice.action;
    exports org.jabref.logic.openoffice.frontend;
    exports org.jabref.logic.openoffice.oocsltext;
    exports org.jabref.model.openoffice.rangesort;
    exports org.jabref.model.openoffice.style;
    exports org.jabref.model.openoffice.uno;
    exports org.jabref.model.openoffice.util;
    exports org.jabref.logic.importer.plaincitation;
    exports org.jabref.logic.ai.templates;
    exports org.jabref.logic.bst;
    exports org.jabref.model.study;
    exports org.jabref.logic.shared.security;
    exports org.jabref.logic.shared.event;
    exports org.jabref.logic.citation;
    exports org.jabref.logic.crawler;
    exports org.jabref.logic.pseudonymization;
    exports org.jabref.logic.citation.repository;
    exports org.jabref.model.paging;
    exports org.jabref.logic.git;
    exports org.jabref.logic.git.conflicts;
    exports org.jabref.logic.git.io;
    exports org.jabref.logic.git.model;
    exports org.jabref.logic.git.status;
    exports org.jabref.logic.command;
    exports org.jabref.logic.git.util;
    exports org.jabref.logic.git.preferences;
    exports org.jabref.logic.icore;
    exports org.jabref.model.icore;
    exports org.jabref.logic.git.merge.planning;
    exports org.jabref.logic.git.merge.execution;
    exports org.jabref.model.sciteTallies;

    requires java.base;

    requires javafx.base;
    requires javafx.graphics; // because of javafx.scene.paint.Color
    requires afterburner.fx;
    requires com.tobiasdiez.easybind;

    // for java.awt.geom.Rectangle2D required by org.jabref.logic.pdf.TextExtractor
    requires java.desktop;

    // SQL
    requires java.sql;
    requires java.sql.rowset;

    // region: Logging
    requires org.slf4j;
    requires jul.to.slf4j;
    requires org.apache.logging.log4j.to.slf4j;
    // endregion

    // Preferences and XML
    requires java.prefs;
    requires com.fasterxml.aalto;

    // YAML
    requires org.yaml.snakeyaml;

    // region: Annotations (@PostConstruct)
    requires jakarta.annotation;
    requires jakarta.inject;
    // endregion

    // region: data mapping
    requires jdk.xml.dom;
    requires com.google.gson;
    requires tools.jackson.databind;
    requires tools.jackson.dataformat.yaml;
    requires tools.jackson.core;
    // endregion

    // region HTTP clients
    requires java.net.http;
    requires jakarta.ws.rs;
    requires org.apache.httpcomponents.core5.httpcore5;
    requires org.jsoup;
    requires unirest.java.core;
    requires unirest.modules.gson;
    // endregion

    // region: SQL databases
    requires embedded.postgres;
    requires org.tukaani.xz;
    requires org.postgresql.jdbc;
    // endregion

    // region: Apache Commons and other (similar) helper libraries
    requires com.google.common;
    requires io.github.javadiffutils;
    requires java.string.similarity;
    requires org.apache.commons.compress;
    requires org.apache.commons.csv;
    requires org.apache.commons.io;
    requires org.apache.commons.lang3;
    requires org.apache.commons.text;
    requires org.apache.commons.logging;
    // endregion

    // region: caching
    requires com.github.benmanes.caffeine;
    // endregion

    // region: latex2unicode
    requires com.github.tomtung.latex2unicode;
    requires fastparse;
    requires scala.library;
    // endregion

    requires jbibtex;
    requires citeproc.java;

    requires snuggletex.core;

    requires org.apache.pdfbox;
    requires org.apache.xmpbox;
    requires com.ibm.icu;

    requires flexmark;
    requires flexmark.html2md.converter;
    requires flexmark.util.ast;
    requires flexmark.util.data;

    requires com.h2database.mvstore;

    requires java.keyring;
    requires org.freedesktop.dbus;

    // region AI
    requires ai.djl.api;
    requires ai.djl.pytorch_model_zoo;
    requires ai.djl.tokenizers;
    requires jvm.openai;
    requires langchain4j;
    requires langchain4j.core;
    uses ai.djl.engine.EngineProvider;
    uses ai.djl.repository.RepositoryFactory;
    uses ai.djl.repository.zoo.ZooProvider;
    uses dev.langchain4j.spi.prompt.PromptTemplateFactory;
    requires velocity.engine.core;
    // endregion

    // region: Lucene
    /*
     * In case the version is updated, please also increment {@link org.jabref.model.search.LinkedFilesConstants.VERSION} to trigger reindexing.
     */
    uses org.apache.lucene.codecs.lucene103.Lucene103Codec;
    requires org.apache.lucene.analysis.common;
    requires org.apache.lucene.core;
    requires org.apache.lucene.highlighter;
    requires org.apache.lucene.queryparser;
    // endregion

    // region: appdirs
    requires net.harawata.appdirs;
    requires com.sun.jna;
    requires com.sun.jna.platform;
    // endregion

    // region: jgit
    requires org.eclipse.jgit;
    uses org.eclipse.jgit.transport.SshSessionFactory;
    uses org.eclipse.jgit.lib.Signer;
    // endregion

    // region: other libraries (alphabetically)
    requires cuid;
    requires dd.plist;
    requires io.github.adr;
    requires io.github.darvil.terminal.textformatter;
    // required by okhttp and some AI library
    requires kotlin.stdlib;
    requires mslinks;
    requires org.antlr.antlr4.runtime;
    requires org.jooq.jool;
    requires org.libreoffice.uno;
    requires transitive org.jspecify;
    // endregion
}
