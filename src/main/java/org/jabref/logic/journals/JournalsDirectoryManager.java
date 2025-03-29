package org.jabref.logic.journals;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

import javafx.collections.ObservableList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JournalsDirectoryManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(JournalsDirectoryManager.class);

    private static final String CUSTOM_CSV_FILE = "custom.csv";
    private static final String MV_FILE_PATTERN = "*.mv";
    private static final String TIMESTAMPS_FILE = "timestamps.mv";

    public static void updateJournalsDir(String directory, ObservableList<String> externalJournalLists) {
        // Remove old .mv paths if exist
        externalJournalLists.removeIf(path -> path.endsWith(".mv"));

        Path dirPath = Path.of(directory);
        try {
            initializeDirectory(dirPath, externalJournalLists);
        } catch (IOException e) {
            LOGGER.error("Error initializing the journal directory", e);
        }
    }

    /**
     * Ensures the journal abbreviation directory exists and initializes necessary files.
     * <p>
     * This method performs the following steps:
     * - Creates the journal abbreviation directory if it does not already exist.
     * - Ensures the existence of the "CUSTOM_CSV_FILE" file, creating an empty one if missing.
     * - Converts all `.csv` files in the directory to `.mv` format using {@link JournalAbbreviationMvGenerator#convertAllCsvToMv}.
     * - Scans the directory for `.mv` files (excluding TIMESTAMPS_FILE) and adds them to {@code externalJournalLists}.
     * <p>
     * If any I/O errors occur during these operations, they are logged.
     *
     * @param journalsDir The path to the journal abbreviation directory.
     */
    private static void initializeDirectory(Path journalsDir, ObservableList<String> externalJournalLists) throws IOException {
        Files.createDirectories(journalsDir);
        Path customCsv = journalsDir.resolve(CUSTOM_CSV_FILE);
        if (!Files.exists(customCsv)) {
            Files.createFile(customCsv);
        }

        JournalAbbreviationMvGenerator.convertAllCsvToMv(journalsDir);

        // Iterate through the directory and add all .mv files to externalJournalLists
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(journalsDir, MV_FILE_PATTERN)) {
            for (Path mvFile : stream) {
                if (!TIMESTAMPS_FILE.equals(mvFile.getFileName().toString())) { // Exclude TIMESTAMPS_FILE
                    externalJournalLists.add(mvFile.toString());
                }
            }
        }
    }
}
