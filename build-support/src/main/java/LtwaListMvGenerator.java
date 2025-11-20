//JAVA 24
//RUNTIME_OPTIONS --enable-native-access=ALL-UNNAMED

//DEPS com.h2database:h2:2.4.240
//DEPS org.antlr:antlr4-runtime:4.13.2
//DEPS org.apache.commons:commons-csv:1.14.1
//DEPS info.debatty:java-string-similarity:2.0.0
//DEPS org.jooq:jool:0.9.15
//DEPS org.openjfx:javafx-base:24.0.2
//DEPS org.jspecify:jspecify:1.0.0
//DEPS org.slf4j:slf4j-api:2.0.17
//DEPS org.slf4j:slf4j-simple:2.0.17

//SOURCES ../../../../jablib/src/main/java/org/jabref/logic/journals/Abbreviation.java
//SOURCES ../../../../jablib/src/main/java/org/jabref/logic/journals/AbbreviationFormat.java
//SOURCES ../../../../jablib/src/main/java/org/jabref/logic/journals/AbbreviationParser.java
//SOURCES ../../../../jablib/src/main/java/org/jabref/logic/journals/JournalAbbreviationLoader.java
//SOURCES ../../../../jablib/src/main/java/org/jabref/logic/journals/JournalAbbreviationPreferences.java
//SOURCES ../../../../jablib/src/main/java/org/jabref/logic/journals/JournalAbbreviationRepository.java
//SOURCES ../../../../jablib/src/main/java/org/jabref/logic/journals/ltwa/*.java
//SOURCES ../../../../jablib/src/main/java/org/jabref/logic/util/strings/StringSimilarity.java

//SOURCES ../../../../jablib/build/generated-src/antlr/main/org/jabref/logic/journals/ltwa/*.java

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jabref.logic.journals.ltwa.LtwaEntry;
import org.jabref.logic.journals.ltwa.LtwaTsvParser;
import org.jabref.logic.journals.ltwa.NormalizeUtils;
import org.jabref.logic.journals.ltwa.PrefixTree;

import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// CLI tool for downloading the LTWA CSV file and converting it to an MVStore file.
///
/// Has to be started in the root of the repository due to <https://github.com/jbangdev/jbang-gradle-plugin/issues/11>
public class LtwaListMvGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(LtwaListMvGenerator.class);

    public static void main(String[] args) {
        try {
            Path tempCsvFile = Path.of("jablib", "build", "tmp", "ltwa_20210702.csv");
            if (!Files.exists(tempCsvFile)) {
                LOGGER.error("LTWA CSV file not found at {}. Please execute gradle task downloadLtwaFile.", tempCsvFile);
                return;
            }
            Path outputDir = Path.of("jablib", "build", "generated", "resources", "journals");

            Files.createDirectories(outputDir);
            Path outputFile = outputDir.resolve("ltwa-list.mv");

            generateMvStore(tempCsvFile, outputFile);

            LOGGER.info("LTWA MVStore file generated successfully at {}.", outputFile.toAbsolutePath());
        } catch (IOException e) {
            LOGGER.error("Error generating LTWA MVStore file.", e);
        }
    }

    /**
     * Generates an MVStore file from the LTWA CSV file.
     *
     * @param inputFile  Path to the LTWA CSV file
     * @param outputFile Path where the MVStore file will be written
     * @throws IOException If an I/O error occurs
     */
    private static void generateMvStore(Path inputFile, Path outputFile) throws IOException {
        LOGGER.info("Parsing LTWA file...");
        LtwaTsvParser parser = new LtwaTsvParser(inputFile);
        List<LtwaEntry> entries = parser.parse();

        LOGGER.info("Found {} LTWA entries", entries.size());

        try (MVStore store = new MVStore.Builder()
                .fileName(outputFile.toString())
                .compressHigh()
                .open()) {
            MVMap<String, List<LtwaEntry>> prefixMap = store.openMap("Prefixes");
            MVMap<String, List<LtwaEntry>> suffixMap = store.openMap("Suffixes");
            String inflection = Character.toString(PrefixTree.WILD_CARD).repeat(3) + " ";

            entries.forEach(entry ->
                    NormalizeUtils.normalize(entry.word())
                                  .map(String::toLowerCase)
                                  .map(word -> word.replace(" ", inflection))
                                  .ifPresent(word -> {
                                      if (word.startsWith("-")) {
                                          String key = word.substring(1);
                                          suffixMap.computeIfAbsent(key, _ ->
                                                  Stream.<LtwaEntry>builder().build().collect(Collectors.toList())
                                          ).add(entry);
                                      } else {
                                          String key = word.endsWith("-") ? word.substring(0, word.length() - 1) : word;
                                          prefixMap.computeIfAbsent(key, _ ->
                                                  Stream.<LtwaEntry>builder().build().collect(Collectors.toList())
                                          ).add(entry);
                                      }
                                  })
            );

            LOGGER.info("Stored {} prefixes and {} suffixes", prefixMap.size(), suffixMap.size());
        }
    }
}
