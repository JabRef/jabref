/*  Copyright (C) 2003-2012 JabRef contributors.
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

import java.util.HashMap;
import java.util.Vector;

import net.sf.jabref.model.entry.AuthorList;
import net.sf.jabref.model.entry.BibtexEntry;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.logic.util.strings.StringUtil;
import net.sf.jabref.specialfields.Priority;
import net.sf.jabref.specialfields.Rank;
import net.sf.jabref.specialfields.ReadStatus;
import net.sf.jabref.specialfields.SpecialFieldValue;
import net.sf.jabref.specialfields.SpecialFieldsUtils;
import ca.odell.glazedlists.gui.TableFormat;

import javax.swing.JLabel;

/**
 * Class defining the contents and column headers of the main table.
 */
public class MainTableFormat implements TableFormat<BibtexEntry> {

    // Character separating field names that are to be used in sequence as
    // fallbacks for a single column (e.g. "author/editor" to use editor where
    // author is not set):
    public static final String COL_DEFINITION_FIELD_SEPARATOR = "/";

    public static final String ICON_COLUMN_PREFIX = "iconcol:";

    // Values to gather iconImages for those columns
    // These values are also used to put a heading into the table; see getColumnName(int)
    private static final String[]
            PDF = {"pdf", "ps"};
    private static final String[] URL_FIRST = {"url", "doi"};
    private static final String[] DOI_FIRST = {"doi", "url"};
    public static final String[] CITESEER = {"citeseerurl"};
    private static final String[] ARXIV = {"eprint"};
    private static final String[] RANKING = {SpecialFieldsUtils.FIELDNAME_RANKING};
    private static final String[] PRIORITY = {SpecialFieldsUtils.FIELDNAME_PRIORITY};
    private static final String[] RELEVANCE = {SpecialFieldsUtils.FIELDNAME_RELEVANCE};
    private static final String[] QUALITY = {SpecialFieldsUtils.FIELDNAME_QUALITY};
    private static final String[] PRINTED = {SpecialFieldsUtils.FIELDNAME_PRINTED};
    private static final String[] READ = {SpecialFieldsUtils.FIELDNAME_READ};
    public static final String[] FILE = {Globals.FILE_FIELD};

    private final BasePanel panel;

    private String[][] columns; // Contains the current column names.
    public int padleft = -1; // padleft indicates how many columns (starting from left) are
    // special columns (number column or icon column).
    private final HashMap<Integer, String[]> iconCols = new HashMap<>();
    private int[][] nameCols;
    private boolean namesAsIs;
    private boolean abbr_names;
    private boolean namesNatbib;
    private boolean namesFf;
    private boolean namesLf;
    private boolean namesLastOnly;


    public MainTableFormat(BasePanel panel) {
        this.panel = panel;
    }

    @Override
    public int getColumnCount() {
        return padleft + columns.length;
    }

    /**
     * @return the string that should be put in the column header
     */
    @Override
    public String getColumnName(int col) {
        if (col == 0) {
            return GUIGlobals.NUMBER_COL;
        } else if (getIconTypeForColumn(col) != null) {
            if (Globals.prefs.getBoolean(JabRefPreferences.SHOW_ONE_LETTER_HEADING_FOR_ICON_COLUMNS)) {
                return getIconTypeForColumn(col)[0].substring(0, 1).toUpperCase();
            } else {
                return null;
            }
        } else // try to find an alternative fieldname (for display)
        {
            String[] fld = columns[col - padleft];
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < fld.length; i++) {
                if (i > 0) {
                    sb.append('/');
                }
                String disName = BibtexFields.getFieldDisplayName(fld[i]);
                if (disName != null) {
                    sb.append(disName);
                } else {
                    sb.append(StringUtil.capitalizeFirst(fld[i]));
                }
            }
            return sb.toString();
            /*String disName = BibtexFields.getFieldDisplayName(columns[col - padleft]) ;
            if ( disName != null)
            {
              return disName ;
            } */
        }
        //return Util.capitalizeFirst(columns[col - padleft]);
    }

    /**
     * Get the column title, or a string identifying the column if it is an icon
     * column without a title.
     *
     * @param col The column number
     * @return the String identifying the column
     */
    public String getColumnType(int col) {
        String name = getColumnName(col);
        if (name != null) {
            return name;
        }
        String[] icon = getIconTypeForColumn(col);
        if ((icon != null) && (icon.length > 0)) {
            return MainTableFormat.ICON_COLUMN_PREFIX + icon[0];
        }
        return null;
    }

    /**
     * This method returns a string array indicating the types of icons to be displayed in the given column.
     * It returns null if the column is not an icon column, and thereby also serves to identify icon
     * columns.
     */
    public String[] getIconTypeForColumn(int col) {
        Object o = iconCols.get(new Integer(col));
        if (o != null) {
            return (String[]) o;
        } else {
            return null;
        }
    }

    /**
     * Finds the column index for the given column name.
     *
     * @param colName The column name
     * @return The column index if any, or -1 if no column has that name.
     */
    public int getColumnIndex(String colName) {
        for (int i = 0; i < columns.length; i++) {
            // TODO: is the following line correct with [0] ?
            if (columns[i][0].equalsIgnoreCase(colName)) {
                return i + padleft;
            }
        }
        return -1;
    }

    /**
     * Checks, if the Column (int col) is a Ranking-Column
     *
     * @param col Column Number
     * @return Is Ranking-Column or not?
     */
    public boolean isRankingColumn(int col) {
        if (iconCols.get(col) != null) {
            if (iconCols.get(col)[0].equals(MainTableFormat.RANKING[0])) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Object getColumnValue(BibtexEntry be, int col) {
        Object o = null;
        String[] iconType = getIconTypeForColumn(col); // If non-null, indicates an icon column's type.

        if (col == 0) {
            o = "#";// + (row + 1);
        } else if (iconType != null) {
            int hasField;

            int[] fieldCount = hasField(be, iconType);
            hasField = fieldCount[0];

            if (hasField < 0) {
                return null;
            }

            // Ok, so we are going to display an icon. Find out which one, and return it:
            if (iconType[hasField].equals(Globals.FILE_FIELD)) {
                o = FileListTableModel.getFirstLabel(be.getField(Globals.FILE_FIELD));

                if (fieldCount[1] > 1) {
                    o = new JLabel(IconTheme.JabRefIcon.FILE_MULTIPLE.getSmallIcon());
                }

                // Handle priority column special
                // Extra handling because the icon depends on a FieldValue
            } else if (iconType[hasField].equals(MainTableFormat.PRIORITY[0])) {
                SpecialFieldValue prio = Priority.getInstance().parse(be.getField(SpecialFieldsUtils.FIELDNAME_PRIORITY));
                if (prio != null) {
                    // prio might be null if fieldvalue is an invalid value, therefore we check for != null
                    o = prio.createLabel();
                }
                // Handle ranking column special
                // Extra handling because the icon depends on a FieldValue
            } else if (iconType[hasField].equals(MainTableFormat.RANKING[0])) {
                SpecialFieldValue rank = Rank.getInstance().parse(be.getField(SpecialFieldsUtils.FIELDNAME_RANKING));
                if (rank != null) {
                    o = rank.createLabel();
                }
                // Handle read status column special
                // Extra handling because the icon depends on a FieldValue
            } else if (iconType[hasField].equals(MainTableFormat.READ[0])) {
                SpecialFieldValue status = ReadStatus.getInstance().parse(be.getField(SpecialFieldsUtils.FIELDNAME_READ));
                if (status != null) {
                    o = status.createLabel();
                }
            } else {
                o = GUIGlobals.getTableIcon(iconType[hasField]);

                if (fieldCount[1] > 1) {
                    o = new JLabel(IconTheme.JabRefIcon.FILE_MULTIPLE.getSmallIcon());
                }
            }
        } else {
            String[] fld = columns[col - padleft];
            // Go through the fields until we find one with content:
            int j = 0;
            for (int i = 0; i < fld.length; i++) {
                if (fld[i].equals(BibtexEntry.TYPE_HEADER)) {
                    o = be.getType().getName();
                } else {
                    o = be.getFieldOrAlias(fld[i]);
                    if (getColumnName(col).equals("Author") && (o != null)) {
                        o = panel.database().resolveForStrings((String) o);
                    }
                }
                if (o != null) {
                    j = i;
                    break;
                }
            }

            for (int[] nameCol : nameCols) {
                if (((col - padleft) == nameCol[0]) && (nameCol[1] == j)) {
                    return formatName(o);
                }
            }

        }

        return o;
    }

    /**
     * Format a name field for the table, according to user preferences.
     *
     * @param o The contents of the name field.
     * @return The formatted name field.
     */
    public Object formatName(Object o) {
        if (o == null) {
            return null;
        }
        if (namesAsIs) {
            return o;
        }
        if (namesNatbib) {
            o = AuthorList.fixAuthor_Natbib((String) o);
        } else if (namesLastOnly) {
            o = AuthorList.fixAuthor_lastNameOnlyCommas((String) o, false);
        } else if (namesFf) {
            o = AuthorList.fixAuthor_firstNameFirstCommas((String) o, abbr_names, false);
        } else if (namesLf) {
            o = AuthorList.fixAuthor_lastNameFirstCommas((String) o, abbr_names, false);
        }
        return o;
    }

    private boolean hasField(BibtexEntry be, String field) {
        // Returns true iff the entry has a nonzero value in its
        // 'search' field.
        return ((be != null) && (be.getFieldOrAlias(field) != null));
    }

    private int[] hasField(BibtexEntry be, String[] field) {
        // If the entry has a nonzero value in any of the
        // 'search' fields, returns the smallest index for which it does.
        // Otherwise returns -1. When field indicates one or more file types,
        // returns the index of the first present file type.
        if ((be == null) || (field == null) || (field.length < 1)) {
            return new int[]{-1, -1};
        }
        int hasField = -1;
        if (!field[0].equals(Globals.FILE_FIELD)) {
            for (int i = field.length - 1; i >= 0; i--) {
                if (hasField(be, field[i])) {
                    hasField = i;
                }
            }
            return new int[]{hasField, -1};
        } else {
            // We use a FileListTableModel to parse the field content:
            Object o = be.getField(Globals.FILE_FIELD);
            FileListTableModel fileList = new FileListTableModel();
            fileList.setContent((String) o);
            if (field.length == 1) {
                if (fileList.getRowCount() == 0) {
                    return new int[]{-1, -1};
                } else {
                    return new int[]{0, fileList.getRowCount()};
                }
            }
            int lastLinkPosition = -1;
            int countLinks = 0;
            for (int i = 1; i < field.length; i++) {
                // Count the number of links of correct type.
                for (int j = 0; j < fileList.getRowCount(); j++) {
                    FileListEntry flEntry = fileList.getEntry(j);
                    if (flEntry.getType().toString().equals(field[i])) {
                        lastLinkPosition = i;
                        countLinks++;
                    }
                }
            }
            return new int[]{lastLinkPosition, countLinks};
        }
    }

    public void updateTableFormat() {

        // Read table columns from prefs:
        String[] colSettings = Globals.prefs.getStringArray(JabRefPreferences.COLUMN_NAMES);
        columns = new String[colSettings.length][];
        for (int i = 0; i < colSettings.length; i++) {
            String[] fields = colSettings[i].split(MainTableFormat.COL_DEFINITION_FIELD_SEPARATOR);
            columns[i] = new String[fields.length];
            System.arraycopy(fields, 0, columns[i], 0, fields.length);
        }

        // Read name format options:
        namesNatbib = Globals.prefs.getBoolean(JabRefPreferences.NAMES_NATBIB); //MK:
        namesLastOnly = Globals.prefs.getBoolean(JabRefPreferences.NAMES_LAST_ONLY);
        namesAsIs = Globals.prefs.getBoolean(JabRefPreferences.NAMES_AS_IS);
        abbr_names = Globals.prefs.getBoolean(JabRefPreferences.ABBR_AUTHOR_NAMES); //MK:
        namesFf = Globals.prefs.getBoolean(JabRefPreferences.NAMES_FIRST_LAST);
        namesLf = !(namesAsIs || namesFf || namesNatbib || namesLastOnly); // None of the above.

        // Set the icon columns, indicating the number of special columns to the left.
        // We add those that are enabled in preferences.
        iconCols.clear();
        int coln = 1;

        // Add special Icon Columns
        if (Globals.prefs.getBoolean(SpecialFieldsUtils.PREF_SPECIALFIELDSENABLED)) {
            if (Globals.prefs.getBoolean(SpecialFieldsUtils.PREF_SHOWCOLUMN_RANKING)) {
                iconCols.put(coln, MainTableFormat.RANKING);
                coln++;
            }
            if (Globals.prefs.getBoolean(SpecialFieldsUtils.PREF_SHOWCOLUMN_RELEVANCE)) {
                iconCols.put(coln, MainTableFormat.RELEVANCE);
                coln++;
            }
            if (Globals.prefs.getBoolean(SpecialFieldsUtils.PREF_SHOWCOLUMN_QUALITY)) {
                iconCols.put(coln, MainTableFormat.QUALITY);
                coln++;
            }
            if (Globals.prefs.getBoolean(SpecialFieldsUtils.PREF_SHOWCOLUMN_PRIORITY)) {
                iconCols.put(coln, MainTableFormat.PRIORITY);
                coln++;
            }
            if (Globals.prefs.getBoolean(SpecialFieldsUtils.PREF_SHOWCOLUMN_PRINTED)) {
                iconCols.put(coln, MainTableFormat.PRINTED);
                coln++;
            }
            if (Globals.prefs.getBoolean(SpecialFieldsUtils.PREF_SHOWCOLUMN_READ)) {
                iconCols.put(coln, MainTableFormat.READ);
                coln++;
            }
        }

        if (Globals.prefs.getBoolean(JabRefPreferences.FILE_COLUMN)) {
            iconCols.put(coln, MainTableFormat.FILE);
            coln++;
        }
        if (Globals.prefs.getBoolean(JabRefPreferences.PDF_COLUMN)) {
            iconCols.put(coln, MainTableFormat.PDF);
            coln++;
        }
        if (Globals.prefs.getBoolean(JabRefPreferences.URL_COLUMN)) {
            if (Globals.prefs.getBoolean(JabRefPreferences.PREFER_URL_DOI)) {
                iconCols.put(coln, MainTableFormat.DOI_FIRST);
                coln++;
            } else {
                iconCols.put(coln, MainTableFormat.URL_FIRST);
                coln++;
            }

        }

        if (Globals.prefs.getBoolean(JabRefPreferences.ARXIV_COLUMN)) {
            iconCols.put(coln, MainTableFormat.ARXIV);
            coln++;
        }

        if (Globals.prefs.getBoolean(JabRefPreferences.EXTRA_FILE_COLUMNS)) {
            String[] desiredColumns = Globals.prefs.getStringArray(JabRefPreferences.LIST_OF_FILE_COLUMNS);
            for (String desiredColumn : desiredColumns) {
                iconCols.put(coln, new String[]{Globals.FILE_FIELD, desiredColumn});
                coln++;
            }
        }

        // Add 1 to the number of icon columns to get padleft.
        padleft = 1 + iconCols.size();

        // Set up the int[][] nameCols, to mark which columns should be
        // treated as lists of names. This is to provide a correct presentation
        // of names as efficiently as possible.
        // Each subarray contains the column number (before padding) and the
        // subfield number in case a column has fallback fields.
        Vector<int[]> tmp = new Vector<>(2, 1);
        for (int i = 0; i < columns.length; i++) {
            for (int j = 0; j < columns[i].length; j++) {
                if (columns[i][j].equals("author")
                        || columns[i][j].equals("editor")) {
                    tmp.add(new int[]{i, j});
                }
            }
        }
        nameCols = new int[tmp.size()][];
        for (int i = 0; i < nameCols.length; i++) {
            nameCols[i] = tmp.elementAt(i);
        }
    }

}
