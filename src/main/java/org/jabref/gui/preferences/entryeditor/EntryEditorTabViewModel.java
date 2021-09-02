package org.jabref.gui.preferences.entryeditor;

import java.util.ArrayList;
import java.util.List;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.gui.autocompleter.AutoCompleteFirstNameMode;
import org.jabref.gui.autocompleter.AutoCompletePreferences;
import org.jabref.gui.entryeditor.EntryEditorPreferences;
import org.jabref.gui.preferences.PreferenceTabViewModel;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.preferences.PreferencesService;

public class EntryEditorTabViewModel implements PreferenceTabViewModel {

    private final BooleanProperty openOnNewEntryProperty = new SimpleBooleanProperty();
    private final BooleanProperty defaultSourceProperty = new SimpleBooleanProperty();
    private final BooleanProperty enableRelatedArticlesTabProperty = new SimpleBooleanProperty();
    private final BooleanProperty acceptRecommendationsProperty = new SimpleBooleanProperty();
    private final BooleanProperty enableLatexCitationsTabProperty = new SimpleBooleanProperty();
    private final BooleanProperty enableValidationProperty = new SimpleBooleanProperty();
    private final BooleanProperty allowIntegerEditionProperty = new SimpleBooleanProperty();
    private final BooleanProperty enableAutoCompleteProperty = new SimpleBooleanProperty();
    private final StringProperty autoCompleteFieldsProperty = new SimpleStringProperty();
    private final BooleanProperty autoCompleteFirstLastProperty = new SimpleBooleanProperty();
    private final BooleanProperty autoCompleteLastFirstProperty = new SimpleBooleanProperty();
    private final BooleanProperty autoCompleteBothProperty = new SimpleBooleanProperty();
    private final BooleanProperty firstNameModeAbbreviatedProperty = new SimpleBooleanProperty();
    private final BooleanProperty firstNameModeFullProperty = new SimpleBooleanProperty();
    private final BooleanProperty firstNameModeBothProperty = new SimpleBooleanProperty();

    private final PreferencesService preferencesService;
    private final EntryEditorPreferences entryEditorPreferences;
    private final AutoCompletePreferences initialAutoCompletePreferences;

    private final List<String> restartWarnings = new ArrayList<>();

    public EntryEditorTabViewModel(PreferencesService preferencesService) {
        this.preferencesService = preferencesService;
        this.entryEditorPreferences = preferencesService.getEntryEditorPreferences();
        this.initialAutoCompletePreferences = preferencesService.getAutoCompletePreferences();
    }

    @Override
    public void setValues() {
        // ToDo: Include CustomizeGeneralFieldsDialog in PreferencesDialog
        // therefore yet unused: entryEditorPreferences.getEntryEditorTabList();

        openOnNewEntryProperty.setValue(entryEditorPreferences.shouldOpenOnNewEntry());
        defaultSourceProperty.setValue(entryEditorPreferences.showSourceTabByDefault());
        enableRelatedArticlesTabProperty.setValue(entryEditorPreferences.shouldShowRecommendationsTab());
        acceptRecommendationsProperty.setValue(entryEditorPreferences.isMrdlibAccepted());
        enableLatexCitationsTabProperty.setValue(entryEditorPreferences.shouldShowLatexCitationsTab());
        enableValidationProperty.setValue(entryEditorPreferences.shouldEnableValidation());
        allowIntegerEditionProperty.setValue(entryEditorPreferences.shouldAllowIntegerEditionBibtex());

        enableAutoCompleteProperty.setValue(initialAutoCompletePreferences.shouldAutoComplete());
        autoCompleteFieldsProperty.setValue(initialAutoCompletePreferences.getCompleteNamesAsString());

        if (initialAutoCompletePreferences.getNameFormat() == AutoCompletePreferences.NameFormat.FIRST_LAST) {
            autoCompleteFirstLastProperty.setValue(true);
        } else if (initialAutoCompletePreferences.getNameFormat() == AutoCompletePreferences.NameFormat.LAST_FIRST) {
            autoCompleteLastFirstProperty.setValue(true);
        } else {
            autoCompleteBothProperty.setValue(true);
        }

        switch (initialAutoCompletePreferences.getFirstNameMode()) {
            case ONLY_ABBREVIATED -> firstNameModeAbbreviatedProperty.setValue(true);
            case ONLY_FULL -> firstNameModeFullProperty.setValue(true);
            default -> firstNameModeBothProperty.setValue(true);
        }
    }

    @Override
    public void storeSettings() {
        // entryEditorPreferences.setEntryEditorTabList();
        entryEditorPreferences.setShouldOpenOnNewEntry(openOnNewEntryProperty.getValue());
        entryEditorPreferences.setShouldShowRecommendationsTab(enableRelatedArticlesTabProperty.getValue());
        entryEditorPreferences.setIsMrdlibAccepted(acceptRecommendationsProperty.getValue());
        entryEditorPreferences.setShouldShowLatexCitationsTab(enableLatexCitationsTabProperty.getValue());
        entryEditorPreferences.setShowSourceTabByDefault(defaultSourceProperty.getValue());
        entryEditorPreferences.setEnableValidation(enableValidationProperty.getValue());
        entryEditorPreferences.setAllowIntegerEditionBibtex(allowIntegerEditionProperty.getValue());
        // entryEditorPreferences.setDividerPosition();

        // default
        AutoCompletePreferences.NameFormat nameFormat = AutoCompletePreferences.NameFormat.BOTH;
        if (autoCompleteFirstLastProperty.getValue()) {
            nameFormat = AutoCompletePreferences.NameFormat.FIRST_LAST;
        } else if (autoCompleteLastFirstProperty.getValue()) {
            nameFormat = AutoCompletePreferences.NameFormat.LAST_FIRST;
        }

        // default: AutoCompleteFirstNameMode.BOTH
        AutoCompleteFirstNameMode firstNameMode = AutoCompleteFirstNameMode.BOTH;
        if (firstNameModeAbbreviatedProperty.getValue()) {
            firstNameMode = AutoCompleteFirstNameMode.ONLY_ABBREVIATED;
        } else if (firstNameModeFullProperty.getValue()) {
            firstNameMode = AutoCompleteFirstNameMode.ONLY_FULL;
        }

        if (initialAutoCompletePreferences.shouldAutoComplete() != enableAutoCompleteProperty.getValue()) {
            if (enableAutoCompleteProperty.getValue()) {
                restartWarnings.add(Localization.lang("Auto complete enabled."));
            } else {
                restartWarnings.add(Localization.lang("Auto complete disabled."));
            }
        }

        preferencesService.storeAutoCompletePreferences(new AutoCompletePreferences(
                enableAutoCompleteProperty.getValue(),
                firstNameMode,
                nameFormat,
                FieldFactory.parseFieldList(autoCompleteFieldsProperty.getValue()),
                preferencesService.getJournalAbbreviationPreferences()));
    }

    @Override
    public List<String> getRestartWarnings() {
        return restartWarnings;
    }

    public BooleanProperty openOnNewEntryProperty() {
        return openOnNewEntryProperty;
    }

    public BooleanProperty defaultSourceProperty() {
        return defaultSourceProperty;
    }

    public BooleanProperty enableRelatedArticlesTabProperty() {
        return enableRelatedArticlesTabProperty;
    }

    public BooleanProperty acceptRecommendationsProperty() {
        return acceptRecommendationsProperty;
    }

    public BooleanProperty enableLatexCitationsTabProperty() {
        return enableLatexCitationsTabProperty;
    }

    public BooleanProperty enableValidationProperty() {
        return enableValidationProperty;
    }

    public BooleanProperty allowIntegerEditionProperty() {
        return this.allowIntegerEditionProperty;
    }

    public BooleanProperty enableAutoCompleteProperty() {
        return enableAutoCompleteProperty;
    }

    public StringProperty autoCompleteFieldsProperty() {
        return autoCompleteFieldsProperty;
    }

    public BooleanProperty autoCompleteFirstLastProperty() {
        return autoCompleteFirstLastProperty;
    }

    public BooleanProperty autoCompleteLastFirstProperty() {
        return autoCompleteLastFirstProperty;
    }

    public BooleanProperty autoCompleteBothProperty() {
        return autoCompleteBothProperty;
    }

    public BooleanProperty firstNameModeAbbreviatedProperty() {
        return firstNameModeAbbreviatedProperty;
    }

    public BooleanProperty firstNameModeFullProperty() {
        return firstNameModeFullProperty;
    }

    public BooleanProperty firstNameModeBothProperty() {
        return firstNameModeBothProperty;
    }
}
