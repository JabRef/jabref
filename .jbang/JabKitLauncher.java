///usr/bin/env jbang "$0" "$@" ; exit $?

//DESCRIPTION jabkit - mange BibTeX files using JabRef

//JAVA 24
//RUNTIME_OPTIONS --enable-native-access=ALL-UNNAMED

//SOURCES ../jabkit/src/main/java/org/jabref/cli/ArgumentProcessor.java
//SOURCES ../jabkit/src/main/java/org/jabref/cli/CheckConsistency.java
//SOURCES ../jabkit/src/main/java/org/jabref/cli/CheckIntegrity.java
//SOURCES ../jabkit/src/main/java/org/jabref/cli/Convert.java
//SOURCES ../jabkit/src/main/java/org/jabref/cli/Fetch.java
//SOURCES ../jabkit/src/main/java/org/jabref/cli/GenerateBibFromAux.java
//SOURCES ../jabkit/src/main/java/org/jabref/cli/GenerateCitationKeys.java
//SOURCES ../jabkit/src/main/java/org/jabref/cli/Pdf.java
//SOURCES ../jabkit/src/main/java/org/jabref/cli/PdfUpdate.java
//SOURCES ../jabkit/src/main/java/org/jabref/cli/Preferences.java
//SOURCES ../jabkit/src/main/java/org/jabref/cli/Pseudonymize.java
//SOURCES ../jabkit/src/main/java/org/jabref/cli/Search.java
//SOURCES ../jabkit/src/main/java/org/jabref/JabKit.java
//FILES tinylog.properties=../jabkit/src/main/resources/tinylog.properties

//REPOS mavencentral,mavencentralsnapshots=https://central.sonatype.com/repository/maven-snapshots/,s01oss=https://s01.oss.sonatype.org/content/repositories/snapshots/,oss=https://oss.sonatype.org/content/repositories,jitpack=https://jitpack.io,oss2=https://oss.sonatype.org/content/groups/public,ossrh=https://oss.sonatype.org/content/repositories/snapshots

//DEPS org.jabref:jablib:6.+
//DEPS info.picocli:picocli:4.7.7

import org.jabref.JabKit;

/// This class is required for [jbang](https://www.jbang.dev/)
public class JabKitLauncher {
    public static void main(String[] args) {
        org.jabref.JabKit.main(args);
    }
}
