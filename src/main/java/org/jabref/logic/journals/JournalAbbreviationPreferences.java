package org.jabref.logic.journals;

import java.io.IOException;
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

    private void updateJournalsDir(String directory) {
        if (directory == null) {
            return;
        }
        // Remove old custom.csv path if it exists
        externalJournalLists.removeIf(path -> path.endsWith("custom.csv"));
        // Add new custom.csv path
        Path dirPath = Path.of(directory);
        String newCsvPath = dirPath.resolve("custom.csv").toString();

        if (!externalJournalLists.contains(newCsvPath)) {
            externalJournalLists.add(newCsvPath);
        }

        initializeDirectory(dirPath);
        setJournalAbbreviationDir(directory);
    }

    /**
     * Creates the journal abbreviation directory and custom.csv if they don't exist.
     */
    private void initializeDirectory(Path journalsDir) {
        try {
            Files.createDirectories(journalsDir);
            Path customCsv = journalsDir.resolve("custom.csv");
            if (!Files.exists(customCsv)) {
                Files.createFile(customCsv);
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
            journalsDir = new SimpleStringProperty(Directories.getJournalAbbreviationsDirectory().toString());
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
