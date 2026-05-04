package org.jabref.logic.journals;

import java.util.List;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class JournalAbbreviationPreferences {

    private final ObservableList<String> externalJournalLists;
    private final BooleanProperty useFJournalField;

    public JournalAbbreviationPreferences(List<String> externalJournalLists,
                                          boolean useFJournalField) {
        this.externalJournalLists = FXCollections.observableArrayList(externalJournalLists);
        this.useFJournalField = new SimpleBooleanProperty(useFJournalField);
    }

    private JournalAbbreviationPreferences() {
        this(
                List.of(), // externalJournalLists
                true       // useFJournalField
        );
    }

    public void setAll(JournalAbbreviationPreferences preferences) {
        this.externalJournalLists.setAll(preferences.externalJournalLists);
        this.useFJournalField.set(preferences.shouldUseFJournalField());
    }

    public static JournalAbbreviationPreferences getDefault() {
        return new JournalAbbreviationPreferences();
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
