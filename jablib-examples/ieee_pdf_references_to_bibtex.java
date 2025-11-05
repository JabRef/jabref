///usr/bin/env jbang "$0" "$@" ; exit $?

import org.jabref.logic.exporter.BibWriter;
import org.jabref.logic.exporter.BibDatabaseWriter;
import org.jabref.logic.importer.fileformat.BibliographyFromPdfImporter;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.preferences.JabRefCliPreferences;
import org.tinylog.Logger;

//DESCRIPTION Opens the given PDF file, parses the references and outputs BibTeX

//JAVA 25+
//RUNTIME_OPTIONS --enable-native-access=ALL-UNNAMED
//FILES tinylog.properties=tinylog.properties

//DEPS org.jabref:jablib:6.0-SNAPSHOT
//REPOS mavencentral,mavencentralsnapshots=https://central.sonatype.com/repository/maven-snapshots/,s01oss=https://s01.oss.sonatype.org/content/repositories/snapshots/,oss=https://oss.sonatype.org/content/repositories,jitpack=https://jitpack.io,oss2=https://oss.sonatype.org/content/groups/public,ossrh=https://oss.sonatype.org/content/repositories/snapshots,raw=https://raw.githubusercontent.com/JabRef/jabref/refs/heads/main/jablib/lib/

// generate with
//     git diff --name-only main | grep jablib/src/main | sed "s#\(.*\)#//SOURCES ../\1#" | grep -v module-info | grep -v .properties
//SOURCES ../jablib/src/main/java/org/jabref/logic/ai/AiDefaultPreferences.java
//SOURCES ../jablib/src/main/java/org/jabref/logic/bibtex/BibEntryWriter.java
//SOURCES ../jablib/src/main/java/org/jabref/logic/citation/repository/BibEntrySerializer.java
//SOURCES ../jablib/src/main/java/org/jabref/logic/crawler/StudyRepository.java
//SOURCES ../jablib/src/main/java/org/jabref/logic/exporter/BibDatabaseWriter.java
//SOURCES ../jablib/src/main/java/org/jabref/logic/git/io/GitFileWriter.java
//SOURCES ../jablib/src/main/java/org/jabref/logic/importer/Importer.java
//SOURCES ../jablib/src/main/java/org/jabref/logic/importer/ParserResult.java
//SOURCES ../jablib/src/main/java/org/jabref/logic/importer/fileformat/BibliographyFromPdfImporter.java
//SOURCES ../jablib/src/main/java/org/jabref/logic/importer/fileformat/BibtexParser.java
//SOURCES ../jablib/src/main/java/org/jabref/logic/importer/fileformat/PdfMergeMetadataImporter.java
//SOURCES ../jablib/src/main/java/org/jabref/logic/importer/fileformat/pdf/PdfContentImporter.java
//SOURCES ../jablib/src/main/java/org/jabref/logic/importer/fileformat/pdf/PdfEmbeddedBibFileImporter.java
//SOURCES ../jablib/src/main/java/org/jabref/logic/importer/fileformat/pdf/PdfGrobidImporter.java
//SOURCES ../jablib/src/main/java/org/jabref/logic/importer/fileformat/pdf/PdfImporter.java
//SOURCES ../jablib/src/main/java/org/jabref/logic/importer/fileformat/pdf/PdfVerbatimBibtexImporter.java
//SOURCES ../jablib/src/main/java/org/jabref/logic/importer/fileformat/pdf/PdfXmpImporter.java
//SOURCES ../jablib/src/main/java/org/jabref/logic/importer/plaincitation/LlmPlainCitationParser.java
//SOURCES ../jablib/src/main/java/org/jabref/logic/importer/plaincitation/MultiplePlainCitationsParser.java
//SOURCES ../jablib/src/main/java/org/jabref/logic/importer/plaincitation/PlainCitationParserChoice.java
//SOURCES ../jablib/src/main/java/org/jabref/logic/importer/plaincitation/ReferencesBlockFromPdfFinder.java
//SOURCES ../jablib/src/main/java/org/jabref/logic/importer/util/GrobidService.java
//SOURCES ../jablib/src/main/java/org/jabref/logic/preferences/JabRefCliPreferences.jav

void main() throws Exception {
    var preferences = JabRefCliPreferences.getInstance();

    extractEntriesUsingBibliographyFromPdfImporter()

    var importer = new BibliographyFromPdfImporter(preferences.getCitationKeyPatternPreferences());
    var result = importer.importDatabase(Path.of("pdfs", "test.pdf"));
    if (result.hasWarnings()) {
        Logger.warn("There were warnings during the import. {}", result.getWarningsMap());
    }
    var context = result.getDatabaseContext();
    var writer = new OutputStreamWriter(System.out, StandardCharsets.UTF_8);
    var entries = result.getDatabase().getEntries();
    var bibWriter = new BibDatabaseWriter(writer, context, preferences);
    bibWriter.writeDatabase(context);
}
