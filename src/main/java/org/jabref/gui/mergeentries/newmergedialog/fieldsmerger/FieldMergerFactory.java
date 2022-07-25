package org.jabref.gui.mergeentries.newmergedialog.fieldsmerger;

import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.preferences.PreferencesService;

public class FieldMergerFactory {
    private final PreferencesService preferencesService;

    public FieldMergerFactory(PreferencesService preferencesService) {
        this.preferencesService = preferencesService;
    }

    public FieldMerger create(Field field) {
        if (field.equals(StandardField.GROUPS)) {
            return new GroupMerger();
        } else if (field.equals(StandardField.KEYWORDS)) {
            return new KeywordMerger(preferencesService);
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
