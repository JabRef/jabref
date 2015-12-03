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
package net.sf.jabref.gui.maintable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import net.sf.jabref.gui.*;
import net.sf.jabref.model.entry.AuthorList;
import net.sf.jabref.model.entry.BibtexEntry;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.model.entry.EntryUtil;
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
    private static final String[] PDF = {"pdf", "ps"};
    private static final String[] URL_FIRST = {"url", "doi"};
    private static final String[] DOI_FIRST = {"doi", "url"};
    private static final String[] ARXIV = {"eprint"};
    public static final String[] FILE = {Globals.FILE_FIELD};

    private final BasePanel panel;

    private String[][] columns; // Contains the current column names.

    private List<MainTableColumn> tableColumns = new ArrayList<>();

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
        return tableColumns.size();
    }

    /**
     * @return the string that should be put in the column header
     */
    @Override
    public String getColumnName(int col) {

        return tableColumns.get(col).getDisplayName();

    }

    /**
     * Get the column title, or a string identifying the column if it is an icon
     * column without a title.
     *
     * @param col The column number
     * @return the String identifying the column
     */
    //TODO can be removed?
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
    //TODO to be removed
    public String[] getIconTypeForColumn(int col) {
        return null;
//        Object o = iconCols.get(Integer.valueOf(col));
//        if (o != null) {
//            return (String[]) o;
//        } else {
//            return null;
//        }
    }

    /**
     * Finds the column index for the given column name.
     *
     * @param colName The column name
     * @return The column index if any, or -1 if no column has that name.
     */
    public int getColumnIndex(String colName) {

        for (MainTableColumn tableColumn : tableColumns) {
            if (tableColumn.getColumnName().equalsIgnoreCase(colName)) {
                return tableColumns.lastIndexOf(tableColumn);
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
        return tableColumns.get(col).getColumnName().equals(SpecialFieldsUtils.FIELDNAME_RANKING);
    }

    @Override
    public Object getColumnValue(BibtexEntry be, int col) {

        return "#";

//        Object o = null;
//        String[] iconType = getIconTypeForColumn(col); // If non-null, indicates an icon column's type.
//
//        if (col == 0) {
//            o = "#";// + (row + 1);
//        } else if (iconType != null) {
//            int hasField;
//
//            int[] fieldCount = hasField(be, iconType);
//            hasField = fieldCount[0];
//
//            if (hasField < 0) {
//                return null;
//            }
//
//            // Ok, so we are going to display an icon. Find out which one, and return it:
//            if (iconType[hasField].equals(Globals.FILE_FIELD)) {
//                o = FileListTableModel.getFirstLabel(be.getField(Globals.FILE_FIELD));
//
//                if (fieldCount[1] > 1) {
//                    o = new JLabel(IconTheme.JabRefIcon.FILE_MULTIPLE.getSmallIcon());
//                }
//
//                // Handle priority column special
//                // Extra handling because the icon depends on a FieldValue
//            } else if (iconType[hasField].equals(MainTableFormat.PRIORITY[0])) {
//                SpecialFieldValue prio = Priority.getInstance().parse(be.getField(SpecialFieldsUtils.FIELDNAME_PRIORITY));
//                if (prio != null) {
//                    // prio might be null if fieldvalue is an invalid value, therefore we check for != null
//                    o = prio.createLabel();
//                }
//                // Handle ranking column special
//                // Extra handling because the icon depends on a FieldValue
//            } else if (iconType[hasField].equals(MainTableFormat.RANKING[0])) {
//                SpecialFieldValue rank = Rank.getInstance().parse(be.getField(SpecialFieldsUtils.FIELDNAME_RANKING));
//                if (rank != null) {
//                    o = rank.createLabel();
//                }
//                // Handle read status column special
//                // Extra handling because the icon depends on a FieldValue
//            } else if (iconType[hasField].equals(MainTableFormat.READ[0])) {
//                SpecialFieldValue status = ReadStatus.getInstance().parse(be.getField(SpecialFieldsUtils.FIELDNAME_READ));
//                if (status != null) {
//                    o = status.createLabel();
//                }
//            } else {
//                o = GUIGlobals.getTableIcon(iconType[hasField]);
//
//                if (fieldCount[1] > 1) {
//                    o = new JLabel(IconTheme.JabRefIcon.FILE_MULTIPLE.getSmallIcon());
//                }
//            }
//        } else {
//            String[] fld = columns[col - padleft];
//            // Go through the fields until we find one with content:
//            int j = 0;
//            for (int i = 0; i < fld.length; i++) {
//                if (fld[i].equals(BibtexEntry.TYPE_HEADER)) {
//                    o = be.getType().getName();
//                } else {
//                    o = be.getFieldOrAlias(fld[i]);
//                    if ("Author".equals(getColumnName(col)) && (o != null)) {
//                        o = panel.database().resolveForStrings((String) o);
//                    }
//                }
//                if (o != null) {
//                    j = i;
//                    break;
//                }
//            }
//
//            for (int[] nameCol : nameCols) {
//                if (((col - padleft) == nameCol[0]) && (nameCol[1] == j)) {
//                    return formatName((String) o);
//                }
//            }
//
//        }
//
//        return o;
    }

    /**
     * Format a name field for the table, according to user preferences.
     *
     * @param nameToFormat The contents of the name field.
     * @return The formatted name field.
     */
    public String formatName(String nameToFormat) {
        if (nameToFormat == null) {
            return null;
        } else if (namesAsIs) {
            return nameToFormat;
        } else if (namesNatbib) {
            nameToFormat = AuthorList.fixAuthor_Natbib(nameToFormat);
        } else if (namesLastOnly) {
            nameToFormat = AuthorList.fixAuthor_lastNameOnlyCommas(nameToFormat, false);
        } else if (namesFf) {
            nameToFormat = AuthorList.fixAuthor_firstNameFirstCommas(nameToFormat, abbr_names, false);
        } else if (namesLf) {
            nameToFormat = AuthorList.fixAuthor_lastNameFirstCommas(nameToFormat, abbr_names, false);
        }
        return nameToFormat;
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
        // clear existing column configuration
        tableColumns.clear();

        // Add numbering column to tableColumns
        tableColumns.add(new MainTableColumn(GUIGlobals.NUMBER_COL));

        // Add 'normal' bibtex fields as configured in the preferences
        // Read table columns from prefs:
        String[] colSettings = Globals.prefs.getStringArray(JabRefPreferences.COLUMN_NAMES);
        columns = new String[colSettings.length][];
        for (int i = 0; i < colSettings.length; i++) {
            // stored column name will be used as columnName
            String columnName = colSettings[i];
            // There might be more than one field to display, e.g., "author/editor" or "date/year" - so split
            // at MainTableFormat.COL_DEFINITION_FIELD_SEPARATOR
            String[] fields = colSettings[i].split(MainTableFormat.COL_DEFINITION_FIELD_SEPARATOR);
            tableColumns.add(new MainTableColumn(columnName, fields));
        }


        // Add the "special" icon columns (e.g., ranking, file, ...) that are enabled in preferences.
        if (Globals.prefs.getBoolean(SpecialFieldsUtils.PREF_SPECIALFIELDSENABLED)) {
            if (Globals.prefs.getBoolean(SpecialFieldsUtils.PREF_SHOWCOLUMN_RANKING)) {
                tableColumns.add(new MainTableColumn(SpecialFieldsUtils.FIELDNAME_RANKING));
            }
            if (Globals.prefs.getBoolean(SpecialFieldsUtils.PREF_SHOWCOLUMN_RELEVANCE)) {
                tableColumns.add(new MainTableColumn(SpecialFieldsUtils.FIELDNAME_RELEVANCE));
            }
            if (Globals.prefs.getBoolean(SpecialFieldsUtils.PREF_SHOWCOLUMN_QUALITY)) {
                tableColumns.add(new MainTableColumn(SpecialFieldsUtils.FIELDNAME_QUALITY));
            }
            if (Globals.prefs.getBoolean(SpecialFieldsUtils.PREF_SHOWCOLUMN_PRIORITY)) {
                tableColumns.add(new MainTableColumn(SpecialFieldsUtils.FIELDNAME_PRIORITY));
            }
            if (Globals.prefs.getBoolean(SpecialFieldsUtils.PREF_SHOWCOLUMN_PRINTED)) {
                tableColumns.add(new MainTableColumn(SpecialFieldsUtils.FIELDNAME_PRINTED));
            }
            if (Globals.prefs.getBoolean(SpecialFieldsUtils.PREF_SHOWCOLUMN_READ)) {
                tableColumns.add(new MainTableColumn(SpecialFieldsUtils.FIELDNAME_READ));
            }
        }

        if (Globals.prefs.getBoolean(JabRefPreferences.FILE_COLUMN)) {
            tableColumns.add(new MainTableColumn(JabRefPreferences.FILE_COLUMN));
        }
        if (Globals.prefs.getBoolean(JabRefPreferences.PDF_COLUMN)) {
            tableColumns.add(new MainTableColumn(JabRefPreferences.PDF_COLUMN, MainTableFormat.PDF));
        }
        if (Globals.prefs.getBoolean(JabRefPreferences.URL_COLUMN)) {
            if (Globals.prefs.getBoolean(JabRefPreferences.PREFER_URL_DOI)) {
                tableColumns.add(new MainTableColumn(JabRefPreferences.URL_COLUMN, MainTableFormat.DOI_FIRST));
            } else {
                tableColumns.add(new MainTableColumn(JabRefPreferences.URL_COLUMN, MainTableFormat.URL_FIRST));
            }

        }

        if (Globals.prefs.getBoolean(JabRefPreferences.ARXIV_COLUMN)) {
            tableColumns.add(new MainTableColumn(JabRefPreferences.ARXIV_COLUMN, MainTableFormat.ARXIV));
        }

        if (Globals.prefs.getBoolean(JabRefPreferences.EXTRA_FILE_COLUMNS)) {
            String[] desiredColumns = Globals.prefs.getStringArray(JabRefPreferences.LIST_OF_FILE_COLUMNS);
            for (String desiredColumn : desiredColumns) {
                tableColumns.add(new MainTableColumn(desiredColumn, new String[]{Globals.FILE_FIELD}));
            }
        }

        // Read name format options:
        namesNatbib = Globals.prefs.getBoolean(JabRefPreferences.NAMES_NATBIB); //MK:
        namesLastOnly = Globals.prefs.getBoolean(JabRefPreferences.NAMES_LAST_ONLY);
        namesAsIs = Globals.prefs.getBoolean(JabRefPreferences.NAMES_AS_IS);
        abbr_names = Globals.prefs.getBoolean(JabRefPreferences.ABBR_AUTHOR_NAMES); //MK:
        namesFf = Globals.prefs.getBoolean(JabRefPreferences.NAMES_FIRST_LAST);
        namesLf = !(namesAsIs || namesFf || namesNatbib || namesLastOnly); // None of the above.

    }

}
