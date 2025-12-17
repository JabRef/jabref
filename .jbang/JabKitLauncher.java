///usr/bin/env jbang "$0" "$@" ; exit $?

//DESCRIPTION jabkit - mange BibTeX files using JabRef

//JAVA 25+
//RUNTIME_OPTIONS --enable-native-access=ALL-UNNAMED

// raw is for https://github.com/unicode-org/icu/pull/2127
//REPOS mavencentral,mavencentralsnapshots=https://central.sonatype.com/repository/maven-snapshots/,raw=https://raw.githubusercontent.com/JabRef/jabref/refs/heads/main/jablib/lib/

//DEPS org.jabref:jablib:6.0-SNAPSHOT

// requirements needed by jabkit projecxt need to be listed; requirements by jablib are loaded transitively
//DEPS info.picocli:picocli:4.7.7

//SOURCES ../jabkit/src/main/java/org/jabref/toolkit/arguments/Provider.java
//SOURCES ../jabkit/src/main/java/org/jabref/toolkit/converter/CaseInsensitiveEnumConverter.java
//SOURCES ../jabkit/src/main/java/org/jabref/toolkit/converter/CygWinPathConverter.java
//SOURCES ../jabkit/src/main/java/org/jabref/toolkit/converter/ProviderConverter.java
//SOURCES ../jabkit/src/main/java/org/jabref/toolkit/commands/CheckConsistency.java
//SOURCES ../jabkit/src/main/java/org/jabref/toolkit/commands/CheckIntegrity.java
//SOURCES ../jabkit/src/main/java/org/jabref/toolkit/commands/Convert.java
//SOURCES ../jabkit/src/main/java/org/jabref/toolkit/commands/DoiToBibtex.java
//SOURCES ../jabkit/src/main/java/org/jabref/toolkit/commands/Fetch.java
//SOURCES ../jabkit/src/main/java/org/jabref/toolkit/commands/GenerateBibFromAux.java
//SOURCES ../jabkit/src/main/java/org/jabref/toolkit/commands/GenerateCitationKeys.java
//SOURCES ../jabkit/src/main/java/org/jabref/toolkit/commands/GetCitedWorks.java
//SOURCES ../jabkit/src/main/java/org/jabref/toolkit/commands/GetCitingWorks.java
//SOURCES ../jabkit/src/main/java/org/jabref/toolkit/commands/JabKit.java
//SOURCES ../jabkit/src/main/java/org/jabref/toolkit/commands/Pdf.java
//SOURCES ../jabkit/src/main/java/org/jabref/toolkit/commands/PdfUpdate.java
//SOURCES ../jabkit/src/main/java/org/jabref/toolkit/commands/Preferences.java
//SOURCES ../jabkit/src/main/java/org/jabref/toolkit/commands/Pseudonymize.java
//SOURCES ../jabkit/src/main/java/org/jabref/toolkit/commands/Search.java

//FILES tinylog.properties=../jabkit/src/main/resources/tinylog.properties

// This is the main class - directly called by JBang
//SOURCES ../jabkit/src/main/java/org/jabref/toolkit/JabKitLauncher.java
