package net.sf.jabref.specialfields;

import java.util.ArrayList;

import javax.swing.*;

import net.sf.jabref.gui.IconTheme;
import net.sf.jabref.logic.l10n.Localization;

/**
 * Wider representation of icons
 */
public class RankExtended extends Rank {

    private static RankExtended INSTANCE;


    private RankExtended() {
        super();
        ArrayList<SpecialFieldValue> values = new ArrayList<SpecialFieldValue>();
        values.add(new SpecialFieldValue(this, null, "clearRank", Localization.lang("Clear rank"), null, Localization.lang("No rank information")));
        // DO NOT TRANSLATE "rank1" etc. as this makes the .bib files non portable
        values.add(new SpecialFieldValue(this, "rank1", "setRank1", Localization.lang("Set rank to one star"), IconTheme.getImage("rank1"), Localization.lang("One star")));
        values.add(new SpecialFieldValue(this, "rank2", "setRank2", Localization.lang("Set rank to two stars"), IconTheme.getImage("rank2"), Localization.lang("Two stars")));
        values.add(new SpecialFieldValue(this, "rank3", "setRank3", Localization.lang("Set rank to three stars"), IconTheme.getImage("rank3"), Localization.lang("Three stars")));
        values.add(new SpecialFieldValue(this, "rank4", "setRank4", Localization.lang("Set rank to four stars"), IconTheme.getImage("rank4"), Localization.lang("Four stars")));
        values.add(new SpecialFieldValue(this, "rank5", "setRank5", Localization.lang("Set rank to five stars"), IconTheme.getImage("rank5"), Localization.lang("Five stars")));
        this.setValues(values);
    }

    public static RankExtended getInstance() {
        if (RankExtended.INSTANCE == null) {
            RankExtended.INSTANCE = new RankExtended();
        }
        return RankExtended.INSTANCE;
    }

    @Override
    public Icon getRepresentingIcon() {
        return this.getValues().get(1).getIcon();
    }

}
