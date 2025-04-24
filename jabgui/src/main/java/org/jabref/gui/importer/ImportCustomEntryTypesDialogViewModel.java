package org.jabref.gui.importer;

import java.util.List;
import java.util.Optional;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.logic.preferences.CliPreferences;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntryType;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.types.EntryTypeFactory;

import com.airhacks.afterburner.injection.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImportCustomEntryTypesDialogViewModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImportCustomEntryTypesDialogViewModel.class);

    private final BibDatabaseMode mode;
    private final CliPreferences preferences;

    private final ObservableList<BibEntryType> newTypes = FXCollections.observableArrayList();
    private final ObservableList<BibEntryTypePrefsAndFileViewModel> differentCustomizationTypes = FXCollections.observableArrayList();

    public ImportCustomEntryTypesDialogViewModel(BibDatabaseMode mode, List<BibEntryType> entryTypes, CliPreferences preferences) {
        this.mode = mode;
        this.preferences = preferences;

        BibEntryTypesManager entryTypesManager = Injector.instantiateModelOrService(BibEntryTypesManager.class);
        for (BibEntryType customType : entryTypes) {
            Optional<BibEntryType> currentlyStoredType = entryTypesManager.enrich(customType.getType(), mode);
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
        BibEntryTypesManager entryTypesManager = Injector.instantiateModelOrService(BibEntryTypesManager.class);
        if (!checkedUnknownEntryTypes.isEmpty()) {
            checkedUnknownEntryTypes.forEach(type -> entryTypesManager.addCustomOrModifiedType(type, mode));
            preferences.storeCustomEntryTypesRepository(entryTypesManager);
        }
        if (!checkedDifferentEntryTypes.isEmpty()) {
            checkedUnknownEntryTypes.forEach(type -> entryTypesManager.addCustomOrModifiedType(type, mode));
            preferences.storeCustomEntryTypesRepository(entryTypesManager);
        }
    }
}
