/*
Copyright (C) 2003 Morten O. Alver, Nizar N. Batada

All programs in this directory and
subdirectories are published under the GNU General Public License as
described below.

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or (at
your option) any later version.

This program is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
USA

Further information about the GNU GPL is available at:
http://www.gnu.org/copyleft/gpl.ja.html

*/

package net.sf.jabref;

import java.io.*;
import javax.swing.*;
import java.awt.Point;
import java.awt.Dimension;
import java.util.prefs.*;
import java.util.*;

public class JabRefPreferences {
    
    Preferences prefs;
    HashMap defaults = new HashMap();

    public JabRefPreferences() {

	prefs = Preferences.userNodeForPackage(JabRef.class);
	//Util.pr(prefs.toString());

	defaults.put("posX", new Integer(0));
	defaults.put("posY", new Integer(0));
	defaults.put("sizeX", new Integer(840));
	defaults.put("sizeY", new Integer(680));
	defaults.put("autoResizeMode", new Integer(JTable.AUTO_RESIZE_OFF));
	defaults.put("tableColorCodesOn", new Boolean(true));
	defaults.put("language", "no");
	defaults.put("priSort", "author");
	defaults.put("priDescending", new Boolean(false));
	defaults.put("secSort", "year");
	defaults.put("secDescending", new Boolean(true));
	defaults.put("terSort", "author");
	defaults.put("terDescending", new Boolean(false));
	defaults.put("columnNames",
		     "author:title:year:journal:bibtexkey");
	defaults.put("workingDirectory", (String)null);
	
	defaults.put("autoOpenForm", new Boolean(true));
	defaults.put("entryTypeFormHeightFactor", new Integer(1));
	defaults.put("entryTypeFormWidth", new Integer(1));
	defaults.put("backup", new Boolean(true));
	defaults.put("openLastEdited", new Boolean(true));
	defaults.put("lastEdited", (String)null);
	defaults.put("stringsPosX", new Integer(0));
	defaults.put("stringsPosY", new Integer(0));
	defaults.put("stringsSizeX", new Integer(600));
	defaults.put("stringsSizeY", new Integer(400));
	defaults.put("defaultShowSource", new Boolean(false));
	defaults.put("enableSourceEditing", new Boolean(false));
	defaults.put("caseSensitiveSearch", new Boolean(false));
	defaults.put("searchReq", new Boolean(true));
	defaults.put("searchOpt", new Boolean(true));
	defaults.put("searchGen", new Boolean(true));
	defaults.put("searchAll", new Boolean(false));
	defaults.put("regExpSearch", new Boolean(true));
	defaults.put("searchPanePosX", new Integer(0));
	defaults.put("searchPanePosY", new Integer(0));
	defaults.put("autoComplete", new Boolean(true));
	defaults.put("autoCompFields", new byte[] {0, 1, 28});
	defaults.put("groupSelectorVisible", new Boolean(true));
	defaults.put("groupsDefaultField", "keywords");
	

	//defaults.put("oooWarning", new Boolean(true));
    }

    public String get(String key) {
	return prefs.get(key, (String)defaults.get(key));
    }

    public boolean getBoolean(String key) {
	return prefs.getBoolean(key, ((Boolean)defaults.get(key)).booleanValue());
    }

    public double getDouble(String key) {
	return prefs.getDouble(key, ((Double)defaults.get(key)).doubleValue());
    }

    public int getInt(String key) {
	return prefs.getInt(key, ((Integer)defaults.get(key)).intValue());
    }

    public byte[] getByteArray(String key) {
	return prefs.getByteArray(key, (byte[])defaults.get(key));
    }

    public void put(String key, String value) {
	prefs.put(key, value);
    }

    public void putBoolean(String key, boolean value) {
	prefs.putBoolean(key, value);
    }

    public void putDouble(String key, double value) {
	prefs.putDouble(key, value);
    }

    public void putInt(String key, int value) {
	prefs.putInt(key, value);
    }

    public void putByteArray(String key, byte[] value) {
	prefs.putByteArray(key, value);
    }

    public void remove(String key) {
	prefs.remove(key);
    }
}
