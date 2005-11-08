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
import net.sf.jabref.export.LatexFieldFormatter;
import java.util.*;

public class EntryTableModel
    extends AbstractTableModel {

  BibtexDatabase db;
  BasePanel panel;
  JabRefFrame frame;
  String[] columns; // Contains the current column names.
  private EntrySorter sorter;
  private int visibleRows = 0;


  // Testing something:
  Object[][] allCache = null;

  //private Object[] entryIDs; // Temporary

  // Constants used to define how a cell should be rendered.
  public static final int REQUIRED = 1, OPTIONAL = 2,
      REQ_STRING = 1,
      REQ_NUMBER = 2,
      OPT_STRING = 3,
      OTHER = 3,
      BOOLEAN = 4,
      //PDF_COL = 1, // The column displaying icons for linked pdfs.
      ICON_COL = 8; // Constant to indicate that an icon cell renderer should be used.
  public static final String[]
      PDF = {"pdf", "ps"},
      URL_ = {"url", "doi"},
        CITESEER = {"citeseerurl"};

  public int padleft = -1; // padleft indicates how many columns (starting from left) are
  // special columns (number column or icon column).
  private HashMap iconCols = new HashMap();
  int[] nameCols = null;
  boolean showShort, namesNatbib, namesLastOnly;                               //MK:
  boolean namesAsIs, namesFf, namesLf, abbr_names;              //MK:

    //ImageIcon pdfIcon = new ImageIcon(GUIGlobals.pdfSmallIcon);

  public EntryTableModel(JabRefFrame frame_,
                         BasePanel panel_,
                         BibtexDatabase db_) {
    panel = panel_;
    frame = frame_;
    db = db_;

    columns = Globals.prefs
        .getStringArray("columnNames"); // This must be done again if the column
    // preferences get changed.

    remap();
  }

  /* This is the old getColumnName().
   * This function now returns the field name
   * with the original lower/upper case of the field name */
  public String getFieldName(int col) {
    if (col == 0) {
      return GUIGlobals.NUMBER_COL;
    }
    else if (getIconTypeForColumn(col) != null) {
      return getIconTypeForColumn(col)[0];
    }
    return columns[col - padleft];
  }

  public String getColumnName(int col) {
      if (col == 0) {
      return GUIGlobals.NUMBER_COL;
    }
    else if (getIconTypeForColumn(col) != null) {
      return "";
    }
    else if(GUIGlobals.FIELD_DISPLAYS.get(columns[col - padleft]) != null) {
        return((String) GUIGlobals.FIELD_DISPLAYS.get(columns[col - padleft]));
    }
    return Util.nCase(columns[col - padleft]);
  }

    public void showAllEntries() {
    visibleRows = sorter.getEntryCount();
    }

    public void setRowCount(int rows) {
    visibleRows = rows;
    }

  public int getRowCount() {
    //Util.pr("rc "+sorter.getEntryCount());
    //return sorter.getEntryCount();
      return visibleRows;
    //entryIDs.length;  // Temporary?
  }

  public int getColumnCount() {
    return padleft + columns.length;
  }

  public Class getColumnClass(int column) {

    //return (getIconTypeForColumn(column) != null ? Icon.class : String.class);
      if (column == 0)
      return Boolean.class;
      else
      return (getIconTypeForColumn(column) != null ? JLabel.class : String.class);
  }

  public Object getValueAt_(int row, int col) {
      return allCache[row][col];
  }

  public void updateAllCache() {
      /*long start = System.currentTimeMillis();
      int rows = getRowCount();
      int cols = getColumnCount();
      allCache = new Object[rows][cols];
      for (int row=0; row<rows; row++)
          for (int col=0; col<cols; col++)
              allCache[row][col] = getValueAt_old(row, col);
      Globals.logger("Time spent: "+(System.currentTimeMillis()-start));*/
  }

  public Object getValueAt(int row, int col) {
    // Return the field named frame.prefs.columnNames[col] from the Entry
    // corresponding to the row.
    Object o;
    BibtexEntry be = sorter.getEntryAt(row);
    String[] iconType = getIconTypeForColumn(col); // If non-null, indicates an icon column's type.
    if (col == 0) {
        o = "" + (row + 1);
    }
/*      if (!isComplete(row)) {
      	//JLabel incomplete = new JLabel("" + (row + 1),GUIGlobals.incompleteLabel.getIcon(), JLabel.RIGHT);
        //JLabel incomplete = new JLabel("" + (row + 1));
        //incomplete.setToolTipText(Globals.lang("This entry is incomplete"));
        //return incomplete;        
      } else
*/

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

    //MK:vvv
    o = null; if (showShort) o = be.getField("short"+columns[col-padleft]);   //MK:vvv
       if (o==null) {
         o = be.getField(columns[col - padleft]);
         for (int i = 0; i < nameCols.length; i++) {
           if (col - padleft == nameCols[i]) {
             if (o == null) { return null; }
             if (namesAsIs) return o;
             if (namesNatbib) o = AuthorList.fixAuthor_Natbib((String)o);
             else if (namesLastOnly) o = AuthorList.fixAuthor_lastNameOnlyCommas((String)o);
             else if (namesFf) o = AuthorList.fixAuthor_firstNameFirstCommas((String) o, abbr_names);
             else if (namesLf) o = AuthorList.fixAuthor_lastNameFirstCommas((String) o, abbr_names);

             return o;
 //            if (!namesAsIs) {
 //              if (namesFf) {
 //                return ImportFormatReader.fixAuthor_firstNameFirst( (String) o);
 //              }
 //              else {
 //                return ImportFormatReader.fixAuthor_lastNameFirst( (String)o);
 //              }
 //            }
  //MK:^^^
          }
     }
       }
    }
    /*if (o != null) {
        String processed = Globals.getCached((String)o);
        if (processed == null) {
            StringBuffer sb = new StringBuffer("");//"<html>");
            sb.append((String)o);
            //sb.append("</html>");
            processed = sb.toString();
            Globals.cache((String)o, processed);
            o = processed;
        } else
            o = processed;
        
            
    }*/
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
      return BOOLEAN;
    }
    if (getIconTypeForColumn(col) != null) {
      return ICON_COL;
    }

    BibtexEntryType type = (db.getEntryById(getIdForRow(row)))
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
    BibtexEntry be = db.getEntryById(getIdForRow(row));
    return (be != null ? be.hasAllRequiredFields() : false);
  }

  public boolean hasCrossRef(int row) {
    BibtexEntry be = db.getEntryById(getIdForRow(row));
    return (be.getField("crossref") != null);
  }

  public boolean nonZeroField(int row, String field) {
    // Returns true iff the entry has a nonzero value in its
    // 'search' field.
    BibtexEntry be = db.getEntryById(getIdForRow(row));
    if (be == null)
        return false; // TODO: JZ: I think this should never happen, but it does
    String o = (String) (be.getField(field));
    return ( (o != null) && !o.equals("0"));
  }

  public boolean hasField(int row, String field) {
    // Returns true iff the entry has a nonzero value in its
    // 'search' field.
    BibtexEntry be = db.getEntryById(getIdForRow(row));
    return ((be != null) && (be.getField(field) != null));
  }

  private void updateSorter() {

    // Set the icon columns, indicating the number of special columns to the left.
    // We add those that are enabled in preferences.
    iconCols.clear();
    int coln = 1;
    if (Globals.prefs.getBoolean("pdfColumn"))
      iconCols.put(new Integer(coln++), PDF);
    if (Globals.prefs.getBoolean("urlColumn"))
      iconCols.put(new Integer(coln++), URL_);
    if (Globals.prefs.getBoolean("citeseerColumn"))
        iconCols.put(new Integer(coln++), CITESEER);

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
    showShort = Globals.prefs.getBoolean("showShort");        //MK:
    namesNatbib = Globals.prefs.getBoolean("namesNatbib");    //MK:
    namesLastOnly = Globals.prefs.getBoolean("namesLastOnly");

    namesAsIs = Globals.prefs.getBoolean("namesAsIs");
    abbr_names = Globals.prefs.getBoolean("abbrAuthorNames"); //MK:
    namesFf = Globals.prefs.getBoolean("namesFf");
    namesLf = !(namesAsIs || namesFf || namesNatbib || namesLastOnly); // None of the above.
        //namesLastOnly = Globals.prefs.getBoolean("namesLastOnly");
    // Build a vector of prioritized search objectives,
    // then pick the 3 first.
    List fields = new ArrayList(6),
        directions = new ArrayList(6),
        binary = new ArrayList(6); // Signifies whether the sort criterion should only separate on/off or
                                    // also sort within set field values.

    // For testing MARKED feature. With this IF clause, the marked entries will only float to the top when
    // no sorting/grouping reordering is active.
    if  (!panel.sortingBySearchResults && !panel.sortingByCiteSeerResults && !panel.sortingByGroup) {
        fields.add(Globals.MARKED);
        directions.add(Boolean.TRUE);
        binary.add(Boolean.FALSE);
    }
    if (panel.sortingByGroup) {
      // Group search has the highest priority if active.
      fields.add(Globals.GROUPSEARCH);
      directions.add(Boolean.TRUE);
        binary.add(Boolean.FALSE);
    }
    if (panel.sortingBySearchResults) {
      // Normal search has priority over regular sorting.
      fields.add(Globals.SEARCH);
      directions.add(Boolean.TRUE);
        binary.add(Boolean.FALSE);
    }
    if(panel.sortingByCiteSeerResults) {
        fields.add("citeseercitationcount");
        directions.add(Boolean.TRUE);
        binary.add(Boolean.FALSE);
    }

    // Then the sort options:
    directions.add(Boolean.valueOf(frame.prefs.getBoolean("priDescending")));
    directions.add(Boolean.valueOf(frame.prefs.getBoolean("secDescending")));
    directions.add(Boolean.valueOf(frame.prefs.getBoolean("terDescending")));
    fields.add(frame.prefs.get("priSort"));
    fields.add(frame.prefs.get("secSort"));
    fields.add(frame.prefs.get("terSort"));
    binary.add(Boolean.valueOf(Globals.prefs.getBoolean("priBinary"))); // TRUE if we are sorting on an icon.
    binary.add(Boolean.FALSE);
    binary.add(Boolean.FALSE);

    // Remove the old sorter as change listener for the database:
    if (sorter != null)
    db.removeDatabaseChangeListener(sorter);

    // Then pick the up to four highest ranking ones, and go.
      int piv = Math.min(directions.size()-1, 3);
      Comparator comp = new EntryComparator(
              ((Boolean)binary.get(piv)).booleanValue(),
              ((Boolean)directions.get(piv)).booleanValue(),
              (String)fields.get(piv));
      piv--;
      while (piv >= 0) {
          // Loop down towards the highest ranking criterion, wrapping new sorters around the
          // ones we have:
          String field = (String)fields.get(piv);
          if (field.equals(Globals.MARKED)) {
                comp = new MarkedComparator(comp);
          }
          else
            comp = new EntryComparator(
                  ((Boolean)binary.get(piv)).booleanValue(),
                  ((Boolean)directions.get(piv)).booleanValue(),
                  field,
                  comp);
          piv--;
      }

      sorter = db.getSorter(comp);


  }

    /**
     * Remaps and resorts the table model.
     */
    public void remap() {
    updateSorter();
    showAllEntries(); // Update the visible row count.
        updateAllCache();
    fireTableDataChanged();

    }

    /**
     * Remaps and resorts the table model, and restricts the row number
     * as directed.
     */
    public void remap(int rows) {
    updateSorter();
    setRowCount(rows);
        updateAllCache();
    fireTableDataChanged();
    }

    /**
     * Quick remap of the table model. Sufficient for all operations except
     * those that require a changed sort regime.
     */
    public void update() {
    sorter.index();
    showAllEntries();
        updateAllCache();
    fireTableDataChanged();

    }

    /**
     * Quick remap of the table model. Sufficient for all operations except
     * those that require a changed sort regime.
     * Restricts the row number as directed.
     */
    public void update(int rows) {
    sorter.index();
    setRowCount(rows);
        updateAllCache();
    fireTableDataChanged();
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
      if (!getFieldName(col).equals(GUIGlobals.TYPE_HEADER)) {

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

    BibtexEntry be = db.getEntryById(getIdForRow(row));
    boolean set = false;
    String toSet = null,
        fieldName = getFieldName(col),
        text;
    if (value != null) {
      text = value.toString();
      if (text.length() > 0) {
        toSet = text;
        Object o;
        if ( ( (o = be.getField(fieldName)) == null)
            || ( (o != null)
                && !o.toString().equals(toSet))) {
          set = true;
        }
      }
      else if (be.getField(fieldName) != null) {
        set = true;
      }
    }
    if (set) {
      try {
        if (toSet != null) {
          (new LatexFieldFormatter()).format
              (toSet, fieldName);
        }

        // Store this change in the UndoManager to facilitate undo.
        Object oldVal = be.getField(fieldName);
        panel.undoManager.addEdit
            (new net.sf.jabref.undo.UndoableFieldChange
             (be, fieldName.toLowerCase(), oldVal, toSet));
        // .. ok.

        be.setField(fieldName, toSet);
        panel.markBaseChanged();
        //panel.updateViewToSelected();
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

   /**
    * Returns the internal ID of the entry at the given row.
    * @param number The row number.
    * @return The ID for the entry at the given row.
    */
  public String getIdForRow(int number) {
    // Return the name of the Entry corresponding to the row. The
    // Entry will be retrieved from a DatabaseQuery. This is just
    // a temporary implementation.
    return sorter.getIdAt(number);
    //entryIDs[number].toString();
  }

    /**
     * Returns the entry currently displayed at the given row.
     * @param row The row.
     * @return The entry at the given row.
     */
  public BibtexEntry getEntryForRow(int row) {
      return sorter.getEntryAt(row);
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

    /**
     * Returns true iff the entry's Globals.MARKED field contains the
     * current user's wrapped username.
     * @param row The table row where the entry is.
     * @return true if the MARKED field contains the wrapped username.
     */
    public boolean isMarked(int row) {
        BibtexEntry be = db.getEntryById(getIdForRow(row));
        if (be == null)
            return false;
        return Util.isMarked(be);

    }


}
