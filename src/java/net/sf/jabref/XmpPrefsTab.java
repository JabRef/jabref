/*  Copyright (C) 2003-2011 JabRef contributors.
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
package net.sf.jabref;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.util.Vector;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

/**
 * Preference Tab for XMP.
 * 
 * Allows the user to enable and configure the XMP privacy filter.
 * 
 * @author $Author$
 * @version $Revision$ ($Date$)
 * 
 */
class XmpPrefsTab extends JPanel implements PrefsTab {

	boolean tableChanged = false;

	int rowCount;

	JTable table;

	JCheckBox privacyFilterCheckBox = new JCheckBox(Globals
		.lang("Do not write the following fields to XMP Metadata:"));

	Vector<Object> tableRows = new Vector<Object>(10);

	/**
	 * Customization of external program paths.
	 */
	public XmpPrefsTab() {
		setLayout(new BorderLayout());

		TableModel tm = new AbstractTableModel() {
			public int getRowCount() {
				return rowCount;
			}

			public int getColumnCount() {
				return 1;
			}

			public Object getValueAt(int row, int column) {
				if (row >= tableRows.size())
					return "";
				Object rowContent = tableRows.elementAt(row);
				if (rowContent == null)
					return "";
				return rowContent;
			}

			public String getColumnName(int col) {
				return Globals.lang("Field to filter");
			}

			public Class<?> getColumnClass(int column) {
				return String.class;
			}

			public boolean isCellEditable(int row, int col) {
				return true;
			}

			public void setValueAt(Object value, int row, int col) {
				tableChanged = true;

				if (tableRows.size() <= row) {
					tableRows.setSize(row + 1);
				}

				tableRows.setElementAt(value, row);
			}

		};

		table = new JTable(tm);
		TableColumnModel cm = table.getColumnModel();
		cm.getColumn(0).setPreferredWidth(140);

		FormLayout layout = new FormLayout("1dlu, 8dlu, left:pref, 4dlu, fill:pref", "");
		DefaultFormBuilder builder = new DefaultFormBuilder(layout);
		JPanel pan = new JPanel();

		JPanel tablePanel = new JPanel();
		tablePanel.setLayout(new BorderLayout());
		JScrollPane scrollPane = new JScrollPane(table, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
			JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		table.setPreferredScrollableViewportSize(new Dimension(250, 200));
		scrollPane.setMinimumSize(new Dimension(250, 300));
		tablePanel.add(scrollPane, BorderLayout.CENTER);

		JToolBar toolbar = new JToolBar(SwingConstants.VERTICAL);
		toolbar.setFloatable(false);
		toolbar.setBorder(null);
		toolbar.add(new AddRowAction());
		toolbar.add(new DeleteRowAction());

		tablePanel.add(toolbar, BorderLayout.EAST);

		// Build Prefs Tabs
		builder.appendSeparator(Globals.lang("XMP Export Privacy Settings"));
		builder.nextLine();

		builder.append(pan);
		builder.append(privacyFilterCheckBox);
		builder.nextLine();

		builder.append(pan);
		builder.append(tablePanel);
		builder.nextLine();

		pan = builder.getPanel();
		pan.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		add(pan, BorderLayout.CENTER);
	}

	class DeleteRowAction extends AbstractAction {
		public DeleteRowAction() {
			super("Delete row", GUIGlobals.getImage("remove"));
			putValue(SHORT_DESCRIPTION, Globals.lang("Delete rows"));
		}

		public void actionPerformed(ActionEvent e) {
			int[] rows = table.getSelectedRows();
			if (rows.length == 0)
				return;

			for (int i = rows.length - 1; i >= 0; i--) {
				if (rows[i] < tableRows.size()) {
					tableRows.remove(rows[i]);
				}
			}
			rowCount -= rows.length;
			if (rows.length > 1)
				table.clearSelection();
			table.revalidate();
			table.repaint();
			tableChanged = true;
		}
	}

	class AddRowAction extends AbstractAction {
		public AddRowAction() {
			super("Add row", GUIGlobals.getImage("add"));
			putValue(SHORT_DESCRIPTION, Globals.lang("Insert rows"));
		}

		public void actionPerformed(ActionEvent e) {
			int[] rows = table.getSelectedRows();
			if (rows.length == 0) {
				// No rows selected, so we just add one at the end.
				rowCount++;
				table.revalidate();
				table.repaint();
				return;
			}
			for (int i = 0; i < rows.length; i++) {
				if (rows[i] + i < tableRows.size())
					tableRows.add(rows[i] + i, "");
			}
			rowCount += rows.length;
			if (rows.length > 1)
				table.clearSelection();
			table.revalidate();
			table.repaint();
			tableChanged = true;
		}
	}

	/**
	 * Load settings from the preferences and initialize the table.
	 */
	public void setValues() {
		tableRows.clear();
		String[] names = JabRefPreferences.getInstance().getStringArray("xmpPrivacyFilters");
		for (int i = 0; i < names.length; i++) {
			tableRows.add(names[i]);
		}
		rowCount = tableRows.size() + 5;

		privacyFilterCheckBox.setSelected(JabRefPreferences.getInstance().getBoolean(
			"useXmpPrivacyFilter"));
	}

	/**
	 * Store changes to table preferences. This method is called when the user
	 * clicks Ok.
	 * 
	 */
	public void storeSettings() {

		if (table.isEditing()) {
			int col = table.getEditingColumn();
			int row = table.getEditingRow();
			table.getCellEditor(row, col).stopCellEditing();
		}

		// Now we need to make sense of the contents the user has made to the
		// table setup table. This needs to be done either if changes were made, or
        // if the checkbox is checked and no field values have been stored previously: 
        if (tableChanged ||
                (privacyFilterCheckBox.isSelected() && !Globals.prefs.hasKey("xmpPrivacyFilters"))) {

			// First we remove all rows with empty names.
			for (int i = tableRows.size() - 1; i >= 0; i--) {
				if (tableRows.elementAt(i).equals(""))
					tableRows.removeElementAt(i);
			}

			// Finally, we store the new preferences.
			JabRefPreferences.getInstance().putStringArray("xmpPrivacyFilters",
				tableRows.toArray(new String[tableRows.size()]));
		}

		JabRefPreferences.getInstance().putBoolean("useXmpPrivacyFilter", privacyFilterCheckBox.isSelected());
	}

	public boolean readyToClose() {
		return true;
	}

	public String getTabName() {
		return Globals.lang("XMP metadata");
	}
}
