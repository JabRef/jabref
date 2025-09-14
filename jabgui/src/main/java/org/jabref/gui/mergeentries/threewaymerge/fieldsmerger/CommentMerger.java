package org.jabref.gui.mergeentries.threewaymerge.fieldsmerger;

import org.jabref.logic.os.OS;

/// A merger for the {@link org.jabref.model.entry.field.StandardField#COMMENT} field
public class CommentMerger implements FieldMerger {
    @Override
    public String merge(String fieldValueA, String fieldValueB) {
        return fieldValueA + OS.NEWLINE + fieldValueB;
    }
}
