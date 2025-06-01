package org.jabref.generators;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
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

/**
 * CLI tool for downloading the LTWA CSV file and converting it to an MVStore file.
 */
public class LtwaListMvGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(LtwaListMvGenerator.class);
    private static final String LTWA_URL = "https://www.issn.org/wp-content/uploads/2021/07/ltwa_20210702.csv";

    public static void main(String[] args) {
        try {
            Path tempCsvFile = downloadLtwaFile();
            Path outputDir = Path.of("build", "resources", "main", "journals");
            Files.createDirectories(outputDir);
            Path outputFile = outputDir.resolve("ltwa-list.mv");

            generateMvStore(tempCsvFile, outputFile);

            // Delete temp file
            Files.deleteIfExists(tempCsvFile);

            LOGGER.info("LTWA MVStore file generated successfully at {}.", outputFile);
        } catch (IOException e) {
            LOGGER.error("Error generating LTWA MVStore file.", e);
        } catch (URISyntaxException e) {
            LOGGER.error("Invalid URL for LTWA file (this should never happen).", e);
        }
    }

    /**
     * Downloads the LTWA CSV file from the specified URL.
     *
     * @return Path to the downloaded file
     * @throws IOException If an I/O error occurs
     */
    private static Path downloadLtwaFile() throws IOException, URISyntaxException {
        LOGGER.info("Downloading LTWA file from {}.", LtwaListMvGenerator.LTWA_URL);
        InputStream inputStream = new URI(LTWA_URL).toURL().openStream();
        Path path = Files.writeString(
                Files.createTempFile("ltwa", ".csv"),
                new String(inputStream.readAllBytes()),
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING);
        inputStream.close();
        return path;
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
