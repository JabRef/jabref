package org.jabref.gui.mergeentries.newmergedialog.cell;

import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;

public class FieldNameCellFactory {

    public static FieldNameCell create(Field field, int rowIndex, boolean groupsMerged) {
        if (field.equals(StandardField.GROUPS)) {
            return new GroupsFieldNameCell("Groups", rowIndex, groupsMerged);
        } else {
            return new FieldNameCell(field.getDisplayName(), rowIndex);
        }
    }
}
