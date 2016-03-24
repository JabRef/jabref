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
package net.sf.jabref.openoffice;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.gui.TableFormat;
import ca.odell.glazedlists.swing.DefaultEventTableModel;
import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import com.sun.star.beans.IllegalTypeException;
import com.sun.star.beans.NotRemoveableException;
import com.sun.star.beans.PropertyExistException;
import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.container.NoSuchElementException;
import com.sun.star.container.XNameAccess;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.lang.WrappedTargetException;

import net.sf.jabref.Globals;
import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.gui.keyboard.KeyBinding;
import net.sf.jabref.logic.l10n.Localization;

import javax.swing.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Optional;

/**
 * Dialog for modifying existing citations.
 */
class CitationManager {

    private final OOBibBase ooBase;
    private final JDialog diag;
    private final EventList<CitEntry> list;
    private final JTable table;
    private final DefaultEventTableModel<CitEntry> tableModel;

    private static final Log LOGGER = LogFactory.getLog(CitationManager.class);


    public CitationManager(final JabRefFrame frame, OOBibBase ooBase)
            throws NoSuchElementException, WrappedTargetException, UnknownPropertyException {
        diag = new JDialog(frame, Localization.lang("Manage citations"), true);
        this.ooBase = ooBase;

        list = new BasicEventList<>();
        XNameAccess nameAccess = ooBase.getReferenceMarks();
        java.util.List<String> names = ooBase.getJabRefReferenceMarks(nameAccess);
        for (String name : names) {
            list.add(new CitEntry(name,
                    "<html>..." + ooBase.getCitationContext(nameAccess, name, 30, 30, true) + "...</html>",
                    ooBase.getCustomProperty(name)));
        }
        tableModel = new DefaultEventTableModel<>(list, new CitEntryFormat());
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

        table.getColumnModel().getColumn(0).setPreferredWidth(600);
        table.getColumnModel().getColumn(1).setPreferredWidth(90);
        table.setPreferredScrollableViewportSize(new Dimension(700, 500));
        table.addMouseListener(new TableClickListener());
    }

    private void storeSettings() throws UnknownPropertyException, NotRemoveableException, PropertyExistException,
            IllegalTypeException, IllegalArgumentException {
        for (CitEntry entry : list) {
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


    static class CitEntry implements Comparable<CitEntry> {

        private final String refMarkName;
        private Optional<String> pageInfo;
        private final String context;
        private final Optional<String> origPageInfo;


        public CitEntry(String refMarkName, String context, Optional<String> optional) {
            this.refMarkName = refMarkName;
            this.context = context;
            this.pageInfo = optional;
            this.origPageInfo = optional;
        }

        public Optional<String> getPageInfo() {
            return pageInfo;
        }

        public String getRefMarkName() {
            return refMarkName;
        }

        public boolean pageInfoChanged() {
            if (pageInfo.isPresent() ^ origPageInfo.isPresent()) {
                return true;
            }
            if (!pageInfo.isPresent()) {
                // This means that origPageInfo.isPresent is also false
                return false;
            } else {
                // So origPageInfo.isPresent is true here
                return pageInfo.get().compareTo(origPageInfo.get()) != 0;
            }
        }

        @Override
        public int compareTo(CitEntry other) {
            return this.refMarkName.compareTo(other.refMarkName);
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof CitEntry) {
                CitEntry other = (CitEntry) o;
                return this.refMarkName.equals(other.refMarkName);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return this.refMarkName.hashCode();
        }

        public String getContext() {
            return context;
        }

        public void setPageInfo(Optional<String> trim) {
            pageInfo = trim;

        }
    }

    private static class CitEntryFormat implements TableFormat<CitEntry> {

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
        public Object getColumnValue(CitEntry citEntry, int i) {
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
                    SingleCitDialog scd = new SingleCitDialog(list.get(row));
                    scd.showDialog();
                }
            }
        }
    }

    class SingleCitDialog {

        private final JDialog singleCiteDialog;
        private final JTextField pageInfo = new JTextField(20);
        private final JLabel title;
        private final JButton okButton = new JButton(Localization.lang("OK"));
        private final JButton cancelButton = new JButton(Localization.lang("Cancel"));
        private final CitEntry entry;


        public SingleCitDialog(CitEntry citEntry) {
            this.entry = citEntry;
            title = new JLabel(entry.getContext());
            pageInfo.setText(entry.getPageInfo().orElse(""));

            singleCiteDialog = new JDialog(CitationManager.this.diag, Localization.lang("Citation"), true);

            DefaultFormBuilder b = new DefaultFormBuilder(
                    new FormLayout("left:pref, 4dlu, left:150dlu", ""));
            b.append(title, 3);
            b.nextLine();
            b.append(Localization.lang("Extra information (e.g. page number)"));
            b.append(pageInfo);
            b.getPanel().setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            singleCiteDialog.getContentPane().add(b.getPanel(), BorderLayout.CENTER);

            ButtonBarBuilder bb = new ButtonBarBuilder();
            bb.addGlue();
            bb.addButton(okButton);
            bb.addButton(cancelButton);
            bb.addGlue();
            bb.getPanel().setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            singleCiteDialog.add(bb.getPanel(), BorderLayout.SOUTH);

            okButton.addActionListener(e -> {
                if (pageInfo.getText().trim().isEmpty()) {
                    entry.setPageInfo(Optional.empty());
                } else {
                    entry.setPageInfo(Optional.of(pageInfo.getText().trim()));
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

            b.getPanel().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put
                    (Globals.getKeyPrefs().getKey(KeyBinding.CLOSE_DIALOG), "close");
            b.getPanel().getActionMap().put("close", cancelAction);

        }

        public void showDialog() {
            singleCiteDialog.pack();
            singleCiteDialog.setLocationRelativeTo(singleCiteDialog.getParent());
            singleCiteDialog.setVisible(true);
        }
    }
}
