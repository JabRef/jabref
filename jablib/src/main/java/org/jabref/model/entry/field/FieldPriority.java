package org.jabref.model.entry.field;

import java.util.Locale;

/**
 * Determines whether the field is in the {@link org.jabref.gui.entryeditor.ImportantOptionalFieldsTab} or {@link org.jabref.gui.entryeditor.DetailOptionalFieldsTab} tab
 *
 * See {@link org.jabref.model.entry.BibEntryType#getImportantOptionalFields()}
 * and {@link org.jabref.model.entry.BibEntryType#getDetailOptionalFields()}.
 */
public enum FieldPriority {
    IMPORTANT,
    DETAIL;

    @Override
    public String toString() {
        return this.name().toLowerCase(Locale.ROOT);
    }
}
