package net.sf.jabref.export;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumnModel;

import net.sf.jabref.*;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import ca.odell.glazedlists.gui.TableFormat;
import ca.odell.glazedlists.swing.EventTableModel;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: </p>
 * @author not attributable
 * @version 1.0
 */

public class ExportCustomizationDialog extends JDialog {

  JabRefFrame frame;
  JButton addExport = new JButton(Globals.lang("Add new")),
      modify = new JButton(Globals.lang("Modify")),
      remove = new JButton(Globals.lang("Remove")),
      close = new JButton(Globals.lang("Close")),
      help = new JButton(Globals.lang("Help"));

  JPanel buttons = new JPanel(),
      main = new JPanel();
  JTable table;


  public ExportCustomizationDialog(JabRefFrame frame_) throws HeadlessException {

    super(frame_, Globals.lang("Manage custom exports"), false);
    frame = frame_;
    addExport.addActionListener(new ActionListener() {
     public void actionPerformed(ActionEvent e) {
       CustomExportDialog ecd = new CustomExportDialog(frame);
       ecd.setVisible(true); // ecd.show(); -> deprecated since 1.5
       if (ecd.okPressed()) {
         String[] newFormat = new String[] {ecd.name(), ecd.layoutFile(), ecd.extension() };
         Globals.prefs.customExports.addFormat(newFormat);
         Globals.prefs.customExports.store();
       }
     }
    });

    modify.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        int row = table.getSelectedRow();
        if (row == -1) return;
        String[] old = Globals.prefs.customExports.getSortedList().get(row);
       CustomExportDialog ecd = new CustomExportDialog(frame, old[0], old[1], old[2]);
       ecd.setVisible(true); // ecd.show(); -> deprecated since 1.5
       if (ecd.okPressed()) {
         old[0] = ecd.name();
         old[1] = ecd.layoutFile();
         old[2] = ecd.extension();
         table.revalidate();
         table.repaint();
         Globals.prefs.customExports.store();
       }
     }
    });

    remove.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        int[] rows = table.getSelectedRows();
          if (rows.length == 0) return;
          String[][] entries = new String[rows.length][];
          for (int i=0; i<rows.length; i++)
            entries[i] = Globals.prefs.customExports.getSortedList().get(rows[i]);
          for (int i=0; i<rows.length; i++)
            Globals.prefs.customExports.remove(entries[i]);
          Globals.prefs.customExports.store();
      }
    });

    AbstractAction closeAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        dispose();
      }
    };

    close.addActionListener(closeAction);

    help.addActionListener(new HelpAction(frame.helpDiag, GUIGlobals.exportCustomizationHelp,
                                          "Help"));

    EventTableModel tableModel = new EventTableModel(Globals.prefs.customExports.getSortedList(), new ExportTableFormat());
    table = new JTable(tableModel);
    TableColumnModel cm = table.getColumnModel();
    cm.getColumn(0).setPreferredWidth(GUIGlobals.EXPORT_DIALOG_COL_0_WIDTH);
    cm.getColumn(1).setPreferredWidth(GUIGlobals.EXPORT_DIALOG_COL_1_WIDTH);
    cm.getColumn(2).setPreferredWidth(GUIGlobals.EXPORT_DIALOG_COL_2_WIDTH);
    JScrollPane sp = new JScrollPane(table, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                     JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    table.setPreferredScrollableViewportSize(
      new Dimension(500, 150));
    if (table.getRowCount() > 0)
      table.setRowSelectionInterval(0, 0);

    // Key bindings:
    ActionMap am = main.getActionMap();
    InputMap im = main.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
    im.put(frame.prefs().getKey("Close dialog"), "close");
    am.put("close", closeAction);
    //am = table.getActionMap();
    //im = table.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
    //im.put(frame.prefs().getKey("Close dialog"), "close");
    //am.put("close", closeAction);
    main.setLayout(new BorderLayout());
    main.add(sp, BorderLayout.CENTER);
    ButtonBarBuilder bb = new ButtonBarBuilder(buttons);
    buttons.setBorder(BorderFactory.createEmptyBorder(2,2,2,2));
    bb.addGlue();
    bb.addGridded(addExport);
    bb.addGridded(modify);
    bb.addGridded(remove);
    bb.addGridded(close);
    bb.addUnrelatedGap();
    bb.addGridded(help);
    bb.addGlue();

    getContentPane().add(main, BorderLayout.CENTER);
    getContentPane().add(buttons, BorderLayout.SOUTH);
    pack();
    Util.placeDialog(this, frame);
    new FocusRequester(table);
  }

  class ExportTableFormat implements TableFormat<String[]> {

      public Object getColumnValue(String[] strings, int i) {
          return strings[i];
      }

      public int getColumnCount() {
      return 3;
    }

    public String getColumnName(int col) {
      switch (col) {
        case 0:
          return Globals.lang("Export name");
        case 1:
          return Globals.lang("Main layout file");
        default:
          return Globals.lang("File extension");
      }
    }

  }

}
