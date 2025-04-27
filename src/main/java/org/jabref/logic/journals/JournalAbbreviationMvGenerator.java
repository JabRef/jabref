package org.jabref.logic.journals;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import org.h2.mvstore.MVStoreException;
import org.jooq.lambda.Unchecked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class converts each CSV abbreviation file in a given directory into an MV file.
 * For each CSV file, an MV file is created in the same directory with the same base name.
 */
public class JournalAbbreviationMvGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(JournalAbbreviationMvGenerator.class);
    private static final String TIMESTAMPS_FILE = "timestamps.mv";
    private static final Set<String> IGNORED_NAMES = Set.of(
            "journal_abbreviations_entrez.csv",
            "journal_abbreviations_medicus.csv",
            "journal_abbreviations_webofscience-dotless.csv",
            "journal_abbreviations_ieee_strings.csv"
    );
    private static final String FILE_TIMESTAMPS_MAP = "fileTimestamps";
    private static final String ABBREVIATION_MAP = "FullToAbbreviation";

    /**
     * Scans the given directory for CSV files and converts each
     * CSV file into a corresponding MV file in the same directory.
     *
     * @param abbreviationsDirectory the directory containing journal abbreviation CSV files.
     */
    public static void convertAllCsvToMv(Path abbreviationsDirectory) {
        // Open or create a persistent MVStore file for storing CSV file timestamps.
        Path timestampFile = abbreviationsDirectory.resolve(TIMESTAMPS_FILE);
        try (MVStore store = new MVStore.Builder()
                .fileName(timestampFile.toString())
                .compressHigh()
                .open()) {
            MVMap<String, Long> timestampMap = store.openMap(FILE_TIMESTAMPS_MAP);

            // Iterate through all CSV files in the directory.
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(abbreviationsDirectory, "*.csv")) {
                stream.forEach(Unchecked.consumer(csvFile -> {
                    String fileName = csvFile.getFileName().toString();
                    if (IGNORED_NAMES.contains(fileName)) {
                        LOGGER.info("{} ignored", fileName);
                    } else {
                        long currentTimestamp = Files.getLastModifiedTime(csvFile).toMillis();
                        Long storedTimestamp = timestampMap.get(fileName);

                        // Compute the MV file path by replacing the .csv extension with .mv
                        Path mvFile = csvFile.resolveSibling(fileName.replaceFirst("\\.csv$", ".mv"));

                        // If MV file is missing OR the CSV file has been updated, process it
                        if (!Files.exists(mvFile) || storedTimestamp == null || storedTimestamp != currentTimestamp) {
                            convertCsvToMv(csvFile, mvFile);
                            LOGGER.info("Processing {} -> Creating MV file: {}", fileName, mvFile.getFileName());
                            // Update the timestamp in the persistent map
                            timestampMap.put(fileName, currentTimestamp);
                        } else {
                            LOGGER.info("File {} is up-to-date, skipping conversion.", fileName);
                        }
                    }
                }));
            }
            // Commit changes to the timestamp map
            store.commit();
        } catch (IOException e) {
            LOGGER.error("Error while processing abbreviation files in directory: {}", abbreviationsDirectory, e);
        }
    }
    /**
     * Converts a CSV file into an MV file.
     * Reads the CSV file and stores its abbreviations into an MVMap inside the MV file.
     *
     * @param csvFile the source CSV file
     * @param mvFile the target MV file
     * @throws IOException if there is an error reading the CSV file or writing the MV file
     */

    public static void convertCsvToMv(Path csvFile, Path mvFile) throws IOException {

        try (MVStore store = new MVStore.Builder()
                .fileName(mvFile.toString())
                .compressHigh()
                .open()) {
            MVMap<String, Abbreviation> fullToAbbreviation = store.openMap(ABBREVIATION_MAP);

            // Clear the existing map to remove outdated entries
            fullToAbbreviation.clear();

            // Read abbreviations from the CSV file using existing logic
            Collection<Abbreviation> abbreviations = JournalAbbreviationLoader.readAbbreviationsFromCsvFile(csvFile);

            // Convert the collection into a map using the full journal name as the key
            Map<String, Abbreviation> abbreviationMap = abbreviations
                    .stream()
                    .collect(Collectors.toMap(
                            Abbreviation::getName,
                            abbr -> abbr,
                            (abbr1, abbr2) -> {
                                LOGGER.info("Duplicate entry found: {}", abbr1.getName());
                                return abbr2;
                            }));

            fullToAbbreviation.putAll(abbreviationMap);
            store.commit();
            LOGGER.info("Saved MV file: {}", mvFile.getFileName());
        } catch (IOException e) {
            LOGGER.error("Failed to convert CSV file: {}", csvFile, e);
        }
    }

    public static Collection<Abbreviation> loadAbbreviationsFromMv(Path path) throws IOException {
        Collection<Abbreviation> abbreviations = new ArrayList<>();

            try (MVStore store = new MVStore.Builder()
                .fileName(path.toString())
                .readOnly()
                .open()) {
            MVMap<String, Abbreviation> abbreviationMap = store.openMap(ABBREVIATION_MAP);

            abbreviationMap.forEach((key, value) -> {
                    Abbreviation fixedAbbreviation = new Abbreviation(
                            key,
                            value.getAbbreviation(),
                            value.getShortestUniqueAbbreviation()
                    );
                    abbreviations.add(fixedAbbreviation);
                });
            store.commit();
        } catch (MVStoreException e) {
            LOGGER.error("MVStoreException: {}", path, e);
        } catch (Exception e) {
            LOGGER.error("Unexpected error while reading MV file: {}", path, e);
        }

        return abbreviations;
    }
}
