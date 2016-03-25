/*  Copyright (C) 2003-2015 JabRef contributors.
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
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.bibtex.InternalBibtexFields;

/**
 * Listens for TableColumnModelEvents to keep track of changes made to the
 * MainTable columns, like reordering or resizing.
 *
 * Changes to columns without a name and the "#" column are not saved. To have
 * consistent behavior (e.g. as in TableColumnsTab).
 *
 * @author Fabian Bieker
 * @author Daniel Waeber
 * @since 12/2008
 *
 */
public class PersistenceTableColumnListener implements TableColumnModelListener {

    public static final String ACTIVATE_PREF_KEY = "ActivatePersistenceTableColumnListener";

    public static final boolean DEFAULT_ENABLED = true;

    private static final String SIMPLE_CLASS_NAME = PersistenceTableColumnListener.class.getSimpleName();

    // needed to get column names / indices mapped from view to model
    // and to access the table model
    private final MainTable mainTable;

    private static final String RECEIVED_NULL_EVENT = " received null event";

    /**
     * @param mainTable
     */
    public PersistenceTableColumnListener(final MainTable mainTable) {
        this.mainTable = mainTable;
    }

    /**
     * update columns names and their width, store it in the global prefs.
     */
    private void updateColumnPrefs() {
        final int columnCount = mainTable.getColumnCount();
        List<String> storedColumns = new ArrayList<>(columnCount - 1);
        List<String> columnsWidths = new ArrayList<>(columnCount - 1);
        int ncWidth = -1;

        for (int i = 0; i < columnCount; i++) {
            final String name = mainTable.getColumnName(i);
            if ((name != null) && !name.isEmpty()) {
                if (InternalBibtexFields.NUMBER_COL.equals(name)) {
                    ncWidth = mainTable.getColumnModel().getColumn(i).getWidth();
                } else {
                    storedColumns.add(name.toLowerCase());
                    columnsWidths.add(String.valueOf(mainTable.getColumnModel().getColumn(i).getWidth()));
                }
            }
        }

        // Finally, we store the new preferences.
        Globals.prefs.putStringList(JabRefPreferences.COLUMN_NAMES, storedColumns);
        Globals.prefs.putStringList(JabRefPreferences.COLUMN_WIDTHS, columnsWidths);

        // width of the number ("#") column
        Globals.prefs.putInt(JabRefPreferences.NUMBER_COL_WIDTH, ncWidth);
    }

    /**
     * @see javax.swing.event.TableColumnModelListener#columnAdded(javax.swing.event.TableColumnModelEvent)
     */
    @Override
    public void columnAdded(TableColumnModelEvent e) {
        assert e != null : PersistenceTableColumnListener.SIMPLE_CLASS_NAME + RECEIVED_NULL_EVENT;

        updateColumnPrefs();
    }

    /**
     * @see javax.swing.event.TableColumnModelListener#columnMarginChanged(javax.swing.event.ChangeEvent)
     */
    @Override
    public void columnMarginChanged(ChangeEvent e) {
        assert e != null : PersistenceTableColumnListener.SIMPLE_CLASS_NAME + RECEIVED_NULL_EVENT;

        updateColumnPrefs();
    }

    /**
     * @see javax.swing.event.TableColumnModelListener#columnMoved(javax.swing.event.TableColumnModelEvent)
     */
    @Override
    public void columnMoved(TableColumnModelEvent e) {
        assert e != null : PersistenceTableColumnListener.SIMPLE_CLASS_NAME + RECEIVED_NULL_EVENT;

        // not really moved, ignore ...
        if (e.getFromIndex() == e.getToIndex()) {
            return;
        }

        updateColumnPrefs();

    }

    /**
     * @see javax.swing.event.TableColumnModelListener#columnRemoved(javax.swing.event.TableColumnModelEvent)
     */
    @Override
    public void columnRemoved(TableColumnModelEvent e) {
        assert e != null : PersistenceTableColumnListener.SIMPLE_CLASS_NAME + RECEIVED_NULL_EVENT;

        updateColumnPrefs();

    }

    /**
     * @see javax.swing.event.TableColumnModelListener#columnSelectionChanged(javax.swing.event.ListSelectionEvent)
     */
    @Override
    public void columnSelectionChanged(ListSelectionEvent e) {
        // ignore
    }

}
