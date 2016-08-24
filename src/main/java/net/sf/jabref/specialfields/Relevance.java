package net.sf.jabref.specialfields;

import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;

import net.sf.jabref.gui.IconTheme;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.SpecialFields;

public class Relevance extends SpecialField {

    private static Relevance INSTANCE;


    private Relevance() {
        List<SpecialFieldValue> values = new ArrayList<>();
        // action directly set by JabRefFrame
        // DO NOT TRANSLATE "relevant" as this makes the produced .bib files non portable
        values.add(new SpecialFieldValue(this, "relevant", "toggleRelevance", Localization.lang("Toggle relevance"), IconTheme.JabRefIcon.RELEVANCE.getSmallIcon(),
                Localization.lang("Toggle relevance")));
        this.setValues(values);
    }

    @Override
    public String getFieldName() {
        return SpecialFields.FIELDNAME_RELEVANCE;
    }

    @Override
    public String getLocalizedFieldName() {
        return Localization.lang("Relevance");
    }

    public static Relevance getInstance() {
        if (Relevance.INSTANCE == null) {
            Relevance.INSTANCE = new Relevance();
        }
        return Relevance.INSTANCE;
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
