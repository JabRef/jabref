package org.jabref.gui.mergeentries.newmergedialog.cell;

import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;

public class FieldNameCellFactory {

    public static boolean isMergeableField(Field field) {
        return field == StandardField.GROUPS;
    }

    public static FieldNameCell create(Field field, int rowIndex) {
        if (isMergeableField(field)) {
            return new MergeableFieldCell(field, rowIndex);
        } else {
            return new FieldNameCell(field.getDisplayName(), rowIndex);
        }
    }
}
