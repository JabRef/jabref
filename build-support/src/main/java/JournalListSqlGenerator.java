//JAVA 25+
//RUNTIME_OPTIONS --enable-native-access=ALL-UNNAMED

//DEPS org.antlr:antlr4-runtime:4.13.2
//DEPS org.apache.commons:commons-csv:1.14.1
//DEPS org.jooq:jool:0.9.15
//DEPS org.jspecify:jspecify:1.0.0
//DEPS org.slf4j:slf4j-api:2.0.17
//DEPS org.slf4j:slf4j-simple:2.0.17

//SOURCES ../../../../jablib/src/main/java/org/jabref/logic/journals/Abbreviation.java
//SOURCES ../../../../jablib/src/main/java/org/jabref/logic/journals/AbbreviationFormat.java
//SOURCES ../../../../jablib/src/main/java/org/jabref/logic/journals/AbbreviationParser.java

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.jabref.logic.journals.Abbreviation;
import org.jabref.logic.journals.AbbreviationParser;

import org.jooq.lambda.Unchecked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Generates a SQL file for populating the Postgres journal abbreviation table
/// Has to be started in the root of the repository due to <https://github.com/jbangdev/jbang-gradle-plugin/issues/11>
public class JournalListSqlGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(JournalListSqlGenerator.class);

    /// Number of rows per INSERT statement for efficient bulk loading
    private static final int BATCH_SIZE = 1000;

    static void main(String[] args) throws IOException {
        boolean verbose = (args.length == 1) && ("--verbose".equals(args[0]));

        Path abbreviationsDirectory = Path.of("jablib", "src", "main", "abbrv.jabref.org", "journals");
        if (!Files.exists(abbreviationsDirectory)) {
            System.out.println("Path " + abbreviationsDirectory.toAbsolutePath() + " does not exist");
            System.exit(0);
        }

        Path journalListSqlFile = Path.of("jablib", "build", "generated", "resources", "journals", "journal-list.sql");

        Set<String> ignoredNames = Set.of(
                // remove all lists without dot in them:
                // we use abbreviation lists containing dots in them only (to be consistent)
                "journal_abbreviations_entrez.csv",
                "journal_abbreviations_medicus.csv",
                "journal_abbreviations_webofscience-dotless.csv",

                // we currently do not have good support for BibTeX strings
                "journal_abbreviations_ieee_strings.csv"
        );

        Files.createDirectories(journalListSqlFile.getParent());

        // Collect all abbreviations, resolving duplicates by keeping the last one
        Map<String, Abbreviation> allAbbreviations = new HashMap<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(abbreviationsDirectory, "*.csv")) {
            stream.forEach(Unchecked.consumer(path -> {
                String fileName = path.getFileName().toString();
                System.out.print("Checking ");
                System.out.print(fileName);
                if (ignoredNames.contains(fileName)) {
                    System.out.println(" ignored");
                } else {
                    System.out.println("...");
                    AbbreviationParser parser = new AbbreviationParser();
                    parser.readJournalListFromFile(path);
                    Collection<Abbreviation> abbreviations = parser.getAbbreviations();
                    for (Abbreviation abbreviation : abbreviations) {
                        if (verbose && allAbbreviations.containsKey(abbreviation.getName())) {
                            System.out.println("Double entry " + abbreviation.getName());
                        }
                        allAbbreviations.put(abbreviation.getName(), abbreviation);
                    }
                }
            }));
        }

        writeSqlFile(journalListSqlFile, allAbbreviations.values());

        LOGGER.info("Generated journal SQL file at {} ({} entries)", journalListSqlFile.toAbsolutePath(), allAbbreviations.size());
    }

    private static void writeSqlFile(Path outputFile, Collection<Abbreviation> abbreviations) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(outputFile)) {
            writeSchema(writer);
            writeData(writer, abbreviations);
        }
    }

    private static void writeSchema(BufferedWriter writer) throws IOException {
        writer.write("""
                CREATE EXTENSION IF NOT EXISTS pg_trgm;

                CREATE TABLE IF NOT EXISTS journal_abbreviation (
                    name                          TEXT PRIMARY KEY,
                    abbreviation                  TEXT NOT NULL,
                    dotless_abbreviation          TEXT NOT NULL,
                    shortest_unique_abbreviation  TEXT NOT NULL
                );

                CREATE INDEX IF NOT EXISTS idx_abbreviation ON journal_abbreviation (abbreviation);
                CREATE INDEX IF NOT EXISTS idx_dotless      ON journal_abbreviation (dotless_abbreviation);
                CREATE INDEX IF NOT EXISTS idx_shortest     ON journal_abbreviation (shortest_unique_abbreviation);
                CREATE INDEX IF NOT EXISTS idx_name_trgm    ON journal_abbreviation USING gin (name gin_trgm_ops);

                """);
    }

    private static void writeData(BufferedWriter writer, Collection<Abbreviation> abbreviations) throws IOException {
        Abbreviation[] entries = abbreviations.toArray(Abbreviation[]::new);
        int total = entries.length;

        for (int i = 0; i < total; i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, total);

            writer.write("INSERT INTO journal_abbreviation (name, abbreviation, dotless_abbreviation, shortest_unique_abbreviation) VALUES\n");

            for (int j = i; j < end; j++) {
                Abbreviation abbr = entries[j];
                String dotless = abbr.getDotlessAbbreviation();
                String shortest = abbr.getShortestUniqueAbbreviation();

                writer.write("(");
                writer.write(escapeSql(abbr.getName()));
                writer.write(", ");
                writer.write(escapeSql(abbr.getAbbreviation()));
                writer.write(", ");
                writer.write(escapeSql(dotless));
                writer.write(", ");
                writer.write(escapeSql(shortest));
                writer.write(")");

                if (j < end - 1) {
                    writer.write(",\n");
                } else {
                    writer.write("\nON CONFLICT (name) DO UPDATE SET\n");
                    writer.write("    abbreviation = EXCLUDED.abbreviation,\n");
                    writer.write("    dotless_abbreviation = EXCLUDED.dotless_abbreviation,\n");
                    writer.write("    shortest_unique_abbreviation = EXCLUDED.shortest_unique_abbreviation;\n\n");
                }
            }
        }
    }

    /// Escapes a string for safe inclusion in a SQL literal by doubling single quotes.
    private static String escapeSql(String value) {
        return "'" + value.replace("'", "''") + "'";
    }
}
