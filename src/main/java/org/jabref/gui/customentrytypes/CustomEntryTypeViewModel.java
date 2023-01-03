package org.jabref.gui.customentrytypes;

import java.util.function.Predicate;

import org.jabref.model.entry.BibEntryType;
import org.jabref.model.entry.field.Field;

public class CustomEntryTypeViewModel extends EntryTypeViewModel {

    public CustomEntryTypeViewModel(BibEntryType entryType, Predicate<Field> isMultiline) {
        super(entryType, isMultiline);
    }
}
