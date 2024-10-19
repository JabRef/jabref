package org.jabref.logic.journals;

import java.nio.file.Path;
import java.nio.file.Files;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class JournalAbbreviationPreferences {

    private static final Logger logger = Logger.getLogger(JournalAbbreviationPreferences.class.getName());
    private static final Path JABREF_DATA_DIRECTORY = Path.of(System.getenv("APPDATA"), "..", "Local", "org.jabref", "jabref");
    private final ObservableList<String> externalJournalLists;
    private final BooleanProperty useFJournalField;
    private final ObjectProperty<Path> journalAbbreviationDir;

    public JournalAbbreviationPreferences(List<String> externalJournalLists, boolean useFJournalField) {
        this(externalJournalLists, useFJournalField, getDefaultAbbreviationDir().toString());
    }

    public JournalAbbreviationPreferences(List<String> externalJournalLists,
                                          boolean useFJournalField, String directory) {
        this.externalJournalLists = FXCollections.observableArrayList(externalJournalLists);
        this.useFJournalField = new SimpleBooleanProperty(useFJournalField);
        this.journalAbbreviationDir = new SimpleObjectProperty<>(
                directory != null ? Path.of(directory) : getDefaultAbbreviationDir()
        );

        updateCustomCsvFile(this.journalAbbreviationDir.get());

        this.journalAbbreviationDir.addListener((observable, oldValue, newValue) -> {
            updateCustomCsvFile(newValue);
        });
    }

    private void updateCustomCsvFile(Path directory) {
        if (directory == null) {
            return;
        }
        Path newFilePath = JournalAbbreviationLoader.ensureJournalAbbreviationFileExists(directory);
        if (newFilePath != null) {
            String newFilePathString = newFilePath.toString();
            // Remove old custom.csv path if it exists
            externalJournalLists.removeIf(path -> path.endsWith("custom.csv"));
            // Add new custom.csv path
            if (!externalJournalLists.contains(newFilePathString)) {
                externalJournalLists.add(newFilePathString);
            }
        }
    }

    public Path getJournalAbbreviationDir() {
        return journalAbbreviationDir.get() != null ? journalAbbreviationDir.get() : getDefaultAbbreviationDir();
    }

    public static Path getDefaultAbbreviationDir() {
        Path journalDir = JABREF_DATA_DIRECTORY.resolve("journals");
        try {
            Files.createDirectories(journalDir);
            logger.info("Using journal abbreviation directory: " + journalDir);
            return journalDir;
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to create journal abbreviation directory in JabRef data folder", e);
            return getTempDirectory();
        }
    }

    private static Path getTempDirectory() {
        Path tempDir = Path.of(System.getProperty("java.io.tmpdir"), "JabRef", "journals");
        try {
            Files.createDirectories(tempDir);
            logger.info("Using temporary directory for journal abbreviations: " + tempDir);
            return tempDir;
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to create temporary directory", e);
            throw new IllegalStateException("Unable to create journal abbreviation directory", e);
        }
    }

    public void setJournalAbbreviationDir(Path journalAbbreviationDir) {
        this.journalAbbreviationDir.set(journalAbbreviationDir);
    }

    public ObjectProperty<Path> journalAbbreviationDirProperty() {
        return journalAbbreviationDir;
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
}
