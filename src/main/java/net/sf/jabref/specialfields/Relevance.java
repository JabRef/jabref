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

import net.sf.jabref.GUIGlobals;
import net.sf.jabref.Globals;

public class Relevance extends SpecialField {

	private static Relevance INSTANCE;
	
	public Relevance() {
		ArrayList<SpecialFieldValue> values = new ArrayList<SpecialFieldValue>();
    	// action directly set by JabRefFrame
		values.add(new SpecialFieldValue(this, "relevant", "toggleRelevance", Globals.lang("Toggle relevance"), GUIGlobals.getImage("relevant"), Globals.lang("Toggle relevance")));
		this.setValues(values);
		TEXT_DONE_PATTERN = "Toggled relevance for %0 entries";
	}

	public String getFieldName() {
		return SpecialFieldsUtils.FIELDNAME_RELEVANCE;
	}

	public static Relevance getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new Relevance();
		}
		return INSTANCE;
	}	
	
	public ImageIcon getRepresentingIcon() {
		return this.getValues().get(0).getIcon();
	}
	
	public String getToolTip() {
		return this.getValues().get(0).getToolTipText();
	}

	public String getMenuString() {
		return Globals.lang("Relevance");
	}
	
	public boolean isSingleValueField() {
		return true;
	}
}
