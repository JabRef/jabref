package org.jabref.gui.importer;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.logic.exporter.MetaDataSerializer;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntryType;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.types.EntryTypeFactory;

import com.google.common.hash.Hashing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImportCustomEntryTypesDialogViewModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImportCustomEntryTypesDialogViewModel.class);

    private final BibDatabaseMode mode;
    private final CliPreferences preferences;
    private final BibEntryTypesManager entryTypesManager;

    private final ObservableList<BibEntryType> newTypes = FXCollections.observableArrayList();
    private final ObservableList<BibEntryTypePrefsAndFileViewModel> differentCustomizationTypes = FXCollections.observableArrayList();

    /// The decision each offered type from the file stands for, see [#decision(BibEntryType, Optional, BibDatabaseMode)]
    private final Map<BibEntryType, String> offeredDecisions = new HashMap<>();

    public ImportCustomEntryTypesDialogViewModel(BibDatabaseMode mode,
                                                 List<BibEntryType> entryTypes,
                                                 CliPreferences preferences,
                                                 BibEntryTypesManager entryTypesManager) {
        this.mode = mode;
        this.preferences = preferences;
        this.entryTypesManager = entryTypesManager;

        Set<String> declinedDecisions = preferences.getDeclinedCustomEntryTypes();
        for (BibEntryType customType : entryTypes) {
            Optional<BibEntryType> currentlyStoredType = entryTypesManager.enrich(customType.getType(), mode);
            String decision = decision(customType, currentlyStoredType, mode);
            if (declinedDecisions.contains(decision)) {
                continue;
            }
            if (currentlyStoredType.isEmpty()) {
                newTypes.add(customType);
                offeredDecisions.put(customType, decision);
            } else {
                if (!EntryTypeFactory.nameAndFieldsAreEqual(customType, currentlyStoredType.get())) {
                    LOGGER.info("currently stored type:    {}", currentlyStoredType.get());
                    LOGGER.info("type provided by library: {}", customType);
                    differentCustomizationTypes.add(new BibEntryTypePrefsAndFileViewModel(currentlyStoredType.get(), customType));
                    offeredDecisions.put(customType, decision);
                }
            }
        }
    }

    /// Identifies what the user is asked: storing the definition from the file, replacing the one stored at that time.
    /// A declined decision is not offered again - unless the definition in the file or the stored one changes.
    ///
    /// Returns a fixed-length fingerprint, because a definition can be longer than a preference value may be.
    public static String decision(BibEntryType typeFromFile, Optional<BibEntryType> storedType, BibDatabaseMode mode) {
        String decision = mode.getAsString() + ": " + signature(typeFromFile)
                + storedType.map(stored -> " replacing " + signature(stored)).orElse("");
        return Hashing.sha256().hashString(decision, StandardCharsets.UTF_8).toString();
    }

    /// Everything [EntryTypeFactory#nameAndFieldsAreEqual(BibEntryType, BibEntryType)] compares, including which optional fields are detail fields
    private static String signature(BibEntryType entryType) {
        return MetaDataSerializer.serializeCustomEntryTypesV2(entryType)
                + " detail[" + FieldFactory.serializeFieldsListV2(entryType.getDetailOptionalFields()) + "]";
    }

    public ObservableList<BibEntryType> newTypes() {
        return this.newTypes;
    }

    public ObservableList<BibEntryTypePrefsAndFileViewModel> differentCustomizations() {
        return this.differentCustomizationTypes;
    }

    /// Stores the entry types the user selected in the entry types manager and in the preferences,
    /// and remembers the ones left unchecked as declined.
    ///
    /// Both lists hold the definition **from the file**: for the different customizations, the stored
    /// customization is overwritten - otherwise the dialog would be shown again at the next start.
    /// See <https://github.com/JabRef/jabref/issues/9930>.
    ///
    /// [impl->req~import.entry-types.offered-once~1]
    public void importBibEntryTypes(List<BibEntryType> checkedUnknownEntryTypes, List<BibEntryType> checkedDifferentEntryTypes) {
        List<BibEntryType> typesToImport = new ArrayList<>(checkedUnknownEntryTypes);
        typesToImport.addAll(checkedDifferentEntryTypes);

        List<String> declined = offeredDecisions.entrySet().stream()
                                                .filter(offered -> !typesToImport.contains(offered.getKey()))
                                                .map(Map.Entry::getValue)
                                                .toList();
        if (!declined.isEmpty()) {
            preferences.addDeclinedCustomEntryTypes(declined);
        }

        if (!typesToImport.isEmpty()) {
            typesToImport.forEach(type -> entryTypesManager.addCustomOrModifiedType(type, mode));
            preferences.storeCustomEntryTypesRepository(entryTypesManager);
        }
    }
}
