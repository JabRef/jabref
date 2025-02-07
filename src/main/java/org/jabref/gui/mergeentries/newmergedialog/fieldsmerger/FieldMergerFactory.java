package org.jabref.gui.mergeentries.newmergedialog.fieldsmerger;

import org.jabref.model.entry.BibEntryPreferences;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;

public class FieldMergerFactory {
    private final BibEntryPreferences bibEntryPreferences;

    public FieldMergerFactory(BibEntryPreferences bibEntryPreferences) {
        this.bibEntryPreferences = bibEntryPreferences;
    }

    public FieldMerger create(Field field) {
        return switch (field) {
            case StandardField.GROUPS -> new GroupMerger();
            case StandardField.KEYWORDS -> new KeywordMerger(bibEntryPreferences);
            case StandardField.COMMENT -> new CommentMerger();
            case StandardField.FILE -> new FileMerger();
            case null -> throw new IllegalArgumentException("Field must not be null");
            default -> throw new IllegalArgumentException("No implementation found for merging the given field: " + field.getDisplayName());
        };
    }

    public static boolean canMerge(Field field) {
        return field == StandardField.GROUPS || field == StandardField.KEYWORDS || field == StandardField.COMMENT || field == StandardField.FILE;
    }
}
