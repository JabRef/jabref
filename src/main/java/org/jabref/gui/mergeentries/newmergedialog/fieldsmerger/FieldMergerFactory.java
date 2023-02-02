package org.jabref.gui.mergeentries.newmergedialog.fieldsmerger;

import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.preferences.BibEntryPreferences;

public class FieldMergerFactory {
    private final BibEntryPreferences bibEntryPreferences;

    public FieldMergerFactory(BibEntryPreferences bibEntryPreferences) {
        this.bibEntryPreferences = bibEntryPreferences;
    }

    public FieldMerger create(Field field) {
        if (field == StandardField.GROUPS) {
            return new GroupMerger();
        } else if (field == StandardField.KEYWORDS) {
            return new KeywordMerger(bibEntryPreferences);
        } else if (field == StandardField.COMMENT) {
            return new CommentMerger();
        } else if (field == StandardField.FILE) {
            return new FileMerger();
        } else {
            throw new IllegalArgumentException("No implementation found for merging the given field: " + field.getDisplayName());
        }
    }

    public static boolean canMerge(Field field) {
        return field == StandardField.GROUPS || field == StandardField.KEYWORDS || field == StandardField.COMMENT || field == StandardField.FILE;
    }
}
