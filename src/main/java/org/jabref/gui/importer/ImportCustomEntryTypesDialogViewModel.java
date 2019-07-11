package org.jabref.gui.importer;

import java.util.List;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntryType;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.EntryType;
import org.jabref.model.entry.EntryTypeFactory;
import org.jabref.preferences.PreferencesService;

public class ImportBibEntryTypesDialogViewModel {

    private final BibDatabaseMode mode;
    private final PreferencesService preferencesService;

    private final ObservableList<EntryType> newTypes = FXCollections.observableArrayList();
    private final ObservableList<EntryType> differentCustomizationTypes = FXCollections.observableArrayList();

    public ImportBibEntryTypesDialogViewModel(BibDatabaseMode mode, List<EntryType> BibEntryTypes, PreferencesService preferencesService) {
        this.mode = mode;
        this.preferencesService = preferencesService;

        for (EntryType customType : BibEntryTypes) {
            if (!BibEntryTypesManager.getType(customType, mode).isPresent()) {
                newTypes.add(customType);
            } else {
                BibEntryType currentlyStoredType = BibEntryTypesManager.getType(customType, mode).get();
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

    public void importBibEntryTypes(List<EntryType> checkedUnknownEntryTypes, List<EntryType> checkedDifferentEntryTypes) {
        if (!checkedUnknownEntryTypes.isEmpty()) {
            checkedUnknownEntryTypes.forEach(type -> BibEntryTypesManager.addOrModifyBibEntryType((BibEntryType) type, mode));
            preferencesService.saveBibEntryTypes();
        }
        if (!checkedDifferentEntryTypes.isEmpty()) {
            checkedUnknownEntryTypes.forEach(type -> BibEntryTypesManager.addOrModifyBibEntryType((BibEntryType) type, mode));
            preferencesService.saveBibEntryTypes();
        }

    }
}
