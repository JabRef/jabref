/*
Copyright (C) 2003 Nizar N. Batada, Morten O. Alver

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

import java.util.Comparator;

/**
 * This implementation of Comparator takes care of most of the details of sorting BibTeX entries in JabRef.
 * It is structured as a node in a linked list of comparators, where each node can contain a link to a
 * new comparator that decides the ordering (by recursion) if this one can't find a difference. The next
 * node, if any, is given at construction time, and an arbitrary number of nodes can be included.
 * If the entries are equal by this comparator, and there is no next entry, the entries' unique IDs will
 * decide the ordering. Consequently, this comparator can never return 0 unless the entries are the same
 * object.
 */
public class EntryComparator implements Comparator<BibtexEntry> {

    String sortField;
    boolean descending, binary=false, numeric;
    Comparator<BibtexEntry> next;

    public EntryComparator(boolean binary, boolean desc, String field, Comparator<BibtexEntry> next) {
        this.binary = binary;
        this.sortField = field;
        this.descending = desc;
        this.next = next;
        this.numeric = BibtexFields.isNumeric(sortField);
    }

    public EntryComparator(boolean binary, boolean desc, String field) {
        this.binary = binary;
        this.sortField = field;
        this.descending = desc;
        this.next = null;
        this.numeric = BibtexFields.isNumeric(sortField);
    }


    public int compare(BibtexEntry e1, BibtexEntry e2) throws ClassCastException {

    if (e1 == e2)
        return 0;

    //Util.pr("EntryComparator: "+e1+" : "+e2);
    Object f1 = e1.getField(sortField),
        f2 = e2.getField(sortField);

    if (binary) {
        // We just separate on set and unset fields:
        if (f1 != null)
            return (f2 == null) ? -1 :
                    (next != null ? next.compare(e1, e2) : idCompare(e1, e2));
        else
            return (f2 == null) ? (next != null ? next.compare(e1, e2) : idCompare(e1, e2))
                    : 1;
    }

    // If the field is author or editor, we rearrange names so they are
    // sorted according to last name.
    if (sortField.equals("author") || sortField.equals("editor")) {
        if (f1 != null)
        f1 = AuthorList.fixAuthorForAlphabetization((String)f1).toLowerCase();
                //ImportFormatReader.fixAuthor_lastNameFirst((String)f1);
        if (f2 != null)
        f2 = AuthorList.fixAuthorForAlphabetization((String)f2).toLowerCase();
                //ImportFormatReader.fixAuthor_lastNameFirst((String)f2);

    } else if (sortField.equals(GUIGlobals.TYPE_HEADER)) {
          // Sort by type.
          f1 = e1.getType().getName();
          f2 = e2.getType().getName();
        }
    else if (numeric) {
        try {
            Integer i1 = Integer.parseInt((String)f1);
            Integer i2 = Integer.parseInt((String)f2);
            // Ok, parsing was successful. Update f1 and f2:
            f1 = i1;
            f2 = i2;
        } catch (NumberFormatException ex) {
            // Parsing failed. Give up treating these as numbers.
            // TODO: should we check which of them failed, and sort based on that?
        }
    }

    if ((f1 == null) && (f2 == null)) return (next != null ? next.compare(e1, e2) : idCompare(e1, e2));
	if ((f1 != null) && (f2 == null)) return -1;
	if ((f1 == null) && (f2 != null)) return 1;

	int result = 0;

	//String ours = ((String)e1.getField(sortField)).toLowerCase(),
	//    theirs = ((String)e2.getField(sortField)).toLowerCase();
	if ((f1 instanceof Integer) && (f2 instanceof Integer)) {
		result = -(((Integer) f1).compareTo((Integer) f2));
	} else if (f2 instanceof Integer) {
		Integer f1AsInteger = new Integer(f1.toString());
		result = -((f1AsInteger).compareTo((Integer) f2));
	} else if (f1 instanceof Integer) {
		Integer f2AsInteger = new Integer(f2.toString());
		result = -(((Integer) f1).compareTo(f2AsInteger));
	} else {
        String ours = ((String) f1).toLowerCase(),
	    	theirs = ((String) f2).toLowerCase();
        int comp = ours.compareTo(theirs);
		result = -comp;
	}
	if (result != 0)
	    return (descending ? result : -result); // Primary sort.
	if (next != null)
	    return next.compare(e1, e2); // Secondary sort if existent.
	else {

        return idCompare(e1, e2); // If still equal, we use the unique IDs.
    }
    }

    private int idCompare(BibtexEntry b1, BibtexEntry b2) {
    return ((b1.getId())).compareTo((b2.getId()));
    }

}
