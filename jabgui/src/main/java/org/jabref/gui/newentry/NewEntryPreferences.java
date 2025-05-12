package org.jabref.gui.newentry;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.model.entry.types.EntryType;

public class NewEntryPreferences {
    private final ObjectProperty<NewEntryDialogTab> latestApproach;
    private final BooleanProperty typesRecommendedExpanded;
    private final BooleanProperty typesOtherExpanded;
    private final BooleanProperty typesCustomExpanded;
    private final ObjectProperty<EntryType> latestImmediateType;
    private final BooleanProperty idLookupGuessing;
    private final StringProperty latestIdFetcherName;
    private final StringProperty latestInterpretParserName;

    public NewEntryPreferences(NewEntryDialogTab approach,
                               boolean expandRecommended,
                               boolean expandOther,
                               boolean expandCustom,
                               EntryType immediateType,
                               boolean idLookupGuessing,
                               String idFetcherName,
                               String interpretParserName) {
        this.latestApproach = new SimpleObjectProperty<>(approach);
        this.typesRecommendedExpanded = new SimpleBooleanProperty(expandRecommended);
        this.typesOtherExpanded = new SimpleBooleanProperty(expandOther);
        this.typesCustomExpanded = new SimpleBooleanProperty(expandCustom);
        this.latestImmediateType = new SimpleObjectProperty<>(immediateType);
        this.idLookupGuessing = new SimpleBooleanProperty(idLookupGuessing);
        this.latestIdFetcherName = new SimpleStringProperty(idFetcherName);
        this.latestInterpretParserName = new SimpleStringProperty(interpretParserName);
    }

    public NewEntryDialogTab getLatestApproach() {
        return latestApproach.get();
    }

    public void setLatestApproach(NewEntryDialogTab approach) {
        latestApproach.set(approach);
    }

    public ObjectProperty<NewEntryDialogTab> latestApproachProperty() {
        return latestApproach;
    }

    public boolean getTypesRecommendedExpanded() {
        return typesRecommendedExpanded.get();
    }

    public void getTypesRecommendedExpanded(boolean expanded) {
        typesRecommendedExpanded.set(expanded);
    }

    public BooleanProperty typesRecommendedExpandedProperty() {
        return typesRecommendedExpanded;
    }

    public boolean getTypesOtherExpanded() {
        return typesOtherExpanded.get();
    }

    public void getTypesOtherExpanded(boolean expanded) {
        typesOtherExpanded.set(expanded);
    }

    public BooleanProperty typesOtherExpandedProperty() {
        return typesOtherExpanded;
    }

    public boolean getTypesCustomExpanded() {
        return typesCustomExpanded.get();
    }

    public void getTypesCustomExpanded(boolean expanded) {
        typesCustomExpanded.set(expanded);
    }

    public BooleanProperty typesCustomExpandedProperty() {
        return typesCustomExpanded;
    }

    public EntryType getLatestImmediateType() {
        return latestImmediateType.get();
    }

    public void setLatestImmediateType(EntryType type) {
        latestImmediateType.set(type);
    }

    public ObjectProperty<EntryType> latestImmediateTypeProperty() {
        return latestImmediateType;
    }

    public boolean getIdLookupGuessing() {
        return idLookupGuessing.get();
    }

    public void setIdLookupGuessing(boolean guessing) {
        idLookupGuessing.set(guessing);
    }

    public BooleanProperty idLookupGuessingProperty() {
        return idLookupGuessing;
    }

    public String getLatestIdFetcher() {
        return latestIdFetcherName.get();
    }

    public void setLatestIdFetcher(String idFetcherName) {
        latestIdFetcherName.set(idFetcherName);
    }

    public StringProperty latestIdFetcherProperty() {
        return latestIdFetcherName;
    }

    public String getLatestInterpretParser() {
        return latestInterpretParserName.get();
    }

    public void setLatestInterpretParser(String interpretParserName) {
        latestInterpretParserName.set(interpretParserName);
    }

    public StringProperty latestInterpretParserProperty() {
        return latestInterpretParserName;
    }
}
