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
package net.sf.jabref.imports;

import java.util.HashMap;
import java.io.File;
import net.sf.jabref.*;
import java.util.Vector;
import java.util.ArrayList;

public class ParserResult {

    public static ParserResult INVALID_FORMAT = new ParserResult(null, null, null);
    private BibtexDatabase base;
    private HashMap metaData, entryTypes;
    private File file = null;
    private ArrayList warnings = new ArrayList();
    private String encoding = null; // Which encoding was used?
    private boolean toOpenTab = false;

    public ParserResult(BibtexDatabase base, HashMap metaData, HashMap entryTypes) {
	this.base = base;
	this.metaData = metaData;
	this.entryTypes = entryTypes;
    }

    /**
     * Check if this base is marked to be added to the currently open tab. Default is false.
     * @return
     */
    public boolean toOpenTab() {
        return toOpenTab;
    }

    public void setToOpenTab(boolean toOpenTab) {
        this.toOpenTab = toOpenTab;
    }

    public BibtexDatabase getDatabase() {
	return base;
    }

    public HashMap getMetaData() {
	return metaData;
    }

    public HashMap getEntryTypes() {
	return entryTypes;
    }

    public File getFile() {
      return file;
    }

    public void setFile(File f) {
      file = f;
    }

    /**
     * Sets the variable indicating which encoding was used during parsing.
     *
     * @param enc String the name of the encoding.
     */
    public void setEncoding(String enc) {
      encoding = enc;
    }

    /**
     * Returns the name of the encoding used during parsing, or null if not specified
     * (indicates that prefs.get("defaultEncoding") was used).
     */
    public String getEncoding() {
      return encoding;
    }

    /**
     * Add a parser warning.
     *
     * @param s String Warning text. Must be pretranslated. Only added if there isn't already a dupe.
     */
    public void addWarning(String s) {
        if (!warnings.contains(s))
            warnings.add(s);
    }

    public boolean hasWarnings() {
      return (warnings.size() > 0);
    }

    public String[] warnings() {
      String[] s = new String[warnings.size()];
      for (int i=0; i<warnings.size(); i++)
        s[i] = (String)warnings.get(i);
      return s;
    }

}
