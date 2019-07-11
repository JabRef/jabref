package org.jabref.gui.importer;

import java.util.List;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.CustomEntryType;
import org.jabref.model.entry.EntryType;
import org.jabref.model.entry.EntryTypeFactory;
import org.jabref.preferences.PreferencesService;

public class ImportCustomEntryTypesDialogViewModel {

    private final BibDatabaseMode mode;
    private final PreferencesService preferencesService;

    private final ObservableList<EntryType> newTypes = FXCollections.observableArrayList();
    private final ObservableList<EntryType> differentCustomizationTypes = FXCollections.observableArrayList();

    public ImportCustomEntryTypesDialogViewModel(BibDatabaseMode mode, List<EntryType> customEntryTypes, PreferencesService preferencesService) {
        this.mode = mode;
        this.preferencesService = preferencesService;

        for (EntryType customType : customEntryTypes) {
            if (!BibEntryTypesManager.getType(customType.getType(), mode).isPresent()) {
                newTypes.add(customType);
            } else {
                EntryType currentlyStoredType = BibEntryTypesManager.getType(customType.getType(), mode).get();
                if (!EntryTypeFactory.isEqualNameAndFieldBased(customType, currentlyStoredType)) {
                    differentCustomizationTypes.add(customType);
                }
            }
        }

    }

    public ObservableList<EntryType> newTypes() {
        return this.newTypes;
    }

    public ObservableList<EntryType> differentCustomizations() {
        return this.differentCustomizationTypes;
    }

    public void importCustomEntryTypes(List<EntryType> checkedUnknownEntryTypes, List<EntryType> checkedDifferentEntryTypes) {
        if (!checkedUnknownEntryTypes.isEmpty()) {
            checkedUnknownEntryTypes.forEach(type -> BibEntryTypesManager.addOrModifyCustomEntryType((CustomEntryType) type, mode));
            preferencesService.saveCustomEntryTypes();
        }
        if (!checkedDifferentEntryTypes.isEmpty()) {
            checkedUnknownEntryTypes.forEach(type -> BibEntryTypesManager.addOrModifyCustomEntryType((CustomEntryType) type, mode));
            preferencesService.saveCustomEntryTypes();
        }

    }
}
