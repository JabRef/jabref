package org.jabref.gui.mergeentries.newmergedialog.cell;

import org.jabref.gui.mergeentries.newmergedialog.fieldsmerger.FieldMergerFactory;
import org.jabref.model.entry.field.Field;

public class FieldNameCellFactory {
    public static FieldNameCell create(Field field, int rowIndex) {
        if (FieldMergerFactory.canMerge(field)) {
            return new MergeableFieldCell(field, rowIndex);
        } else {
            return new FieldNameCell(field.getDisplayName(), rowIndex);
        }
    }
}
