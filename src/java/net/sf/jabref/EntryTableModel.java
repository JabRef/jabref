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

import javax.swing.*;
import javax.swing.table.*;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import net.sf.jabref.export.LatexFieldFormatter;
import java.util.Vector;
import java.util.StringTokenizer;


public class EntryTableModel extends AbstractTableModel {


    private final String TYPE = "entrytype";

    BibtexDatabase db;
    BasePanel panel;
    JabRefFrame frame;
    String[] columns; // Contains the current column names.
    private EntrySorter sorter;
    //private Object[] entryIDs; // Temporary

    // Constants used to define how a cell should be rendered.
    public static final int REQUIRED = 1, OPTIONAL = 2,
	REQ_STRING = 1,
	REQ_NUMBER = 2,
	OPT_STRING = 3,
	OTHER = 3;

    public EntryTableModel(JabRefFrame frame_,
			   BasePanel panel_, 
			   BibtexDatabase db_) {
	panel = panel_;
	frame = frame_;
	db = db_;

	columns = panel.prefs
	    .getStringArray("columnNames"); // This must be done again if the column
	                   // preferences get changed.
						      
	remap();
	//	entryIDs = db.getKeySet().toArray(); // Temporary
    }

    public String getColumnName(int col) { 
	if (col == 0)
	    return Util.nCase(TYPE);
	else {
	    return Util.nCase(columns[col-1]); 
	}
    }

    public int getRowCount() {
	//Util.pr("rc "+sorter.getEntryCount());
	return sorter.getEntryCount();
	    //entryIDs.length;  // Temporary?
    }

    public int getColumnCount() { 
	return 1+columns.length;
    }

    public Class getColumnClass(int column) {
	if (column == 0)
	    return String.class;
	else
	    return String.class;

		/*((ObjectType)(FieldTypes.GLOBAL_FIELD_TYPES.get
				 (GUIGlobals.ALL_FIELDS
				  [(frame.prefs.getByteArray("columnNames"))[column-1]])))
				  .getValueClass();*/
    }

    public Object getValueAt(int row, int col) { 
	// Return the field named frame.prefs.columnNames[col] from the Entry
	// corresponding to the row.
	Object o;
	BibtexEntry be = db.getEntryById(getNameFromNumber(row));
	if (col == 0) 
	    o = be.getType().getName();
	else {
	    o = be.getField(columns[col-1]);
	}
	return o;
    }

    public int getCellStatus(int row, int col) {
	if (col == 0) return OTHER;
	BibtexEntryType type = (db.getEntryById(getNameFromNumber(row)))
	    .getType();
	if (columns[col-1].equals(GUIGlobals.KEY_FIELD) 
	    || type.isRequired(columns[col-1])) return REQUIRED;
	if (type.isOptional(columns[col-1])) return OPTIONAL;
	return OTHER;
    }

    public boolean isComplete(int row) {
	BibtexEntry be = db.getEntryById(getNameFromNumber(row));
	return be.hasAllRequiredFields();
    }

    public boolean hasCrossRef(int row) {
	BibtexEntry be = db.getEntryById(getNameFromNumber(row));
	return (be.getField("crossref") != null);
    }

    public boolean nonZeroField(int row, String field) {
	// Returns true iff the entry has a nonzero value in its
	// 'search' field.
	BibtexEntry be = db.getEntryById(getNameFromNumber(row));
	String o = (String)(be.getField(field));
	return ((o != null) && !o.equals("0"));
    }

    public void remap() {
	// Build a vector of prioritized search objectives,
	// then pick the 3 first.
	Vector fields = new Vector(5,1),
	    directions = new Vector(5,1);
	if (panel.showingGroup) {
	    // Group search has the highest priority if active.
	    fields.add(Globals.GROUPSEARCH);
	    directions.add(new Boolean(true));
	}
	if (panel.showingSearchResults) {
	    // Normal search has priority over regular sorting.
	    fields.add(Globals.SEARCH);
	    directions.add(new Boolean(true));
	}

	// Then the sort options:
	directions.add(new Boolean(frame.prefs.getBoolean("priDescending")));
	directions.add(new Boolean(frame.prefs.getBoolean("secDescending")));
	directions.add(new Boolean(frame.prefs.getBoolean("terDescending")));
	fields.add(frame.prefs.get("priSort"));
	fields.add(frame.prefs.get("secSort"));
	fields.add(frame.prefs.get("terSort"));

	// Then pick the three highest ranking ones, and go.
	sorter = db.getSorter(new EntryComparator(
	    ((Boolean)directions.elementAt(0)).booleanValue(),
	    ((Boolean)directions.elementAt(1)).booleanValue(),
	    ((Boolean)directions.elementAt(2)).booleanValue(),
	    (String)fields.elementAt(0),
	    (String)fields.elementAt(1),
	    (String)fields.elementAt(2)));
	/*    remapAfterSearch();
	else
	remapNormal();*/
    }

    protected void remapNormal() {
	// Changes have occured in order or entry count.
	String pri = frame.prefs.get("priSort"),
	    sec = frame.prefs.get("secSort"),
	    ter = frame.prefs.get("terSort");
	// Make a three-layered sorted view.
	sorter = db.getSorter(new EntryComparator(
	    frame.prefs.getBoolean("priDescending"),
	    frame.prefs.getBoolean("secDescending"), 
	    frame.prefs.getBoolean("terDescending"), 
	    pri, sec, ter));
	//Util.pr("jau");
    }

    protected void remapAfterSearch() {
	// Changes have occured in order or entry count.
	String pri = frame.prefs.get("priSort"),
	    sec = frame.prefs.get("secSort");
	// Make a three-layered sorted view.
	sorter = db.getSorter(new EntryComparator(
            true,
	    frame.prefs.getBoolean("priDescending"), 
	    frame.prefs.getBoolean("secDescending"), 
	    "search", pri, sec));
	//Util.pr("jau");
    }

    public boolean isCellEditable(int row, int col) {
	if (col == 0) return false;
	// getColumnClass will throw a NullPointerException if there is no
	// entry in FieldTypes.GLOBAL_FIELD_TYPES for the column.
	try {
	    getColumnClass(col);
	    return true;
	} catch (NullPointerException ex) {
	    return false;
	}
    }

    public void setValueAt(Object value, int row, int col) {
	// Called by the table cell editor when the user has edited a
	// field. From here the edited value is stored.

	BibtexEntry be = db.getEntryById(getNameFromNumber(row));
	boolean set = false;
	String toSet = null, 
	    fieldName = getColumnName(col),
	    text;
	if (value != null) {
	    text = value.toString();
	    if (text.length() > 0) {
		toSet = text;
		Object o;
		if (((o = be.getField(fieldName.toLowerCase())) == null)
		    || ((o != null)
			&& !o.toString().equals(toSet)))
		    set = true;
	    } else if (be.getField(fieldName.toLowerCase()) != null)
		set = true;
	}
	if (set) try {
	    if (toSet != null)
		(new LatexFieldFormatter()).format(toSet);

	    // Store this change in the UndoManager to facilitate undo.
	    Object oldVal = be.getField(fieldName.toLowerCase());
	    panel.undoManager.addEdit
		(new net.sf.jabref.undo.UndoableFieldChange
		 (be, fieldName.toLowerCase(), oldVal, toSet));
	    // .. ok.

	    be.setField(fieldName.toLowerCase(), toSet);
	    panel.markBaseChanged();
	    // Should the table also be scheduled for repaint?
	} catch (IllegalArgumentException ex) {
	    //frame.output("Invalid field format. Use '#' only in pairs wrapping "
	    //	  +"string names.");
	    frame.output("Invalid field format: "+ex.getMessage());
	}
    }

    public String getNameFromNumber(int number) {
	// Return the name of the Entry corresponding to the row. The
	// Entry will be retrieved from a DatabaseQuery. This is just
	// a temporary implementation.
	return sorter.getIdAt(number);
	    //entryIDs[number].toString();
    }

    public int getNumberFromName(String name) {
	// Not very fast. Intended for use only in highlighting erronous
	// entry if save fails.
	int res = -1, i = 0;
	while ((i<sorter.getEntryCount()) && (res < 0)) {
	    if (name.equals(sorter.getIdAt(i)))
		res = i;
	    i++;
	}
	return res;
    }

}
