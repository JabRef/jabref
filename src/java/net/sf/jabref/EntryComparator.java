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
import net.sf.jabref.imports.ImportFormatReader;

public class EntryComparator implements Comparator {

    String key, sortField;
    boolean descending;
    EntryComparator next;

    public EntryComparator(boolean priDesc, boolean secDesc, boolean terDesc, boolean fourthDesc,
                           String pri, String sec, String ter, String fourth) {
        // This constructor creates a three-layered sort rule.
        next = new EntryComparator(secDesc, terDesc, fourthDesc, sec, ter, fourth); // Secondary sort field.
        descending = priDesc;
        sortField = pri;

    }

    public EntryComparator(boolean priDesc, boolean secDesc, boolean terDesc, String pri, String sec, String ter) {
        // This constructor creates a three-layered sort rule.
        next = new EntryComparator(secDesc, terDesc, sec, ter); // Secondary sort field.
        descending = priDesc;
        sortField = pri;
    }

    public EntryComparator(boolean priDesc, boolean secDesc, String pri, String sec) {
	// This constructor creates a two-layered sort rule.
	next = new EntryComparator(secDesc, sec); // Secondary sort field.
	descending = priDesc;
	sortField = pri;
    }

    public EntryComparator(boolean priDesc, String pri) {
	descending = priDesc;
	sortField = pri;
    }

    public int compare(Object o1, Object o2) throws ClassCastException {
      //if (o1 == null) Util.pr("o1 == null");
     //if (o2 == null) Util.pr("o2 == null");

	/*  The explicit instanceof test is unnecessary, since the 
	    explicit casts below will throw ClassCastException anyway 
	    if there is trouble.

	if (!(o1 instanceof BibtexEntry) || !(o2 instanceof BibtexEntry))
	  throw new ClassCastException("Trouble comparing objects: "+o1.toString()+"\n\n"+o2.toString());*/

	BibtexEntry e1 = (BibtexEntry)o1,
	    e2 = (BibtexEntry)o2;

	if (e1 == e2)
	    return 0;

	//Util.pr("EntryComparator: "+e1+" : "+e2);
	Object f1 = e1.getField(sortField),
	    f2 = e2.getField(sortField);
	
	// If the field is author or editor, we rearrange names so they are
	// sorted according to last name.
	if (sortField.equals("author") || sortField.equals("editor")) {
	    if (f1 != null)
		f1 = ImportFormatReader.fixAuthor_lastnameFirst((String)f1);
	    if (f2 != null)
		f2 = ImportFormatReader.fixAuthor_lastnameFirst((String)f2);
	} else if (sortField.equals(GUIGlobals.TYPE_HEADER)) {
          // Sort by type.
          f1 = e1.getType().getName();
          f2 = e2.getType().getName();
        }

	if ((f1 == null) && (f2 == null)) return (next != null ? next.compare(o1, o2) : idCompare(e1, e2));
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
	    return next.compare(o1, o2); // Secondary sort if existent.
	else
	    return idCompare(e1, e2); // If still equal, we use the unique IDs.
    }

    private int idCompare(BibtexEntry b1, BibtexEntry b2) {
	return ((String)(b1.getId())).compareTo((String)(b2.getId()));
    }

}
