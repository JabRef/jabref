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
import net.sf.jabref.imports.ImportFormatReader;
import java.util.*;

public class EntryTableModel
    extends AbstractTableModel {

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
      OTHER = 3,
      //PDF_COL = 1, // The column displaying icons for linked pdfs.
      ICON_COL = 8; // Constant to indicate that an icon cell renderer should be used.
  public static final String[]
      PDF = {"pdf", "ps"},
      URL_ = {"url", "doi"};

  public int padleft = -1; // padleft indicates how many columns (starting from left) are
  // special columns (number column or icon column).
  private HashMap iconCols = new HashMap();
  int[] nameCols = null;
  boolean namesAsIs, namesFf;

    //ImageIcon pdfIcon = new ImageIcon(GUIGlobals.pdfSmallIcon);

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
  }

  public String getColumnName(int col) {
    if (col == 0) {
      return GUIGlobals.NUMBER_COL;
    }
    else if (getIconTypeForColumn(col) != null) {
      return "";
    }
//	else {
    return Util.nCase(columns[col - padleft]);
    //}
  }

  public int getRowCount() {
    //Util.pr("rc "+sorter.getEntryCount());
    return sorter.getEntryCount();
    //entryIDs.length;  // Temporary?
  }

  public int getColumnCount() {
    return padleft + columns.length;
  }

  public Class getColumnClass(int column) {
    //return (getIconTypeForColumn(column) != null ? Icon.class : String.class);
    return (getIconTypeForColumn(column) != null ? JLabel.class : String.class);
  }

  public Object getValueAt(int row, int col) {
    // Return the field named frame.prefs.columnNames[col] from the Entry
    // corresponding to the row.
    Object o;
    BibtexEntry be = db.getEntryById(getNameFromNumber(row));
    String[] iconType = getIconTypeForColumn(col); // If non-null, indicates an icon column's type.
    if (col == 0) {
      o = "" + (row + 1);

    }
    else if (iconType != null) {
      int hasField = -1;
      for (int i=iconType.length-1; i>= 0; i--)
        if (hasField(row, iconType[i]))
          hasField = i;
      if (hasField < 0)
        return null;

      // Ok, so we are going to display an icon. Find out which one, and return it:
      return GUIGlobals.getTableIcon(iconType[hasField]);
    }
    //  if (col == 1)
    //  o = be.getType().getName();
    //else {
    else if (columns[col - padleft].equals(GUIGlobals.TYPE_HEADER)) {
      o = be.getType().getName();
    }
    //else if (columns[col-PADLEFT].equals(GUIGlobals.NUMBER_COL)) {
    //  o = ""+(row+1);
    //}
    else {
      o = be.getField(columns[col - padleft]);
      for (int i = 0; i < nameCols.length; i++) {
        if (col - padleft == nameCols[i]) {
          if (o == null) {
            return null;
          }
          if (namesAsIs) {
            return o;
          }
          else {
            if (namesFf) {
              return ImportFormatReader.fixAuthor( (String) o);
            }
            else {
              return ImportFormatReader.fixAuthor_lastnameFirst( (String) o);
            }
          }
        }
      }
    }
    //}

    return o;
  }

  /**
   * This method returns a string array indicating the types of icons to be displayed in the given column.
   * It returns null if the column is not an icon column, and thereby also serves to identify icon
   * columns.
   */
  public String[] getIconTypeForColumn(int col) {
    Object o = iconCols.get(new Integer(col));
    if (o != null)
      return (String[])o;
    else
      return null;
  }

  public int getCellStatus(int row, int col) {
    //if ((col == 0)  || (col == 1)) return OTHER;
    if (col == 0) {
      return OTHER;
    }
    if (getIconTypeForColumn(col) != null) {
      return ICON_COL;
    }
    BibtexEntryType type = (db.getEntryById(getNameFromNumber(row)))
        .getType();
    if (columns[col - padleft].equals(GUIGlobals.KEY_FIELD)
        || type.isRequired(columns[col - padleft])) {
      return REQUIRED;
    }
    if (type.isOptional(columns[col - padleft])) {
      return OPTIONAL;
    }
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
    String o = (String) (be.getField(field));
    return ( (o != null) && !o.equals("0"));
  }

  public boolean hasField(int row, String field) {
    // Returns true iff the entry has a nonzero value in its
    // 'search' field.
    BibtexEntry be = db.getEntryById(getNameFromNumber(row));
    return (be.getField(field) != null);
  }

  public void remap() {

    // Set the icon columns, indicating the number of special columns to the left.
    // We add those that are enabled in preferences.
    iconCols.clear();
    int coln = 1;
    if (panel.prefs.getBoolean("pdfColumn"))
      iconCols.put(new Integer(coln++), PDF);
    if (panel.prefs.getBoolean("urlColumn"))
      iconCols.put(new Integer(coln++), URL_);

    // Add 1 to the number of icon columns to get padleft.
    padleft = 1+iconCols.size();

    // Set up the int[] nameCols, to mark which columns should be
    // treated as lists of names. This is to provide a correct presentation
    // of names as efficiently as possible.
    Vector tmp = new Vector(2, 1);
    for (int i = 0; i < columns.length; i++) {
      if (columns[i].equals("author")
          || columns[i].equals("editor")) {
        tmp.add(new Integer(i));
      }
    }
    nameCols = new int[tmp.size()];
    for (int i = 0; i < nameCols.length; i++) {
      nameCols[i] = ( (Integer) tmp.elementAt(i)).intValue();
    }
    namesAsIs = panel.prefs.getBoolean("namesAsIs");
    namesFf = panel.prefs.getBoolean("namesFf");

    // Build a vector of prioritized search objectives,
    // then pick the 3 first.
    Vector fields = new Vector(5, 1),
        directions = new Vector(5, 1);

    // For testing MARKED feature:
    fields.add(Globals.MARKED);
    directions.add(new Boolean(true));

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
    if (directions.size() < 4) {
      sorter = db.getSorter(new EntryComparator(
          ( (Boolean) directions.elementAt(0)).booleanValue(),
          ( (Boolean) directions.elementAt(1)).booleanValue(),
          ( (Boolean) directions.elementAt(2)).booleanValue(),
          (String) fields.elementAt(0),
          (String) fields.elementAt(1),
          (String) fields.elementAt(2)));
    }
    else {
      sorter = db.getSorter(new EntryComparator(
          ( (Boolean) directions.elementAt(0)).booleanValue(),
          ( (Boolean) directions.elementAt(1)).booleanValue(),
          ( (Boolean) directions.elementAt(2)).booleanValue(),
          ( (Boolean) directions.elementAt(3)).booleanValue(),
          (String) fields.elementAt(0),
          (String) fields.elementAt(1),
          (String) fields.elementAt(2),
          (String) fields.elementAt(3)));

    }
  }

  public boolean isCellEditable(int row, int col) {
    if (!Globals.prefs.getBoolean("allowTableEditing"))
      return false;
    if (col < padleft) {
      return false;
    }
    // getColumnClass will throw a NullPointerException if there is no
    // entry in FieldTypes.GLOBAL_FIELD_TYPES for the column.
    try {
      if (!getColumnName(col).toLowerCase().equals(GUIGlobals.TYPE_HEADER)) {

//	    getColumnClass(col);
        return true;
      }
      else {
        return false;
      }
    }
    catch (NullPointerException ex) {
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
        if ( ( (o = be.getField(fieldName.toLowerCase())) == null)
            || ( (o != null)
                && !o.toString().equals(toSet))) {
          set = true;
        }
      }
      else if (be.getField(fieldName.toLowerCase()) != null) {
        set = true;
      }
    }
    if (set) {
      try {
        if (toSet != null) {
          (new LatexFieldFormatter()).format
              (toSet, GUIGlobals.isStandardField(fieldName));
        }

        // Store this change in the UndoManager to facilitate undo.
        Object oldVal = be.getField(fieldName.toLowerCase());
        panel.undoManager.addEdit
            (new net.sf.jabref.undo.UndoableFieldChange
             (be, fieldName.toLowerCase(), oldVal, toSet));
        // .. ok.

        be.setField(fieldName.toLowerCase(), toSet);
        panel.markBaseChanged();
        panel.updateWiewToSelected();
        //panel.updateEntryEditorIfShowing();
        // Should the table also be scheduled for repaint?
      }
      catch (IllegalArgumentException ex) {
        //frame.output("Invalid field format. Use '#' only in pairs wrapping "
        //	  +"string names.");
        frame.output("Invalid field format: " + ex.getMessage());
      }
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
    while ( (i < sorter.getEntryCount()) && (res < 0)) {
      if (name.equals(sorter.getIdAt(i))) {
        res = i;
      }
      i++;
    }
    return res;
  }

}
