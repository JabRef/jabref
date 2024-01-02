package org.jabref.model.entry.field;

import java.util.Locale;

/**
 * Determines whether the field is in the Optional1 or Optional2 tab
 *
 * See {@link org.jabref.model.entry.BibEntryType#getPrimaryOptionalFields()}
 * and {@link org.jabref.model.entry.BibEntryType#getSecondaryOptionalFields()}.
 */
public enum FieldPriority {
    IMPORTANT,
    DETAIL;

    @Override
    public String toString() {
        return this.name().toLowerCase(Locale.ROOT);
    }
}
