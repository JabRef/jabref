package org.jabref.model.entry.field;

import java.util.Locale;

/// Determines whether the field is in the `org.jabref.gui.entryeditor.ImportantOptionalFieldsTab` or `org.jabref.gui.entryeditor.DetailOptionalFieldsTab` tab
/// See `#getImportantOptionalFields()` and `getDetailOptionalFields()`.
public enum FieldPriority {
    IMPORTANT,
    DETAIL;

    @Override
    public String toString() {
        return this.name().toLowerCase(Locale.ROOT);
    }
}
