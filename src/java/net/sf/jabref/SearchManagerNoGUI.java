/*  Copyright (C) 2003-2011 JabRef contributors.
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
package net.sf.jabref;

import java.lang.Integer;
import java.lang.Math;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Collection;
import java.util.Vector;

import net.sf.jabref.search.*;
import net.sf.jabref.imports.*;
/**
 *
 * @author Silberer, Zirn
 */
public class SearchManagerNoGUI {
    private String searchTerm;
    private BibtexDatabase database, base=null;
    Hashtable searchOptions = new Hashtable();
    
    public SearchManagerNoGUI(String term, BibtexDatabase dataBase) {
        searchTerm = term;
        database = dataBase;
    }
    
    public BibtexDatabase getDBfromMatches() {
        int hits = 0;
                System.out.println("search term: "+searchTerm);
        if(specifiedYears()) { 
            searchTerm = fieldYear(); 
        }

        searchOptions.put("option", searchTerm); 
        SearchRuleSet searchRules = new SearchRuleSet();
        SearchRule rule1;
        rule1 = new BasicSearch(Globals.prefs.getBoolean("caseSensitiveSearch"),
                Globals.prefs.getBoolean("regExpSearch"));
        try {
            rule1 = new SearchExpression(Globals.prefs, searchOptions);
        } catch (Exception e) {

        }
        searchRules.addRule(rule1);
        
        if (!searchRules.validateSearchStrings(searchOptions)) {
            System.out.println(Globals.lang("Search failed: illegal search expression"));
            return base;
        }
        
        Collection entries = database.getEntries();
        Vector matchEntries = new Vector();
        for (Iterator i=entries.iterator(); i.hasNext();) {
            BibtexEntry entry = (BibtexEntry) i.next();
            boolean hit = searchRules.applyRule(searchOptions, entry) > 0;
            entry.setSearchHit(hit);
            if(hit) {
                hits++;
                matchEntries.add(entry);
            }
        }
        
        if (matchEntries != null) {
            base = ImportFormatReader.createDatabase(matchEntries);
        }
        return base; 
    }//end getDBfromMatches()
    
    private boolean specifiedYears() {
        if (searchTerm.matches("year=[0-9]{4}-[0-9]{4}"))
            return true;
        return false;
    }
    
    private String fieldYear() {
        String regPt1="",regPt2="";
        String completeReg=null;
        boolean reg1Set=false, reg2Set=false; //if beginning of timeframe is BEFORE and end of timeframe is AFTER turn of the century
        String[] searchTermsToPr = searchTerm.split("=");
        String field = searchTermsToPr[0];
        String[] years = searchTermsToPr[1].split("-");
        int year1 = Integer.parseInt(years[0]);
        int year2 = Integer.parseInt(years[1]);
        
        if (year1 < 2000 && year2>=2000) { //for 199.
            regPt1 = "199+["+years[0].substring(3,4)+"-9]";
            reg1Set=true;
        } else {
            if (year1<2000) {
                regPt1 = "199+["+years[0].substring(3,4)+"-"
                     +Math.min(Integer.parseInt(years[1].substring(3,4)),9)+"]";
                reg1Set=true;     
            }
        }
        if (Integer.parseInt(years[1]) >=2000 && year1<2000) { //for 200.
            regPt2 = "200+[0-"+years[1].substring(3,4)+"]";
            reg2Set = true;
        } else {
            if (year2 >=2000) {
                regPt2 = "200+["+years[0].substring(3,4)+"-"
                     +Math.min(Integer.parseInt(years[1].substring(3,4)),9)+"]";
                reg2Set = true;
            }
        }
        if(reg1Set&&reg2Set) {
            completeReg = field+"="+regPt1+"|"+regPt2;
        } else {
            if (reg1Set) {
                completeReg=field+"="+regPt1;
            }
            if (reg2Set) {
                completeReg=field+"="+regPt2;           
            }
        }
        
        return completeReg;
    }
}
