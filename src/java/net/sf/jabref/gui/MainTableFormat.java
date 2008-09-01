package net.sf.jabref.gui;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Vector;

import net.sf.jabref.AuthorList;
import net.sf.jabref.BasePanel;
import net.sf.jabref.BibtexEntry;
import net.sf.jabref.BibtexFields;
import net.sf.jabref.GUIGlobals;
import net.sf.jabref.Globals;
import net.sf.jabref.SearchRuleSet;
import net.sf.jabref.Util;
import ca.odell.glazedlists.gui.TableFormat;
import ca.odell.glazedlists.matchers.Matcher;

/**
 * Class defining the contents and column headers of the main table.
 */
public class MainTableFormat implements TableFormat<BibtexEntry> {

    // Character separating field names that are to be used in sequence as
    // fallbacks for a single column (e.g. "author/editor" to use editor where
    // author is not set):
    public static final String COL_DEFINITION_FIELD_SEPARATOR = "/";

    public static final String[]
            PDF = {"pdf", "ps"}
    ,
    URL_ = {"url", "doi"}
    ,
    CITESEER = {"citeseerurl"},
    FILE = {GUIGlobals.FILE_FIELD};

    BasePanel panel;

    private String[][] columns; // Contains the current column names.
    public int padleft = -1; // padleft indicates how many columns (starting from left) are
    // special columns (number column or icon column).
    private HashMap<Integer, String[]> iconCols = new HashMap<Integer, String[]>();
    int[][] nameCols = null;
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
        }
        else // try to find an alternative fieldname (for display)
        {
            String[] fld = columns[col - padleft];
            StringBuilder sb = new StringBuilder();
            for (int i=0; i<fld.length; i++) {
                if (i > 0)
                    sb.append('/');
                String disName = BibtexFields.getFieldDisplayName(fld[i]);
                if (disName != null)
                    sb.append(disName);
                else
                    sb.append(Util.nCase(fld[i]));
            }
            return sb.toString();
          /*String disName = BibtexFields.getFieldDisplayName(columns[col - padleft]) ;
          if ( disName != null)
          {
            return disName ;
          } */
        }
        //return Util.nCase(columns[col - padleft]);
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
            // TODO: is the following line correct with [0] ?
            if (columns[i][0].equalsIgnoreCase(colName))
                return i+padleft;
        }
        return -1;
    }

    public Object getColumnValue(BibtexEntry be, int col) {
        Object o = null;
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
            if (iconType[hasField].equals(GUIGlobals.FILE_FIELD)) {
                o = FileListTableModel.getFirstLabel(be.getField(GUIGlobals.FILE_FIELD));
            } else
                o = GUIGlobals.getTableIcon(iconType[hasField]);
        } else {
            String[] fld = columns[col - padleft];
            // Go through the fields until we find one with content:
            int j = 0;
            for (int i = 0; i < fld.length; i++) {
                if (fld[i].equals(GUIGlobals.TYPE_HEADER))
                    o = be.getType().getName();
                else
                    o = be.getField(fld[i]);
                if (o != null) {
                    j = i;
                    break;
                }
            }

            for (int i = 0; i < nameCols.length; i++) {
                if ((col - padleft == nameCols[i][0]) && (nameCols[i][1] == j)) {
                    return formatName(o);
                }
            }


        }

        return o;
    }

    /**
     * Format a name field for the table, according to user preferences.
     * @param o The contents of the name field.
     * @return The formatted name field.
     */
    public Object formatName(Object o) {
        if (o == null) {
            return null;
        }
        if (namesAsIs) return o;
        if (namesNatbib) o = AuthorList.fixAuthor_Natbib((String) o);
        else if (namesLastOnly) o = AuthorList.fixAuthor_lastNameOnlyCommas((String) o, false);
        else if (namesFf) o = AuthorList.fixAuthor_firstNameFirstCommas((String) o, abbr_names, false);
        else if (namesLf) o = AuthorList.fixAuthor_lastNameFirstCommas((String) o, abbr_names, false);
        return o;
    }

    public boolean hasField(BibtexEntry be, String field) {
        // Returns true iff the entry has a nonzero value in its
        // 'search' field.
        return ((be != null) && (be.getField(field) != null));
    }

    public void updateTableFormat() {

        // Read table columns from prefs:
        String[] colSettings = Globals.prefs.getStringArray("columnNames");
        columns = new String[colSettings.length][];
        for (int i=0; i<colSettings.length; i++) {
            String[] fields = colSettings[i].split(COL_DEFINITION_FIELD_SEPARATOR);
            columns[i] = new String[fields.length];
            for (int j = 0; j < fields.length; j++) {
                columns[i][j] = fields[j];
            }
        }
        
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
        if (Globals.prefs.getBoolean("fileColumn"))
            iconCols.put(coln++, FILE);
        if (Globals.prefs.getBoolean("pdfColumn"))
            iconCols.put(coln++, PDF);
        if (Globals.prefs.getBoolean("urlColumn"))
            iconCols.put(coln++, URL_);
        if (Globals.prefs.getBoolean("citeseerColumn"))
            iconCols.put(coln++, CITESEER);

        // Add 1 to the number of icon columns to get padleft.
        padleft = 1 + iconCols.size();

        // Set up the int[][] nameCols, to mark which columns should be
        // treated as lists of names. This is to provide a correct presentation
        // of names as efficiently as possible.
        // Each subarray contains the column number (before padding) and the
        // subfield number in case a column has fallback fields.
        Vector<int[]> tmp = new Vector<int[]>(2, 1);
        for (int i = 0; i < columns.length; i++) {
            for (int j = 0; j < columns[i].length; j++) {
                if (columns[i][j].equals("author")
                    || columns[i][j].equals("editor")) {
                    tmp.add(new int[] {i, j});
                }
            }
        }
        nameCols = new int[tmp.size()][];
        for (int i = 0; i < nameCols.length; i++) {
            nameCols[i] = tmp.elementAt(i);
        }
    }

    public boolean isIconColumn(int col) {
        return (getIconTypeForColumn(col) != null);
    }



    static class NoSearchMatcher implements Matcher<BibtexEntry> {
        public boolean matches(BibtexEntry object) {
            return true;
        }
    }

    static class SearchMatcher implements Matcher<BibtexEntry> {
        private SearchRuleSet ruleSet;
        private Hashtable<String, String> searchOptions;

        public SearchMatcher(SearchRuleSet ruleSet, Hashtable<String, String> searchOptions) {
            this.ruleSet = ruleSet;
            this.searchOptions = searchOptions;
        }
        public boolean matches(BibtexEntry entry) {
            int result = ruleSet.applyRule(searchOptions, entry);
            return result > 0;
        }
    }
}
