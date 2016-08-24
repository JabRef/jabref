package net.sf.jabref.specialfields;

import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;

import net.sf.jabref.gui.IconTheme;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.SpecialFields;

public class Printed extends SpecialField {

    private static Printed INSTANCE;


    private Printed() {
        List<SpecialFieldValue> values = new ArrayList<>();
        // DO NOT TRANSLATE "printed" as this makes the produced .bib files non portable
        values.add(new SpecialFieldValue(this, "printed", "togglePrinted", Localization.lang("Toggle print status"), IconTheme.JabRefIcon.PRINTED.getSmallIcon(),
                Localization.lang("Toggle print status")));
        this.setValues(values);
    }

    @Override
    public String getFieldName() {
        return SpecialFields.FIELDNAME_PRINTED;
    }

    @Override
    public String getLocalizedFieldName() {
        return Localization.lang("Printed");
    }

    public static Printed getInstance() {
        if (Printed.INSTANCE == null) {
            Printed.INSTANCE = new Printed();
        }
        return Printed.INSTANCE;
    }

    @Override
    public Icon getRepresentingIcon() {
        return this.getValues().get(0).getIcon();
    }

    @Override
    public boolean isSingleValueField() {
        return true;
    }

}
