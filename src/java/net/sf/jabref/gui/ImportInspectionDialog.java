package net.sf.jabref.gui;

import net.sf.jabref.*;
import net.sf.jabref.undo.NamedCompound;
import net.sf.jabref.undo.UndoableInsertEntry;
import net.sf.jabref.imports.ImportFormatReader;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.HashMap;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.ButtonStackBuilder;

/**
 * Created by IntelliJ IDEA.
 * User: alver
 * Date: 20.mar.2005
 * Time: 22:02:35
 * To change this template use File | Settings | File Templates.
 */
public class ImportInspectionDialog extends JDialog {
    private BasePanel panel;
    private JabRefFrame frame;
    private DefaultTableModel tableModel = new MyTableModel();
    private JTable table = new MyTable(tableModel);
    private String[] fields;
    private JProgressBar progressBar = new JProgressBar(JProgressBar.HORIZONTAL);
    private JButton ok = new JButton(Globals.lang("Ok")),
        cancel = new JButton(Globals.lang("Cancel"));
    private List entries = new ArrayList();
    private String undoName;
    private ArrayList callBacks = new ArrayList();
    private boolean newDatabase;
    private JButton selectAll = new JButton(Globals.lang("Select all"));
    private JButton deselectAll = new JButton(Globals.lang("Deselect all"));
    private JButton stop = new JButton(Globals.lang("Stop"));
    /**
     * Creates a dialog that displays the given set of fields in the table.
     * The dialog allows another process to add entries dynamically while the dialog
     * is shown.
     * @param frame
     * @param panel
     * @param fields
     */
    public ImportInspectionDialog(JabRefFrame frame, BasePanel panel, String[] fields,
                                  String undoName, boolean newDatabase) {
        this.frame = frame;
        this.panel = panel;
        this.fields = fields;
        this.undoName = undoName;
        this.newDatabase = newDatabase;

        tableModel.addColumn(Globals.lang("Keep"));

        for (int i=0; i<fields.length; i++) {
            tableModel.addColumn(Util.nCase(fields[i]));
            Object o = GUIGlobals.fieldLength.get(fields[i]);
            int width = o==null ? GUIGlobals.DEFAULT_FIELD_LENGTH :
                    ((Integer)o).intValue();
            table.getColumnModel().getColumn(i+1).setPreferredWidth(width);
        }
        table.getColumnModel().getColumn(0).setPreferredWidth(25);
        table.setRowSelectionAllowed(false);
        table.setCellSelectionEnabled(false);
        getContentPane().setLayout(new BorderLayout());
        progressBar.setIndeterminate(true);
        JPanel centerPan = new JPanel();
        centerPan.setLayout(new BorderLayout());
        centerPan.add(new JScrollPane(table), BorderLayout.CENTER);
        centerPan.add(progressBar, BorderLayout.SOUTH);
        getContentPane().add(centerPan, BorderLayout.CENTER);


        ButtonBarBuilder bb = new ButtonBarBuilder();
        bb.addGlue();
        bb.addGridded(ok);
        bb.addGridded(stop);
        bb.addGridded(cancel);
        bb.addGlue();
        bb.getPanel().setBorder(BorderFactory.createEmptyBorder(2,2,2,2));

        ButtonStackBuilder builder = new ButtonStackBuilder();
        builder.addGridded(selectAll);
        builder.addGridded(deselectAll);
        builder.getPanel().setBorder(BorderFactory.createEmptyBorder(2,2,2,2));
        centerPan.add(builder.getPanel(), BorderLayout.WEST);

        ok.setEnabled(false);
        ok.addActionListener(new OkListener());
        cancel.addActionListener(new CancelListener());
        stop.addActionListener(new StopListener());
        selectAll.addActionListener(new SelectionButton(true));
        deselectAll.addActionListener(new SelectionButton(false));
        getContentPane().add(bb.getPanel(), BorderLayout.SOUTH);
        setSize(new Dimension(650, 500));
    }

    public void setProgress(int current, int max) {
        progressBar.setIndeterminate(false);
        progressBar.setMinimum(0);
        progressBar.setMaximum(max);
        progressBar.setValue(current);
    }

    /**
     * Add a List of entries to the table view. The table will update to show the
     * added entries.
     * @param entries
     */
    public void addEntries(List entries) {
        for (Iterator i=entries.iterator(); i.hasNext();) {
            BibtexEntry entry = (BibtexEntry)i.next();
            this.entries.add(entry);
            Object[] values = new Object[tableModel.getColumnCount()];
            values[0] = Boolean.TRUE;
            for (int j=0; j<fields.length; j++)
                values[1+j] = entry.getField(fields[j]);
            tableModel.addRow(values);
        }


    }

    /**
     * When this method is called, the dialog will visually change to indicate
     * that all entries are in place.
     */
    public void entryListComplete() {
        progressBar.setIndeterminate(false);
        progressBar.setVisible(false);
        ok.setEnabled(true);
    }


    /**
     * This method returns a List containing all entries that are selected
     * (checkbox checked).
     * @return
     */
    public List getSelectedEntries() {
        List selected = new ArrayList();
        for (int i=0; i<table.getRowCount(); i++) {
            Boolean sel = (Boolean)table.getValueAt(i, 0);
            if (sel.booleanValue()) {
                selected.add(entries.get(i));
            }
        }
        return selected;
    }

    public void addCallBack(CallBack cb) {
        callBacks.add(cb);
    }

    class OkListener implements ActionListener {
        public void actionPerformed(ActionEvent event) {
            final List selected = getSelectedEntries();
            if (selected.size() == 0) {
                dispose();
                return;
            }

            NamedCompound ce = new NamedCompound(undoName);

            if (newDatabase) {
                // Create a new BasePanel for the entries:
                BibtexDatabase base = new BibtexDatabase();
                panel = new BasePanel(frame, base,  null, new HashMap(), Globals.prefs);
            }

            for (Iterator i=selected.iterator(); i.hasNext();) {
                BibtexEntry entry = (BibtexEntry)i.next();
                entry.clone();
                try {
                entry.setId(Util.createId(entry.getType(), panel.database()));
                    panel.database().insertEntry(entry);
                    ce.addEdit(new UndoableInsertEntry(panel.database(), entry,  panel));
                } catch (KeyCollisionException e) {
                    e.printStackTrace();
                }
            }

            ce.end();
            panel.undoManager.addEdit(ce);

            dispose();
            SwingUtilities.invokeLater(new Thread() {
                public void run() {
                    if (newDatabase) {
                        frame.addTab(panel, null, true);
                    }
                    panel.markBaseChanged();
                    panel.refreshTable();
                    for (Iterator i=callBacks.iterator(); i.hasNext();) {
                        ((CallBack)i.next()).done(selected.size());
                    }
                }
            });

        }

    }

    private void signalStopFetching() {
        for (Iterator i=callBacks.iterator(); i.hasNext();) {
            ((CallBack)i.next()).stopFetching();
        }
    }

    class StopListener implements ActionListener {
        public void actionPerformed(ActionEvent event) {
            signalStopFetching();
            entryListComplete();
        }
    }

    class CancelListener implements ActionListener {
        public void actionPerformed(ActionEvent event) {
            signalStopFetching();
            dispose();
        }
    }

    class MyTable extends JTable {
        public MyTable(TableModel model) {
            super(model);
            //setDefaultRenderer(Boolean.class, );
        }

        public boolean isCellEditable(int row, int col) {
            return col==0;
        }
    }

    class MyTableModel extends DefaultTableModel {
        public Class getColumnClass(int i) {
            if (i==0)
                return Boolean.class;
            else
                return String.class;
        }
    }

    class SelectionButton implements ActionListener {
        private Boolean enable;

        public SelectionButton(boolean enable) {
            this.enable = new Boolean(enable);
        }
        public void actionPerformed(ActionEvent event) {
            for (int i=0; i<table.getRowCount(); i++) {
                table.setValueAt(enable, i, 0);
            }
        }
    }

    public static interface CallBack {
        // This method is called by the dialog when the user has selected the
        // wanted entries, and clicked Ok. The callback object can update status
        // line etc.
        public void done(int entriesImported);
        // This method is called by the dialog when the user has cancelled or
        // signalled a stop. It is expected that any long-running fetch operations
        // will stop after this method is called.
        public void stopFetching();
    }
}
