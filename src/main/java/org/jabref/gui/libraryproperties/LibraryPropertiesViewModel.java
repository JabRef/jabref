package org.jabref.gui.libraryproperties;

import java.util.List;

import org.jabref.gui.libraryproperties.constants.ConstantsPropertiesView;
import org.jabref.gui.libraryproperties.general.GeneralPropertiesView;
import org.jabref.gui.libraryproperties.keypattern.KeyPatternPropertiesView;
import org.jabref.gui.libraryproperties.saving.SavingPropertiesView;
import org.jabref.model.database.BibDatabaseContext;

public class LibraryPropertiesViewModel {

    private final List<PropertiesTab> propertiesTabs;

    public LibraryPropertiesViewModel(BibDatabaseContext databaseContext) {
        propertiesTabs = List.of(
                new GeneralPropertiesView(databaseContext),
                new SavingPropertiesView(databaseContext),
                new ConstantsPropertiesView(databaseContext),
                new KeyPatternPropertiesView(databaseContext)
        );
    }

    public void setValues() {
        for (PropertiesTab propertiesTab : propertiesTabs) {
            propertiesTab.setValues();
        }
    }

    public void storeAllSettings() {
        for (PropertiesTab propertiesTab : propertiesTabs) {
            propertiesTab.storeSettings();
        }
    }

    public List<PropertiesTab> getPropertiesTabs() {
        return propertiesTabs;
    }
}
