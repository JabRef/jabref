package org.jabref.logic.journals;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.logic.util.Directories;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JournalAbbreviationPreferences {
    private static final Logger LOGGER = LoggerFactory.getLogger(JournalAbbreviationPreferences.class);

    private static final String CUSTOM_CSV_FILE = "custom.csv";
    private static final String MV_FILE_PATTERN = "*.mv";
    private static final String TIMESTAMPS_FILE = "timestamps.mv";
    private final ObservableList<String> externalJournalLists;
    private final BooleanProperty useFJournalField;
    private StringProperty journalsDir;

    public JournalAbbreviationPreferences(List<String> externalJournalLists,
                                          boolean useFJournalField,
                                          StringProperty journalsDir) {

        if (journalsDir == null || journalsDir.get() == null) {
            this.journalsDir = new SimpleStringProperty(Directories.getJournalAbbreviationsDirectory().toString()); // default directory
        } else {
            this.journalsDir = journalsDir;
        }

        this.journalsDir.addListener((observable, oldValue, newValue) -> {
            updateJournalsDir(newValue);
        });

        this.externalJournalLists = FXCollections.observableArrayList(externalJournalLists);
        this.useFJournalField = new SimpleBooleanProperty(useFJournalField);
    }

    public void updateJournalsDir(String directory) {
        // Remove old .mv paths if exist
        externalJournalLists.removeIf(path -> path.endsWith(".mv"));

        Path dirPath = Path.of(directory);
        try {
            initializeDirectory(dirPath);
        } catch (IOException e) {
            LOGGER.error("Error initializing the journal directory", e);
        }
        setJournalAbbreviationDir(directory);
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
    private void initializeDirectory(Path journalsDir) throws IOException {
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

    public ObservableList<String> getExternalJournalLists() {
        return externalJournalLists;
    }

    public void setExternalJournalLists(List<String> list) {
        externalJournalLists.clear();
        externalJournalLists.addAll(list);
    }

    public boolean shouldUseFJournalField() {
        return useFJournalField.get();
    }

    public BooleanProperty useFJournalFieldProperty() {
        return useFJournalField;
    }

    public void setUseFJournalField(boolean useFJournalField) {
        this.useFJournalField.set(useFJournalField);
    }

    public String getJournalAbbreviationDir() {
        if (journalsDir == null) {
            String defaultDir = Directories.getJournalAbbreviationsDirectory().toString();
            setJournalAbbreviationDir(defaultDir);
            return defaultDir;
        }
        return journalsDir.get();
    }

    public StringProperty journalAbbreviationDirectoryProperty() {
        return journalsDir;
    }

    public void setJournalAbbreviationDir(String journalsDir) {
        this.journalsDir.set(journalsDir);
    }
}
