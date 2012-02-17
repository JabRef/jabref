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

import java.util.ArrayList;

import javax.swing.ImageIcon;
import javax.swing.JLabel;

import net.sf.jabref.GUIGlobals;
import net.sf.jabref.Globals;

public class Rank extends SpecialField {
	
	private static Rank INSTANCE;

	public Rank() {
		ArrayList<SpecialFieldValue> values = new ArrayList<SpecialFieldValue>();
    	//lab.setName("i");
		values.add(new SpecialFieldValue(this, null, "clearRank", Globals.lang("Clear rank"), null, Globals.lang("No rank information")));
		values.add(new SpecialFieldValue(this, "rank1", "setRank1", Globals.lang("Set rank to one star"), GUIGlobals.getImage("rank1"), Globals.lang("One star")));
		values.add(new SpecialFieldValue(this, "rank2", "setRank2", Globals.lang("Set rank to two stars"), GUIGlobals.getImage("rank2"), Globals.lang("Two stars")));
		values.add(new SpecialFieldValue(this, "rank3", "setRank3", Globals.lang("Set rank to three stars"), GUIGlobals.getImage("rank3"), Globals.lang("Three stars")));
		values.add(new SpecialFieldValue(this, "rank4", "setRank4", Globals.lang("Set rank to four stars"), GUIGlobals.getImage("rank4"), Globals.lang("Four stars")));
		values.add(new SpecialFieldValue(this, "rank5", "setRank5", Globals.lang("Set rank to five stars"), GUIGlobals.getImage("rank5"), Globals.lang("Five stars")));
		this.setValues(values);
		TEXT_DONE_PATTERN = "Set rank %0 for %1 entries";
	}
	
	public static Rank getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new Rank();
		}
		return INSTANCE;
	}
	
	public String getFieldName() {
		return SpecialFieldsUtils.FIELDNAME_RANKING;
	}

	public ImageIcon getRepresentingIcon() {
		return this.getValues().get(1).getIcon();
	}
	
	public String getToolTip() {
		return Globals.lang("Rank");
	}
	
	public String getMenuString() {
		return Globals.lang("Rank");
	}
	
}
