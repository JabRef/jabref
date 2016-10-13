package net.sf.jabref.gui.specialfields;

import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.logic.specialfields.SpecialField;
import net.sf.jabref.model.entry.SpecialFieldValue;

public class SpecialFieldActions {

    public static SpecialFieldAction getSpecialFieldAction(SpecialField field, SpecialFieldValue value, JabRefFrame frame) {
        return new SpecialFieldAction(frame, field, value.getFieldValue().orElse(null),
                // if field contains only one value, it has to be nulled
                // otherwise, another setting does not empty the field
                field.getValues().size() == 1,
                field.getMenuString());
    }
}
