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

import javax.swing.* ; 
import java.awt.* ; 
import java.util.* ; 

// 1. initially just dump out the number of matches
// 2. later, do sort and highlight, then indicating the # of matches
public class DatabaseSearch extends Thread {

    BasePanel panel = null ; 
    BibtexDatabase thisDatabase = null ; 
    SearchRuleSet thisRuleSet = null ; 
	Hashtable thisSearchOptions = null ; 
    EntryTableModel thisTableModel = null ; 
    String searchValueField = null;

    public static String
	SEARCH = "search",
	GROUPSEARCH = "groupsearch";

    
    public DatabaseSearch(Hashtable searchOptions,SearchRuleSet searchRules,
			  BasePanel panel, String searchValueField) {
        this.panel = panel; 
	thisDatabase = panel.getDatabase() ; 
        thisTableModel = panel.getTableModel() ; 
        thisSearchOptions = searchOptions; 
        thisRuleSet = searchRules ; 
	this.searchValueField = searchValueField;
    }

    public void run() {
        String searchString =  null ; 
        Enumeration strings = thisSearchOptions.elements() ; 
        searchString = (String) strings.nextElement() ; 
        int searchScore = 0 ; 


        BibtexEntry bes = null ;
//        System.out.println("doing search using this regular expr: "+searchString) ; 
        // 0. for each field in the database
        int numRows = thisDatabase.getEntryCount() ; 

	for(int row = 0 ; row < numRows ; row++){
	    // 1. search all required fields using searchString
	    
	    bes = thisDatabase.getEntryById(thisTableModel.getNameFromNumber(row));

	    // 2. add score per each hit
	    searchScore = thisRuleSet.applyRules(thisSearchOptions,bes) ; 
	    // 2.1 set score to search field               
	    bes.setField(searchValueField, String.valueOf(searchScore)) ; 
                
	    // 3. fire "sort" on "search"
	    // 4. add "search all optional fields"
	    // 5. modify algorithm to taste
              
	}
	 
	panel.showSearchResults(searchValueField);
    }

}


