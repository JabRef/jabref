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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImportCustomEntryTypesDialogViewModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImportCustomEntryTypesDialogViewModel.class);

    private final BibDatabaseMode mode;
    private final PreferencesService preferencesService;

    private final ObservableList<BibEntryType> newTypes = FXCollections.observableArrayList();
    private final ObservableList<BibEntryTypePrefsAndFileViewModel> differentCustomizationTypes = FXCollections.observableArrayList();

    public ImportCustomEntryTypesDialogViewModel(BibDatabaseMode mode, List<BibEntryType> entryTypes, PreferencesService preferencesService) {
        this.mode = mode;
        this.preferencesService = preferencesService;

        for (BibEntryType customType : entryTypes) {
            Optional<BibEntryType> currentlyStoredType = Globals.entryTypesManager.enrich(customType.getType(), mode);
            if (currentlyStoredType.isEmpty()) {
                newTypes.add(customType);
            } else {
                if (!EntryTypeFactory.nameAndFieldsAreEqual(customType, currentlyStoredType.get())) {
                    LOGGER.info("currently stored type:    {}", currentlyStoredType.get());
                    LOGGER.info("type provided by library: {}", customType);
                    differentCustomizationTypes.add(new BibEntryTypePrefsAndFileViewModel(currentlyStoredType.get(), customType));
                }
            }
        }
    }

    public ObservableList<BibEntryType> newTypes() {
        return this.newTypes;
    }

    public ObservableList<BibEntryTypePrefsAndFileViewModel> differentCustomizations() {
        return this.differentCustomizationTypes;
    }

    public void importBibEntryTypes(List<BibEntryType> checkedUnknownEntryTypes, List<BibEntryType> checkedDifferentEntryTypes) {
        if (!checkedUnknownEntryTypes.isEmpty()) {
            checkedUnknownEntryTypes.forEach(type -> Globals.entryTypesManager.addCustomOrModifiedType(type, mode));
            preferencesService.storeCustomEntryTypesRepository(Globals.entryTypesManager);
        }
        if (!checkedDifferentEntryTypes.isEmpty()) {
            checkedUnknownEntryTypes.forEach(type -> Globals.entryTypesManager.addCustomOrModifiedType(type, mode));
            preferencesService.storeCustomEntryTypesRepository(Globals.entryTypesManager);
        }
    }
}
