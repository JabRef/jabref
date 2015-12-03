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

import java.util.Vector;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;

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

    public static final String ACTIVATE_PREF_KEY =
            "ActivatePersistenceTableColumnListener";

    public static final boolean DEFAULT_ENABLED = true;

    private static final String simpleClassName =
            PersistenceTableColumnListener.class.getSimpleName();

    // needed to get column names / indices mapped from view to model
    // and to access the table model
    private final MainTable mainTable;


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
        Vector<String> storedColumns = new Vector<>(columnCount - 1);
        Vector<String> columnsWidths = new Vector<>(columnCount - 1);
        int ncWidth = -1;

        for (int i = 0; i < columnCount; i++) {
            final String name = mainTable.getColumnName(i);
            if ((name != null) && !name.isEmpty()) {
                if ("#".equals(name)) { // TODO: get "#" from prefs?
                    ncWidth = mainTable.getColumnModel().getColumn(i).getWidth();
                } else {
                    storedColumns.add(name.toLowerCase());
                    columnsWidths.add(String.valueOf(mainTable.getColumnModel().getColumn(
                            i).getWidth()));

                }
            }
        }

        // Finally, we store the new preferences.
        Globals.prefs.putStringArray(JabRefPreferences.COLUMN_NAMES,
                storedColumns.toArray(new String[storedColumns.size()]));
        Globals.prefs.putStringArray(JabRefPreferences.COLUMN_WIDTHS,
                columnsWidths.toArray(new String[columnsWidths.size()]));

        // width of the number ("#") column
        Globals.prefs.putInt(JabRefPreferences.NUMBER_COL_WIDTH, ncWidth);
    }

    /**
     * @see javax.swing.event.TableColumnModelListener#columnAdded(javax.swing.event.TableColumnModelEvent)
     */
    @Override
    public void columnAdded(TableColumnModelEvent e) {
        assert e != null : PersistenceTableColumnListener.simpleClassName + " received null event";

        updateColumnPrefs();
    }

    /**
     * @see javax.swing.event.TableColumnModelListener#columnMarginChanged(javax.swing.event.ChangeEvent)
     */
    @Override
    public void columnMarginChanged(ChangeEvent e) {
        assert e != null : PersistenceTableColumnListener.simpleClassName + " received null event";

        updateColumnPrefs();
    }

    /**
     * @see javax.swing.event.TableColumnModelListener#columnMoved(javax.swing.event.TableColumnModelEvent)
     */
    @Override
    public void columnMoved(TableColumnModelEvent e) {
        assert e != null : PersistenceTableColumnListener.simpleClassName + " received null event";

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
        assert e != null : PersistenceTableColumnListener.simpleClassName + " received null event";

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
