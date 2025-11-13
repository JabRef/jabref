///usr/bin/env jbang "$0" "$@" ; exit $?

import org.jabref.logic.exporter.BibWriter;
import org.jabref.logic.exporter.BibDatabaseWriter;
import org.jabref.logic.importer.fetcher.CrossRef;
import org.jabref.logic.preferences.JabRefCliPreferences;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;

import org.tinylog.Logger;

//DESCRIPTION Converts a DOI to BibTeX

//JAVA 25+
//RUNTIME_OPTIONS --enable-native-access=ALL-UNNAMED
//FILES tinylog.properties=tinylog.properties

//REPOS mavencentral,mavencentralsnapshots=https://central.sonatype.com/repository/maven-snapshots/,jitpack=https://jitpack.io,ossrh=https://oss.sonatype.org/content/repositories/snapshots
//DEPS org.jabref:jablib:6.0-SNAPSHOT
// JabRef relies on PR https://github.com/unicode-org/icu/pull/2127; for experiments the release version is OK.
//DEPS com.ibm.icu:icu4j:78.1

void main() throws Exception {
    var preferences = JabRefCliPreferences.getInstance();

    // All `IdParserFetcher<DOI>` can do. In JabRef, there is currently only one implemented

    var fetcher = new CrossRef();
    var entry = fetcher.performSearchById("10.47397/tb/44-3/tb138kopp-jabref").get(); // will throw an exception if not found

    try (var writer = new OutputStreamWriter(System.out, StandardCharsets.UTF_8)) {
        var context = new BibDatabaseContext(new BibDatabase(List.of(entry)));
        var bibWriter = new BibDatabaseWriter(writer, context, preferences);
        bibWriter.writeDatabase(context);
    }
}
