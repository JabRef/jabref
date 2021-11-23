package org.jabref.gui.libraryproperties;

import javafx.beans.property.ReadOnlyListWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.gui.libraryproperties.constants.ConstantsPropertiesView;
import org.jabref.gui.libraryproperties.general.GeneralPropertiesView;
import org.jabref.gui.libraryproperties.keypattern.KeyPatternPropertiesView;
import org.jabref.gui.libraryproperties.saving.SavingPropertiesView;
import org.jabref.model.database.BibDatabaseContext;

public class LibraryPropertiesViewModel {

    private final ObservableList<PropertiesTab> propertiesTabs;

    public LibraryPropertiesViewModel(BibDatabaseContext databaseContext) {

        propertiesTabs = FXCollections.observableArrayList(
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

    public ObservableList<PropertiesTab> getPropertiesTabs() {
        return new ReadOnlyListWrapper<>(propertiesTabs);
    }
}
