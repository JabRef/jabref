/*  Copyright (C) 2005-2015 JabRef contributors.
 Copyright (C) 2005 Andreas Rudert, based on ExportCustomizationDialog by ??

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
package net.sf.jabref.importer;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.zip.ZipFile;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumnModel;

import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.gui.keyboard.KeyBinding;
import net.sf.jabref.importer.fileformat.ImportFormat;
import net.sf.jabref.logic.l10n.Localization;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import net.sf.jabref.*;
import net.sf.jabref.gui.FileDialogs;
import net.sf.jabref.gui.GUIGlobals;
import net.sf.jabref.gui.help.HelpAction;
import net.sf.jabref.gui.util.FocusRequester;
import net.sf.jabref.gui.util.PositionWindow;

import com.jgoodies.forms.builder.ButtonBarBuilder;

/**
 * Dialog to manage custom importers.
 */
public class ImportCustomizationDialog extends JDialog {
    private final JabRefFrame frame;

    private JTable customImporterTable;
    private final JabRefPreferences prefs = Globals.prefs;
    private final ImportCustomizationDialog importCustomizationDialog;

    private static final Log LOGGER = LogFactory.getLog(ImportCustomizationDialog.class);


    /*
    *  (non-Javadoc)
    * @see java.awt.Component#getSize()
    */
    @Override
    public Dimension getSize() {
        int width = GUIGlobals.IMPORT_DIALOG_COL_0_WIDTH
                + GUIGlobals.IMPORT_DIALOG_COL_1_WIDTH
                + GUIGlobals.IMPORT_DIALOG_COL_2_WIDTH
                + GUIGlobals.IMPORT_DIALOG_COL_3_WIDTH;
        return new Dimension(width, width / 2);
    }

    /**
     * Converts a path relative to a base-path into a class name.
     *
     * @param basePath  base path
     * @param path  path that includes base-path as a prefix
     * @return  class name
     */
    private static String pathToClass(File basePath, File path) {
        String className = null;
        // remove leading basepath from path
        while (!path.equals(basePath)) {
            className = path.getName() + (className != null ? "." + className : "");
            path = path.getParentFile();
        }
        int lastDot = className.lastIndexOf('.');
        if (lastDot < 0) {
            return className;
        }
        className = className.substring(0, lastDot);
        return className;
    }

    /**
     * Adds an importer to the model that underlies the custom importers.
     *
     * @param importer  importer
     */
    void addOrReplaceImporter(CustomImportList.Importer importer) {
        prefs.customImports.replaceImporter(importer);
        Globals.importFormatReader.resetImportFormats();
        ((ImportTableModel) customImporterTable.getModel()).fireTableDataChanged();
    }

    /**
     *
     * @param frame_
     * @throws HeadlessException
     */
    public ImportCustomizationDialog(JabRefFrame frame_) throws HeadlessException {
        super(frame_, Localization.lang("Manage custom imports"), false);
        this.importCustomizationDialog = this;
        frame = frame_;

        JButton addFromFolderButton = new JButton(Localization.lang("Add from folder"));
        addFromFolderButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                String chosenFileStr = null;
                CustomImportList.Importer importer = new CustomImportList.Importer();
                importer.setBasePath(FileDialogs.getNewDir(frame, new File(prefs.get(JabRefPreferences.WORKING_DIRECTORY)), "",
                        Localization.lang("Select Classpath of New Importer"), JFileChooser.CUSTOM_DIALOG, false));
                if (importer.getBasePath() != null) {
                    chosenFileStr = FileDialogs.getNewFile(frame, importer.getFileFromBasePath(), ".class",
                            Localization.lang("Select new ImportFormat Subclass"), JFileChooser.CUSTOM_DIALOG, false);
                }
                if (chosenFileStr != null) {
                    try {
                        importer.setClassName(pathToClass(importer.getFileFromBasePath(), new File(chosenFileStr)));
                        importer.setName(importer.getInstance().getFormatName());
                        importer.setCliId(importer.getInstance().getCLIId());
                        addOrReplaceImporter(importer);
                        customImporterTable.revalidate();
                        customImporterTable.repaint();
                    } catch (Exception exc) {
                        JOptionPane.showMessageDialog(frame, Localization.lang("Could not instantiate %0", chosenFileStr));
                    } catch (NoClassDefFoundError exc) {
                        JOptionPane.showMessageDialog(frame, Localization.lang("Could not instantiate %0. Have you chosen the correct package path?", chosenFileStr));
                    }

                }
            }
        });
        addFromFolderButton.setToolTipText(Localization.lang("Add a (compiled) custom ImportFormat class from a class path.") + "\n" + Localization.lang("The path need not be on the classpath of JabRef."));

        JButton addFromJarButton = new JButton(Localization.lang("Add from jar"));
        addFromJarButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                String basePath = FileDialogs.getNewFile(frame, new File(prefs.get(JabRefPreferences.WORKING_DIRECTORY)), ".zip,.jar",
                        Localization.lang("Select a Zip-archive"), JFileChooser.CUSTOM_DIALOG, false);

                if (basePath != null) {
                    try (ZipFile zipFile = new ZipFile(new File(basePath), ZipFile.OPEN_READ)) {
                        ZipFileChooser zipFileChooser = new ZipFileChooser(importCustomizationDialog, zipFile);
                        zipFileChooser.setVisible(true);
                        customImporterTable.revalidate();
                        customImporterTable.repaint(10);
                    } catch (IOException exc) {
                        LOGGER.info("Could not open Zip-archive.", exc);
                        JOptionPane.showMessageDialog(frame, Localization.lang("Could not open %0", basePath)
                                + "\n" + Localization.lang("Have you chosen the correct package path?"));
                    } catch (NoClassDefFoundError exc) {
                        LOGGER.info("Could not instantiate Zip-archive reader.", exc);
                        JOptionPane.showMessageDialog(frame, Localization.lang("Could not instantiate %0", basePath)
                                + "\n" + Localization.lang("Have you chosen the correct package path?"));
                    }
                }
            }
        });
        addFromJarButton.setToolTipText(Localization.lang("Add a (compiled) custom ImportFormat class from a Zip-archive.") + "\n" +
                Localization.lang("The Zip-archive need not be on the classpath of JabRef."));

        JButton showDescButton = new JButton(Localization.lang("Show description"));
        showDescButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                int row = customImporterTable.getSelectedRow();
                if (row != -1) {
                    CustomImportList.Importer importer = ((ImportTableModel) customImporterTable.getModel()).getImporter(row);
                    try {
                        ImportFormat importFormat = importer.getInstance();
                        JOptionPane.showMessageDialog(frame, importFormat.getDescription());
                    } catch (Exception exc) {
                        exc.printStackTrace();
                        JOptionPane.showMessageDialog(frame, Localization.lang("Could not instantiate %0 %1", importer.getName() + ":\n", exc.getMessage()));
                    }
                } else {
                    JOptionPane.showMessageDialog(frame, Localization.lang("Please select an importer."));
                }
            }
        });

        JButton removeButton = new JButton(Localization.lang("Remove"));
        removeButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                int row = customImporterTable.getSelectedRow();
                if (row != -1) {
                    customImporterTable.removeRowSelectionInterval(row, row);
                    prefs.customImports.remove(((ImportTableModel) customImporterTable.getModel()).getImporter(row));
                    Globals.importFormatReader.resetImportFormats();
                    customImporterTable.revalidate();
                    customImporterTable.repaint();
                } else {
                    JOptionPane.showMessageDialog(frame, Localization.lang("Please select an importer."));
                }
            }
        });

        AbstractAction closeAction = new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        };

        JButton closeButton = new JButton(Localization.lang("Close"));
        closeButton.addActionListener(closeAction);

        JButton helpButton = new HelpAction(frame.helpDiag, GUIGlobals.importCustomizationHelp).getIconButton();

        ImportTableModel tableModel = new ImportTableModel();
        customImporterTable = new JTable(tableModel);
        TableColumnModel cm = customImporterTable.getColumnModel();
        cm.getColumn(0).setPreferredWidth(GUIGlobals.IMPORT_DIALOG_COL_0_WIDTH);
        cm.getColumn(1).setPreferredWidth(GUIGlobals.IMPORT_DIALOG_COL_1_WIDTH);
        cm.getColumn(2).setPreferredWidth(GUIGlobals.IMPORT_DIALOG_COL_2_WIDTH);
        cm.getColumn(3).setPreferredWidth(GUIGlobals.IMPORT_DIALOG_COL_3_WIDTH);
        JScrollPane sp = new JScrollPane(customImporterTable, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        customImporterTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        customImporterTable.setPreferredScrollableViewportSize(getSize());
        if (customImporterTable.getRowCount() > 0) {
            customImporterTable.setRowSelectionInterval(0, 0);
        }

        // Key bindings:
        JPanel mainPanel = new JPanel();
        ActionMap am = mainPanel.getActionMap();
        InputMap im = mainPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        im.put(Globals.getKeyPrefs().getKey(KeyBinding.CLOSE_DIALOG), "close");
        am.put("close", closeAction);
        mainPanel.setLayout(new BorderLayout());
        mainPanel.add(sp, BorderLayout.CENTER);
        JPanel buttons = new JPanel();
        ButtonBarBuilder bb = new ButtonBarBuilder(buttons);
        buttons.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        bb.addGlue();
        bb.addButton(addFromFolderButton);
        bb.addButton(addFromJarButton);
        bb.addButton(showDescButton);
        bb.addButton(removeButton);
        bb.addButton(closeButton);
        bb.addUnrelatedGap();
        bb.addButton(helpButton);
        bb.addGlue();

        getContentPane().add(mainPanel, BorderLayout.CENTER);
        getContentPane().add(buttons, BorderLayout.SOUTH);
        this.setSize(getSize());
        pack();
        PositionWindow.placeDialog(this, frame);
        new FocusRequester(customImporterTable);
    }


    /**
     * Table model for the custom importer table.
     */
    class ImportTableModel extends AbstractTableModel {

        private final String[] columnNames = new String[] {
                Localization.lang("Import name"),
                Localization.lang("Command line id"),
                Localization.lang("ImportFormat class"),
                Localization.lang("Contained in")
        };


        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            Object value = null;
            CustomImportList.Importer importer = getImporter(rowIndex);
            if (columnIndex == 0) {
                value = importer.getName();
            } else if (columnIndex == 1) {
                value = importer.getClidId();
            } else if (columnIndex == 2) {
                value = importer.getClassName();
            } else if (columnIndex == 3) {
                value = importer.getFileFromBasePath();
            }
            return value;
        }

        @Override
        public int getColumnCount() {
            return columnNames.length;
        }

        @Override
        public int getRowCount() {
            return Globals.prefs.customImports.size();
        }

        @Override
        public String getColumnName(int col) {
            return columnNames[col];
        }

        public CustomImportList.Importer getImporter(int rowIndex) {
            CustomImportList.Importer[] importers = Globals.prefs.customImports.toArray(new CustomImportList.Importer[Globals.prefs.customImports.size()]);
            return importers[rowIndex];
        }
    }

}
