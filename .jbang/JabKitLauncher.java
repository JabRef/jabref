///usr/bin/env jbang "$0" "$@" ; exit $?

//DESCRIPTION jabkit - mange BibTeX files using JabRef

//JAVA 24
//RUNTIME_OPTIONS --enable-native-access=ALL-UNNAMED

//SOURCES ../jabkit/src/main/java/org/jabref/toolkit/cli/converter/CygWinPathConverter.java
//SOURCES ../jabkit/src/main/java/org/jabref/toolkit/cli/ArgumentProcessor.java
//SOURCES ../jabkit/src/main/java/org/jabref/toolkit/cli/CheckConsistency.java
//SOURCES ../jabkit/src/main/java/org/jabref/toolkit/cli/CheckIntegrity.java
//SOURCES ../jabkit/src/main/java/org/jabref/toolkit/cli/Convert.java
//SOURCES ../jabkit/src/main/java/org/jabref/toolkit/cli/DoiToBibtex.java
//SOURCES ../jabkit/src/main/java/org/jabref/toolkit/cli/Fetch.java
//SOURCES ../jabkit/src/main/java/org/jabref/toolkit/cli/GenerateBibFromAux.java
//SOURCES ../jabkit/src/main/java/org/jabref/toolkit/cli/GenerateCitationKeys.java
//SOURCES ../jabkit/src/main/java/org/jabref/toolkit/cli/Pdf.java
//SOURCES ../jabkit/src/main/java/org/jabref/toolkit/cli/PdfUpdate.java
//SOURCES ../jabkit/src/main/java/org/jabref/toolkit/cli/Preferences.java
//SOURCES ../jabkit/src/main/java/org/jabref/toolkit/cli/Pseudonymize.java
//SOURCES ../jabkit/src/main/java/org/jabref/toolkit/cli/Search.java
//SOURCES ../jabkit/src/main/java/org/jabref/toolkit/JabKit.java
//FILES tinylog.properties=../jabkit/src/main/resources/tinylog.properties

// raw is for https://github.com/unicode-org/icu/pull/2127
//REPOS mavencentral,mavencentralsnapshots=https://central.sonatype.com/repository/maven-snapshots/,raw=https://raw.githubusercontent.com/JabRef/jabref/refs/heads/main/jablib/lib/

//DEPS org.jabref:jablib:6.0-SNAPSHOT

// requirements needed by jabkit projecxt need to be listed; requirements by jablib are loaded transitively
//DEPS info.picocli:picocli:4.7.7

/// This class is required for [jbang](https://www.jbang.dev/)
public class JabKitLauncher {
    public static void main(String[] args) {
        org.jabref.toolkit.JabKit.main(args);
    }
}
