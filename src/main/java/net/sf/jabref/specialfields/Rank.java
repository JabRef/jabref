package net.sf.jabref.specialfields;

import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;

import net.sf.jabref.gui.IconTheme;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.SpecialFields;

public class Rank extends SpecialField {

    private static Rank INSTANCE;

    private Rank() {
        List<SpecialFieldValue> values = new ArrayList<>();
        // lab.setName("i");
        values.add(new SpecialFieldValue(this, null, "clearRank", Localization.lang("Clear rank"), null,
                Localization.lang("No rank information")));
        // DO NOT TRANSLATE "rank1" etc. as this makes the .bib files non portable
        values.add(new SpecialFieldValue(this, "rank1", "setRank1", "", IconTheme.JabRefIcon.RANK1.getSmallIcon(), Localization.lang("One star")));
        values.add(new SpecialFieldValue(this, "rank2", "setRank2", "", IconTheme.JabRefIcon.RANK2.getSmallIcon(), Localization.lang("Two stars")));
        values.add(new SpecialFieldValue(this, "rank3", "setRank3", "", IconTheme.JabRefIcon.RANK3.getSmallIcon(), Localization.lang("Three stars")));
        values.add(new SpecialFieldValue(this, "rank4", "setRank4", "", IconTheme.JabRefIcon.RANK4.getSmallIcon(), Localization.lang("Four stars")));
        values.add(new SpecialFieldValue(this, "rank5", "setRank5", "", IconTheme.JabRefIcon.RANK5.getSmallIcon(), Localization.lang("Five stars")));
        this.setValues(values);
    }

    public static Rank getInstance() {
        if (Rank.INSTANCE == null) {
            Rank.INSTANCE = new Rank();
        }
        return Rank.INSTANCE;
    }

    @Override
    public Icon getRepresentingIcon() {
        return IconTheme.JabRefIcon.RANKING.getIcon();
    }

    @Override
    public String getFieldName() {
        return SpecialFields.FIELDNAME_RANKING;
    }

    @Override public String getLocalizedFieldName() {
        return Localization.lang("Rank");
    }
}
