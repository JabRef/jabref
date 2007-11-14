/*
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
package net.sf.jabref.imports;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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

import net.sf.jabref.FocusRequester;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.Util;


/**
 * Dialog to allow users to choose a file contained in a ZIP file.
 * 
 * @author andreas_sf at rudert-home dot de
 */
class ZipFileChooser extends JDialog {

  /**
   * Table model for the ZIP archive contents.
   * 
   * <p>Contains one row for each entry.
   * Does not contain rows for directory entries.</p>
   * 
   * <p>The columns contain information about ZIIP file entries:
   * <ol><li>
   *   name {@link String}
   * </li><li>
   *   time of last modification {@link Date}
   * </li><li>
   *   size (uncompressed) {@link Long}
   * </li></ol></p>
   */
  class ZipFileChooserTableModel extends AbstractTableModel {
    
    private String[] columnNames = new String[] {
      Globals.lang("Name"),
      Globals.lang("Last modified"),
      Globals.lang("Size")
    };
    private ZipEntry[] rows = null;
    private ZipFile zipFile = null;
    
    ZipFileChooserTableModel(ZipFile zipFile, ZipEntry[] rows) {
      super();
      this.rows = rows;
      this.zipFile = zipFile;        
    }
    
    /*
     *  (non-Javadoc)
     * @see javax.swing.table.TableModel#getColumnCount()
     */
    public int getColumnCount() {
      return columnNames.length;
    }

    /*
     *  (non-Javadoc)
     * @see javax.swing.table.TableModel#getRowCount()
     */
    public int getRowCount() {
      return this.rows.length;
    }

    /*
     *  (non-Javadoc)
     * @see javax.swing.table.TableModel#getColumnName(int)
     */
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
    public Object getValueAt(int rowIndex, int columnIndex) {
      Object value = null;
      ZipEntry entry = getZipEntry(rowIndex);
      if (columnIndex == 0) {
        value = entry.getName();
      } else if (columnIndex == 1) {
        value = SimpleDateFormat.getDateTimeInstance().format(new Date(entry.getTime()));
      } else if (columnIndex == 2) {
        value = new Long(entry.getSize());
      }
      return value;
    }
  }

  private JButton okButton = new JButton(Globals.lang("Ok"));
  private JButton cancelButton = new JButton(Globals.lang("Cancel"));

  /** table of Zip entries */
  private JTable table;
  /** shortcut to preferences */
  private JabRefPreferences prefs = Globals.prefs;
  /** this */
  private ZipFileChooser zipFileChooser;
  /** import customization dialog, owner of this dialog */
  private ImportCustomizationDialog importCustomizationDialog;
  
  /*
   *  (non-Javadoc)
   * @see java.awt.Component#getSize()
   */
  public Dimension getSize() {
    return new Dimension(400, 300);
  }
  
  /**
   * Entries that can be selected with this dialog.
   * 
   * @param zipFile  Zip-File
   * @return  entries that can be selected
   */
  private ZipEntry[] getSelectableZipEntries(ZipFile zipFile) {
    List<ZipEntry> entries = new ArrayList<ZipEntry>();
    Enumeration<? extends ZipEntry> e = zipFile.entries();
    while (e.hasMoreElements()) {
      ZipEntry entry = e.nextElement();
      if (!entry.isDirectory() && entry.getName().endsWith(".class")) {
        entries.add(entry); 
      }
    }
    return entries.toArray(new ZipEntry[]{});
  }
  
  /**
   * New Zip file chooser.
   * 
   * @param owner  Owner of the file chooser
   * @param zipFile  Zip-Fle to choose from, must be readable
   * @throws HeadlessException
   */
  public ZipFileChooser(ImportCustomizationDialog owner, ZipFile zipFile) throws HeadlessException {
    super(owner, Globals.lang("Select file from ZIP-archive"), false);
    
    this.importCustomizationDialog = owner;
    this.zipFileChooser = this;
    
    // cancel: no entry is selected
    cancelButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        dispose();
      }
    });

    // ok: get selected class and check if it is instantiable as an importer
    okButton.addActionListener(new ActionListener() {
     public void actionPerformed(ActionEvent e) {
       int row = table.getSelectedRow();
       if (row != -1) {
         ZipFileChooserTableModel model = (ZipFileChooserTableModel)table.getModel();
         ZipEntry tempZipEntry = model.getZipEntry(row);
         CustomImportList.Importer importer = prefs.customImports.new Importer();
         importer.setBasePath(model.getZipFile().getName());
         String className = tempZipEntry.getName().substring(0, tempZipEntry.getName().lastIndexOf('.'));
         importer.setClassName(className);
         try {
           ImportFormat importFormat = importer.getInstance();
           importer.setName(importFormat.getFormatName());
           importer.setCliId(importFormat.getCLIId());
           importCustomizationDialog.addOrReplaceImporter(importer);
           dispose();
         } catch (Exception exc) {           
           exc.printStackTrace();
           JOptionPane.showMessageDialog(zipFileChooser, Globals.lang("Could not instantiate %0 %1", importer.getName() + ":\n", exc.getMessage()));
         }
       } else {
         JOptionPane.showMessageDialog(zipFileChooser, Globals.lang("Please select an importer."));
       }
     }
    });
    

    ZipFileChooserTableModel tableModel = new ZipFileChooserTableModel( zipFile, getSelectableZipEntries(zipFile) );
    table = new JTable(tableModel);
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

    // Key bindings:
    JPanel mainPanel = new JPanel();
    //ActionMap am = mainPanel.getActionMap();
    //InputMap im = mainPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
    //im.put(prefs.getKey("Close dialog"), "close");
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
    Util.placeDialog(this, owner);
    new FocusRequester(table);
  }
}
