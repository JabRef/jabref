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
package net.sf.jabref.gui;

import net.sf.jabref.BibtexEntry;

import java.util.Comparator;

/**
 * Comparator that handles icon columns. 
 */
public class IconComparator implements Comparator<BibtexEntry> {

    private String[] fields;

    public IconComparator(String[] fields) {
        this.fields = fields;
    }

    /**
     * Replaced old Method (see below) to give this Comparator the
     * capability to sort with more levels than "HasGotOrHasNot"
     *
    public int compare(BibtexEntry e1, BibtexEntry e2) {

        for (int i=0; i<fields.length; i++) {
            String val1 = e1.getField(fields[i]),
                    val2 = e2.getField(fields[i]);
            if (val1 == null) {
                if (val2 != null)
                    return 1;
            } else {
                if (val2 == null)
                    return -1;
            }
        }
        return 0;
    }
    */
    
    /**
     * Replacement of old Method - Works slightly different
     */
    public int compare(BibtexEntry e1, BibtexEntry e2) {
    	// First get the Field Values
        for (int i=0; i<fields.length; i++) {
            String val1 = e1.getField(fields[i]),
                    val2 = e2.getField(fields[i]);
            // Try to cast the Field Values to an Integer
            // Leave at "0" if casting failed
            int v1 = 0;
            try{
            	v1 = Integer.valueOf(val1);
            }catch(Exception e){
            	
            }
            int v2 = 0;
            try{
            	v2 = Integer.valueOf(val2);
            }catch(Exception e){
            	
            }
            // Compare Values like a usual Integer
    		if (v1 < v2) {
    			return -1;
    		}else if (v1 > v2) {
    			return 1;
    		}else {
    			return 0;
    		}
        }
        return 0;
    }
}
