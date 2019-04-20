package org.jabref.gui.importer;

import java.util.List;

import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.Globals;
import org.jabref.gui.customentrytypes.CustomEntryTypesManager;
import org.jabref.model.EntryTypes;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.CustomEntryType;
import org.jabref.model.entry.EntryType;

public class ImportCustomEntryTypesDialogViewModel {

    private final ListProperty<EntryType> newTypesProperty;
    private final ListProperty<EntryType> differentCustomizationsProperty;
    private final BibDatabaseMode mode;

    public ImportCustomEntryTypesDialogViewModel(BibDatabaseMode mode, List<EntryType> customEntryTypes) {
        this.mode = mode;

        ObservableList<EntryType> newTypes = FXCollections.observableArrayList();
        ObservableList<EntryType> differentCustomizationTypes = FXCollections.observableArrayList();

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

        newTypesProperty = new SimpleListProperty<>(newTypes);
        differentCustomizationsProperty = new SimpleListProperty<>(differentCustomizationTypes);

    }

    public ListProperty<EntryType> newTypesProperty() {
        return this.newTypesProperty;
    }

    public ListProperty<EntryType> differentCustomizationsProperty() {
        return this.differentCustomizationsProperty;
    }

    public void importCustomEntryTypes(List<EntryType> checkedUnknownEntryTypes, List<EntryType> checkedDifferentEntryTypes) {
        if (!checkedUnknownEntryTypes.isEmpty()) {
            checkedUnknownEntryTypes.forEach(type -> EntryTypes.addOrModifyCustomEntryType((CustomEntryType) type, mode));
            CustomEntryTypesManager.saveCustomEntryTypes(Globals.prefs);
        }
        if (!checkedDifferentEntryTypes.isEmpty()) {
            checkedUnknownEntryTypes.forEach(type -> EntryTypes.addOrModifyCustomEntryType((CustomEntryType) type, mode));
            CustomEntryTypesManager.saveCustomEntryTypes(Globals.prefs);
        }

    }
}
