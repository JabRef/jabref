/* Copyright (C) 2015-2016 JabRef contributors.
 Copyright (C) 2005 Andreas Rudert

 All programs in this directory and
 subdirectories are published under the GNU General Public License as
 described below.

 This program is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation; either version 2 of the License, or (at
 your option) any later version.

 This program is distributed in the hope that it will be useful, but
 WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 USA

 Further information about the GNU GPL is available at:
 http://www.gnu.org/copyleft/gpl.ja.html

 */
package net.sf.jabref.importer;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumnModel;

import net.sf.jabref.gui.util.FocusRequester;
import net.sf.jabref.importer.fileformat.ImportFormat;
import net.sf.jabref.logic.l10n.Localization;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Dialog to allow users to choose a file contained in a ZIP file.
 *
 * @author andreas_sf at rudert-home dot de
 */
class ZipFileChooser extends JDialog {

    private static final Log LOGGER = LogFactory.getLog(ZipFileChooser.class);


    /**
     * New Zip file chooser.
     *
     * @param owner  Owner of the file chooser
     * @param zipFile  Zip-Fle to choose from, must be readable
     */
    public ZipFileChooser(ImportCustomizationDialog importCustomizationDialog, ZipFile zipFile) {
        super(importCustomizationDialog, Localization.lang("Select file from ZIP-archive"), false);


        ZipFileChooserTableModel tableModel = new ZipFileChooserTableModel(zipFile, getSelectableZipEntries(zipFile));
        JTable table = new JTable(tableModel);
        TableColumnModel cm = table.getColumnModel();
        cm.getColumn(0).setPreferredWidth(200);
        cm.getColumn(1).setPreferredWidth(150);
        cm.getColumn(2).setPreferredWidth(100);
        JScrollPane sp = new JScrollPane(table, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setPreferredScrollableViewportSize(new Dimension(500, 150));
        if (table.getRowCount() > 0) {
            table.setRowSelectionInterval(0, 0);
        }

        // cancel: no entry is selected
        JButton cancelButton = new JButton(Localization.lang("Cancel"));
        cancelButton.addActionListener(e -> dispose());
        // ok: get selected class and check if it is instantiable as an importer
        JButton okButton = new JButton(Localization.lang("OK"));
        okButton.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(this, Localization.lang("Please select an importer."));
            } else {
                ZipFileChooserTableModel model = (ZipFileChooserTableModel) table.getModel();
                ZipEntry tempZipEntry = model.getZipEntry(row);
                CustomImporter importer = new CustomImporter();
                importer.setBasePath(model.getZipFile().getName());
                String className = tempZipEntry.getName().substring(0, tempZipEntry.getName().lastIndexOf('.'))
                        .replace("/", ".");
                importer.setClassName(className);
                try {
                    ImportFormat importFormat = importer.getInstance();
                    importer.setName(importFormat.getFormatName());
                    importer.setCliId(importFormat.getCLIId());
                    importCustomizationDialog.addOrReplaceImporter(importer);
                    dispose();
                } catch (IOException | ClassNotFoundException | InstantiationException | IllegalAccessException exc) {
                    LOGGER.warn("Could not instantiate importer: " + importer.getName(), exc);
                    JOptionPane.showMessageDialog(this, Localization.lang("Could not instantiate %0 %1",
                            importer.getName() + ":\n", exc.getMessage()));
                }
            }
        });


        // Key bindings:
        JPanel mainPanel = new JPanel();
        //ActionMap am = mainPanel.getActionMap();
        //InputMap im = mainPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        //im.put(Globals.getKeyPrefs().getKey(KeyBinds.CLOSE_DIALOG), "close");
        //am.put("close", closeAction);
        mainPanel.setLayout(new BorderLayout());
        mainPanel.add(sp, BorderLayout.CENTER);

        JPanel optionsPanel = new JPanel();
        optionsPanel.add(okButton);
        optionsPanel.add(cancelButton);
        optionsPanel.add(Box.createHorizontalStrut(5));

        getContentPane().add(mainPanel, BorderLayout.CENTER);
        getContentPane().add(optionsPanel, BorderLayout.SOUTH);
        this.setSize(getSize());
        pack();
        this.setLocationRelativeTo(importCustomizationDialog);
        new FocusRequester(table);
    }

    /**
     * Entries that can be selected with this dialog.
     *
     * @param zipFile  Zip-File
     * @return  entries that can be selected
     */
    private static ZipEntry[] getSelectableZipEntries(ZipFile zipFile) {
        List<ZipEntry> entries = new ArrayList<>();
        Enumeration<? extends ZipEntry> e = zipFile.entries();
        for (ZipEntry entry : Collections.list(e)) {
            if (!entry.isDirectory() && entry.getName().endsWith(".class")) {
                entries.add(entry);
            }
        }
        return entries.toArray(new ZipEntry[entries.size()]);
    }

    /*
     *  (non-Javadoc)
     * @see java.awt.Component#getSize()
     */
    @Override
    public Dimension getSize() {
        return new Dimension(400, 300);
    }


    /**
     * Table model for the ZIP archive contents.
     *
     * <p>Contains one row for each entry.
     * Does not contain rows for directory entries.</p>
     *
     * <p>The columns contain information about ZIP file entries:
     * <ol><li>
     *   name {@link String}
     * </li><li>
     *   time of last modification {@link Date}
     * </li><li>
     *   size (uncompressed) {@link Long}
     * </li></ol></p>
     */
    private static class ZipFileChooserTableModel extends AbstractTableModel {

        private final String[] columnNames = new String[] {Localization.lang("Name"),
                Localization.lang("Last modified"), Localization.lang("Size")};
        private final ZipEntry[] rows;
        private final ZipFile zipFile;


        ZipFileChooserTableModel(ZipFile zipFile, ZipEntry[] rows) {
            super();
            this.rows = rows;
            this.zipFile = zipFile;
        }

        /*
         *  (non-Javadoc)
         * @see javax.swing.table.TableModel#getColumnCount()
         */
        @Override
        public int getColumnCount() {
            return columnNames.length;
        }

        /*
         *  (non-Javadoc)
         * @see javax.swing.table.TableModel#getRowCount()
         */
        @Override
        public int getRowCount() {
            return this.rows.length;
        }

        /*
         *  (non-Javadoc)
         * @see javax.swing.table.TableModel#getColumnName(int)
         */
        @Override
        public String getColumnName(int col) {
            return columnNames[col];
        }

        /**
         * Zip-File entry at the given row index.
         *
         * @param rowIndex  row index
         * @return  Zip file entry
         */
        public ZipEntry getZipEntry(int rowIndex) {
            return this.rows[rowIndex];
        }

        /**
         * Zip file which contains all entries of this model.
         *
         * @return zip file
         */
        public ZipFile getZipFile() {
            return this.zipFile;
        }

        /*
         *  (non-Javadoc)
         * @see javax.swing.table.TableModel#getValueAt(int, int)
         */
        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            Object value = null;
            ZipEntry entry = getZipEntry(rowIndex);
            if (columnIndex == 0) {
                value = entry.getName();
            } else if (columnIndex == 1) {
                value = SimpleDateFormat.getDateTimeInstance().format(new Date(entry.getTime()));
            } else if (columnIndex == 2) {
                value = entry.getSize();
            }
            return value;
        }
    }

}
