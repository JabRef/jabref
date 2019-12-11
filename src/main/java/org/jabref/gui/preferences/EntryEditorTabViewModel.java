package org.jabref.gui.preferences;

import java.util.ArrayList;
import java.util.List;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.gui.DialogService;
import org.jabref.gui.autocompleter.AutoCompleteFirstNameMode;
import org.jabref.gui.autocompleter.AutoCompletePreferences;
import org.jabref.preferences.JabRefPreferences;

import static org.jabref.gui.autocompleter.AutoCompleteFirstNameMode.ONLY_ABBREVIATED;
import static org.jabref.gui.autocompleter.AutoCompleteFirstNameMode.ONLY_FULL;

public class EntryEditorTabViewModel implements PreferenceTabViewModel {

    private final BooleanProperty openOnNewEntryProperty = new SimpleBooleanProperty();
    private final BooleanProperty defaultSourceProperty = new SimpleBooleanProperty();
    private final BooleanProperty enableRelatedArticlesTabProperty = new SimpleBooleanProperty();
    private final BooleanProperty acceptRecommendationsProperty = new SimpleBooleanProperty();
    private final BooleanProperty enableLatexCitationsTabProperty = new SimpleBooleanProperty();
    private final BooleanProperty enableValidationProperty = new SimpleBooleanProperty();
    private final BooleanProperty enableAutoCompleteProperty = new SimpleBooleanProperty();
    private final StringProperty autoCompleteFieldsProperty = new SimpleStringProperty();
    private final BooleanProperty autoCompleteFirstLastProperty = new SimpleBooleanProperty();
    private final BooleanProperty autoCompleteLastFirstProperty = new SimpleBooleanProperty();
    private final BooleanProperty autoCompleteBothProperty = new SimpleBooleanProperty();
    private final BooleanProperty firstNameModeAbbreviatedProperty = new SimpleBooleanProperty();
    private final BooleanProperty firstNameModeFullProperty = new SimpleBooleanProperty();
    private final BooleanProperty firstNameModeBothProperty = new SimpleBooleanProperty();

    private AutoCompletePreferences autoCompletePreferences;

    private final DialogService dialogService;
    private final JabRefPreferences preferences;

    public EntryEditorTabViewModel(DialogService dialogService, JabRefPreferences preferences) {
        this.dialogService = dialogService;
        this.preferences = preferences;
        this.autoCompletePreferences = preferences.getAutoCompletePreferences();
    }

    @Override
    public void setValues() {
        openOnNewEntryProperty.setValue(preferences.getBoolean(JabRefPreferences.AUTO_OPEN_FORM));
        defaultSourceProperty.setValue(preferences.getBoolean(JabRefPreferences.DEFAULT_SHOW_SOURCE));
        enableRelatedArticlesTabProperty.setValue(preferences.getBoolean(JabRefPreferences.SHOW_RECOMMENDATIONS));
        acceptRecommendationsProperty.setValue(preferences.getBoolean(JabRefPreferences.ACCEPT_RECOMMENDATIONS));
        enableLatexCitationsTabProperty.setValue(preferences.getBoolean(JabRefPreferences.SHOW_LATEX_CITATIONS));
        enableValidationProperty.setValue(preferences.getBoolean(JabRefPreferences.VALIDATE_IN_ENTRY_EDITOR));
        enableAutoCompleteProperty.setValue(autoCompletePreferences.shouldAutoComplete());
        autoCompleteFieldsProperty.setValue(autoCompletePreferences.getCompleteNamesAsString());

        if (autoCompletePreferences.getOnlyCompleteFirstLast()) {
            autoCompleteFirstLastProperty.setValue(true);
        } else if (autoCompletePreferences.getOnlyCompleteLastFirst()) {
            autoCompleteLastFirstProperty.setValue(true);
        } else {
            autoCompleteBothProperty.setValue(true);
        }

        switch (autoCompletePreferences.getFirstNameMode()) {
            case ONLY_ABBREVIATED:
                firstNameModeAbbreviatedProperty.setValue(true);
                break;
            case ONLY_FULL:
                firstNameModeFullProperty.setValue(true);
                break;
            default:
                firstNameModeBothProperty.setValue(true);
                break;
        }
    }

    @Override
    public void storeSettings() {
        preferences.putBoolean(JabRefPreferences.AUTO_OPEN_FORM, openOnNewEntryProperty.getValue());
        preferences.putBoolean(JabRefPreferences.DEFAULT_SHOW_SOURCE, defaultSourceProperty.getValue());
        preferences.putBoolean(JabRefPreferences.SHOW_RECOMMENDATIONS, enableRelatedArticlesTabProperty.getValue());
        preferences.putBoolean(JabRefPreferences.ACCEPT_RECOMMENDATIONS, acceptRecommendationsProperty.getValue());
        preferences.putBoolean(JabRefPreferences.SHOW_LATEX_CITATIONS, enableLatexCitationsTabProperty.getValue());
        preferences.putBoolean(JabRefPreferences.VALIDATE_IN_ENTRY_EDITOR, enableValidationProperty.getValue());

        autoCompletePreferences.setShouldAutoComplete(enableAutoCompleteProperty.getValue());
        autoCompletePreferences.setCompleteNames(autoCompleteFieldsProperty.getValue());
        if (autoCompleteBothProperty.getValue()) {
            autoCompletePreferences.setOnlyCompleteFirstLast(false);
            autoCompletePreferences.setOnlyCompleteLastFirst(false);
        }
        else if (autoCompleteFirstLastProperty.getValue()) {
            autoCompletePreferences.setOnlyCompleteFirstLast(true);
            autoCompletePreferences.setOnlyCompleteLastFirst(false);
        }
        else {
            autoCompletePreferences.setOnlyCompleteFirstLast(false);
            autoCompletePreferences.setOnlyCompleteLastFirst(true);
        }

        if (firstNameModeAbbreviatedProperty.getValue()) {
            autoCompletePreferences.setFirstNameMode(ONLY_ABBREVIATED);
        } else if (firstNameModeFullProperty.getValue()) {
            autoCompletePreferences.setFirstNameMode(ONLY_FULL);
        } else {
            autoCompletePreferences.setFirstNameMode(AutoCompleteFirstNameMode.BOTH);
        }

        preferences.storeAutoCompletePreferences(autoCompletePreferences);
    }

    @Override
    public boolean validateSettings() {
        return true;
    }

    @Override
    public List<String> getRestartWarnings() {
        return new ArrayList<>();
    }

    public BooleanProperty openOnNewEntryProperty() { return openOnNewEntryProperty; }

    public BooleanProperty defaultSourceProperty() { return defaultSourceProperty; }

    public BooleanProperty enableRelatedArticlesTabProperty() { return enableRelatedArticlesTabProperty; }

    public BooleanProperty acceptRecommendationsProperty() { return acceptRecommendationsProperty; }

    public BooleanProperty enableLatexCitationsTabProperty() { return enableLatexCitationsTabProperty; }

    public BooleanProperty enableValidationProperty() { return enableValidationProperty; }

    public BooleanProperty enableAutoCompleteProperty() { return enableAutoCompleteProperty; }

    public StringProperty autoCompleteFieldsProperty() { return autoCompleteFieldsProperty; }

    public BooleanProperty autoCompleteFirstLastProperty() { return autoCompleteFirstLastProperty; }

    public BooleanProperty autoCompleteLastFirstProperty() { return autoCompleteLastFirstProperty; }

    public BooleanProperty autoCompleteBothProperty() { return autoCompleteBothProperty; }

    public BooleanProperty firstNameModeAbbreviatedProperty() { return firstNameModeAbbreviatedProperty; }

    public BooleanProperty firstNameModeFullProperty() { return firstNameModeFullProperty; }

    public BooleanProperty firstNameModeBothProperty() { return firstNameModeBothProperty; }
}
