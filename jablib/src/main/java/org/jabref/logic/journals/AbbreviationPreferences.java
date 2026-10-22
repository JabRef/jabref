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
    private final BooleanProperty convertJournalOnFetch;
    private final BooleanProperty abbreviateOnFetch;
    private final ObjectProperty<AbbreviationType> abbreviationTypeOnFetch;

    public AbbreviationPreferences(List<String> externalJournalLists,
                                   boolean useFJournalField,
                                   boolean shouldEnableMscKeywordDescriptions,
                                   boolean convertJournalOnFetch,
                                   boolean abbreviateOnFetch,
                                   AbbreviationType abbreviationTypeOnFetch
    ) {
        this.externalJournalLists = FXCollections.observableArrayList(externalJournalLists);
        this.useFJournalField = new SimpleBooleanProperty(useFJournalField);
        this.shouldEnableMscKeywordDescriptions = new SimpleBooleanProperty(shouldEnableMscKeywordDescriptions);
        this.convertJournalOnFetch = new SimpleBooleanProperty(convertJournalOnFetch);
        this.abbreviateOnFetch = new SimpleBooleanProperty(abbreviateOnFetch);
        this.abbreviationTypeOnFetch = new SimpleObjectProperty<>(abbreviationTypeOnFetch);
    }

    private AbbreviationPreferences() {
        this(
                List.of(), // externalJournalLists
                true,      // useFJournalField
                false,      // Enable MSC codes as Keyword
                false,      // dont edit journal title by default
                true,      // abbreviate is on
                AbbreviationType.DEFAULT // use the default option
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

    public BooleanProperty convertJournalOnFetchProperty() {
        return convertJournalOnFetch;
    }

    public boolean shouldConvertJournalOnFetch() {
        return convertJournalOnFetch.get();
    }

    public void setConvertJournalOnFetch(boolean value) {
        this.convertJournalOnFetch.set(value);
    }

    public BooleanProperty getAbbreviateOnFetch() {
        return abbreviateOnFetch;
    }

    public boolean shouldAbbreviateOnFetch() {
        return abbreviateOnFetch.get();
    }

    public void setAbbreviateOnFetch(boolean value) {
        this.abbreviateOnFetch.set(value);
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
