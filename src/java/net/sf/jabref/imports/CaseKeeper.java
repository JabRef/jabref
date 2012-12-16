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
package net.sf.jabref.imports;

import java.util.Arrays;
import java.util.Comparator;

import net.sf.jabref.export.layout.LayoutFormatter;

public class CaseKeeper implements LayoutFormatter {
    
    public CaseKeeper() {
	super();
    }
    
    public String format(String text, String [] listOfWords) {
	if (text == null) {
	    return null;
        }
        Arrays.sort(listOfWords, new LengthComparator());  
        // For each word in the list
	for (int i = 0; i < listOfWords.length; i++) {
            // Add {} if the character before is a space, -, /, (, [, ", or } or if it is at the start of the string but not if it is followed by a }
	    text = text.replaceAll("(^|[- /\\[(}\"])" + listOfWords[i] + "($|[^}])","$1\\{" + listOfWords[i] + "\\}$2");
	}
	return text;
    }
    

    public String format(String text) {
	if (text == null) {
	    return null;
        }
        final CaseKeeperList list = new CaseKeeperList();
	return this.format(text,list.getAll());
    }
    

}

class LengthComparator implements Comparator<String>{
    @Override
    public int compare(String o1, String o2) {  
        if (o1.length() > o2.length()) {
            return -1;
        } else if (o1.length() == o2.length()) {
            return 0;
        }
        return 1;
    }
}