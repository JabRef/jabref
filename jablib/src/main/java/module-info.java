open module org.jabref.jablib {
    requires javafx.base;
    requires com.tobiasdiez.easybind;
    requires javafx.graphics;

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
    requires jakarta.xml.bind;
    requires jdk.xml.dom;
    requires com.google.gson;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.dataformat.yaml;
    requires com.fasterxml.jackson.datatype.jsr310;
    // needs to be loaded here as it's otherwise not found at runtime
    //OK requires org.glassfish.jaxb.runtime;
    // endregion

    // dependency injection using HK2
    requires org.glassfish.hk2.api;

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
    requires ojdbc10;
    requires org.postgresql.jdbc;
    requires org.mariadb.jdbc;
    uses org.mariadb.jdbc.credential.CredentialPlugin;
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

    requires org.jooq.jool;

    // region AI
    requires ai.djl.api;
    requires ai.djl.pytorch_model_zoo;
    requires ai.djl.tokenizers;
    requires jvm.openai;
    requires langchain4j;
    requires langchain4j.core;
    requires langchain4j.google.ai.gemini;
    requires langchain4j.hugging.face;
    requires langchain4j.mistral.ai;
    requires langchain4j.open.ai;
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
    uses org.apache.lucene.codecs.lucene101.Lucene101Codec;
    requires org.apache.lucene.analysis.common;
    requires org.apache.lucene.core;
    requires org.apache.lucene.highlighter;
    requires org.apache.lucene.queryparser;
    // endregion

    requires net.harawata.appdirs;
    requires com.sun.jna;
    requires com.sun.jna.platform;

    requires org.eclipse.jgit;
    uses org.eclipse.jgit.transport.SshSessionFactory;
    uses org.eclipse.jgit.lib.Signer;

    requires transitive org.jspecify;

    // region: other libraries (alphabetically)
    requires cuid;
    requires dd.plist;
    requires io.github.adr;
    // required by okhttp and some AI library
    requires kotlin.stdlib;
    requires mslinks;
    requires org.antlr.antlr4.runtime;
    requires org.libreoffice.uno;
    // endregion
}
