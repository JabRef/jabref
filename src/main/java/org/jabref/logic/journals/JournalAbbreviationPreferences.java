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
        if (directory == null) {
            return;
        }
        // Remove old .mv paths if exist
        externalJournalLists.removeIf(path -> path.endsWith(".mv"));

        Path dirPath = Path.of(directory);
        initializeDirectory(dirPath);
        setJournalAbbreviationDir(directory);
    }

    /**
     * Ensures the journal abbreviation directory exists and initializes necessary files.
     * <p>
     * This method performs the following steps:
     * - Creates the journal abbreviation directory if it does not already exist.
     * - Ensures the existence of the "custom.csv" file, creating an empty one if missing.
     * - Converts all `.csv` files in the directory to `.mv` format using {@link JournalAbbreviationMvGenerator#convertAllCsvToMv}.
     * - Scans the directory for `.mv` files (excluding "timestamps.mv") and adds them to {@code externalJournalLists}.
     * <p>
     * If any I/O errors occur during these operations, they are logged.
     *
     * @param journalsDir The path to the journal abbreviation directory.
     */
    private void initializeDirectory(Path journalsDir) {
        try {
            Files.createDirectories(journalsDir);
            Path customCsv = journalsDir.resolve("custom.csv");
            if (!Files.exists(customCsv)) {
                Files.createFile(customCsv);
            }

            JournalAbbreviationMvGenerator.convertAllCsvToMv(journalsDir);

            // Iterate through the directory and add all .mv files to externalJournalLists
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(journalsDir, "*.mv")) {
                for (Path mvFile : stream) {
                    if (!"timestamps.mv".equals(mvFile.getFileName().toString())) { // Exclude timestamps.mv
                        externalJournalLists.add(mvFile.toString());
                    }
                }
            } catch (IOException e) {
                LOGGER.error("Error reading MV files from directory: {}", journalsDir, e);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to create journal abbreviation directory", e);
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
             setJournalAbbreviationDir(Directories.getJournalAbbreviationsDirectory().toString()); // default directory
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
