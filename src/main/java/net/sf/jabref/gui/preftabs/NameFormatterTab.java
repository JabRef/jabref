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
package net.sf.jabref.gui.preftabs;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import net.sf.jabref.gui.GUIGlobals;
import net.sf.jabref.Globals;
import net.sf.jabref.exporter.layout.format.NameFormatter;
import net.sf.jabref.gui.IconTheme;
import net.sf.jabref.gui.help.HelpAction;
import net.sf.jabref.gui.help.HelpDialog;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import net.sf.jabref.logic.l10n.Localization;

public class NameFormatterTab extends JPanel implements PrefsTab {

    public static final String NAME_FORMATTER_VALUE = "nameFormatterFormats";

    public static final String NAME_FORMATER_KEY = "nameFormatterNames";


    public static Map<String, String> getNameFormatters() {

        Map<String, String> result = new HashMap<String, String>();

        String[] names = Globals.prefs.getStringArray(NameFormatterTab.NAME_FORMATER_KEY);
        String[] formats = Globals.prefs.getStringArray(NameFormatterTab.NAME_FORMATTER_VALUE);

        if (names == null) {
            names = new String[] {};
        }
        if (formats == null) {
            formats = new String[] {};
        }

        for (int i = 0; i < names.length; i++) {
            if (i < formats.length) {
                result.put(names[i], formats[i]);
            } else {
                result.put(names[i], NameFormatter.DEFAULT_FORMAT);
            }
        }

        return result;
    }


    private boolean tableChanged;

    private final JTable table;

    private int rowCount = -1;

    private final Vector<TableRow> tableRows = new Vector<TableRow>(10);


    static class TableRow {

        String name;

        String format;


        public TableRow() {
            this("");
        }

        public TableRow(String name) {
            this(name, NameFormatter.DEFAULT_FORMAT);
        }

        public TableRow(String name, String format) {
            this.name = name;
            this.format = format;
        }
    }


    /**
     * Tab to create custom Name Formatters
     * 
     */
    public NameFormatterTab(HelpDialog helpDialog) {
        setLayout(new BorderLayout());

        TableModel tableModel = new AbstractTableModel() {

            @Override
            public int getRowCount() {
                return rowCount;
            }

            @Override
            public int getColumnCount() {
                return 2;
            }

            @Override
            public Object getValueAt(int row, int column) {
                if (row >= tableRows.size()) {
                    return "";
                }
                TableRow tr = tableRows.elementAt(row);
                if (tr == null) {
                    return "";
                }
                switch (column) {
                case 0:
                    return tr.name;
                case 1:
                    return tr.format;
                }
                return null; // Unreachable.
            }

            @Override
            public String getColumnName(int col) {
                return col == 0 ? Localization.lang("Formatter Name") : Localization.lang("Format String");
            }

            @Override
            public Class<String> getColumnClass(int column) {
                return String.class;
            }

            @Override
            public boolean isCellEditable(int row, int col) {
                return true;
            }

            @Override
            public void setValueAt(Object value, int row, int col) {
                tableChanged = true;

                // Make sure the vector is long enough.
                while (row >= tableRows.size()) {
                    tableRows.add(new TableRow());
                }

                TableRow rowContent = tableRows.elementAt(row);

                if (col == 0) {
                    rowContent.name = value.toString();
                } else {
                    rowContent.format = value.toString();
                }
            }
        };

        table = new JTable(tableModel);
        TableColumnModel columnModel = table.getColumnModel();
        columnModel.getColumn(0).setPreferredWidth(140);
        columnModel.getColumn(1).setPreferredWidth(400);

        FormLayout layout = new FormLayout("1dlu, 8dlu, left:pref, 4dlu, fill:pref", "");

        DefaultFormBuilder builder = new DefaultFormBuilder(layout);

        JPanel pan = new JPanel();

        JPanel tabPanel = new JPanel();
        tabPanel.setLayout(new BorderLayout());
        JScrollPane scrollPane = new JScrollPane(table, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        table.setPreferredScrollableViewportSize(new Dimension(250, 200));
        scrollPane.setMinimumSize(new Dimension(250, 300));
        scrollPane.setPreferredSize(new Dimension(600, 300));
        tabPanel.add(scrollPane, BorderLayout.CENTER);

        JToolBar toolBar = new JToolBar(SwingConstants.VERTICAL);
        toolBar.setFloatable(false);
        toolBar.setBorder(null);
        toolBar.add(new AddRowAction());
        toolBar.add(new DeleteRowAction());
        toolBar.add(new HelpAction(helpDialog, GUIGlobals.nameFormatterHelp,
                Localization.lang("Help on Name Formatting"), IconTheme.JabRefIcon.HELP.getIcon()));

        tabPanel.add(toolBar, BorderLayout.EAST);

        builder.appendSeparator(Localization.lang("Special Name Formatters"));
        builder.nextLine();
        builder.append(pan);
        builder.append(tabPanel);
        builder.nextLine();

        pan = builder.getPanel();
        pan.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        add(pan, BorderLayout.CENTER);
    }

    @Override
    public void setValues() {
        tableRows.clear();
        String[] names = Globals.prefs.getStringArray(NameFormatterTab.NAME_FORMATER_KEY);
        String[] formats = Globals.prefs.getStringArray(NameFormatterTab.NAME_FORMATTER_VALUE);

        if (names == null) {
            names = new String[] {};
        }
        if (formats == null) {
            formats = new String[] {};
        }

        for (int i = 0; i < names.length; i++) {
            if (i < formats.length) {
                tableRows.add(new TableRow(names[i], formats[i]));
            } else {
                tableRows.add(new TableRow(names[i]));
            }
        }
        rowCount = tableRows.size() + 5;
    }


    class DeleteRowAction extends AbstractAction {

        public DeleteRowAction() {
            super("Delete row", IconTheme.JabRefIcon.REMOVE_NOBOX.getIcon());
            putValue(Action.SHORT_DESCRIPTION, Localization.lang("Delete rows"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            tableChanged = true;

            int[] selectedRows = table.getSelectedRows();

            int numberDeleted = 0;

            for (int i = selectedRows.length - 1; i >= 0; i--) {
                if (selectedRows[i] < tableRows.size()) {
                    tableRows.remove(selectedRows[i]);
                    numberDeleted++;
                }
            }

            rowCount -= numberDeleted;

            if (selectedRows.length > 1) {
                table.clearSelection();
            }

            table.revalidate();
            table.repaint();
        }
    }

    class AddRowAction extends AbstractAction {

        public AddRowAction() {
            super("Add row", IconTheme.JabRefIcon.ADD_NOBOX.getIcon());
            putValue(Action.SHORT_DESCRIPTION, Localization.lang("Insert rows"));
        }

        @Override
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
                if (rows[i] + i - 1 < tableRows.size()) {
                    tableRows.add(Math.max(0, rows[i] + i - 1), new TableRow());
                }
            }
            rowCount += rows.length;
            if (rows.length > 1) {
                table.clearSelection();
            }
            table.revalidate();
            table.repaint();
            tableChanged = true;
        }
    }


    /**
     * Store changes to table preferences. This method is called when the user
     * clicks Ok.
     * 
     */
    @Override
    public void storeSettings() {

        if (table.isEditing()) {
            int col = table.getEditingColumn();
            int row = table.getEditingRow();
            table.getCellEditor(row, col).stopCellEditing();
        }

        // Now we need to make sense of the contents the user has made to the
        // table setup table.
        if (tableChanged) {
            // First we remove all rows with empty names.
            int i = 0;
            while (i < tableRows.size()) {
                if (tableRows.elementAt(i).name.isEmpty()) {
                    tableRows.removeElementAt(i);
                } else {
                    i++;
                }
            }
            // Then we make arrays
            String[] names = new String[tableRows.size()];
            String[] formats = new String[tableRows.size()];

            for (i = 0; i < tableRows.size(); i++) {
                TableRow tr = tableRows.elementAt(i);
                names[i] = tr.name;
                formats[i] = tr.format;
            }

            // Finally, we store the new preferences.
            Globals.prefs.putStringArray(NameFormatterTab.NAME_FORMATER_KEY, names);
            Globals.prefs.putStringArray(NameFormatterTab.NAME_FORMATTER_VALUE, formats);
        }
    }

    @Override
    public boolean validateSettings() {
        return true;
    }

    @Override
    public String getTabName() {
        return Localization.lang("Name formatter");
    }
}
