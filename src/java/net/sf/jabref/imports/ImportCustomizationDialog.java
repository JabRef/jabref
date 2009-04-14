/*
 Copyright (C) 2005 Andreas Rudert, based on ExportCustomizationDialog by ??

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
package net.sf.jabref.imports;

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

import net.sf.jabref.*;
import net.sf.jabref.gui.FileDialogs;

import com.jgoodies.forms.builder.ButtonBarBuilder;

/**
 * Dialog to manage custom importers.
 */
public class ImportCustomizationDialog extends JDialog {

  private final JabRefFrame frame;
  private JButton addFromFolderButton = new JButton(Globals.lang("Add from folder"));
  private JButton addFromJarButton = new JButton(Globals.lang("Add from jar"));
  private JButton showDescButton = new JButton(Globals.lang("Show description"));
  private JButton removeButton = new JButton(Globals.lang("Remove"));
  private JButton closeButton = new JButton(Globals.lang("Close"));
  private JButton helpButton = new JButton(Globals.lang("Help"));

  private JPanel buttons = new JPanel();
  private JPanel mainPanel = new JPanel();
  private JTable customImporterTable;
  private JabRefPreferences prefs = Globals.prefs;
  private ImportCustomizationDialog importCustomizationDialog;

  /*
  *  (non-Javadoc)
  * @see java.awt.Component#getSize()
  */
  public Dimension getSize() {
    int width = GUIGlobals.IMPORT_DIALOG_COL_0_WIDTH
              + GUIGlobals.IMPORT_DIALOG_COL_1_WIDTH
              + GUIGlobals.IMPORT_DIALOG_COL_2_WIDTH
              + GUIGlobals.IMPORT_DIALOG_COL_3_WIDTH;
    return new Dimension(width, width/2);
  }

  /**
   * Converts a path relative to a base-path into a class name.
   * 
   * @param basePath  base path
   * @param path  path that includes base-path as a prefix
   * @return  class name
   */
  private String pathToClass(File basePath, File path) {
    String className = null;
    // remove leading basepath from path
    while (!path.equals(basePath)) {
      className = path.getName() + (className != null ? "." + className : "");
      path = path.getParentFile();
    }
    className = className.substring(0, className.lastIndexOf('.'));
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
    ((ImportTableModel)customImporterTable.getModel()).fireTableDataChanged();
  }

  /**
   * 
   * @param frame_
   * @throws HeadlessException
   */
  public ImportCustomizationDialog(JabRefFrame frame_) throws HeadlessException {
    super(frame_, Globals.lang("Manage custom imports"), false);
    this.importCustomizationDialog = this;
    frame = frame_;

    addFromFolderButton.addActionListener(new ActionListener() {
     public void actionPerformed(ActionEvent e) {
       CustomImportList.Importer importer = prefs.customImports.new Importer();
       importer.setBasePath( FileDialogs.getNewDir(frame, new File(prefs.get("workingDirectory")), "",
           Globals.lang("Select Classpath of New Importer"), JFileChooser.CUSTOM_DIALOG, false) );
       String chosenFileStr = FileDialogs.getNewFile(frame, importer.getBasePath(), ".class",
           Globals.lang("Select new ImportFormat Subclass"), JFileChooser.CUSTOM_DIALOG, false);
       if (chosenFileStr != null) {
         try {
           importer.setClassName( pathToClass(importer.getBasePath(), new File(chosenFileStr)) );
           importer.setName( importer.getInstance().getFormatName() );
           importer.setCliId( importer.getInstance().getCLIId() );
         } catch (Exception exc) {
           exc.printStackTrace();
           JOptionPane.showMessageDialog(frame, Globals.lang("Could not instantiate %0 %1", chosenFileStr + ":\n", exc.getMessage()));
         } catch (NoClassDefFoundError exc) {
           exc.printStackTrace();
           JOptionPane.showMessageDialog(frame, Globals.lang("Could not instantiate %0 %1. Have you chosen the correct package path?", chosenFileStr + ":\n", exc.getMessage()));
         }

         addOrReplaceImporter(importer);
         customImporterTable.revalidate();
         customImporterTable.repaint();
         frame.setUpImportMenus();
       }
      }
    });
    addFromFolderButton.setToolTipText(Globals.lang("Add a (compiled) custom ImportFormat class from a class path. \nThe path need not be on the classpath of JabRef."));

    addFromJarButton.addActionListener(new ActionListener() {
     public void actionPerformed(ActionEvent e) {
       String basePath = FileDialogs.getNewFile(frame, new File(prefs.get("workingDirectory")), ".zip,.jar",
           Globals.lang("Select a Zip-archive"), JFileChooser.CUSTOM_DIALOG, false);
       ZipFile zipFile = null;
       if (basePath != null) {
         try {
           zipFile = new ZipFile(new File(basePath), ZipFile.OPEN_READ);
         } catch (IOException exc) {
           exc.printStackTrace();
           JOptionPane.showMessageDialog(frame, Globals.lang("Could not open %0 %1", basePath + ":\n", exc.getMessage())
                                              + "\n" + Globals.lang("Have you chosen the correct package path?"));
           return;
         } catch (NoClassDefFoundError exc) {
           exc.printStackTrace();
           JOptionPane.showMessageDialog(frame, Globals.lang("Could not instantiate %0 %1", basePath + ":\n", exc.getMessage())
                                              + "\n" + Globals.lang("Have you chosen the correct package path?"));
         }
       }

       if (zipFile != null) {
         ZipFileChooser zipFileChooser = new ZipFileChooser(importCustomizationDialog, zipFile);
         zipFileChooser.setVisible(true);
       }
       customImporterTable.revalidate();
       customImporterTable.repaint(10);
       frame.setUpImportMenus();
      }
    });
    addFromJarButton.setToolTipText(Globals.lang("Add a (compiled) custom ImportFormat class from a Zip-archive.\nThe Zip-archive need not be on the classpath of JabRef."));

    showDescButton.addActionListener(new ActionListener() {
     public void actionPerformed(ActionEvent e) {
       int row = customImporterTable.getSelectedRow();
       if (row != -1) {
         CustomImportList.Importer importer = ((ImportTableModel)customImporterTable.getModel()).getImporter(row);
         try {
           ImportFormat importFormat = importer.getInstance();
           JOptionPane.showMessageDialog(frame, importFormat.getDescription());
         } catch (Exception exc) {
           exc.printStackTrace();
           JOptionPane.showMessageDialog(frame, Globals.lang("Could not instantiate %0 %1", importer.getName() + ":\n", exc.getMessage()));
         }
       } else {
         JOptionPane.showMessageDialog(frame, Globals.lang("Please select an importer"));
       }
     }
    });

    removeButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        int row = customImporterTable.getSelectedRow();
        if (row != -1) {
          customImporterTable.removeRowSelectionInterval(row,row);
          prefs.customImports.remove(((ImportTableModel)customImporterTable.getModel()).getImporter(row));
          Globals.importFormatReader.resetImportFormats();
          customImporterTable.revalidate();
          customImporterTable.repaint();
          frame.setUpImportMenus();
        }  else {
          JOptionPane.showMessageDialog(frame, Globals.lang("Please select an importer."));
        }
      }
    });

    AbstractAction closeAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        dispose();
      }
    };

    closeButton.addActionListener(closeAction);

    helpButton.addActionListener(new HelpAction(frame.helpDiag, GUIGlobals.importCustomizationHelp,
                                          "Help"));

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
    ActionMap am = mainPanel.getActionMap();
    InputMap im = mainPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
    im.put(frame.prefs().getKey("Close dialog"), "close");
    am.put("close", closeAction);
    mainPanel.setLayout(new BorderLayout());
    mainPanel.add(sp, BorderLayout.CENTER);
    ButtonBarBuilder bb = new ButtonBarBuilder(buttons);
    buttons.setBorder(BorderFactory.createEmptyBorder(2,2,2,2));
    bb.addGlue();
    bb.addGridded(addFromFolderButton);
    bb.addGridded(addFromJarButton);
    bb.addGridded(showDescButton);
    bb.addGridded(removeButton);
    bb.addGridded(closeButton);
    bb.addUnrelatedGap();
    bb.addGridded(helpButton);
    bb.addGlue();

    getContentPane().add(mainPanel, BorderLayout.CENTER);
    getContentPane().add(buttons, BorderLayout.SOUTH);
    this.setSize(getSize());
    pack();
    Util.placeDialog(this, frame);
    new FocusRequester(customImporterTable);
  }

  /**
   * Table model for the custom importer table.
   */
  class ImportTableModel extends AbstractTableModel {
    private String[] columnNames = new String[] {
      Globals.lang("Import name"),
      Globals.lang("Command line id"),
      Globals.lang("ImportFormat class"),
      Globals.lang("Contained in")
    };

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
        value = importer.getBasePath();
      }
      return value;
    }

    public int getColumnCount() {
      return columnNames.length;
    }

    public int getRowCount() {
      return Globals.prefs.customImports.size();
    }

    public String getColumnName(int col) {
      return columnNames[col];
    }

    public CustomImportList.Importer getImporter(int rowIndex) {
      CustomImportList.Importer[] importers = Globals.prefs.customImports.toArray(new CustomImportList.Importer[] {});
      return importers[rowIndex];
    }
  }

}
