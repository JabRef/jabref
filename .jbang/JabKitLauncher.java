///usr/bin/env jbang "$0" "$@" ; exit $?

//DESCRIPTION jabkit - mange BibTeX files using JabRef

//JAVA 25
//RUNTIME_OPTIONS --enable-native-access=ALL-UNNAMED

//SOURCES ../jabkit/src/main/java/org/jabref/cli/converter/CygWinPathConverter.java
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

// REPOS mavencentral,snapshots=https://central.sonatype.com/repository/maven-snapshots/
// REPOS mavencentral,mavencentralsnapshots=https://central.sonatype.com/repository/maven-snapshots/,s01oss=https://s01.oss.sonatype.org/content/repositories/snapshots/,oss=https://oss.sonatype.org/content/repositories,jitpack=https://jitpack.io,oss2=https://oss.sonatype.org/content/groups/public,ossrh=https://oss.sonatype.org/content/repositories/snapshots
//REPOS mavencentral,mavencentralsnapshots=https://central.sonatype.com/repository/maven-snapshots/,s01oss=https://s01.oss.sonatype.org/content/repositories/snapshots/,oss=https://oss.sonatype.org/content/repositories,jitpack=https://jitpack.io,oss2=https://oss.sonatype.org/content/groups/public,ossrh=https://oss.sonatype.org/content/repositories/snapshots,raw=https://raw.githubusercontent.com/JabRef/jabref/refs/heads/main/jablib/lib/
// REPOS mavencentral,jitpack=https://jitpack.io

// TODO: ASCII things won't work, but we accept for now to keep things going
//DEPS com.ibm.icu:icu4j:77.1

// Choose one - both should work
// https://central.sonatype.com/service/rest/repository/browse/maven-snapshots/org/jabref/jablib/
//DEPS org.jabref:jablib:6.0-SNAPSHOT
// https://jitpack.io/#jabref/jabref/main-SNAPSHOT
// DEPS com.github.jabref:jabref:main-SNAPSHOT
//DEPS io.github.darvil82:terminal-text-formatter:2.2.0
//DEPS info.picocli:picocli:4.7.7

import org.jabref.JabKit;

/// This class is required for [jbang](https://www.jbang.dev/)
public class JabKitLauncher {
    static void main(String[] args) {
        org.jabref.JabKit.main(args);
    }
}
