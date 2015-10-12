package net.sf.jabref.specialfields;

import java.util.ArrayList;

import javax.swing.*;

import net.sf.jabref.gui.IconTheme;
import net.sf.jabref.logic.l10n.Localization;

/**
 * Compact representation of icons
 */
public class RankCompact extends Rank {

    private static RankCompact INSTANCE;


    private RankCompact() {
        ArrayList<SpecialFieldValue> values = new ArrayList<SpecialFieldValue>();
        //lab.setName("i");
        values.add(new SpecialFieldValue(this, null, "clearRank", Localization.lang("Clear rank"), null, Localization.lang("No rank information")));
        // DO NOT TRANSLATE "rank1" etc. as this makes the .bib files non portable
        values.add(new SpecialFieldValue(this, "rank1", "setRank1", Localization.lang("Set rank to one star"), IconTheme.getImage("rankc1"), Localization.lang("One star")));
        values.add(new SpecialFieldValue(this, "rank2", "setRank2", Localization.lang("Set rank to two stars"), IconTheme.getImage("rankc2"), Localization.lang("Two stars")));
        values.add(new SpecialFieldValue(this, "rank3", "setRank3", Localization.lang("Set rank to three stars"), IconTheme.getImage("rankc3"), Localization.lang("Three stars")));
        values.add(new SpecialFieldValue(this, "rank4", "setRank4", Localization.lang("Set rank to four stars"), IconTheme.getImage("rankc4"), Localization.lang("Four stars")));
        values.add(new SpecialFieldValue(this, "rank5", "setRank5", Localization.lang("Set rank to five stars"), IconTheme.getImage("rankc5"), Localization.lang("Five stars")));
        this.setValues(values);
    }

    public static RankCompact getInstance() {
        if (RankCompact.INSTANCE == null) {
            RankCompact.INSTANCE = new RankCompact();
        }
        return RankCompact.INSTANCE;
    }

    @Override
    public Icon getRepresentingIcon() {
        return IconTheme.getImage("ranking");
    }

}
