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

public abstract class Rank extends SpecialField {
	
	public Rank() {
		TEXT_DONE_PATTERN = "Set rank %0 for %1 entries";
	}

	public static Rank getInstance() {
		if (Globals.prefs.getBoolean(SpecialFieldsUtils.PREF_RANKING_COMPACT)) {
			return RankCompact.getInstance();
		} else {
			return RankExtended.getInstance();
		}
	}
	
	public String getFieldName() {
		return SpecialFieldsUtils.FIELDNAME_RANKING;
	}

	public String getToolTip() {
		return Globals.lang("Rank");
	}
	
	public String getMenuString() {
		return Globals.lang("Rank");
	}
	
}
