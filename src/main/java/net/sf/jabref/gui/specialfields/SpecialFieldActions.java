package net.sf.jabref.gui.specialfields;

import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.specialfields.SpecialFieldValue;

public class SpecialFieldActions {

    public static SpecialFieldAction getSpecialFieldAction(SpecialFieldValue value, JabRefFrame frame) {
        return new SpecialFieldAction(frame, value.getField(), value.getFieldValue().orElse(null),
                // if field contains only one value, it has to be nulled
                // otherwise, another setting does not empty the field
                value.getField().getValues().size() == 1,
                value.getField().getMenuString());
    }
}
