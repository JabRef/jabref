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
package net.sf.jabref.label;

import net.sf.jabref.*;
import java.util.Hashtable ; 


/**
 *  This class is the abstract class which contains all of the rules
 *  for making the different types of Rules.
 */
public class LabelMaker {

    public BibtexEntry applyRule(BibtexEntry newEntry){
        if(ruleTable.containsKey(newEntry.getType().getName())){
            newEntry = ((LabelRule) ruleTable.get(newEntry.getType().getName())).applyRule(newEntry) ; 
        }
        else{
			newEntry = applyDefaultRule(newEntry) ; 
        }

	// Remove all occurences of # from the key, since it's illegal.
	String key = (String)newEntry.getField(GUIGlobals.KEY_FIELD),
	    newKey = Util.checkLegalKey(key);
	if (!newKey.equals(key))
	    newEntry.setField(GUIGlobals.KEY_FIELD, newKey);
	// ...

		return newEntry ; 
    }
    
    public void setDefaultRule(LabelRule newRule) {
		defaultRule = newRule ; 
    }

    public BibtexEntry applyDefaultRule(BibtexEntry newEntry) {
        newEntry = defaultRule.applyRule(newEntry) ;  
        return newEntry ; 
    }

    
    // there should be a default rule for any type
    public void addRule(LabelRule rule,BibtexEntryType type){
       ruleTable.put(type.getName(),rule) ; 
    }

    protected LabelRule defaultRule = new ArticleLabelRule() ; 
    protected Hashtable ruleTable = new Hashtable() ; 

}


