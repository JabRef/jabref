package net.sf.jabref.gui;

import java.util.Vector;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;

import net.sf.jabref.Globals;

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
		Vector<String> storedColumns = new Vector<String>(columnCount - 1);
		Vector<String> columnsWidths = new Vector<String>(columnCount - 1);
		int ncWidth = -1;

		for (int i = 0; i < columnCount; i++) {
			final String name = mainTable.getColumnName(i);
            if (name == null || name.equals("")) {
				continue;
			} else if (name.equals("#")) { // TODO: get "#" from prefs?
				ncWidth = mainTable.getColumnModel().getColumn(i).getWidth();

			} else {
				storedColumns.add(name.toLowerCase());
				columnsWidths.add(String.valueOf(mainTable.getColumnModel().getColumn(
						i).getWidth()));

			}
		}

		// Finally, we store the new preferences.
		Globals.prefs.putStringArray("columnNames",
				storedColumns.toArray(new String[0]));
		Globals.prefs.putStringArray("columnWidths",
				columnsWidths.toArray(new String[0]));

		// width of the number ("#") column
		Globals.prefs.putInt("numberColWidth", ncWidth);
	}

	/**
	 * @see javax.swing.event.TableColumnModelListener#columnAdded(javax.swing.event.TableColumnModelEvent)
	 */
	public void columnAdded(TableColumnModelEvent e) {
		assert e != null : simpleClassName + " received null event";

		updateColumnPrefs();
	}

	/**
	 * @see javax.swing.event.TableColumnModelListener#columnMarginChanged(javax.swing.event.ChangeEvent)
	 */
	public void columnMarginChanged(ChangeEvent e) {
		assert e != null : simpleClassName + " received null event";
		
		updateColumnPrefs();
	}

	/**
	 * @see javax.swing.event.TableColumnModelListener#columnMoved(javax.swing.event.TableColumnModelEvent)
	 */
	public void columnMoved(TableColumnModelEvent e) {
		assert e != null : simpleClassName + " received null event";

		// not really moved, ignore ...
		if (e.getFromIndex() == e.getToIndex())
			return;

		updateColumnPrefs();

	}

	/**
	 * @see javax.swing.event.TableColumnModelListener#columnRemoved(javax.swing.event.TableColumnModelEvent)
	 */
	public void columnRemoved(TableColumnModelEvent e) {
		assert e != null : simpleClassName + " received null event";

		updateColumnPrefs();

	}

	/**
	 * @see javax.swing.event.TableColumnModelListener#columnSelectionChanged(javax.swing.event.ListSelectionEvent)
	 */
	public void columnSelectionChanged(ListSelectionEvent e) {
		// ignore
	}

}
