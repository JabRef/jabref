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
package net.sf.jabref.groups;

import java.util.Hashtable; 
import java.util.Enumeration ; 
import java.util.regex.Pattern;
import net.sf.jabref.SearchRule;
import net.sf.jabref.BibtexEntry;

public class QuickSearchRule implements SearchRule {

    String field;
    Pattern pattern;

    public QuickSearchRule(String field, String searchString) {
	this.field = field;

        if(!searchString.matches("\\.\\*")){
            searchString = ".*"+searchString+".*" ; 
        }
	int flags = Pattern.CASE_INSENSITIVE;
	pattern = Pattern.compile(searchString, flags);
    }

    public int applyRule(Hashtable searchOptions, BibtexEntry bibtexEntry) {

        int score =0 ; 

	String content = (String)bibtexEntry.getField(field);
	if ((content != null) 
	    && (pattern.matcher(content).matches())) {
	    score = 1;
	}
        return score ; 
    }   

    /**
     * Removes matches of searchString in the entry's field.
     */
    public void removeMatches(BibtexEntry bibtexEntry) {
	
	String content = (String)bibtexEntry.getField(field);
	StringBuffer sb = new StringBuffer();
	if (content != null) {
	    String[] split = pattern.split(content);
	    for (int i=0; i<split.length; i++)
		sb.append(split[i]);
	}

	bibtexEntry.setField(field, (sb.length() > 0 ? sb.toString() : null));
    }
}

