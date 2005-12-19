package net.sf.jabref.imports;

import javax.swing.JDialog;
import java.awt.*;
import net.sf.jabref.*;
import javax.swing.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumnModel;

/**
 * Dialog to manage custom importers.
 */
public class ImportCustomizationDialog extends JDialog {

  private final JabRefFrame frame;
  private JButton addFromFolderButton = new JButton(Globals.lang("Add from folder"));
  // TODO: allow importers to be added from Jar-Files
  // private JButton addFromJarButton = new JButton(Globals.lang("Add from jar"));
  private JButton showDescButton = new JButton(Globals.lang("Show description"));
  private JButton removeButton = new JButton(Globals.lang("Remove"));
  private JButton closeButton = new JButton(Globals.lang("Close"));
  private JButton helpButton = new JButton(Globals.lang("Help"));

  private JPanel optionsPanel = new JPanel();
  private JPanel mainPanel = new JPanel();
  private JTable customImporterTable;
  private JabRefPreferences prefs = Globals.prefs;
  
  /**
   * 
   * @param frame_
   * @throws HeadlessException
   */
  public ImportCustomizationDialog(JabRefFrame frame_) throws HeadlessException {
    super(frame_, Globals.lang("Manage custom imports"), false);
    frame = frame_;
    
    addFromFolderButton.addActionListener(new ActionListener() {
     public void actionPerformed(ActionEvent e) {
       CustomImportList.Importer importer = prefs.customImports.new Importer();  
       importer.setBasePath( Globals.getNewDir(frame, prefs, new File(prefs.get("workingDirectory")), null,
           "Select Classpath of New Importer", JFileChooser.CUSTOM_DIALOG, true) );
       String chosenFileStr = Globals.getNewFile(frame, prefs, importer.getBasePath(), ".class",
           "Select new ImportFormat Subclass", JFileChooser.CUSTOM_DIALOG, true);
       if (chosenFileStr != null) {
         try {
           File chosenFile = new File(chosenFileStr);
           String className = null;
           while (!chosenFile.equals(importer.getBasePath())) {
             className = chosenFile.getName() + (className != null ? "." + className : "");
             chosenFile = chosenFile.getParentFile();
           }
           // TODO: there should be a less system dependent way of removing the extension
           className = className.substring(0, className.lastIndexOf('.'));
           importer.setClassName(className);
           importer.setName( importer.getInstance().getFormatName() );
         } catch (Exception exc) {           
           exc.printStackTrace();
           JOptionPane.showMessageDialog(frame, "Could not instantiate " + chosenFileStr + ":\n " + exc.getMessage());
         }

         prefs.customImports.addImporter(importer);
         Globals.importFormatReader.resetImportFormats();
         customImporterTable.revalidate();
         customImporterTable.repaint();
         frame.setUpImportMenus();
       }
      }
    });
    addFromFolderButton.setToolTipText("Add a (compiled) custom ImportFormat class from a class path. \nThe path need not be on the classpath of JabRef.");

    // TODO: select class from jar
    /*
    addFromJarButton.addActionListener(new ActionListener() {
     public void actionPerformed(ActionEvent e) {
       String basePath = Globals.getNewFile(frame, prefs, new File(prefs.get("workingDirectory")), ".jar",
            JFileChooser.OPEN_DIALOG, true);
       String chosenFile = Globals.getNewFile(frame, prefs, new File(basePath), ".class",
                                              JFileChooser.OPEN_DIALOG, true);

       if (chosenFile != null) {
         URL[] urls = new URL[1];
         try {
           urls[0] = new URL("file:/" + new File(basePath).getAbsolutePath());
           URLClassLoader cl = new URLClassLoader(urls);
           // TODO: allow classes in packages 
           String className = chosenFile.substring(basePath.length()+1, chosenFile.lastIndexOf('.')).replace('\\','.');
           Class importFormatClass = Class.forName(className, true, cl);
           ImportFormat importFormat = (ImportFormat)importFormatClass.newInstance();
           System.out.println(importFormat.getFormatName());
         } catch (Exception exc) {
           exc.printStackTrace();
         }

         String[] newFormat = new String[] {ecd.name(), ecd.layoutFile(), ecd.extension() };
         Globals.importFormatReader.resetImportFormats();
         customImporterTable.revalidate();
         customImporterTable.repaint();
         frame.setUpImportMenus();
       }
      }
    );
    */
    
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
           JOptionPane.showMessageDialog(frame, "Could not instantiate " + importer.getClassName() + ": " + exc.getMessage());
         }
       } else {
         JOptionPane.showMessageDialog(frame, "Please select an importer.");
       }
     }
    });
    
    removeButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        int row = customImporterTable.getSelectedRow();
        if (row != -1) {
          prefs.customImports.remove(((ImportTableModel)customImporterTable.getModel()).getImporter(row));
          Globals.importFormatReader.resetImportFormats();
          customImporterTable.revalidate();
          customImporterTable.repaint();
          frame.setUpImportMenus();
        }  else {
          JOptionPane.showMessageDialog(frame, "Please select an importer.");
        }
      }
    });

    AbstractAction closeAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        dispose();
      }
    };

    closeButton.addActionListener(closeAction);

    helpButton.addActionListener(new HelpAction(frame.helpDiag, GUIGlobals.exportCustomizationHelp,
                                          "Help"));

    ImportTableModel tableModel = new ImportTableModel();
    customImporterTable = new JTable(tableModel);
    TableColumnModel cm = customImporterTable.getColumnModel();
    cm.getColumn(0).setPreferredWidth(GUIGlobals.EXPORT_DIALOG_COL_0_WIDTH);
    cm.getColumn(1).setPreferredWidth(GUIGlobals.EXPORT_DIALOG_COL_1_WIDTH);
    cm.getColumn(2).setPreferredWidth(GUIGlobals.EXPORT_DIALOG_COL_2_WIDTH);
    JScrollPane sp = new JScrollPane(customImporterTable, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                     JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    customImporterTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    customImporterTable.setPreferredScrollableViewportSize(new Dimension(500, 150));
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
    optionsPanel.add(addFromFolderButton);
    // optionsPanel.add(addFromJarButton);
    optionsPanel.add(showDescButton);
    optionsPanel.add(removeButton);
    optionsPanel.add(closeButton);
    optionsPanel.add(Box.createHorizontalStrut(5));
    optionsPanel.add(helpButton);

    getContentPane().add(mainPanel, BorderLayout.CENTER);
    getContentPane().add(optionsPanel, BorderLayout.SOUTH);
    pack();
    Util.placeDialog(this, frame);
    new FocusRequester(customImporterTable);
  }

  class ImportTableModel extends AbstractTableModel {
    public int getColumnCount() {
      return 3;
    }

    public int getRowCount() {
      return Globals.prefs.customImports.size();
    }

    public String getColumnName(int col) {
      switch (col) {
        case 0:
          return Globals.lang("Import name");
        case 1:
          return Globals.lang("ImportFormat class");
        default:
          return Globals.lang("Contained in");
      }
    }

    public CustomImportList.Importer getImporter(int rowIndex) {
      CustomImportList.Importer[] importers = (CustomImportList.Importer[])Globals.prefs.customImports.toArray(new CustomImportList.Importer[] {});
      return importers[rowIndex];
    }
    
    public Object getValueAt(int rowIndex, int columnIndex) {
      Object value = null;
      CustomImportList.Importer importer = getImporter(rowIndex);
      if (columnIndex == 0) {
        value = importer.getName();
      } else if (columnIndex == 1) {
        value = importer.getClassName();
      } else if (columnIndex == 2) {
        value = importer.getBasePath();
      }
      return value;
    }
  }

}
