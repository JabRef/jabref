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

// REPOS mavencentral,snapshots=https://central.sonatype.com/repository/maven-snapshots/
// REPOS mavencentral,mavencentralsnapshots=https://central.sonatype.com/repository/maven-snapshots/,s01oss=https://s01.oss.sonatype.org/content/repositories/snapshots/,oss=https://oss.sonatype.org/content/repositories,jitpack=https://jitpack.io,oss2=https://oss.sonatype.org/content/groups/public,ossrh=https://oss.sonatype.org/content/repositories/snapshots
//REPOS mavencentral,mavencentralsnapshots=https://central.sonatype.com/repository/maven-snapshots/,s01oss=https://s01.oss.sonatype.org/content/repositories/snapshots/,oss=https://oss.sonatype.org/content/repositories,jitpack=https://jitpack.io,oss2=https://oss.sonatype.org/content/groups/public,ossrh=https://oss.sonatype.org/content/repositories/snapshots,raw=https://raw.githubusercontent.com/JabRef/jabref/refs/heads/main/jablib/lib/
// REPOS mavencentral,jitpack=https://jitpack.io

// Choose one - both should work
// https://central.sonatype.com/service/rest/repository/browse/maven-snapshots/org/jabref/jablib/
//DEPS org.jabref:jablib:6.0-SNAPSHOT
// https://jitpack.io/#jabref/jabref/main-SNAPSHOT
// DEPS com.github.jabref:jabref:main-SNAPSHOT

//DEPS info.picocli:picocli:4.7.7
//DEPS org.jspecify:jspecify:1.0.0

// from jabls
//DEPS com.fasterxml.jackson.core:jackson-databind:2.20.0
//DEPS com.github.eclipse:lsp4j:0.24.0
//DEPS info.picocli:picocli:4.7.7
//DEPS org.apache.logging.log4j:log4j-to-slf4j:2.25.2
//DEPS org.jabref:afterburner.fx:2.0.0
//DEPS org.slf4j:jul-to-slf4j:2.0.17
//DEPS org.slf4j:slf4j-api:2.0.17
//DEPS org.tinylog:slf4j-tinylog:2.7.0
//DEPS org.tinylog:tinylog-impl:2.7.0

/// This class is required for [jbang](https://www.jbang.dev/)
public class JabLsLauncher {
    public static void main(String[] args) throws Exception {
        org.jabref.languageserver.cli.ServerCli.main(args);
    }
}
