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

public class ParserResult {

    private BibtexDatabase base;
    private HashMap metaData;
    private File file = null;
    private Vector warnings = new Vector();

    public ParserResult(BibtexDatabase base, HashMap metaData) {
	this.base = base;
	this.metaData = metaData;
    }

    public BibtexDatabase getDatabase() {
	return base;
    }

    public HashMap getMetaData() {
	return metaData;
    }

    public File getFile() {
      return file;
    }

    public void setFile(File f) {
      file = f;
    }

    /**
     * Add a parser warning.
     *
     * @param s String Warning text. Must be pretranslated.
     */
    public void addWarning(String s) {
      warnings.add(s);
    }

    public boolean hasWarnings() {
      return (warnings.size() > 0);
    }

    public String[] warnings() {
      String[] s = new String[warnings.size()];
      for (int i=0; i<warnings.size(); i++)
        s[i] = (String)warnings.elementAt(i);
      return s;
    }

}
