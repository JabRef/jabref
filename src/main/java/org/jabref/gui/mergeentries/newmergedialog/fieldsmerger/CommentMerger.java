package org.jabref.gui.mergeentries.newmergedialog.fieldsmerger;

import org.jabref.gui.desktop.os.NativeDesktop;
import org.jabref.model.entry.field.StandardField;

/**
 * A merger for the {@link StandardField#COMMENT} field
 * */
public class CommentMerger implements FieldMerger {
    @Override
    public String merge(String fieldValueA, String fieldValueB) {
        return fieldValueA + NativeDesktop.NEWLINE + fieldValueB;
    }
}
