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
import java.util.List;

import net.sf.jabref.gui.*;
import net.sf.jabref.model.database.BibtexDatabase;
import net.sf.jabref.model.entry.BibtexEntry;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
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

    // Values to gather iconImages for those columns
    // These values are also used to put a heading into the table; see getColumnName(int)
    private static final String[] URL_FIRST = {"url", "doi"};
    private static final String[] DOI_FIRST = {"doi", "url"};
    private static final String[] ARXIV = {"eprint"};
    public static final String[] FILE = {Globals.FILE_FIELD};

    private final BibtexDatabase database;

    private List<MainTableColumn> tableColumns = new ArrayList<>();

    public MainTableFormat(BibtexDatabase database) {
        this.database = database;
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

    public MainTableColumn getTableColumn(int index) {
        return tableColumns.get(index);
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

    @Override
    public Object getColumnValue(BibtexEntry be, int col) {

        return tableColumns.get(col).getColumnValue(be);

    }

    public void updateTableFormat() {
        // clear existing column configuration
        tableColumns.clear();

        // Add numbering column to tableColumns
        tableColumns.add(SpecialMainTableColumns.NUMBER_COL);

        // Add 'normal' bibtex fields as configured in the preferences
        // Read table columns from prefs:
        String[] colSettings = Globals.prefs.getStringArray(JabRefPreferences.COLUMN_NAMES);

        for (String columnName : colSettings) {
            // stored column name will be used as columnName
            // There might be more than one field to display, e.g., "author/editor" or "date/year" - so split
            // at MainTableFormat.COL_DEFINITION_FIELD_SEPARATOR
            String[] fields = columnName.split(MainTableFormat.COL_DEFINITION_FIELD_SEPARATOR);
            tableColumns.add(new MainTableColumn(columnName, fields, database));
        }


        // Add the "special" icon columns (e.g., ranking, file, ...) that are enabled in preferences.
        if (Globals.prefs.getBoolean(SpecialFieldsUtils.PREF_SPECIALFIELDSENABLED)) {
            if (Globals.prefs.getBoolean(SpecialFieldsUtils.PREF_SHOWCOLUMN_RANKING)) {
                tableColumns.add(SpecialMainTableColumns.RANKING_COLUMN);
            }
            if (Globals.prefs.getBoolean(SpecialFieldsUtils.PREF_SHOWCOLUMN_RELEVANCE)) {
                tableColumns.add(SpecialMainTableColumns.RELEVANCE_COLUMN);
            }
            if (Globals.prefs.getBoolean(SpecialFieldsUtils.PREF_SHOWCOLUMN_QUALITY)) {
                tableColumns.add(SpecialMainTableColumns.QUALITY_COLUMN);
            }
            if (Globals.prefs.getBoolean(SpecialFieldsUtils.PREF_SHOWCOLUMN_PRIORITY)) {
                tableColumns.add(SpecialMainTableColumns.PRIORITY_COLUMN);
            }
            if (Globals.prefs.getBoolean(SpecialFieldsUtils.PREF_SHOWCOLUMN_PRINTED)) {
                tableColumns.add(SpecialMainTableColumns.PRINTED_COLUMN);
            }
            if (Globals.prefs.getBoolean(SpecialFieldsUtils.PREF_SHOWCOLUMN_READ)) {
                tableColumns.add(SpecialMainTableColumns.READ_STATUS_COLUMN);
            }
        }

        if (Globals.prefs.getBoolean(JabRefPreferences.FILE_COLUMN)) {
            tableColumns.add(SpecialMainTableColumns.FILE_COLUMN);
        }

        if (Globals.prefs.getBoolean(JabRefPreferences.URL_COLUMN)) {
            if (Globals.prefs.getBoolean(JabRefPreferences.PREFER_URL_DOI)) {
                tableColumns.add(SpecialMainTableColumns
                        .createIconColumn(JabRefPreferences.URL_COLUMN, MainTableFormat.DOI_FIRST,
                                new JLabel(IconTheme.JabRefIcon.DOI.getSmallIcon())));
            } else {
                tableColumns.add(SpecialMainTableColumns
                        .createIconColumn(JabRefPreferences.URL_COLUMN, MainTableFormat.URL_FIRST,
                                new JLabel(IconTheme.JabRefIcon.WWW.getSmallIcon())));
            }

        }

        if (Globals.prefs.getBoolean(JabRefPreferences.ARXIV_COLUMN)) {
            tableColumns.add(SpecialMainTableColumns
                    .createIconColumn(JabRefPreferences.ARXIV_COLUMN, MainTableFormat.ARXIV,
                            new JLabel(IconTheme.JabRefIcon.WWW.getSmallIcon())));
        }

        if (Globals.prefs.getBoolean(JabRefPreferences.EXTRA_FILE_COLUMNS)) {
            String[] desiredColumns = Globals.prefs.getStringArray(JabRefPreferences.LIST_OF_FILE_COLUMNS);
            for (String desiredColumn : desiredColumns) {
                tableColumns.add(SpecialMainTableColumns.createFileIconColumn(desiredColumn));
            }
        }
    }

}
