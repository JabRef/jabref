package org.jabref.gui.mergeentries.newmergedialog.fieldsmerger;

import org.jabref.logic.util.OS;

public class CommentMerger implements FieldMerger {
    @Override
    public String merge(String fieldValueA, String fieldValueB) {
        return fieldValueA + OS.NEWLINE + fieldValueB;
    }
}
