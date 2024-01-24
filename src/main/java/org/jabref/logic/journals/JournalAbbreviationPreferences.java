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

    private final ObjectProperty<Path> journalAbbreviationsDirectory = new SimpleObjectProperty<>();

    public JournalAbbreviationPreferences(List<String> externalJournalLists,
                                          boolean useFJournalField, Path journalAbbreviationsDirectory) {
        this.externalJournalLists = FXCollections.observableArrayList(externalJournalLists);
        this.useFJournalField = new SimpleBooleanProperty(useFJournalField);
        this.journalAbbreviationsDirectory.setValue(journalAbbreviationsDirectory);
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

    public ObjectProperty<Path> getJournalAbbreviationsDirectory() {
        return journalAbbreviationsDirectory;
    }

    public void setJournalAbbreviationsDirectory(Path journalAbbreviationsDirectory) {
        this.journalAbbreviationsDirectory.setValue(journalAbbreviationsDirectory);
    }
}
