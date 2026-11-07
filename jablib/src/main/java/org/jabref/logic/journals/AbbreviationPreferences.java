package org.jabref.logic.journals;

import java.util.List;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class AbbreviationPreferences {
    private final ObservableList<String> externalJournalLists;
    private final BooleanProperty useFJournalField;
    private final BooleanProperty shouldEnableMscKeywordDescriptions;
    private final ObjectProperty<AbbreviationType> abbreviationTypeOnFetch;
    private final ObjectProperty<JournalMode> journalAbbreviationMode;

    public AbbreviationPreferences(List<String> externalJournalLists,
                                   boolean useFJournalField,
                                   boolean shouldEnableMscKeywordDescriptions,
                                   JournalMode journalAbbreviationMode,
                                   AbbreviationType abbreviationTypeOnFetch
    ) {
        this.externalJournalLists = FXCollections.observableArrayList(externalJournalLists);
        this.useFJournalField = new SimpleBooleanProperty(useFJournalField);
        this.shouldEnableMscKeywordDescriptions = new SimpleBooleanProperty(shouldEnableMscKeywordDescriptions);
        this.journalAbbreviationMode = new SimpleObjectProperty<>(journalAbbreviationMode);
        this.abbreviationTypeOnFetch = new SimpleObjectProperty<>(abbreviationTypeOnFetch);
    }

    private AbbreviationPreferences() {
        this(
                List.of(),               // externalJournalLists
                true,                    // useFJournalField
                false,                   // Enable MSC codes as Keyword
                JournalMode.NONE,        // Do NOT change journal title on fetch
                AbbreviationType.DEFAULT
        );
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

    public JournalMode useJournalAbbreviationMode() {
        return journalAbbreviationMode.get();
    }

    public void setJournalAbbreviationMode(JournalMode mode) {
        this.journalAbbreviationMode.set(mode);
    }

    public ObjectProperty<JournalMode> getJournalAbbreviationMode() {
        return journalAbbreviationMode;
    }

    public ObjectProperty<AbbreviationType> getAbbreviationTypeOnFetch() {
        return abbreviationTypeOnFetch;
    }

    public AbbreviationType useAbbreviationTypeOnFetch() {
        return abbreviationTypeOnFetch.get();
    }

    public void setAbbreviationTypeOnFetch(AbbreviationType value) {
        this.abbreviationTypeOnFetch.set(value);
    }
}
