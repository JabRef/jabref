package net.sf.jabref.gui;

import ca.odell.glazedlists.gui.TableFormat;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.matchers.Matcher;
import ca.odell.glazedlists.swing.EventTableModel;
import ca.odell.glazedlists.swing.EventSelectionModel;
import ca.odell.glazedlists.swing.TableComparatorChooser;
import net.sf.jabref.*;
import net.sf.jabref.imports.ImportFormatReader;

import javax.swing.*;
import java.util.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * Created by IntelliJ IDEA.
 * User: alver
 * Date: Oct 12, 2005
 * Time: 7:37:48 PM
 * To change this template use File | Settings | File Templates.
 */
public class MainTableFormat implements TableFormat {

    public static final String[]
            PDF = {"pdf", "ps"}
    ,
    URL_ = {"url", "doi"}
    ,
    CITESEER = {"citeseerurl"};

    BasePanel panel;

    String[] columns; // Contains the current column names.
    public int padleft = -1; // padleft indicates how many columns (starting from left) are
    // special columns (number column or icon column).
    private HashMap iconCols = new HashMap();
    int[] nameCols = null;
    boolean namesAsIs, abbr_names, namesNatbib, namesFf, namesLf, namesLastOnly, showShort;

    public MainTableFormat(BasePanel panel) {
        this.panel = panel;
    }

    public int getColumnCount() {
        return padleft + columns.length;
    }

    public String getColumnName(int col) {
        if (col == 0) {
            return GUIGlobals.NUMBER_COL;
        } else if (getIconTypeForColumn(col) != null) {
            return "";
        } else if (GUIGlobals.FIELD_DISPLAYS.get(columns[col - padleft]) != null) {
            return ((String) GUIGlobals.FIELD_DISPLAYS.get(columns[col - padleft]));
        }
        return Util.nCase(columns[col - padleft]);
    }

    /**
     * This method returns a string array indicating the types of icons to be displayed in the given column.
     * It returns null if the column is not an icon column, and thereby also serves to identify icon
     * columns.
     */
    public String[] getIconTypeForColumn(int col) {
        Object o = iconCols.get(new Integer(col));
        if (o != null)
            return (String[]) o;
        else
            return null;
    }

    /**
     * Finds the column index for the given column name.
     * @param colName The column name
     * @return The column index if any, or -1 if no column has that name. 
     */
    public int getColumnIndex(String colName) {
        for (int i=0; i<columns.length; i++) {
            if (columns[i].equalsIgnoreCase(colName))
                return i+padleft;
        }
        return -1;
    }

    public Object getColumnValue(Object object, int col) {
        Object o;
        BibtexEntry be = (BibtexEntry) object;
        String[] iconType = getIconTypeForColumn(col); // If non-null, indicates an icon column's type.
        if (col == 0) {
            o = "#";// + (row + 1);
        }

        else if (iconType != null) {
            int hasField = -1;
            for (int i = iconType.length - 1; i >= 0; i--)
                if (hasField(be, iconType[i]))
                    hasField = i;
            if (hasField < 0)
                return null;

            // Ok, so we are going to display an icon. Find out which one, and return it:
            o = GUIGlobals.getTableIcon(iconType[hasField]);
        } else if (columns[col - padleft].equals(GUIGlobals.TYPE_HEADER)) {
            o = be.getType().getName();
        } else {

            o = be.getField(columns[col - padleft]);
            for (int i = 0; i < nameCols.length; i++) {
                if (col - padleft == nameCols[i]) {
                    if (o == null) {
                        return null;
                    }
                    if (namesAsIs) return o;
                    if (namesNatbib) o = AuthorList.fixAuthor_Natbib((String) o);
                    else if (namesLastOnly) o = AuthorList.fixAuthor_lastNameOnlyCommas((String) o);
                    else if (namesFf) o = AuthorList.fixAuthor_firstNameFirstCommas((String) o, abbr_names);
                    else if (namesLf) o = AuthorList.fixAuthor_lastNameFirstCommas((String) o, abbr_names);

                    return o;
                }
            }


        }

        return o;
    }

    public boolean hasField(BibtexEntry be, String field) {
        // Returns true iff the entry has a nonzero value in its
        // 'search' field.
        return ((be != null) && (be.getField(field) != null));
    }

    public void updateTableFormat() {

        // Read table columns from prefs:
        columns = Globals.prefs.getStringArray("columnNames");

        // Read name format options:
        showShort = Globals.prefs.getBoolean("showShort");        //MK:
        namesNatbib = Globals.prefs.getBoolean("namesNatbib");    //MK:
        namesLastOnly = Globals.prefs.getBoolean("namesLastOnly");
        namesAsIs = Globals.prefs.getBoolean("namesAsIs");
        abbr_names = Globals.prefs.getBoolean("abbrAuthorNames"); //MK:
        namesFf = Globals.prefs.getBoolean("namesFf");
        namesLf = !(namesAsIs || namesFf || namesNatbib || namesLastOnly); // None of the above.

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
        padleft = 1 + iconCols.size();

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
            nameCols[i] = ((Integer) tmp.elementAt(i)).intValue();
        }
    }

    public boolean isIconColumn(int col) {
        return (getIconTypeForColumn(col) != null);
    }



    static class NoSearchMatcher implements Matcher {
        public boolean matches(Object object) {
            return true;
        }
    }

    static class SearchMatcher implements Matcher {
        private String field = Globals.SEARCH;
        private SearchRuleSet ruleSet;
        private Hashtable searchOptions;

        public SearchMatcher(SearchRuleSet ruleSet, Hashtable searchOptions) {
            this.ruleSet = ruleSet;
            this.searchOptions = searchOptions;
        }
        public boolean matches(Object object) {
            BibtexEntry entry = (BibtexEntry)object;
            int result = ruleSet.applyRule(searchOptions, entry);
            return result > 0;
        }
    }
}
