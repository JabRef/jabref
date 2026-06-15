package org.jabref.logic.journals;

import java.util.List;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class AbbreviationPreferences {

    private final ObservableList<String> externalJournalLists;
    private final BooleanProperty useFJournalField;
    private final BooleanProperty shouldEnableMscKeywordDescriptions;

    public AbbreviationPreferences(List<String> externalJournalLists,
                                   boolean useFJournalField,
                                   boolean shouldEnableMscKeywordDescriptions) {
        this.externalJournalLists = FXCollections.observableArrayList(externalJournalLists);
        this.useFJournalField = new SimpleBooleanProperty(useFJournalField);
        this.shouldEnableMscKeywordDescriptions = new SimpleBooleanProperty(shouldEnableMscKeywordDescriptions);
    }

    private AbbreviationPreferences() {
        this(
                List.of(), // externalJournalLists
                true,       // useFJournalField
                false       // Enable MSC codes as Keyword
        );
    }

    public void setAll(AbbreviationPreferences preferences) {
        this.externalJournalLists.setAll(preferences.externalJournalLists);
        this.useFJournalField.set(preferences.shouldUseFJournalField());
        this.shouldEnableMscKeywordDescriptions.set(preferences.shouldEnableMscKeywordDescriptions());
    }

    public static AbbreviationPreferences getDefault() {
        return new AbbreviationPreferences();
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

    public boolean shouldEnableMscKeywordDescriptions() {
        return shouldEnableMscKeywordDescriptions.get();
    }

    public void setShouldEnableMscKeywordDescriptions(boolean value) {
        this.shouldEnableMscKeywordDescriptions.set(value);
    }

    public BooleanProperty shouldEnableMscKeywordDescriptionsProperty() {
        return shouldEnableMscKeywordDescriptions;
    }
}
