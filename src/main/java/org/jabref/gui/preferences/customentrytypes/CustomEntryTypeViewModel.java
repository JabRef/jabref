package org.jabref.gui.preferences.customentrytypes;

import java.util.function.Predicate;

import org.jabref.model.entry.BibEntryType;
import org.jabref.model.entry.field.Field;

/**
 * This class is required to check whether a delete button should be displayed at {@link org.jabref.gui.preferences.customentrytypes.CustomEntryTypesTab#setupEntryTypesTable()}
 */
public class CustomEntryTypeViewModel extends EntryTypeViewModel {

    public CustomEntryTypeViewModel(BibEntryType entryType, Predicate<Field> isMultiline) {
        super(entryType, isMultiline);
    }
}
