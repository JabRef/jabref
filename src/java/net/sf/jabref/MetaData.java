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

import java.util.HashMap;
import java.util.Vector;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.io.Writer;
import java.io.StringReader;
import java.io.Reader;
import java.io.IOException;

public class MetaData {

    private HashMap metaData = new HashMap();
    private StringReader data;

    /**
     * The MetaData object stores all meta data sets in Vectors. To
     * ensure that the data is written correctly to string, the user
     * of a meta data Vector must simply make sure the appropriate 
     * changes are reflected in the Vector it has been passed.
     */
    public MetaData(HashMap inData) {	
	for (Iterator i=inData.keySet().iterator(); i.hasNext();) {
	    String key = (String)i.next();
	    data = new StringReader((String)inData.get(key));
	    String unit;
	    Vector orderedData = new Vector();

	    // We must allow for ; and \ in escape sequences.
	    try {
		while ((unit = getNextUnit(data)) != null) {
		    orderedData.add(unit);	           
		}
	    } catch (IOException ex) {
		System.err.println("Weird error while parsing meta data.");
	    }

	    metaData.put(key, orderedData);
	}
    }
    /**
     * The MetaData object can be constructed with no data in it.
     */
    public MetaData() {	
    }

    public Vector getData(String key) {
	return (Vector)metaData.get(key);
    }

    public void putData(String key, Vector orderedData) {
	metaData.put(key, orderedData);
    }

    public void writeMetaData(Writer out) throws IOException {
	for (Iterator i=metaData.keySet().iterator(); i.hasNext();) {
	    String key = (String)i.next();
	    Vector orderedData = (Vector)metaData.get(key);
	    out.write("@comment{"+GUIGlobals.META_FLAG+
		      key+":");
	    for (int j=0; j<orderedData.size(); j++) {
		out.write(makeEscape((String)orderedData.elementAt(j))+";");
	    }
	    out.write("}\n\n");
	}
    }

    private String getNextUnit(Reader data) throws IOException {
	int c;
	boolean escape = false, done = false;
	StringBuffer res = new StringBuffer();
	while (!done && ((c = data.read()) != -1)) {
	    if (c == '\\') {
		if (!escape)
		    escape = true;
		else {
		    escape = false;
		    res.append('\\');
		}
	    } else {
		if (c == ';') {
		    if (!escape)
			done = true;
		    else
			res.append(';');
		} else {
		    res.append((char)c);
		}
		escape = false;
	    }
	}
	if (res.length() > 0)
	    return res.toString();
	else 
	    return null;
    }

    private String makeEscape(String s) {
        StringBuffer sb = new StringBuffer();
	int c;
	for (int i=0; i<s.length(); i++) {
	    c = s.charAt(i);
	    if ((c == '\\') || (c == ';'))
		sb.append('\\');
	    sb.append((char)c);
	}
	return sb.toString();
    }

}
