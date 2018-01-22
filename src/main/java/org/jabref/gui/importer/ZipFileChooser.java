package org.jabref.gui.importer;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumnModel;

import org.jabref.gui.JabRefDialog;
import org.jabref.logic.importer.fileformat.CustomImporter;
import org.jabref.logic.l10n.Localization;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dialog to allow users to choose a file contained in a ZIP file.
 */
class ZipFileChooser extends JabRefDialog {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZipFileChooser.class);


    /**
     * New ZIP file chooser.
     *
     * @param importCustomizationDialog  Owner of the file chooser
     * @param zipFile  ZIP-Fle to choose from, must be readable
     */
    public ZipFileChooser(ImportCustomizationDialog importCustomizationDialog, ZipFile zipFile) {
        super(importCustomizationDialog, Localization.lang("Select file from ZIP-archive"), false, ZipFileChooser.class);


        ZipFileChooserTableModel tableModel = new ZipFileChooserTableModel(zipFile, getSelectableZipEntries(zipFile));
        JTable table = new JTable(tableModel);
        TableColumnModel cm = table.getColumnModel();
        cm.getColumn(0).setPreferredWidth(200);
        cm.getColumn(1).setPreferredWidth(150);
        cm.getColumn(2).setPreferredWidth(100);
        JScrollPane sp = new JScrollPane(table, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
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
                String className = tempZipEntry.getName().substring(0, tempZipEntry.getName().lastIndexOf('.')).replace(
                        "/", ".");

                try {
                    CustomImporter importer = new CustomImporter(model.getZipFile().getName(), className);
                    importCustomizationDialog.addOrReplaceImporter(importer);
                    dispose();
                } catch (ClassNotFoundException exc) {
                    LOGGER.warn("Could not instantiate importer: " + className, exc);
                    JOptionPane.showMessageDialog(this, Localization.lang("Could not instantiate %0 %1",
                            className + ":\n", exc.getMessage()));
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
        table.requestFocus();
    }

    /**
     * Entries that can be selected with this dialog.
     *
     * @param zipFile  ZIP-File
     * @return  entries that can be selected
     */
    private static List<ZipEntry> getSelectableZipEntries(ZipFile zipFile) {
        List<ZipEntry> entries = new ArrayList<>();
        Enumeration<? extends ZipEntry> e = zipFile.entries();
        for (ZipEntry entry : Collections.list(e)) {
            if (!entry.isDirectory() && entry.getName().endsWith(".class")) {
                entries.add(entry);
            }
        }
        return entries;
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

        private final List<String> columnNames = Arrays.asList(Localization.lang("Name"),
                Localization.lang("Last modified"), Localization.lang("Size"));
        private final List<ZipEntry> rows;
        private final ZipFile zipFile;


        ZipFileChooserTableModel(ZipFile zipFile, List<ZipEntry> rows) {
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
            return columnNames.size();
        }

        /*
         *  (non-Javadoc)
         * @see javax.swing.table.TableModel#getRowCount()
         */
        @Override
        public int getRowCount() {
            return this.rows.size();
        }

        /*
         *  (non-Javadoc)
         * @see javax.swing.table.TableModel#getColumnName(int)
         */
        @Override
        public String getColumnName(int col) {
            return columnNames.get(col);
        }

        /**
         * ZIP-File entry at the given row index.
         *
         * @param rowIndex  row index
         * @return  ZIP file entry
         */
        public ZipEntry getZipEntry(int rowIndex) {
            return this.rows.get(rowIndex);
        }

        /**
         * ZIP file which contains all entries of this model.
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
                value = ZonedDateTime.ofInstant(new Date(entry.getTime()).toInstant(),
                        ZoneId.systemDefault())
                        .format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM));
            } else if (columnIndex == 2) {
                value = entry.getSize();
            }
            return value;
        }
    }

}
