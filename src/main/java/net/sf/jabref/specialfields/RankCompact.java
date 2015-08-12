/**
 * Copyright (C) 2015 JabRef contributors
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.sf.jabref.specialfields;

import java.util.ArrayList;

import javax.swing.ImageIcon;

import net.sf.jabref.GUIGlobals;
import net.sf.jabref.logic.l10n.Localization;

/**
 * Compact representation of icons
 */
public class RankCompact extends Rank {

    private static RankCompact INSTANCE = null;


    private RankCompact() {
        ArrayList<SpecialFieldValue> values = new ArrayList<SpecialFieldValue>();
        //lab.setName("i");
        values.add(new SpecialFieldValue(this, null, "clearRank", Localization.lang("Clear rank"), null, Localization.lang("No rank information")));
        // DO NOT TRANSLATE "rank1" etc. as this makes the .bib files non portable
        values.add(new SpecialFieldValue(this, "rank1", "setRank1", Localization.lang("Set rank to one star"), GUIGlobals.getImage("rankc1"), Localization.lang("One star")));
        values.add(new SpecialFieldValue(this, "rank2", "setRank2", Localization.lang("Set rank to two stars"), GUIGlobals.getImage("rankc2"), Localization.lang("Two stars")));
        values.add(new SpecialFieldValue(this, "rank3", "setRank3", Localization.lang("Set rank to three stars"), GUIGlobals.getImage("rankc3"), Localization.lang("Three stars")));
        values.add(new SpecialFieldValue(this, "rank4", "setRank4", Localization.lang("Set rank to four stars"), GUIGlobals.getImage("rankc4"), Localization.lang("Four stars")));
        values.add(new SpecialFieldValue(this, "rank5", "setRank5", Localization.lang("Set rank to five stars"), GUIGlobals.getImage("rankc5"), Localization.lang("Five stars")));
        this.setValues(values);
    }

    public static RankCompact getInstance() {
        if (RankCompact.INSTANCE == null) {
            RankCompact.INSTANCE = new RankCompact();
        }
        return RankCompact.INSTANCE;
    }

    @Override
    public ImageIcon getRepresentingIcon() {
        return GUIGlobals.getImage("ranking");
    }

}
