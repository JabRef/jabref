/*
Copyright (C) 2003 Nathan Dunn, Morten O. Alver

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

import java.util.Hashtable; 
import java.util.Enumeration ; 
import java.util.regex.Pattern;

public class RegExpRule implements SearchRule{

    JabRefPreferences prefs;

    public RegExpRule(JabRefPreferences prefs) {
	this.prefs = prefs;
    }

    public int applyRule(Hashtable searchStrings,BibtexEntry bibtexEntry) {

        int score =0 ; 
        Enumeration e = searchStrings.elements() ; 

        String searchString = (String) e.nextElement() ; 
        if(!searchString.matches("\\.\\*")){
            searchString = ".*"+searchString+".*" ; 
        }
        String tempString = null ; 

	int flags = 0;
	if (!prefs.getBoolean("caseSensitiveSearch"))
	    flags = Pattern.CASE_INSENSITIVE; // testing
	Pattern pattern = Pattern.compile(searchString, flags);

	if (prefs.getBoolean("searchAll")) {
	    Object[] fields = bibtexEntry.getAllFields();
	    score += searchFields(fields, bibtexEntry, pattern);
	} else {
	    if (prefs.getBoolean("searchReq")) {
		String[] requiredField = bibtexEntry.getRequiredFields() ;
		score += searchFields(requiredField, bibtexEntry, pattern);
	    }
	    if (prefs.getBoolean("searchOpt")) {
		String[] optionalField = bibtexEntry.getOptionalFields() ;
		score += searchFields(optionalField, bibtexEntry, pattern);
	    }
	    if (prefs.getBoolean("searchGen")) {
		String[] generalField = bibtexEntry.getGeneralFields() ;
		score += searchFields(generalField, bibtexEntry, pattern);
	    }
	}

        return score ; 
    }

	protected int searchFields(Object[] fields, BibtexEntry bibtexEntry, Pattern pattern) {
	    int score = 0;
	    if (fields != null) {
		for(int i = 0 ; i < fields.length ; i++){
		    try {
			if (pattern.matcher
			    (String.valueOf(bibtexEntry.getField(fields[i].toString()))).matches()) {
			    score++;
			    //Util.pr(String.valueOf(bibtexEntry.getField(fields[i].toString())));
			}
		    }
			
		    catch(Throwable t ){
			System.err.println("Searching error: "+t) ; 
		    }
		}  
	    }
	    return score;
	}

}

