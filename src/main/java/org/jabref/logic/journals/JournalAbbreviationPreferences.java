package org.jabref.logic.journals;

import java.nio.file.Path;
import java.util.List;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class JournalAbbreviationPreferences {

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
        Path dir = journalAbbreviationDir.get();
        return dir != null ? dir : getDefaultAbbreviationDir();
    }

    public static Path getDefaultAbbreviationDir() {
        String appData = System.getenv("APPDATA");
        if (appData == null) {
            appData = System.getProperty("user.home");
        }
        return Path.of(appData, ".jabref", "journals");
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
