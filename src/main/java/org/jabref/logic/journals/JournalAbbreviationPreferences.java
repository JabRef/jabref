package org.jabref.logic.journals;

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
                                          String journalsDir) {
        this.journalsDir = new SimpleStringProperty(journalsDir);
        this.journalsDir.addListener((observable, oldValue, newValue) -> {
            updateJournalsDir(newValue);
        });

        this.externalJournalLists = FXCollections.observableArrayList(externalJournalLists);
        this.useFJournalField = new SimpleBooleanProperty(useFJournalField);
    }

    public void updateJournalsDir(String directory) {
        JournalsDirectoryManager.updateJournalsDir(directory, externalJournalLists);
        setJournalAbbreviationDir(directory);
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
