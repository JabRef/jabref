package org.jabref.gui.importer;

import java.util.List;
import java.util.Optional;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.gui.Globals;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntryType;
import org.jabref.model.entry.types.EntryTypeFactory;
import org.jabref.preferences.PreferencesService;

public class ImportCustomEntryTypesDialogViewModel {

    private final BibDatabaseMode mode;
    private final PreferencesService preferencesService;

    private final ObservableList<BibEntryType> newTypes = FXCollections.observableArrayList();
    private final ObservableList<BibEntryType> differentCustomizationTypes = FXCollections.observableArrayList();

    public ImportCustomEntryTypesDialogViewModel(BibDatabaseMode mode, List<BibEntryType> entryTypes, PreferencesService preferencesService) {
        this.mode = mode;
        this.preferencesService = preferencesService;

        for (BibEntryType customType : entryTypes) {
            Optional<BibEntryType> currentlyStoredType = Globals.entryTypesManager.enrich(customType.getType(), mode);
            if (currentlyStoredType.isEmpty()) {
                newTypes.add(customType);
            } else {
                if (!EntryTypeFactory.isEqualNameAndFieldBased(customType, currentlyStoredType.get())) {
                    differentCustomizationTypes.add(customType);
                }
            }
        }
    }

    public ObservableList<BibEntryType> newTypes() {
        return this.newTypes;
    }

    public ObservableList<BibEntryType> differentCustomizations() {
        return this.differentCustomizationTypes;
    }

    public void importBibEntryTypes(List<BibEntryType> checkedUnknownEntryTypes, List<BibEntryType> checkedDifferentEntryTypes) {
        if (!checkedUnknownEntryTypes.isEmpty()) {
            checkedUnknownEntryTypes.forEach(type -> Globals.entryTypesManager.addCustomOrModifiedType(type, mode));
            preferencesService.storeCustomEntryTypes(Globals.entryTypesManager);
        }
        if (!checkedDifferentEntryTypes.isEmpty()) {
            checkedUnknownEntryTypes.forEach(type -> Globals.entryTypesManager.addCustomOrModifiedType(type, mode));
            preferencesService.storeCustomEntryTypes(Globals.entryTypesManager);
        }
    }
}
