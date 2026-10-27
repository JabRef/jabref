//JAVA 25+
//RUNTIME_OPTIONS --enable-native-access=ALL-UNNAMED

//DEPS com.h2database:h2:2.5.252
//DEPS info.debatty:java-string-similarity:2.0.0
//DEPS org.antlr:antlr4-runtime:4.13.2
//DEPS org.apache.commons:commons-csv:1.14.1
//DEPS org.jabref:easybind:2.3.0
//DEPS org.jooq:jool:0.9.15
//DEPS org.jspecify:jspecify:1.0.1
// Keep this version in sync with versions/build.gradle.kts.
//DEPS org.openjfx:javafx-base:27.0.1
//DEPS org.slf4j:slf4j-api:2.0.20
//DEPS org.slf4j:slf4j-simple:2.0.20

//SOURCES ../../../../jablib/src/main/java/org/jabref/logic/journals/Abbreviation.java
//SOURCES ../../../../jablib/src/main/java/org/jabref/logic/journals/AbbreviationFormat.java
//SOURCES ../../../../jablib/src/main/java/org/jabref/logic/journals/AbbreviationParser.java
//SOURCES ../../../../jablib/src/main/java/org/jabref/logic/journals/AbbreviationLoader.java
//SOURCES ../../../../jablib/src/main/java/org/jabref/logic/journals/JournalAbbreviationLoader.java
//SOURCES ../../../../jablib/src/main/java/org/jabref/logic/journals/AbbreviationPreferences.java
//SOURCES ../../../../jablib/src/main/java/org/jabref/logic/journals/JournalAbbreviationPreferences.java
//SOURCES ../../../../jablib/src/main/java/org/jabref/logic/journals/AbbreviationRepository.java
//SOURCES ../../../../jablib/src/main/java/org/jabref/logic/journals/JournalAbbreviationRepository.java
//SOURCES ../../../../jablib/src/main/java/org/jabref/logic/journals/ltwa/LtwaEntry.java
//SOURCES ../../../../jablib/src/main/java/org/jabref/logic/journals/ltwa/LtwaRepository.java
//SOURCES ../../../../jablib/src/main/java/org/jabref/logic/journals/ltwa/NormalizeUtils.java
//SOURCES ../../../../jablib/src/main/java/org/jabref/logic/journals/ltwa/PrefixTree.java
//SOURCES ../../../../jablib/src/main/java/org/jabref/logic/util/BackgroundTask.java
//SOURCES ../../../../jablib/src/main/java/org/jabref/logic/util/DelayTaskThrottler.java
//SOURCES ../../../../jablib/src/main/java/org/jabref/logic/util/FallbackExceptionHandler.java
//SOURCES ../../../../jablib/src/main/java/org/jabref/logic/util/HeadlessExecutorService.java
//SOURCES ../../../../jablib/src/main/java/org/jabref/logic/util/TaskExecutor.java
//SOURCES ../../../../jablib/src/main/java/org/jabref/logic/util/strings/StringSimilarity.java

//SOURCES ../../../../jablib/build/generated-src/antlr/main/org/jabref/logic/journals/ltwa/*.java

/* Project policy
 > [!IMPORTANT]
 > This project does not accept fully AI-generated pull requests. AI tools may only be used for assistance. You must understand and take responsibility for every change you submit.
 >
 > Read and follow:
 > • [AGENTS.md](./AGENTS.md)
 > • [CONTRIBUTING.md](./CONTRIBUTING.md)
*/

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.jabref.logic.journals.Abbreviation;
import org.jabref.logic.journals.AbbreviationLoader;

import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Has to be started in the root of the repository due to <https://github.com/jbangdev/jbang-gradle-plugin/issues/11>
@NullMarked
public class AbbreviationListMvGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbbreviationListMvGenerator.class);

    static void main(String[] args) throws IOException {
        boolean verbose = (args.length == 1) && ("--verbose".equals(args[0]));

        Set<String> ignoredNames = Set.of(
                // remove all lists without dot in them:
                // we use abbreviation lists containing dots in them only (to be consistent)
                "journal_abbreviations_entrez.csv",
                "journal_abbreviations_medicus.csv",
                "journal_abbreviations_webofscience-dotless.csv",

                // we currently do not have good support for BibTeX strings
                "journal_abbreviations_ieee_strings.csv"
        );

        generate("journals", "journal-list.mv", ignoredNames, verbose);
        // [impl->req~abbreviations.conference-store~1]
        generate("conferences", "conference-list.mv", Set.of(), verbose);
    }

    private static void generate(String directory, String fileName, Set<String> ignoredNames, boolean verbose) throws IOException {
        Path abbreviationsDirectory = Path.of("jablib", "src", "main", "abbrv.jabref.org", directory);
        Path abbreviationListMvFile = Path.of("jablib", "build", "generated", "resources", directory, fileName);

        Files.createDirectories(abbreviationListMvFile.getParent());

        try (MVStore store = new MVStore.Builder().
                     fileName(abbreviationListMvFile.toString()).
                     compressHigh().
                     open()) {
            MVMap<String, Abbreviation> fullToAbbreviation = store.openMap("FullToAbbreviation");
            fullToAbbreviation.clear();
            if (!Files.isDirectory(abbreviationsDirectory)) {
                LOGGER.info("No abbreviation lists available at {}", abbreviationsDirectory);
                return;
            }
            try (var stream = Files.list(abbreviationsDirectory)) {
                for (Path path : stream.filter(path -> path.toString().endsWith(".csv")).toList()) {
                    String csvFileName = path.getFileName().toString();
                    System.out.print("Checking ");
                    System.out.print(csvFileName);
                    if (ignoredNames.contains(csvFileName)) {
                        System.out.println(" ignored");
                    } else {
                        System.out.println("...");
                        Collection<Abbreviation> abbreviations = AbbreviationLoader.readAbbreviationsFromCsvFile(path);
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
                }
            }
        }

        LOGGER.info("Generated abbreviation list at {}", abbreviationListMvFile.toAbsolutePath());
    }
}
