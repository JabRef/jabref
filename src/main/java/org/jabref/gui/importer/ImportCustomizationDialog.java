package org.jabref.gui.importer;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.Optional;
import java.util.zip.ZipFile;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumnModel;

import org.jabref.Globals;
import org.jabref.gui.DialogService;
import org.jabref.gui.FXDialogService;
import org.jabref.gui.JabRefDialog;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.help.HelpAction;
import org.jabref.gui.keyboard.KeyBinding;
import org.jabref.gui.util.DefaultTaskExecutor;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.importer.fileformat.CustomImporter;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.FileType;
import org.jabref.preferences.JabRefPreferences;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dialog to manage custom importers.
 */
public class ImportCustomizationDialog extends JabRefDialog {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImportCustomizationDialog.class);

    // Column widths for import customization dialog table:
    private static final int COL_0_WIDTH = 200;
    private static final int COL_1_WIDTH = 80;
    private static final int COL_2_WIDTH = 200;

    private static final int COL_3_WIDTH = 200;

    private final JTable customImporterTable;

    public ImportCustomizationDialog(final JabRefFrame frame) {
        super(frame, Localization.lang("Manage custom imports"), false, ImportCustomizationDialog.class);

        ImportTableModel tableModel = new ImportTableModel();
        customImporterTable = new JTable(tableModel);
        TableColumnModel cm = customImporterTable.getColumnModel();
        cm.getColumn(0).setPreferredWidth(COL_0_WIDTH);
        cm.getColumn(1).setPreferredWidth(COL_1_WIDTH);
        cm.getColumn(2).setPreferredWidth(COL_2_WIDTH);
        cm.getColumn(3).setPreferredWidth(COL_3_WIDTH);
        JScrollPane sp = new JScrollPane(customImporterTable, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        customImporterTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        customImporterTable.setPreferredScrollableViewportSize(getSize());
        if (customImporterTable.getRowCount() > 0) {
            customImporterTable.setRowSelectionInterval(0, 0);
        }

        JButton addFromFolderButton = new JButton(Localization.lang("Add from folder"));
        addFromFolderButton.addActionListener(e -> {

            FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                    .addExtensionFilter(FileType.CLASS)
                    .withDefaultExtension(FileType.JAR)
                    .withInitialDirectory(Globals.prefs.get(JabRefPreferences.WORKING_DIRECTORY)).build();
            DialogService ds = new FXDialogService();

            Optional<Path> selectedFile = DefaultTaskExecutor
                    .runInJavaFXThread(() -> ds.showFileOpenDialog(fileDialogConfiguration));

            if (selectedFile.isPresent() && (selectedFile.get().getParent() != null)) {
                String chosenFileStr = selectedFile.get().toString();

                try {
                    String basePath = selectedFile.get().getParent().toString();
                    String className = pathToClass(basePath, new File(chosenFileStr));
                    CustomImporter importer = new CustomImporter(basePath, className);

                    addOrReplaceImporter(importer);
                    customImporterTable.revalidate();
                    customImporterTable.repaint();
                } catch (Exception exc) {
                    JOptionPane.showMessageDialog(frame, Localization.lang("Could not instantiate %0", chosenFileStr));
                } catch (NoClassDefFoundError exc) {
                    JOptionPane.showMessageDialog(frame, Localization.lang(
                            "Could not instantiate %0. Have you chosen the correct package path?", chosenFileStr));
                }

            }
        });
        addFromFolderButton
                .setToolTipText(Localization.lang("Add a (compiled) custom Importer class from a class path.")
                        + "\n" + Localization.lang("The path need not be on the classpath of JabRef."));

        JButton addFromJarButton = new JButton(Localization.lang("Add from JAR"));
        addFromJarButton.addActionListener(e -> {
            FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                    .addExtensionFilters(EnumSet.of(FileType.ZIP, FileType.JAR))
                    .withDefaultExtension(FileType.JAR)
                    .withInitialDirectory(Globals.prefs.get(JabRefPreferences.WORKING_DIRECTORY)).build();
            DialogService ds = new FXDialogService();

            Optional<Path> jarZipFile = DefaultTaskExecutor
                    .runInJavaFXThread(() -> ds.showFileOpenDialog(fileDialogConfiguration));

            if (jarZipFile.isPresent()) {
                try (ZipFile zipFile = new ZipFile(jarZipFile.get().toFile(), ZipFile.OPEN_READ)) {
                    ZipFileChooser zipFileChooser = new ZipFileChooser(this, zipFile);
                    zipFileChooser.setVisible(true);
                    customImporterTable.revalidate();
                    customImporterTable.repaint(10);
                } catch (IOException exc) {
                    LOGGER.info("Could not open ZIP-archive.", exc);
                    JOptionPane.showMessageDialog(frame,
                            Localization.lang("Could not open %0", jarZipFile.get().toString()) + "\n"
                                    + Localization.lang("Have you chosen the correct package path?"));
                } catch (NoClassDefFoundError exc) {
                    LOGGER.info("Could not instantiate ZIP-archive reader.", exc);
                    JOptionPane.showMessageDialog(frame,
                            Localization.lang("Could not instantiate %0", jarZipFile.get().toString()) + "\n"
                                    + Localization.lang("Have you chosen the correct package path?"));
                }
            }
        });
        addFromJarButton
                .setToolTipText(Localization.lang("Add a (compiled) custom Importer class from a ZIP-archive.")
                        + "\n" + Localization.lang("The ZIP-archive need not be on the classpath of JabRef."));

        JButton showDescButton = new JButton(Localization.lang("Show description"));
        showDescButton.addActionListener(e -> {
            int row = customImporterTable.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(frame, Localization.lang("Please select an importer."));
            } else {
                CustomImporter importer = ((ImportTableModel) customImporterTable.getModel()).getImporter(row);
                JOptionPane.showMessageDialog(frame, importer.getDescription());
            }
        });

        JButton removeButton = new JButton(Localization.lang("Remove"));
        removeButton.addActionListener(e -> {
            int row = customImporterTable.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(frame, Localization.lang("Please select an importer."));
            } else {
                customImporterTable.removeRowSelectionInterval(row, row);
                Globals.prefs.customImports
                        .remove(((ImportTableModel) customImporterTable.getModel()).getImporter(row));
                Globals.IMPORT_FORMAT_READER.resetImportFormats(Globals.prefs.getImportFormatPreferences(),
                        Globals.prefs.getXMPPreferences(), Globals.getFileUpdateMonitor());
                customImporterTable.revalidate();
                customImporterTable.repaint();
            }
        });

        Action closeAction = new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        };

        JButton closeButton = new JButton(Localization.lang("Close"));
        closeButton.addActionListener(closeAction);

        JButton helpButton = new HelpAction(HelpFile.CUSTOM_IMPORTS).getHelpButton();

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
        this.setLocationRelativeTo(frame);
        customImporterTable.requestFocus();
    }

    /*
    *  (non-Javadoc)
    * @see java.awt.Component#getSize()
    */
    @Override
    public Dimension getSize() {
        int width = COL_0_WIDTH + COL_1_WIDTH + COL_2_WIDTH + COL_3_WIDTH;
        return new Dimension(width, width / 2);
    }

    /**
     * Converts a path relative to a base-path into a class name.
     *
     * @param basePath  base path
     * @param path  path that includes base-path as a prefix
     * @return  class name
     */
    private static String pathToClass(String basePath, File path) {
        String className = null;
        File actualPath = path;
        // remove leading basepath from path
        while (!actualPath.equals(new File(basePath))) {
            className = actualPath.getName() + (className == null ? "" : "." + className);
            actualPath = actualPath.getParentFile();
        }
        if (className != null) {
            int lastDot = className.lastIndexOf('.');
            if (lastDot < 0) {
                return className;
            }
            className = className.substring(0, lastDot);
        }
        return className;
    }

    /**
     * Adds an importer to the model that underlies the custom importers.
     *
     * @param importer  importer
     */
    public void addOrReplaceImporter(CustomImporter importer) {
        Globals.prefs.customImports.replaceImporter(importer);
        Globals.IMPORT_FORMAT_READER.resetImportFormats(Globals.prefs.getImportFormatPreferences(),
                Globals.prefs.getXMPPreferences(), Globals.getFileUpdateMonitor());
        ((ImportTableModel) customImporterTable.getModel()).fireTableDataChanged();
    }

    /**
     * Table model for the custom importer table.
     */
    private class ImportTableModel extends AbstractTableModel {

        private final String[] columnNames = new String[] {Localization.lang("Import name"),
                Localization.lang("Command line id"), Localization.lang("Importer class"),
                Localization.lang("Contained in")};

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            Object value = null;
            CustomImporter importer = getImporter(rowIndex);
            if (columnIndex == 0) {
                value = importer.getName();
            } else if (columnIndex == 1) {
                value = importer.getName();
            } else if (columnIndex == 2) {
                value = importer.getClassName();
            } else if (columnIndex == 3) {
                value = importer.getBasePath();
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

        public CustomImporter getImporter(int rowIndex) {
            CustomImporter[] importers = Globals.prefs.customImports
                    .toArray(new CustomImporter[Globals.prefs.customImports.size()]);
            return importers[rowIndex];
        }
    }

}
