package org.jabref.logic.journals;

import java.util.List;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

public class JournalAbbreviationPreferences extends AbbreviationPreferences {
    private final BooleanProperty useFJournalField;
    public JournalAbbreviationPreferences(List<String> externalLists, boolean useFJournalField) {
        super(externalLists);
        this.useFJournalField = new SimpleBooleanProperty(useFJournalField);
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
