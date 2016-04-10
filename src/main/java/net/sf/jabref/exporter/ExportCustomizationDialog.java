/*  Copyright (C) 2003-2016 JabRef contributors.
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
package net.sf.jabref.exporter;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.*;
import javax.swing.table.TableColumnModel;

import net.sf.jabref.*;
import net.sf.jabref.gui.GUIGlobals;
import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.gui.help.HelpFiles;
import net.sf.jabref.gui.help.HelpAction;
import net.sf.jabref.gui.keyboard.KeyBinding;
import net.sf.jabref.gui.util.FocusRequester;
import com.jgoodies.forms.builder.ButtonBarBuilder;
import ca.odell.glazedlists.gui.TableFormat;
import ca.odell.glazedlists.swing.DefaultEventTableModel;
import net.sf.jabref.logic.l10n.Localization;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: </p>
 * @author not attributable
 * @version 1.0
 */

public class ExportCustomizationDialog extends JDialog {

    public ExportCustomizationDialog(final JabRefFrame frame) {

        super(frame, Localization.lang("Manage custom exports"), false);
        DefaultEventTableModel<List<String>> tableModel = new DefaultEventTableModel<>(
                Globals.prefs.customExports.getSortedList(), new ExportTableFormat());
        JTable table = new JTable(tableModel);
        TableColumnModel cm = table.getColumnModel();
        cm.getColumn(0).setPreferredWidth(GUIGlobals.EXPORT_DIALOG_COL_0_WIDTH);
        cm.getColumn(1).setPreferredWidth(GUIGlobals.EXPORT_DIALOG_COL_1_WIDTH);
        cm.getColumn(2).setPreferredWidth(GUIGlobals.EXPORT_DIALOG_COL_2_WIDTH);
        JScrollPane sp = new JScrollPane(table, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setPreferredScrollableViewportSize(new Dimension(500, 150));
        if (table.getRowCount() > 0) {
            table.setRowSelectionInterval(0, 0);
        }


        JButton addExport = new JButton(Localization.lang("Add new"));
        addExport.addActionListener(e -> {
            CustomExportDialog ecd = new CustomExportDialog(frame);
            ecd.setVisible(true);
            if (ecd.okPressed()) {
                List<String> newFormat = Arrays.asList(ecd.name(), ecd.layoutFile(), ecd.extension());
                Globals.prefs.customExports.addFormat(newFormat);
                Globals.prefs.customExports.store();
            }
        });

        JButton modify = new JButton(Localization.lang("Modify"));
        modify.addActionListener(e -> {
                int row = table.getSelectedRow();
                if (row == -1) {
                    return;
                }
                List<String> old = Globals.prefs.customExports.getSortedList().get(row);
                CustomExportDialog ecd = new CustomExportDialog(frame, old.get(0), old.get(1), old.get(2));
                ecd.setVisible(true); // ecd.show(); -> deprecated since 1.5
                if (ecd.okPressed()) {
                    old.set(0, ecd.name());
                    old.set(1, ecd.layoutFile());
                    old.set(2, ecd.extension());
                    table.revalidate();
                    table.repaint();
                    Globals.prefs.customExports.store();
                }
        });

        JButton remove = new JButton(Localization.lang("Remove"));
        remove.addActionListener(e -> {
                int[] rows = table.getSelectedRows();
                if (rows.length == 0) {
                    return;
                }
                List<List<String>> entries = new ArrayList<>();
                for (int i = 0; i < rows.length; i++) {
                    entries.add(Globals.prefs.customExports.getSortedList().get(rows[i]));
                }
                for (List<String> list : entries) {
                    Globals.prefs.customExports.remove(list);
                }
                Globals.prefs.customExports.store();
        });

        Action closeAction = new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        };

        JButton close = new JButton(Localization.lang("Close"));
        close.addActionListener(closeAction);

        JButton help = new HelpAction(HelpFiles.exportCustomizationHelp).getHelpButton();

        // Key bindings:
        JPanel main = new JPanel();
        ActionMap am = main.getActionMap();
        InputMap im = main.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        im.put(Globals.getKeyPrefs().getKey(KeyBinding.CLOSE_DIALOG), "close");
        am.put("close", closeAction);
        main.setLayout(new BorderLayout());
        main.add(sp, BorderLayout.CENTER);
        JPanel buttons = new JPanel();
        ButtonBarBuilder bb = new ButtonBarBuilder(buttons);
        buttons.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        bb.addGlue();
        bb.addButton(addExport);
        bb.addButton(modify);
        bb.addButton(remove);
        bb.addButton(close);
        bb.addUnrelatedGap();
        bb.addButton(help);
        bb.addGlue();

        getContentPane().add(main, BorderLayout.CENTER);
        getContentPane().add(buttons, BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(frame);
        new FocusRequester(table);
    }


    private static class ExportTableFormat implements TableFormat<List<String>> {

        @Override
        public Object getColumnValue(List<String> strings, int i) {
            return strings.get(i);
        }

        @Override
        public int getColumnCount() {
            return 3;
        }

        @Override
        public String getColumnName(int col) {
            switch (col) {
            case 0:
                return Localization.lang("Export name");
            case 1:
                return Localization.lang("Main layout file");
            default:
                return Localization.lang("Extension");
            }
        }
    }
}
