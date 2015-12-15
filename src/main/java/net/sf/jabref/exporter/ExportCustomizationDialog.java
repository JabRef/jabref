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
package net.sf.jabref.exporter;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;
import javax.swing.table.TableColumnModel;

import net.sf.jabref.*;
import net.sf.jabref.gui.GUIGlobals;
import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.gui.help.HelpAction;
import net.sf.jabref.gui.keyboard.KeyBinding;
import net.sf.jabref.gui.util.FocusRequester;
import net.sf.jabref.gui.util.PositionWindow;

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

    private final JabRefFrame frame;

    private JTable table;


    public ExportCustomizationDialog(JabRefFrame frame_) throws HeadlessException {

        super(frame_, Localization.lang("Manage custom exports"), false);
        frame = frame_;
        JButton addExport = new JButton(Localization.lang("Add new"));
        addExport.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                CustomExportDialog ecd = new CustomExportDialog(frame);
                ecd.setVisible(true); // ecd.show(); -> deprecated since 1.5
                if (ecd.okPressed()) {
                    String[] newFormat = new String[]{ecd.name(), ecd.layoutFile(), ecd.extension()};
                    Globals.prefs.customExports.addFormat(newFormat);
                    Globals.prefs.customExports.store();
                }
            }
        });

        JButton modify = new JButton(Localization.lang("Modify"));
        modify.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                int row = table.getSelectedRow();
                if (row == -1) {
                    return;
                }
                String[] old = Globals.prefs.customExports.getSortedList().get(row);
                CustomExportDialog ecd = new CustomExportDialog(frame, old[0], old[1], old[2]);
                ecd.setVisible(true); // ecd.show(); -> deprecated since 1.5
                if (ecd.okPressed()) {
                    old[0] = ecd.name();
                    old[1] = ecd.layoutFile();
                    old[2] = ecd.extension();
                    table.revalidate();
                    table.repaint();
                    Globals.prefs.customExports.store();
                }
            }
        });

        JButton remove = new JButton(Localization.lang("Remove"));
        remove.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                int[] rows = table.getSelectedRows();
                if (rows.length == 0) {
                    return;
                }
                String[][] entries = new String[rows.length][];
                for (int i = 0; i < rows.length; i++) {
                    entries[i] = Globals.prefs.customExports.getSortedList().get(rows[i]);
                }
                for (int i = 0; i < rows.length; i++) {
                    Globals.prefs.customExports.remove(entries[i]);
                }
                Globals.prefs.customExports.store();
            }
        });

        AbstractAction closeAction = new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        };

        JButton close = new JButton(Localization.lang("Close"));
        close.addActionListener(closeAction);

        JButton help = new HelpAction(frame.helpDiag, GUIGlobals.exportCustomizationHelp).getIconButton();

        DefaultEventTableModel<String[]> tableModel = new DefaultEventTableModel<>(
                Globals.prefs.customExports.getSortedList(),
                new ExportTableFormat());
        table = new JTable(tableModel);
        TableColumnModel cm = table.getColumnModel();
        cm.getColumn(0).setPreferredWidth(GUIGlobals.EXPORT_DIALOG_COL_0_WIDTH);
        cm.getColumn(1).setPreferredWidth(GUIGlobals.EXPORT_DIALOG_COL_1_WIDTH);
        cm.getColumn(2).setPreferredWidth(GUIGlobals.EXPORT_DIALOG_COL_2_WIDTH);
        JScrollPane sp = new JScrollPane(table, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setPreferredScrollableViewportSize(
                new Dimension(500, 150));
        if (table.getRowCount() > 0) {
            table.setRowSelectionInterval(0, 0);
        }

        // Key bindings:
        JPanel main = new JPanel();
        ActionMap am = main.getActionMap();
        InputMap im = main.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        im.put(Globals.getKeyPrefs().getKey(KeyBinding.CLOSE_DIALOG), "close");
        am.put("close", closeAction);
        //am = table.getActionMap();
        //im = table.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        //im.put(Globals.getKeyPrefs().getKey()KeyBinds.CLOSE_DIALOG), "close");
        //am.put("close", closeAction);
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
        PositionWindow.placeDialog(this, frame);
        new FocusRequester(table);
    }


    private static class ExportTableFormat implements TableFormat<String[]> {

        @Override
        public Object getColumnValue(String[] strings, int i) {
            return strings[i];
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
                return Localization.lang("File extension");
            }
        }

    }

}
