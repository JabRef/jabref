package org.jabref.gui.libraryproperties;

import java.util.List;

import org.jabref.gui.libraryproperties.constants.ConstantsPropertiesView;
import org.jabref.gui.libraryproperties.contentselectors.ContentSelectorView;
import org.jabref.gui.libraryproperties.general.GeneralPropertiesView;
import org.jabref.gui.libraryproperties.keypattern.KeyPatternPropertiesView;
import org.jabref.gui.libraryproperties.preamble.PreamblePropertiesView;
import org.jabref.gui.libraryproperties.saving.SavingPropertiesView;
import org.jabref.model.database.BibDatabaseContext;

public class LibraryPropertiesViewModel {

    private final List<PropertiesTab> propertiesTabs;

    public LibraryPropertiesViewModel(BibDatabaseContext databaseContext) {
        propertiesTabs = List.of(
                new GeneralPropertiesView(databaseContext),
                new SavingPropertiesView(databaseContext),
                new KeyPatternPropertiesView(databaseContext),
                new ConstantsPropertiesView(databaseContext),
                new ContentSelectorView(databaseContext),
                new PreamblePropertiesView(databaseContext)
        );
    }

    public void setValues() {
        for (PropertiesTab propertiesTab : propertiesTabs) {
            propertiesTab.setValues();
        }
    }

    public boolean storeAllSettings() {
        boolean allValid = true;

        // Loop through all properties tabs
        for (PropertiesTab propertiesTab : propertiesTabs) {
            if (!propertiesTab.validateSettings()) {
                allValid = false;  // If any validation fails, mark as invalid
                continue;
            }

            propertiesTab.storeSettings();  // If valid, store settings for this tab
        }

        return allValid;  // Return true if all tabs were valid and settings were stored, otherwise false
    }

    public List<PropertiesTab> getPropertiesTabs() {
        return propertiesTabs;
    }
}
