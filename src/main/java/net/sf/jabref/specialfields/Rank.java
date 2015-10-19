/*  Copyright (C) 2012 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package net.sf.jabref.specialfields;

import net.sf.jabref.Globals;
import net.sf.jabref.gui.IconTheme;
import net.sf.jabref.logic.l10n.Localization;

import javax.swing.*;
import java.util.ArrayList;

public class Rank extends SpecialField {

    private static Rank INSTANCE;

    private Rank() {
        TEXT_DONE_PATTERN = "Set rank to '%0' for %1 entries";

        ArrayList<SpecialFieldValue> values = new ArrayList<SpecialFieldValue>();
        //lab.setName("i");
        values.add(new SpecialFieldValue(this, null, "clearRank", Localization.lang("Clear rank"), null, Localization.lang("No rank information")));
        // DO NOT TRANSLATE "rank1" etc. as this makes the .bib files non portable
        values.add(new SpecialFieldValue(this, "rank1", "setRank1", Localization.lang("Set rank to one star"), IconTheme.getImage("rank1"), Localization.lang("One star")));
        values.add(new SpecialFieldValue(this, "rank2", "setRank2", Localization.lang("Set rank to two stars"), IconTheme.getImage("rank2"), Localization.lang("Two stars")));
        values.add(new SpecialFieldValue(this, "rank3", "setRank3", Localization.lang("Set rank to three stars"), IconTheme.getImage("rank3"), Localization.lang("Three stars")));
        values.add(new SpecialFieldValue(this, "rank4", "setRank4", Localization.lang("Set rank to four stars"), IconTheme.getImage("rank4"), Localization.lang("Four stars")));
        values.add(new SpecialFieldValue(this, "rank5", "setRank5", Localization.lang("Set rank to five stars"), IconTheme.getImage("rank5"), Localization.lang("Five stars")));
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
        return IconTheme.getImage("ranking");
    }

    @Override
    public String getFieldName() {
        return SpecialFieldsUtils.FIELDNAME_RANKING;
    }

    @Override
    public String getToolTip() {
        return Localization.lang("Rank");
    }

    @Override
    public String getMenuString() {
        return Localization.lang("Rank");
    }

}
