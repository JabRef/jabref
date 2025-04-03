package org.jabref.cli;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

import org.jabref.logic.journals.ltwa.LtwaEntry;
import org.jabref.logic.journals.ltwa.LtwaParser;
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
            Path tempCsvFile = downloadLtwaFile(LTWA_URL);
            Path outputDir = Path.of("build", "resources", "main", "journals");
            Files.createDirectories(outputDir);
            Path outputFile = outputDir.resolve("ltwa-list.mv");

            generateMvStore(tempCsvFile, outputFile);

            // Delete temp file
            Files.deleteIfExists(tempCsvFile);

            LOGGER.info("LTWA MVStore file generated successfully at {}", outputFile);
        } catch (IOException e) {
            LOGGER.error("Error generating LTWA MVStore file", e);
        }
    }

    /**
     * Downloads the LTWA CSV file from the specified URL.
     *
     * @param url URL of the LTWA CSV file
     * @return Path to the downloaded file
     * @throws IOException If an I/O error occurs
     */
    private static Path downloadLtwaFile(String url) throws IOException {
        LOGGER.info("Downloading LTWA file from {}", url);
        try (var in = URI.create(url).toURL().openStream()) {
            return Files.writeString(
                    Files.createTempFile("ltwa", ".csv"),
                    new String(in.readAllBytes()),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);
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
        LtwaParser parser = new LtwaParser(inputFile);
        List<LtwaEntry> entries = parser.parse();

        LOGGER.info("Found {} LTWA entries", entries.size());

        try (MVStore store = new MVStore.Builder()
                .fileName(outputFile.toString())
                .compressHigh()
                .open()) {
            MVMap<String, LtwaEntry> prefixMap = store.openMap("Prefixes");
            MVMap<String, LtwaEntry> suffixMap = store.openMap("Suffixes");
            var inflection = Character.toString(PrefixTree.WILD_CARD).repeat(3) + " ";

            for (var entry : entries) {
                String word = NormalizeUtils.normalize(entry.word()).toLowerCase().replace(" ",
                        inflection);
                boolean isSuffix = word.startsWith("-");

                if (isSuffix) {
                    suffixMap.put(word.substring(1), entry);
                } else {
                    prefixMap.put(word.endsWith("-") ? word.substring(0, word.length() - 1) : word, entry);
                }
            }

            LOGGER.info("Stored {} prefixes and {} suffixes", prefixMap.size(), suffixMap.size());
        }
    }
}
