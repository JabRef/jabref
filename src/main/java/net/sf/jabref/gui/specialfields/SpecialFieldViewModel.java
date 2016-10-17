package net.sf.jabref.gui.specialfields;

import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.specialfields.SpecialField;
import net.sf.jabref.model.entry.specialfields.SpecialFieldValue;

public class SpecialFieldViewModel {

    private SpecialField field;

    public SpecialFieldViewModel(SpecialField field){
        this.field = field;
    }

    public SpecialFieldAction getSpecialFieldAction(SpecialFieldValue value, JabRefFrame frame){
        return new SpecialFieldAction(frame, field, value.getFieldValue().orElse(null),
                // if field contains only one value, it has to be nulled
                // otherwise, another setting does not empty the field
                field.getValues().size() == 1,
                Localization.lang(field.getLocalizationKey()));
    }
}
