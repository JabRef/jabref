package org.jabref.gui.importer;

import java.util.List;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.model.EntryTypes;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.CustomEntryType;
import org.jabref.model.entry.EntryType;
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
            if (!EntryTypes.getType(customType.getName(), mode).isPresent()) {
                newTypes.add(customType);
            } else {
                EntryType currentlyStoredType = EntryTypes.getType(customType.getName(), mode).get();
                if (!EntryTypes.isEqualNameAndFieldBased(customType, currentlyStoredType)) {
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
            checkedUnknownEntryTypes.forEach(type -> EntryTypes.addOrModifyCustomEntryType((CustomEntryType) type, mode));
            preferencesService.saveCustomEntryTypes();
        }
        if (!checkedDifferentEntryTypes.isEmpty()) {
            checkedUnknownEntryTypes.forEach(type -> EntryTypes.addOrModifyCustomEntryType((CustomEntryType) type, mode));
            preferencesService.saveCustomEntryTypes();
        }

    }
}
