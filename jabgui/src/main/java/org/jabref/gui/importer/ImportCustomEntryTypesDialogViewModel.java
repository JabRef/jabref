package org.jabref.gui.importer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.logic.preferences.CliPreferences;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntryType;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.types.EntryTypeFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImportCustomEntryTypesDialogViewModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImportCustomEntryTypesDialogViewModel.class);

    private final BibDatabaseMode mode;
    private final CliPreferences preferences;
    private final BibEntryTypesManager entryTypesManager;

    private final ObservableList<BibEntryType> newTypes = FXCollections.observableArrayList();
    private final ObservableList<BibEntryTypePrefsAndFileViewModel> differentCustomizationTypes = FXCollections.observableArrayList();

    public ImportCustomEntryTypesDialogViewModel(BibDatabaseMode mode,
                                                 List<BibEntryType> entryTypes,
                                                 CliPreferences preferences,
                                                 BibEntryTypesManager entryTypesManager) {
        this.mode = mode;
        this.preferences = preferences;
        this.entryTypesManager = entryTypesManager;

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

    /// Stores the entry types the user selected in the entry types manager and in the preferences.
    ///
    /// Both lists hold the definition **from the file**: for the different customizations, the stored
    /// customization is overwritten - otherwise the dialog would be shown again at the next start.
    /// See <https://github.com/JabRef/jabref/issues/9930>.
    ///
    /// [impl->req~import.entry-types.offered-once~1]
    public void importBibEntryTypes(List<BibEntryType> checkedUnknownEntryTypes, List<BibEntryType> checkedDifferentEntryTypes) {
        List<BibEntryType> typesToImport = new ArrayList<>(checkedUnknownEntryTypes);
        typesToImport.addAll(checkedDifferentEntryTypes);
        if (typesToImport.isEmpty()) {
            return;
        }
        typesToImport.forEach(type -> entryTypesManager.addCustomOrModifiedType(type, mode));
        preferences.storeCustomEntryTypesRepository(entryTypesManager);
    }
}
