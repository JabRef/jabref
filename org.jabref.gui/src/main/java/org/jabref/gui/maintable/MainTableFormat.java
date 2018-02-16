package org.jabref.gui.maintable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.swing.JLabel;

import org.jabref.Globals;
import org.jabref.gui.IconTheme;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;
import org.jabref.preferences.JabRefPreferences;

import ca.odell.glazedlists.gui.TableFormat;

/**
 * Class defining the contents and column headers of the main table.
 */
public class MainTableFormat implements TableFormat<BibEntry> {

    // Values to gather iconImages for those columns
    // These values are also used to put a heading into the table; see getColumnName(int)
    private static final List<String> URL_FIRST = Arrays.asList(FieldName.URL, FieldName.DOI);
    private static final List<String> DOI_FIRST = Arrays.asList(FieldName.DOI, FieldName.URL);
    private static final List<String> ARXIV = Collections.singletonList(FieldName.EPRINT);

    private final BibDatabase database;

    private final List<MainTableColumn> tableColumns = new ArrayList<>();

    public MainTableFormat(BibDatabase database) {
        this.database = database;
    }

    @Override
    public int getColumnCount() {
        return tableColumns.size();
    }

    /**
     * @return the string that should be put in the column header. null if field is empty.
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
    public Object getColumnValue(BibEntry be, int col) {
        return tableColumns.get(col).getColumnValue(be);
    }

    public void updateTableFormat() {
        // clear existing column configuration
        tableColumns.clear();

        SpecialMainTableColumnsBuilder builder = new SpecialMainTableColumnsBuilder();
        // Add numbering column to tableColumns
        tableColumns.add(builder.buildNumberColumn());

        // Add all file based columns
        if (Globals.prefs.getBoolean(JabRefPreferences.FILE_COLUMN)) {
            tableColumns.add(builder.buildFileColumn());
        }

        if (Globals.prefs.getBoolean(JabRefPreferences.URL_COLUMN)) {
            if (Globals.prefs.getBoolean(JabRefPreferences.PREFER_URL_DOI)) {
                tableColumns.add(builder
                        .createIconColumn(JabRefPreferences.URL_COLUMN, MainTableFormat.DOI_FIRST,
                                new JLabel(IconTheme.JabRefIcon.DOI.getSmallIcon())));
            } else {
                tableColumns.add(builder
                        .createIconColumn(JabRefPreferences.URL_COLUMN, MainTableFormat.URL_FIRST,
                                new JLabel(IconTheme.JabRefIcon.WWW.getSmallIcon())));
            }

        }

        if (Globals.prefs.getBoolean(JabRefPreferences.ARXIV_COLUMN)) {
            tableColumns.add(builder
                    .createIconColumn(JabRefPreferences.ARXIV_COLUMN, MainTableFormat.ARXIV,
                            new JLabel(IconTheme.JabRefIcon.WWW.getSmallIcon())));
        }

        if (Globals.prefs.getBoolean(JabRefPreferences.EXTRA_FILE_COLUMNS)) {
            List<String> desiredColumns = Globals.prefs.getStringList(JabRefPreferences.LIST_OF_FILE_COLUMNS);
            for (String desiredColumn : desiredColumns) {
                tableColumns.add(builder.createFileIconColumn(desiredColumn));
            }
        }

        // Add 'normal' bibtex fields as configured in the preferences
        // Read table columns from prefs:
        List<String> colSettings = Globals.prefs.getStringList(JabRefPreferences.COLUMN_NAMES);

        for (String columnName : colSettings) {
            // stored column name will be used as columnName
            // There might be more than one field to display, e.g., "author/editor" or "date/year" - so split
            // at MainTableFormat.COL_DEFINITION_FIELD_SEPARATOR
            String[] fields = columnName.split(FieldName.FIELD_SEPARATOR);
            tableColumns.add(new MainTableColumn(columnName, Arrays.asList(fields), database));
        }

        // Add the "special" icon columns (e.g., ranking, file, ...) that are enabled in preferences.
        if (Globals.prefs.getBoolean(JabRefPreferences.SPECIALFIELDSENABLED)) {
            if (Globals.prefs.getBoolean(JabRefPreferences.SHOWCOLUMN_RANKING)) {
                tableColumns.add(builder.buildRankingColumn());
            }
            if (Globals.prefs.getBoolean(JabRefPreferences.SHOWCOLUMN_RELEVANCE)) {
                tableColumns.add(builder.buildRelevanceColumn());
            }
            if (Globals.prefs.getBoolean(JabRefPreferences.SHOWCOLUMN_QUALITY)) {
                tableColumns.add(builder.buildQualityColumn());
            }
            if (Globals.prefs.getBoolean(JabRefPreferences.SHOWCOLUMN_PRIORITY)) {
                tableColumns.add(builder.buildPriorityColumn());
            }
            if (Globals.prefs.getBoolean(JabRefPreferences.SHOWCOLUMN_PRINTED)) {
                tableColumns.add(builder.buildPrintedColumn());
            }
            if (Globals.prefs.getBoolean(JabRefPreferences.SHOWCOLUMN_READ)) {
                tableColumns.add(builder.buildReadStatusColumn());
            }
        }

    }

}
