package org.jabref.gui.exporter;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.table.TableColumnModel;

import org.jabref.Globals;
import org.jabref.gui.JabRefDialog;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.help.HelpAction;
import org.jabref.gui.keyboard.KeyBinding;
import org.jabref.logic.exporter.SavePreferences;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.layout.LayoutFormatterPreferences;

import ca.odell.glazedlists.gui.TableFormat;
import ca.odell.glazedlists.swing.DefaultEventTableModel;
import com.jgoodies.forms.builder.ButtonBarBuilder;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: </p>
 * @author not attributable
 * @version 1.0
 */

public class ExportCustomizationDialog extends JabRefDialog {

    // Column widths for export customization dialog table:
    private static final int COL_0_WIDTH = 50;
    private static final int COL_1_WIDTH = 200;
    private static final int COL_2_WIDTH = 30;

    public ExportCustomizationDialog(final JabRefFrame frame) {

        super(frame, Localization.lang("Manage custom exports"), false, ExportCustomizationDialog.class);
        DefaultEventTableModel<List<String>> tableModel = new DefaultEventTableModel<>(
                Globals.prefs.customExports.getSortedList(), new ExportTableFormat());
        JTable table = new JTable(tableModel);
        TableColumnModel cm = table.getColumnModel();
        cm.getColumn(0).setPreferredWidth(COL_0_WIDTH);
        cm.getColumn(1).setPreferredWidth(COL_1_WIDTH);
        cm.getColumn(2).setPreferredWidth(COL_2_WIDTH);
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
                Globals.prefs.customExports.addFormat(ecd.name(), ecd.layoutFile(), ecd.extension(),
                        Globals.prefs.getLayoutFormatterPreferences(Globals.journalAbbreviationLoader),
                        Globals.prefs.loadForExportFromPreferences());
                Globals.prefs.customExports.store(Globals.prefs);
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
                Globals.prefs.customExports.store(Globals.prefs);
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
            LayoutFormatterPreferences layoutPreferences = Globals.prefs
                    .getLayoutFormatterPreferences(Globals.journalAbbreviationLoader);
            SavePreferences savePreferences = Globals.prefs.loadForExportFromPreferences();
            for (List<String> list : entries) {
                Globals.prefs.customExports.remove(list, layoutPreferences, savePreferences);
            }
            Globals.prefs.customExports.store(Globals.prefs);
        });

        Action closeAction = new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        };

        JButton close = new JButton(Localization.lang("Close"));
        close.addActionListener(closeAction);

        JButton help = new HelpAction(HelpFile.CUSTOM_EXPORTS).getHelpButton();

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
        table.requestFocus();
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
