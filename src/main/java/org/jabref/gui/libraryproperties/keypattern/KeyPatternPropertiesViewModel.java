package org.jabref.gui.libraryproperties.keypattern;

import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;

import org.jabref.gui.commonfxcontrols.CitationKeyPatternPanelItemModel;
import org.jabref.gui.commonfxcontrols.CitationKeyPatternPanelViewModel;
import org.jabref.gui.libraryproperties.PropertiesTabViewModel;
import org.jabref.logic.citationkeypattern.DatabaseCitationKeyPattern;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.preferences.PreferencesService;

public class KeyPatternPropertiesViewModel implements PropertiesTabViewModel {

    // The list and the default properties are being overwritten by the bound properties of the tableView, but to
    // prevent an NPE on storing the preferences before lazy-loading of the setValues, they need to be initialized.
    private final ListProperty<CitationKeyPatternPanelItemModel> patternListProperty = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ObjectProperty<CitationKeyPatternPanelItemModel> defaultKeyPatternProperty = new SimpleObjectProperty<>(
            new CitationKeyPatternPanelItemModel(new CitationKeyPatternPanelViewModel.DefaultEntryType(), ""));

    private final PreferencesService preferencesService;

    private final BibDatabaseContext databaseContext;

    public KeyPatternPropertiesViewModel(BibDatabaseContext databaseContext, PreferencesService preferencesService) {
        this.databaseContext = databaseContext;
        this.preferencesService = preferencesService;
    }

    @Override
    public void setValues() {
        // empty
    }

    @Override
    public void storeSettings() {
        DatabaseCitationKeyPattern newKeyPattern = new DatabaseCitationKeyPattern(preferencesService.getCitationKeyPatternPreferences().getKeyPattern());

        patternListProperty.forEach(item -> {
            String patternString = item.getPattern();
            if (!item.getEntryType().getName().equals("default")) {
                if (!patternString.trim().isEmpty()) {
                    newKeyPattern.addCitationKeyPattern(item.getEntryType(), patternString);
                }
            }
        });

        if (!defaultKeyPatternProperty.getValue().getPattern().trim().isEmpty()) {
            // we do not trim the value at the assignment to enable users to have spaces at the beginning and
            // at the end of the pattern
            newKeyPattern.setDefaultValue(defaultKeyPatternProperty.getValue().getPattern());
        }

        databaseContext.getMetaData().setCiteKeyPattern(newKeyPattern);
    }

    public ListProperty<CitationKeyPatternPanelItemModel> patternListProperty() {
        return patternListProperty;
    }

    public ObjectProperty<CitationKeyPatternPanelItemModel> defaultKeyPatternProperty() {
        return defaultKeyPatternProperty;
    }
}
