package net.sf.jabref.gui;

import net.sf.jabref.*;
import net.sf.jabref.groups.GroupTreeNode;
import net.sf.jabref.groups.AllEntriesGroup;
import net.sf.jabref.groups.AbstractGroup;
import net.sf.jabref.groups.UndoableChangeAssignment;
import net.sf.jabref.labelPattern.LabelPatternUtil;
import net.sf.jabref.undo.NamedCompound;
import net.sf.jabref.undo.UndoableInsertEntry;
import net.sf.jabref.imports.ImportFormatReader;

import javax.swing.*;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import java.util.*;
import java.util.List;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.ButtonStackBuilder;
import com.jgoodies.uif_lite.component.UIFSplitPane;

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
    private UIFSplitPane contentPane = new UIFSplitPane(UIFSplitPane.VERTICAL_SPLIT);
    private DefaultTableModel tableModel = new MyTableModel();
    private JTable table = new MyTable(tableModel);
    private String[] fields;
    private JProgressBar progressBar = new JProgressBar(JProgressBar.HORIZONTAL);
    private JButton ok = new JButton(Globals.lang("Ok")),
        cancel = new JButton(Globals.lang("Cancel")),
        generate = new JButton(Globals.lang("Generate keys"));
    private List entries = new ArrayList();
    private String undoName;
    private ArrayList callBacks = new ArrayList();
    private boolean newDatabase;
    private JMenu groupsAdd = new JMenu(Globals.lang("Add to group"));
    private JPopupMenu popup = new JPopupMenu();
    private JButton selectAll = new JButton(Globals.lang("Select all"));
    private JButton deselectAll = new JButton(Globals.lang("Deselect all"));
    private JButton stop = new JButton(Globals.lang("Stop"));
    private JButton delete = new JButton(Globals.lang("Delete"));
    private JButton help = new JButton(Globals.lang("Help"));
    private PreviewPanel preview = new PreviewPanel(Globals.prefs.get("preview1"));
    private ListSelectionListener previewListener = null;
    private boolean generatedKeys = false;
    private Rectangle toRect = new Rectangle(0, 0, 1, 1);
    private Map groupAdditions = new HashMap();


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
        table.getColumnModel().getColumn(0).setPreferredWidth(55);
        table.getColumnModel().getColumn(0).setMaxWidth(55);
        table.setRowSelectionAllowed(true);
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        //table.setCellSelectionEnabled(false);
        previewListener = new TableSelectionListener();
        table.getSelectionModel().addListSelectionListener(previewListener);
        table.addMouseListener(new TableClickListener());
        getContentPane().setLayout(new BorderLayout());
        progressBar.setIndeterminate(true);
        JPanel centerPan = new JPanel();
        centerPan.setLayout(new BorderLayout());
        contentPane.setTopComponent(new JScrollPane(table));
        contentPane.setBottomComponent(new JScrollPane(preview));

        centerPan.add(contentPane, BorderLayout.CENTER);
        centerPan.add(progressBar, BorderLayout.SOUTH);

        if (!newDatabase) {
            GroupTreeNode node = panel.metaData().getGroups();
            groupsAdd.setEnabled(false); // Will get enabled if there are groups that can be added to.
            insertNodes(groupsAdd, node, true);
            popup.add(groupsAdd);
        }
        getContentPane().add(centerPan, BorderLayout.CENTER);


        ButtonBarBuilder bb = new ButtonBarBuilder();
        bb.addGlue();
        bb.addGridded(ok);
        bb.addGridded(stop);
        bb.addGridded(cancel);
        bb.addRelatedGap();
        bb.addGridded(help);
        bb.addGlue();
        bb.getPanel().setBorder(BorderFactory.createEmptyBorder(2,2,2,2));

        ButtonStackBuilder builder = new ButtonStackBuilder();
        builder.addGridded(selectAll);
        builder.addGridded(deselectAll);
        builder.addRelatedGap();
        builder.addGridded(delete);
        builder.addRelatedGap();
        builder.addGridded(generate);
        builder.getPanel().setBorder(BorderFactory.createEmptyBorder(2,2,2,2));
        centerPan.add(builder.getPanel(), BorderLayout.WEST);

        ok.setEnabled(false);
        generate.setEnabled(false);
        ok.addActionListener(new OkListener());
        cancel.addActionListener(new CancelListener());
        generate.addActionListener(new GenerateListener());
        stop.addActionListener(new StopListener());
        selectAll.addActionListener(new SelectionButton(true));
        deselectAll.addActionListener(new SelectionButton(false));
        delete.addActionListener(new DeleteListener());
        help.addActionListener(new HelpAction(frame.helpDiag, GUIGlobals.importInspectionHelp));
        getContentPane().add(bb.getPanel(), BorderLayout.SOUTH);
        setSize(new Dimension(650, 650));
        //contentPane.setDividerLocation(0.6f);
    }

    public void setProgress(int current, int max) {
        progressBar.setIndeterminate(false);
        progressBar.setMinimum(0);
        progressBar.setMaximum(max);
        progressBar.setValue(current);
    }

    /**
     * Add a List of entries to the table view. The table will update to show the
     * added entries. Synchronizes on this.entries to avoid conflict with the delete button
     * which removes entries.
     * @param entries
     */
    public void addEntries(List entries) {
        synchronized (this.entries) {
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
    }

    /**
     * Removes all selected entries from the table. Synchronizes on this.entries to prevent
     * conflict with addition of new entries.
     */
    public void removeSelectedEntries() {
        synchronized (this.entries) {
            int[] rows = table.getSelectedRows();
            if (rows.length > 0) {
                for (int i=rows.length-1; i>=0; i--) {
                    tableModel.removeRow(rows[i]);
                    this.entries.remove(rows[i]);

                }
            }
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
        if (!generatedKeys)
            generate.setEnabled(true);
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

    /**
     * Generate keys for all entries. All keys will be unique with respect to one another,
     * and, if they are destined for an existing database, with respect to existing keys in
     * the database.
     */
    public void generateKeys() {
        BibtexDatabase database = null;
        // Relate to the existing database, if any:
        if (panel != null)
            database = panel.database();
        // ... or create a temporary one:
        else
            database = new BibtexDatabase();
        List keys = new ArrayList(entries.size());
        // Iterate over the entries, add them to the database we are working with,
        // and generate unique keys:
        for (Iterator i=entries.iterator(); i.hasNext();) {
            BibtexEntry entry = (BibtexEntry)i.next();
            //if (newDatabase) {
                try {
                    entry.setId(Util.createNeutralId());
                    database.insertEntry(entry);
                } catch (KeyCollisionException ex) {
                    ex.printStackTrace();
                }
            //}
            LabelPatternUtil.makeLabel(Globals.prefs.getKeyPattern(), database, entry);
            // Add the generated key to our list:
            keys.add(entry.getCiteKey());
        }
        // Remove the entries from the database again, since they are not supposed to
        // added yet. They only needed to be in it while we generated the keys, to keep
        // control over key uniqueness.
        for (Iterator i=entries.iterator(); i.hasNext();) {
            BibtexEntry entry = (BibtexEntry)i.next();
            database.removeEntry(entry.getId());
        }
        // Add a column to the table for displaying the generated keys:
        tableModel.addColumn("Bibtexkey", keys.toArray());
    }

    public void insertNodes(JMenu menu, GroupTreeNode node, boolean add) {
            final AbstractAction action = getAction(node,add);

            if (node.getChildCount() == 0) {
                menu.add(action);
                if (action.isEnabled())
                    menu.setEnabled(true);
                return;
            }

            JMenu submenu = null;
            if (node.getGroup() instanceof AllEntriesGroup) {
                for (int i = 0; i < node.getChildCount(); ++i) {
                    insertNodes(menu,(GroupTreeNode) node.getChildAt(i), add);
                }
            } else {
                submenu = new JMenu("["+node.getGroup().getName()+"]");
                // setEnabled(true) is done above/below if at least one menu
                // entry (item or submenu) is enabled
                submenu.setEnabled(action.isEnabled());
                submenu.add(action);
                submenu.add(new JPopupMenu.Separator());
                for (int i = 0; i < node.getChildCount(); ++i)
                    insertNodes(submenu,(GroupTreeNode) node.getChildAt(i), add);
                menu.add(submenu);
                if (submenu.isEnabled())
                    menu.setEnabled(true);
            }
        }

        private AbstractAction getAction(GroupTreeNode node, boolean add) {
            AbstractAction action = add ? (AbstractAction) new AddToGroupAction(node)
                    : (AbstractAction) new RemoveFromGroupAction(node);
            AbstractGroup group = node.getGroup();
            action.setEnabled(/*add ? */group.supportsAdd());// && !group.containsAll(selection)
            //        : group.supportsRemove() && group.containsAny(selection));
            return action;
        }

    /**
     * Stores the information about the selected entries being scheduled for addition
     * to this group. The entries are *not* added to the group at this time.
     *
     * Synchronizes on this.entries to prevent
     * conflict with threads that modify the entry list.
     */
    class AddToGroupAction extends AbstractAction {
        private GroupTreeNode node;
        public AddToGroupAction(GroupTreeNode node) {
            super(node.getGroup().getName());
            this.node = node;
        }
        public void actionPerformed(ActionEvent event) {
            synchronized (entries) {
                int[] rows = table.getSelectedRows();
                if (rows.length == 0)
                    return;

                for (int i=0; i<rows.length; i++) {
                    BibtexEntry entry = (BibtexEntry) entries.get(rows[i]);
                    // We store the groups this entry should be added to in a Set in the Map:
                    Set groups = (Set) groupAdditions.get(entry);
                    if (groups == null) {
                        // No previous definitions, so we create the Set now:
                        groups = new HashSet();
                        groupAdditions.put(entry, groups);
                    }
                    // Add the group:
                    groups.add(node);
                }
            }
        }
    }

    class RemoveFromGroupAction extends AbstractAction {
        private GroupTreeNode node;
        public RemoveFromGroupAction(GroupTreeNode node) {
            this.node = node;
        }
        public void actionPerformed(ActionEvent event) {
        }
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
                //entry.clone();

                // If this entry should be added to any groups, do it now:
                Set groups = (Set) groupAdditions.get(entry);
                if (groups != null) {
                    if (entry.getField(Globals.KEY_FIELD) == null) {
                        // The entry has no key, so it can't be added to the group.
                        // The best course of ation is probably to ask the user if a key should be generated
                        // immediately.
                        
                    }

                    // If the key was set, or has been set now, go ahead:
                    if (entry.getField(Globals.KEY_FIELD) == null) {
                        for (Iterator i2=groups.iterator(); i2.hasNext();) {
                            GroupTreeNode node = (GroupTreeNode)i2.next();
                            if (node.getGroup().supportsAdd()) {
                                // Add the entry:
                                AbstractUndoableEdit undo = node.getGroup().add(new BibtexEntry[] {entry});
                                if (undo instanceof UndoableChangeAssignment)
                                    ((UndoableChangeAssignment) undo).setEditedNode(node);
                                ce.addEdit(undo);

                            } else {
                                // Shouldn't happen...
                            }
                        }
                    }
                }

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

    class GenerateListener implements ActionListener {
        public void actionPerformed(ActionEvent event) {
            generate.setEnabled(false);
            generatedKeys = true; // To prevent the button from getting enabled again.
            generateKeys(); // Generate the keys.
        }
    }

    class DeleteListener implements ActionListener {
        public void actionPerformed(ActionEvent event) {
             removeSelectedEntries();
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

    class TableSelectionListener implements ListSelectionListener {
        public void valueChanged(ListSelectionEvent event) {
            if (event.getValueIsAdjusting())
                return;
            if (table.getSelectedRowCount() > 1)
                return; // No soup for you!
            int row = table.getSelectedRow();
            if (row < 0)
                return;
            preview.setEntry((BibtexEntry)entries.get(row));
            contentPane.setDividerLocation(0.5f);
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    preview.scrollRectToVisible(toRect);
                }
            });
            
        }

    }

    /**
       * This class handles clicks on the table that should trigger specific
       * events, like opening the popup menu.
       */
      class TableClickListener extends MouseAdapter {
          public void mousePressed(MouseEvent e) {

            // Check if the user has right-clicked. If so, open the right-click menu.
            if (e.isPopupTrigger()) {
            //if ( (e.getButton() == MouseEvent.BUTTON3) ||
            //     (ctrlClick && (e.getButton() == MouseEvent.BUTTON1) && e.isControlDown())) {
              int[] rows = table.getSelectedRows();
              popup.show(table, e.getX(), e.getY());
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
