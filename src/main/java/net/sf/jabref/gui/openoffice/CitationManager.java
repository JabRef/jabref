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
package net.sf.jabref.gui.openoffice;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Optional;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;

import net.sf.jabref.Globals;
import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.gui.keyboard.KeyBinding;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.openoffice.CitationEntry;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.gui.TableFormat;
import ca.odell.glazedlists.swing.DefaultEventTableModel;
import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.FormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import com.sun.star.beans.IllegalTypeException;
import com.sun.star.beans.NotRemoveableException;
import com.sun.star.beans.PropertyExistException;
import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.container.NoSuchElementException;
import com.sun.star.container.XNameAccess;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.lang.WrappedTargetException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Dialog for modifying existing citations.
 */
class CitationManager {

    private final OOBibBase ooBase;
    private final JDialog diag;
    private final EventList<CitationEntry> list;
    private final JTable table;
    private final DefaultEventTableModel<CitationEntry> tableModel;

    private static final Log LOGGER = LogFactory.getLog(CitationManager.class);


    public CitationManager(final JabRefFrame frame, OOBibBase ooBase)
            throws NoSuchElementException, WrappedTargetException, UnknownPropertyException {
        diag = new JDialog(frame, Localization.lang("Manage citations"), true);
        this.ooBase = ooBase;

        list = new BasicEventList<>();
        XNameAccess nameAccess = ooBase.getReferenceMarks();
        List<String> names = ooBase.getJabRefReferenceMarks(nameAccess);
        for (String name : names) {
            list.add(new CitationEntry(name,
                    "<html>..." + ooBase.getCitationContext(nameAccess, name, 30, 30, true) + "...</html>",
                    ooBase.getCustomProperty(name)));
        }
        tableModel = new DefaultEventTableModel<>(list, new CitationEntryFormat());
        table = new JTable(tableModel);
        diag.add(new JScrollPane(table), BorderLayout.CENTER);

        ButtonBarBuilder bb = new ButtonBarBuilder();
        bb.addGlue();
        JButton ok = new JButton(Localization.lang("OK"));
        bb.addButton(ok);
        JButton cancel = new JButton(Localization.lang("Cancel"));
        bb.addButton(cancel);
        bb.addGlue();
        bb.getPanel().setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        diag.add(bb.getPanel(), BorderLayout.SOUTH);

        diag.pack();
        diag.setSize(700, 400);

        ok.addActionListener(e -> {
            try {
                storeSettings();
            } catch (UnknownPropertyException | NotRemoveableException | PropertyExistException | IllegalTypeException |
                    IllegalArgumentException ex) {
                LOGGER.warn("Problem modifying citation", ex);
                JOptionPane.showMessageDialog(frame, Localization.lang("Problem modifying citation"));
            }
            diag.dispose();
        });

        Action cancelAction = new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                diag.dispose();
            }
        };
        cancel.addActionListener(cancelAction);

        bb.getPanel().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put
                (Globals.getKeyPrefs().getKey(KeyBinding.CLOSE_DIALOG), "close");
        bb.getPanel().getActionMap().put("close", cancelAction);

        table.getColumnModel().getColumn(0).setPreferredWidth(580);
        table.getColumnModel().getColumn(1).setPreferredWidth(110);
        table.setPreferredScrollableViewportSize(new Dimension(700, 500));
        table.addMouseListener(new TableClickListener());
    }

    private void storeSettings() throws UnknownPropertyException, NotRemoveableException, PropertyExistException,
            IllegalTypeException, IllegalArgumentException {
        for (CitationEntry entry : list) {
            Optional<String> pageInfo = entry.getPageInfo();
            if (entry.pageInfoChanged() && pageInfo.isPresent()) {
                ooBase.setCustomProperty(entry.getRefMarkName(), pageInfo.get());
            }
        }
    }

    public void showDialog() {
        diag.setLocationRelativeTo(diag.getParent());
        diag.setVisible(true);
    }



    private static class CitationEntryFormat implements TableFormat<CitationEntry> {

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public String getColumnName(int i) {
            if (i == 0) {
                return Localization.lang("Citation");
            } else {
                return Localization.lang("Extra information");
            }
        }

        @Override
        public Object getColumnValue(CitationEntry citEntry, int i) {
            if (i == 0) {
                return citEntry.getContext();
            } else {
                return citEntry.getPageInfo().orElse("");
            }
        }
    }

    private class TableClickListener extends MouseAdapter {

        @Override
        public void mouseClicked(MouseEvent e) {
            if ((e.getButton() == MouseEvent.BUTTON1) && (e.getClickCount() == 2)) {
                int row = table.rowAtPoint(e.getPoint());
                if (row >= 0) {
                    SingleCitationDialog scd = new SingleCitationDialog(list.get(row));
                    scd.showDialog();
                }
            }
        }
    }

    class SingleCitationDialog {

        private final JDialog singleCiteDialog;
        private final JTextField pageInfo = new JTextField(20);
        private final JButton okButton = new JButton(Localization.lang("OK"));
        private final JButton cancelButton = new JButton(Localization.lang("Cancel"));
        private final CitationEntry entry;


        public SingleCitationDialog(CitationEntry citEntry) {
            this.entry = citEntry;
            pageInfo.setText(entry.getPageInfo().orElse(""));

            singleCiteDialog = new JDialog(CitationManager.this.diag, Localization.lang("Citation"), true);

            FormBuilder builder = FormBuilder.create()
                    .layout(new FormLayout("left:pref, 4dlu, fill:150dlu:grow", "pref, 4dlu, pref"));
            builder.add(entry.getContext()).xyw(1, 1, 3);
            builder.add(Localization.lang("Extra information (e.g. page number)")).xy(1, 3);
            builder.add(pageInfo).xy(3, 3);
            builder.padding("10dlu, 10dlu, 10dlu, 10dlu");
            singleCiteDialog.getContentPane().add(builder.getPanel(), BorderLayout.CENTER);

            ButtonBarBuilder bb = new ButtonBarBuilder();
            bb.addGlue();
            bb.addButton(okButton);
            bb.addButton(cancelButton);
            bb.addGlue();
            bb.padding("5dlu, 5dlu, 5dlu, 5dlu");
            singleCiteDialog.add(bb.getPanel(), BorderLayout.SOUTH);

            okButton.addActionListener(e -> {
                if (pageInfo.getText().trim().isEmpty()) {
                    entry.setPageInfo(null);
                } else {
                    entry.setPageInfo(pageInfo.getText().trim());
                }
                tableModel.fireTableDataChanged();
                singleCiteDialog.dispose();
            });

            Action cancelAction = new AbstractAction() {

                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    singleCiteDialog.dispose();
                }
            };
            cancelButton.addActionListener(cancelAction);

            builder.getPanel().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put
                    (Globals.getKeyPrefs().getKey(KeyBinding.CLOSE_DIALOG), "close");
            builder.getPanel().getActionMap().put("close", cancelAction);

        }

        public void showDialog() {
            singleCiteDialog.pack();
            singleCiteDialog.setLocationRelativeTo(singleCiteDialog.getParent());
            singleCiteDialog.setVisible(true);
        }
    }
}
