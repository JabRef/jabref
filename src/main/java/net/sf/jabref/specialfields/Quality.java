package net.sf.jabref.specialfields;

import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;

import net.sf.jabref.gui.IconTheme;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.SpecialFields;

public class Quality extends SpecialField {

    private static Quality INSTANCE;


    private Quality() {
        List<SpecialFieldValue> values = new ArrayList<>();
        // DO NOT TRANSLATE "qualityAssured" as this makes the produced .bib files non portable
        values.add(new SpecialFieldValue(this, "qualityAssured", "toggleQualityAssured",
                Localization.lang("Toggle quality assured"), IconTheme.JabRefIcon.QUALITY_ASSURED.getSmallIcon(),
                Localization.lang("Toggle quality assured")));
        this.setValues(values);
    }

    @Override
    public String getFieldName() {
        return SpecialFields.FIELDNAME_QUALITY;
    }

    @Override
    public String getLocalizedFieldName() {
        return Localization.lang("Quality");
    }

    public static Quality getInstance() {
        if (Quality.INSTANCE == null) {
            Quality.INSTANCE = new Quality();
        }
        return Quality.INSTANCE;
    }

    @Override
    public Icon getRepresentingIcon() {
        return IconTheme.JabRefIcon.QUALITY.getSmallIcon();
    }

    @Override
    public boolean isSingleValueField() {
        return true;
    }

}
