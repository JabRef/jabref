//JAVA 25+
//RUNTIME_OPTIONS --enable-native-access=ALL-UNNAMED

//DEPS com.h2database:h2:2.4.240
//DEPS info.debatty:java-string-similarity:2.0.0
//DEPS org.antlr:antlr4-runtime:4.13.2
//DEPS org.apache.commons:commons-csv:1.14.1
//DEPS org.jooq:jool:0.9.15
//DEPS org.jspecify:jspecify:1.0.0
//DEPS org.openjfx:javafx-base:24.0.2
//DEPS org.slf4j:slf4j-api:2.0.17
//DEPS org.slf4j:slf4j-simple:2.0.17

//SOURCES ../../../../jablib/src/main/java/org/jabref/logic/journals/Abbreviation.java
//SOURCES ../../../../jablib/src/main/java/org/jabref/logic/journals/AbbreviationFormat.java
//SOURCES ../../../../jablib/src/main/java/org/jabref/logic/journals/AbbreviationParser.java
//SOURCES ../../../../jablib/src/main/java/org/jabref/logic/journals/JournalAbbreviationLoader.java
//SOURCES ../../../../jablib/src/main/java/org/jabref/logic/journals/JournalAbbreviationPreferences.java
//SOURCES ../../../../jablib/src/main/java/org/jabref/logic/journals/JournalAbbreviationRepository.java
//SOURCES ../../../../jablib/src/main/java/org/jabref/logic/journals/ltwa/LtwaEntry.java
//SOURCES ../../../../jablib/src/main/java/org/jabref/logic/journals/ltwa/LtwaRepository.java
//SOURCES ../../../../jablib/src/main/java/org/jabref/logic/journals/ltwa/NormalizeUtils.java
//SOURCES ../../../../jablib/src/main/java/org/jabref/logic/journals/ltwa/PrefixTree.java
//SOURCES ../../../../jablib/src/main/java/org/jabref/logic/util/strings/StringSimilarity.java

//SOURCES ../../../../jablib/build/generated-src/antlr/main/org/jabref/logic/journals/ltwa/*.java

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.jabref.logic.journals.Abbreviation;
import org.jabref.logic.journals.JournalAbbreviationLoader;

import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import org.jooq.lambda.Unchecked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Has to be started in the root of the repository due to <https://github.com/jbangdev/jbang-gradle-plugin/issues/11>
public class JournalListMvGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(JournalListMvGenerator.class);

    static void main(String[] args) throws IOException {
        boolean verbose = (args.length == 1) && ("--verbose".equals(args[0]));

        Path abbreviationsDirectory = Path.of("jablib", "src", "main", "abbrv.jabref.org", "journals");
        if (!Files.exists(abbreviationsDirectory)) {
            System.out.println("Path " + abbreviationsDirectory.toAbsolutePath() + " does not exist");
            System.exit(0);
        }
        // Directory layout aligns to other plugins (e.g., XJF plugin (https://github.com/bjornvester/xjc-gradle-plugin))
        Path journalListMvFile = Path.of("jablib", "build", "generated", "resources", "journals", "journal-list.mv");

        Set<String> ignoredNames = Set.of(
                // remove all lists without dot in them:
                // we use abbreviation lists containing dots in them only (to be consistent)
                "journal_abbreviations_entrez.csv",
                "journal_abbreviations_medicus.csv",
                "journal_abbreviations_webofscience-dotless.csv",

                // we currently do not have good support for BibTeX strings
                "journal_abbreviations_ieee_strings.csv"
        );

        Files.createDirectories(journalListMvFile.getParent());

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(abbreviationsDirectory, "*.csv");
             MVStore store = new MVStore.Builder().
                     fileName(journalListMvFile.toString()).
                     compressHigh().
                     open()) {
            MVMap<String, Abbreviation> fullToAbbreviation = store.openMap("FullToAbbreviation");
            stream.forEach(Unchecked.consumer(path -> {
                String fileName = path.getFileName().toString();
                System.out.print("Checking ");
                System.out.print(fileName);
                if (ignoredNames.contains(fileName)) {
                    System.out.println(" ignored");
                } else {
                    System.out.println("...");
                    Collection<Abbreviation> abbreviations = JournalAbbreviationLoader.readAbbreviationsFromCsvFile(path);
                    Map<String, Abbreviation> abbreviationMap = abbreviations
                            .stream()
                            .collect(Collectors.toMap(
                                    Abbreviation::getName,
                                    abbreviation -> abbreviation,
                                    (abbreviation1, abbreviation2) -> {
                                        if (verbose) {
                                            System.out.println("Double entry " + abbreviation1.getName());
                                        }
                                        return abbreviation2;
                                    }));
                    fullToAbbreviation.putAll(abbreviationMap);
                }
            }));
        }

        LOGGER.info("Generated journal list at {}", journalListMvFile.toAbsolutePath());
    }
}
