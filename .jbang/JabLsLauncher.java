///usr/bin/env jbang "$0" "$@" ; exit $?

//DESCRIPTION jabls - start a bibtex languageserver

//JAVA 24
//RUNTIME_OPTIONS --enable-native-access=ALL-UNNAMED

//SOURCES ../jabls-cli/src/main/java/org/jabref/languageserver/cli/ServerCli.java
//FILES tinylog.properties=../jabls-cli/src/main/resources/tinylog.properties

//SOURCES ../jabls/src/main/java/org/jabref/languageserver/BibtexTextDocumentService.java
//SOURCES ../jabls/src/main/java/org/jabref/languageserver/BibtexWorkspaceService.java
//SOURCES ../jabls/src/main/java/org/jabref/languageserver/controller/LanguageServerController.java
//SOURCES ../jabls/src/main/java/org/jabref/languageserver/ExtensionSettings.java
//SOURCES ../jabls/src/main/java/org/jabref/languageserver/LspClientHandler.java
//SOURCES ../jabls/src/main/java/org/jabref/languageserver/LspLauncher.java
//SOURCES ../jabls/src/main/java/org/jabref/languageserver/util/LspConsistencyCheck.java
//SOURCES ../jabls/src/main/java/org/jabref/languageserver/util/LspDiagnosticBuilder.java
//SOURCES ../jabls/src/main/java/org/jabref/languageserver/util/LspDiagnosticHandler.java
//SOURCES ../jabls/src/main/java/org/jabref/languageserver/util/LspIntegrityCheck.java
//SOURCES ../jabls/src/main/java/org/jabref/languageserver/util/LspLinkHandler.java
//SOURCES ../jabls/src/main/java/org/jabref/languageserver/util/LspParserHandler.java
//SOURCES ../jabls/src/main/java/org/jabref/languageserver/util/LspRangeUtil.java
//SOURCES ../jabls/src/main/java/org/jabref/languageserver/util/definition/DefinitionProvider.java
//SOURCES ../jabls/src/main/java/org/jabref/languageserver/util/definition/DefinitionProviderFactory.java
//SOURCES ../jabls/src/main/java/org/jabref/languageserver/util/definition/LatexDefinitionProvider.java
//SOURCES ../jabls/src/main/java/org/jabref/languageserver/util/definition/MarkdownDefinitionProvider.java

// raw is for https://github.com/unicode-org/icu/pull/2127
//REPOS mavencentral,mavencentralsnapshots=https://central.sonatype.com/repository/maven-snapshots/,raw=https://raw.githubusercontent.com/JabRef/jabref/refs/heads/main/jablib/lib/

//DEPS org.jabref:jablib:6.0-SNAPSHOT

//DEPS io.github.darvil82:terminal-text-formatter:2.2.0
//DEPS info.picocli:picocli:4.7.7
//DEPS org.jspecify:jspecify:1.0.0

// from jabls
//DEPS com.fasterxml.jackson.core:jackson-annotations:2.20
//DEPS tools.jackson.core:jackson-core:3.0.2
//DEPS tools.jackson.core:jackson-databind:3.0.2
//DEPS tools.jackson.dataformat:jackson-dataformat-yaml:3.0.2
//DEPS com.github.eclipse:lsp4j:0.24.0
//DEPS info.picocli:picocli:4.7.7
//DEPS org.apache.logging.log4j:log4j-to-slf4j:2.25.2
//DEPS org.eclipse.lsp4j:org.eclipse.lsp4j:0.24.0
//DEPS org.jabref:afterburner.fx:2.0.0
//DEPS org.slf4j:jul-to-slf4j:2.0.17
//DEPS org.slf4j:slf4j-api:2.0.17
//DEPS org.tinylog:slf4j-tinylog:2.7.0
//DEPS org.tinylog:tinylog-impl:2.7.0
//DEPS com.github.ben-manes.caffeine:caffeine:3.2.3
//DEPS org.apache.commons:commons-lang3:3.19.0

/// This class is required for [jbang](https://www.jbang.dev/)
public class JabLsLauncher {
    public static void main(String[] args) throws Exception {
        org.jabref.languageserver.cli.ServerCli.main(args);
    }
}
