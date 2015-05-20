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

public class ReadStatus extends SpecialField {
	
	private static ReadStatus INSTANCE = null;
	
	private ImageIcon icon = new ImageIcon(GUIGlobals.getIconUrl("readstatus"));
	
	public ReadStatus() {
		ArrayList<SpecialFieldValue> values = new ArrayList<SpecialFieldValue>();
		values.add(new SpecialFieldValue(this, Globals.lang("null"), "clearReadStatus", Globals.lang("Clear read status"), null, Globals.lang("No read status information")));
		ImageIcon icon;
		icon = GUIGlobals.getImage("readStatusRead");
		values.add(new SpecialFieldValue(this, Globals.lang("read"), "setReadStatusToRead", Globals.lang("Set read status to read"), icon, Globals.lang("Read status read")));
		icon = GUIGlobals.getImage("readStatusSkimmed");
		values.add(new SpecialFieldValue(this, Globals.lang("skimmed"), "setReadStatusToSkimmed", Globals.lang("Set read status to skimmed"), icon, Globals.lang("Read status skimmed")));
		this.setValues(values);
		TEXT_DONE_PATTERN = "Set read status '%0' for %1 entries";
	}
	
	public static ReadStatus getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new ReadStatus();
		}
		return INSTANCE;
	}
	
	public String getFieldName() {
		return SpecialFieldsUtils.FIELDNAME_READ;
	}
	
	public ImageIcon getRepresentingIcon() {
		return this.icon;
	}
	
	public String getToolTip() {
		return Globals.lang("Read status");
	}
	
	public String getMenuString() {
		return Globals.lang("Read status");
	}
}
