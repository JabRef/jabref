///usr/bin/env jbang "$0" "$@" ; exit $?

import org.jabref.logic.exporter.BibWriter;
import org.jabref.logic.exporter.BibDatabaseWriter;
import org.jabref.logic.importer.fileformat.pdf.CitationsFromPdf;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.preferences.JabRefCliPreferences;

import org.tinylog.Logger;

//DESCRIPTION Opens the given PDF file, parses the references and outputs BibTeX

//JAVA 25+
//RUNTIME_OPTIONS --enable-native-access=ALL-UNNAMED
//FILES tinylog.properties=tinylog.properties

//REPOS mavencentral,mavencentralsnapshots=https://central.sonatype.com/repository/maven-snapshots/
//DEPS org.jabref:jablib:6.0-SNAPSHOT
// JabRef relies on PR https://github.com/unicode-org/icu/pull/2127; for experiments the release version is OK.
//DEPS com.ibm.icu:icu4j:78.1
//DEPS com.fasterxml.jackson.core:jackson-annotations
//DEPS tools.jackson.core:jackson-core:3.0.2
//DEPS tools.jackson.core:jackson-databind:3.0.2
//DEPS tools.jackson.dataformat:jackson-dataformat-yaml:3.0.2

void main() throws Exception {
    var preferences = JabRefCliPreferences.getInstance();

    // This is PDF to BibTeX
    // Choose one
    var result = CitationsFromPdf.extractCitationsUsingRuleBasedAlgorithm(preferences, Path.of("pdfs", "test.pdf"));
    // var result = CitationsFromPdf.extractCitationsUsingGrobid(preferences, Path.of("pdfs", "test.pdf"));
    // var result = CitationsFromPdf.extractCitationsUsingLLM(preferences, Logger::info, Path.of("pdfs", "test.pdf"));

    if (result.hasWarnings()) {
        Logger.warn("There were warnings during the import. {}", result.getWarningsMap());
    }

    // Write the result as BibTeX file
    var context = result.getDatabaseContext();
    try (var writer = new OutputStreamWriter(System.out, StandardCharsets.UTF_8)) {
        var entries = result.getDatabase().getEntries();
        var bibWriter = new BibDatabaseWriter(writer, context, preferences);
        bibWriter.writeDatabase(context);
    }
}
