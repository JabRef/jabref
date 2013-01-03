package net.sf.jabref.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import javax.swing.undo.AbstractUndoableEdit;

import net.sf.jabref.*;
import net.sf.jabref.external.DownloadExternalFile;
import net.sf.jabref.external.ExternalFileMenuItem;
import net.sf.jabref.groups.AbstractGroup;
import net.sf.jabref.groups.AllEntriesGroup;
import net.sf.jabref.groups.GroupTreeNode;
import net.sf.jabref.groups.UndoableChangeAssignment;
import net.sf.jabref.help.HelpAction;
import net.sf.jabref.imports.ImportInspector;
import net.sf.jabref.labelPattern.LabelPatternUtil;
import net.sf.jabref.undo.NamedCompound;
import net.sf.jabref.undo.UndoableInsertEntry;
import net.sf.jabref.undo.UndoableRemoveEntry;
import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;
import ca.odell.glazedlists.gui.TableFormat;
import ca.odell.glazedlists.swing.EventSelectionModel;
import ca.odell.glazedlists.swing.EventTableModel;
import ca.odell.glazedlists.swing.TableComparatorChooser;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.ButtonStackBuilder;
import com.jgoodies.uif_lite.component.UIFSplitPane;

/**
 * Dialog to allow the selection of entries as part of an Import.
 * 
 * The usual way to use this class is to pass it to an Importer which will do
 * the following:
 * <ul>
 * <li>Register itself as a callback to get notified if the user wants to stop
 * the import.</li>
 * <li>Call setVisible(true) to display the dialog</li>
 * <li>For each entry that has been found call addEntry(...)</li>
 * <li>Call entryListComplete() after all entries have been fetched</li>
 * </ul>
 * 
 * If the importer wants to cancel the import, it should call the dispose()
 * method.
 * 
 * If the importer receives the stopFetching-call, it should stop fetching as
 * soon as possible (it is not really critical, but good style to not contribute
 * any more results via addEntry, call entryListComplete() or dispose(), after
 * receiving this call).
 * 
 * @author alver
 */
public class ImportInspectionDialog extends JDialog implements ImportInspector, OutputPrinter {

    public static interface CallBack {

        /**
         * This method is called by the dialog when the user has cancelled or
         * signalled a stop. It is expected that any long-running fetch
         * operations will stop after this method is called.
         */
        public void stopFetching();
    }

	protected ImportInspectionDialog ths = this;

    protected BasePanel panel;

    protected JabRefFrame frame;

    protected MetaData metaData;

    protected UIFSplitPane contentPane = new UIFSplitPane(UIFSplitPane.VERTICAL_SPLIT);

    protected JTable glTable;

    protected TableComparatorChooser<BibtexEntry> comparatorChooser;

    protected EventSelectionModel<BibtexEntry> selectionModel;

    protected String[] fields;

    protected JProgressBar progressBar = new JProgressBar(JProgressBar.HORIZONTAL);

    protected JButton ok = new JButton(Globals.lang("Ok")), cancel = new JButton(Globals
        .lang("Cancel")), generate = new JButton(Globals.lang("Generate now"));

    protected EventList<BibtexEntry> entries = new BasicEventList<BibtexEntry>();

    protected SortedList<BibtexEntry> sortedList;

    /**
     * Duplicate resolving may require deletion of old entries.
     */
    protected List<BibtexEntry> entriesToDelete = new ArrayList<BibtexEntry>();

    protected String undoName;

    protected ArrayList<CallBack> callBacks = new ArrayList<CallBack>();

    protected boolean newDatabase;

    protected JMenu groupsAdd = new JMenu(Globals.lang("Add to group"));

    protected JPopupMenu popup = new JPopupMenu();

    protected JButton selectAll = new JButton(Globals.lang("Select all"));

    protected JButton deselectAll = new JButton(Globals.lang("Deselect all"));

    protected JButton deselectAllDuplicates = new JButton(Globals.lang("Deselect all duplicates"));

    protected JButton stop = new JButton(Globals.lang("Stop"));

    protected JButton delete = new JButton(Globals.lang("Delete"));

    protected JButton help = new JButton(Globals.lang("Help"));

    protected PreviewPanel preview;

    protected boolean generatedKeys = false; // Set to true after keys have
                                                // been

    // generated.

    protected boolean defaultSelected = true;

    protected Rectangle toRect = new Rectangle(0, 0, 1, 1);

    protected Map<BibtexEntry, Set<GroupTreeNode>> groupAdditions = new HashMap<BibtexEntry, Set<GroupTreeNode>>();

    protected JCheckBox autoGenerate = new JCheckBox(Globals.lang("Generate keys"), Globals.prefs
        .getBoolean("generateKeysAfterInspection"));

    protected JLabel duplLabel = new JLabel(GUIGlobals.getImage("duplicate")),
        fileLabel = new JLabel(GUIGlobals.getImage("psSmall")), pdfLabel = new JLabel(GUIGlobals
            .getImage("pdfSmall")), psLabel = new JLabel(GUIGlobals.getImage("psSmall")),
        urlLabel = new JLabel(GUIGlobals.getImage("wwwSmall"));

    protected final int DUPL_COL = 1, FILE_COL = 2, PDF_COL = -1,// 3,
        PS_COL = -2,// 4,
        URL_COL = 3,// 5,
        PAD = 4; // 6;

    /**
     * The "defaultSelected" boolean value determines if new entries added are
     * selected for import or not. This value is true by default.
     * 
     * @param defaultSelected
     *            The desired value.
     */
    public void setDefaultSelected(boolean defaultSelected) {
        this.defaultSelected = defaultSelected;
    }

    /**
     * Creates a dialog that displays the given list of fields in the table. The
     * dialog allows another process to add entries dynamically while the dialog
     * is shown.
     * 
     * @param frame
     * @param panel
     * @param fields
     */
    public ImportInspectionDialog(JabRefFrame frame, BasePanel panel, String[] fields,
        String undoName, boolean newDatabase) {
        this.frame = frame;
        this.panel = panel;
        this.metaData = (panel != null) ? panel.metaData() : new MetaData();
        this.fields = fields;
        this.undoName = undoName;
        this.newDatabase = newDatabase;
        preview = new PreviewPanel(null, metaData, Globals.prefs.get("preview0"));

        duplLabel.setToolTipText(Globals
            .lang("Possible duplicate of existing entry. Click to resolve."));

        sortedList = new SortedList<BibtexEntry>(entries);
        EventTableModel<BibtexEntry> tableModelGl = new EventTableModel<BibtexEntry>(sortedList,
            new EntryTableFormat());
        glTable = new EntryTable(tableModelGl);
        GeneralRenderer renderer = new GeneralRenderer(Color.white);
        glTable.setDefaultRenderer(JLabel.class, renderer);
        glTable.setDefaultRenderer(String.class, renderer);
        glTable.getInputMap().put(Globals.prefs.getKey("Delete"), "delete");
        DeleteListener deleteListener = new DeleteListener();
        glTable.getActionMap().put("delete", deleteListener);

        selectionModel = new EventSelectionModel<BibtexEntry>(sortedList);
        glTable.setSelectionModel(selectionModel);
        selectionModel.getSelected().addListEventListener(new EntrySelectionListener());
        comparatorChooser = TableComparatorChooser.install(glTable, sortedList,
            TableComparatorChooser.MULTIPLE_COLUMN_KEYBOARD);
        setupComparatorChooser();
        glTable.addMouseListener(new TableClickListener());

        setWidths();

        getContentPane().setLayout(new BorderLayout());
        progressBar.setIndeterminate(true);
        JPanel centerPan = new JPanel();
        centerPan.setLayout(new BorderLayout());

        contentPane.setTopComponent(new JScrollPane(glTable));
        contentPane.setBottomComponent(preview);

        centerPan.add(contentPane, BorderLayout.CENTER);
        centerPan.add(progressBar, BorderLayout.SOUTH);

        popup.add(deleteListener);
        popup.addSeparator();
        if (!newDatabase) {
            GroupTreeNode node = metaData.getGroups();
            groupsAdd.setEnabled(false); // Will get enabled if there are
            // groups that can be added to.
            insertNodes(groupsAdd, node);
            popup.add(groupsAdd);
        }

        // Add "Attach file" menu choices to right click menu:
        popup.add(new LinkLocalFile());
        popup.add(new DownloadFile());
        popup.add(new AutoSetLinks());
        // popup.add(new AttachFile("pdf"));
        // popup.add(new AttachFile("ps"));
        popup.add(new AttachUrl());
        getContentPane().add(centerPan, BorderLayout.CENTER);

        ButtonBarBuilder bb = new ButtonBarBuilder();
        bb.addGlue();
        bb.addButton(ok);
        bb.addButton(stop);
        bb.addButton(cancel);
        bb.addRelatedGap();
        bb.addButton(help);
        bb.addGlue();
        bb.getPanel().setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

        ButtonStackBuilder builder = new ButtonStackBuilder();
        builder.addButton(selectAll);
        builder.addButton(deselectAll);
        builder.addButton(deselectAllDuplicates);
        builder.addRelatedGap();
        builder.addButton(delete);
        builder.addRelatedGap();
        builder.addFixed(autoGenerate);
        builder.addButton(generate);
        builder.getPanel().setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        centerPan.add(builder.getPanel(), BorderLayout.WEST);

        ok.setEnabled(false);
        generate.setEnabled(false);
        ok.addActionListener(new OkListener());
        cancel.addActionListener(new CancelListener());
        generate.addActionListener(new GenerateListener());
        stop.addActionListener(new StopListener());
        selectAll.addActionListener(new SelectionButton(true));
        deselectAll.addActionListener(new SelectionButton(false));
        deselectAllDuplicates.addActionListener(new DeselectDuplicatesButtonListener());
        deselectAllDuplicates.setEnabled(false);
        delete.addActionListener(deleteListener);
        help.addActionListener(new HelpAction(frame.helpDiag, GUIGlobals.importInspectionHelp));
        getContentPane().add(bb.getPanel(), BorderLayout.SOUTH);

        // Remember and default to last size:
        setSize(new Dimension(Globals.prefs.getInt("importInspectionDialogWidth"), Globals.prefs
            .getInt("importInspectionDialogHeight")));
        addWindowListener(new WindowAdapter() {

            public void windowOpened(WindowEvent e) {
                contentPane.setDividerLocation(0.5f);
            }

            public void windowClosed(WindowEvent e) {
                Globals.prefs.putInt("importInspectionDialogWidth", getSize().width);
                Globals.prefs.putInt("importInspectionDialogHeight", getSize().height);
            }
        });
        // Key bindings:
        AbstractAction closeAction = new AbstractAction() {
          public void actionPerformed(ActionEvent e) {
            dispose();
          }
        };
        ActionMap am = contentPane.getActionMap();
        InputMap im = contentPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        im.put(Globals.prefs.getKey("Close dialog"), "close");
        am.put("close", closeAction);

    }

    /* (non-Javadoc)
	 * @see net.sf.jabref.gui.ImportInspection#setProgress(int, int)
	 */
    public void setProgress(int current, int max) {
        progressBar.setIndeterminate(false);
        progressBar.setMinimum(0);
        progressBar.setMaximum(max);
        progressBar.setValue(current);
    }

    /* (non-Javadoc)
	 * @see net.sf.jabref.gui.ImportInspection#addEntry(net.sf.jabref.BibtexEntry)
	 */
    public void addEntry(BibtexEntry entry) {
        List<BibtexEntry> list = new ArrayList<BibtexEntry>();
        list.add(entry);
        addEntries(list);
    }

    /* (non-Javadoc)
	 * @see net.sf.jabref.gui.ImportInspection#addEntries(java.util.Collection)
	 */
    public void addEntries(Collection<BibtexEntry> entries) {

        for (BibtexEntry entry : entries) {
            // We exploit the entry's search status for indicating "Keep"
            // status:
            entry.setSearchHit(defaultSelected);
            // We exploit the entry's group status for indicating duplicate
            // status.
            // Checking duplicates means both checking against the background
            // database (if
            // applicable) and against entries already in the table.
            if (((panel != null) && (DuplicateCheck.containsDuplicate(panel.database(), entry) != null)) ||
                (internalDuplicate(this.entries, entry) != null)) {
                entry.setGroupHit(true);
                deselectAllDuplicates.setEnabled(true);
            }
            this.entries.getReadWriteLock().writeLock().lock();
            this.entries.add(entry);
            this.entries.getReadWriteLock().writeLock().unlock();
        }
    }

    /**
     * Checks if there are duplicates to the given entry in the Collection. Does
     * not report the entry as duplicate of itself if it is in the Collection.
     * 
     * @param entries
     *            A Collection of BibtexEntry instances.
     * @param entry
     *            The entry to search for duplicates of.
     * @return A possible duplicate, if any, or null if none were found.
     */
    protected BibtexEntry internalDuplicate(Collection<BibtexEntry> entries, BibtexEntry entry) {
        for (BibtexEntry othEntry : entries) {
            if (othEntry == entry)
                continue; // Don't compare the entry to itself
            if (DuplicateCheck.isDuplicate(entry, othEntry))
                return othEntry;
        }
        return null;
    }

    /**
     * Removes all selected entries from the table. Synchronizes on this.entries
     * to prevent conflict with addition of new entries.
     */
    public void removeSelectedEntries() {
        int row = glTable.getSelectedRow();
        List<Object> toRemove = new ArrayList<Object>();
        toRemove.addAll(selectionModel.getSelected());
        entries.getReadWriteLock().writeLock().lock();
        for (Object o : toRemove) {
            entries.remove(o);
        }
        entries.getReadWriteLock().writeLock().unlock();
        glTable.clearSelection();
        if ((row >= 0) && (entries.size() > 0)) {
            row = Math.min(entries.size() - 1, row);
            glTable.addRowSelectionInterval(row, row);
        }
    }

    /* (non-Javadoc)
	 * @see net.sf.jabref.gui.ImportInspection#entryListComplete()
	 */
    public void entryListComplete() {
        progressBar.setIndeterminate(false);
        progressBar.setVisible(false);
        ok.setEnabled(true);
        if (!generatedKeys)
            generate.setEnabled(true);
        stop.setEnabled(false);
        
        //This is for selecting and displaying the first entry in the glTable
        this.glTable.repaint();
        if(this.glTable.getSelectedRowCount() == 0) //Only if no row selection is done, until now
        	if (this.glTable.getRowCount()>0)
        		this.glTable.setRowSelectionInterval(0, 0); //Select first row in the table
    }

    /**
     * This method returns a List containing all entries that are selected
     * (checkbox checked).
     * 
     * @return a List containing the selected entries.
     */
    public List<BibtexEntry> getSelectedEntries() {
        List<BibtexEntry> selected = new ArrayList<BibtexEntry>();
        for (Iterator<BibtexEntry> i = entries.iterator(); i.hasNext();) {
            BibtexEntry entry = i.next();
            if (entry.isSearchHit())
                selected.add(entry);
        }
        /*
         * for (int i = 0; i < table.getRowCount(); i++) { Boolean sel =
         * (Boolean) table.getValueAt(i, 0); if (sel.booleanValue()) {
         * selected.add(entries.get(i)); } }
         */
        return selected;
    }

    /**
     * Generate key for the selected entry only.
     */
    public void generateKeySelectedEntry() {
        if (selectionModel.getSelected().size() != 1)
            return;
        BibtexEntry entry = selectionModel.getSelected().get(0);
        entries.getReadWriteLock().writeLock().lock();

        BibtexDatabase database;
        MetaData metaData;
        
        // Relate to existing database, if any:
        if (panel != null) {
            database = panel.database();
            metaData = panel.metaData();
        } else {
            database = new BibtexDatabase();
            metaData = new MetaData();
        }
        try {
            entry.setId(Util.createNeutralId());
            // Add the entry to the database we are working with:
            database.insertEntry(entry);
        } catch (KeyCollisionException ex) {
            ex.printStackTrace();
        }
        // Generate a unique key:
        LabelPatternUtil.makeLabel(metaData, database, entry);
        // Remove the entry from the database again, since we only added it in
        // order to
        // make sure the key was unique:
        database.removeEntry(entry.getId());

        entries.getReadWriteLock().writeLock().lock();
        glTable.repaint();
    }

    /**
     * Generate keys for all entries. All keys will be unique with respect to
     * one another, and, if they are destined for an existing database, with
     * respect to existing keys in the database.
     */
    public void generateKeys(boolean addColumn) {
        entries.getReadWriteLock().writeLock().lock();

        BibtexDatabase database;
        MetaData metaData;
        
        // Relate to existing database, if any:
        if (panel != null) {
            database = panel.database();
            metaData = panel.metaData();
        } else {
            database = new BibtexDatabase();
            metaData = new MetaData();
        }

        List<String> keys = new ArrayList<String>(entries.size());
        // Iterate over the entries, add them to the database we are working
        // with,
        // and generate unique keys:
        for (Iterator<BibtexEntry> i = entries.iterator(); i.hasNext();) {
            BibtexEntry entry = i.next();
            // if (newDatabase) {
            try {
                entry.setId(Util.createNeutralId());
                database.insertEntry(entry);
            } catch (KeyCollisionException ex) {
                ex.printStackTrace();
            }
            // }
            LabelPatternUtil.makeLabel(metaData, database, entry);
            // Add the generated key to our list:
            keys.add(entry.getCiteKey());
        }
        // Remove the entries from the database again, since they are not
        // supposed to
        // added yet. They only needed to be in it while we generated the keys,
        // to keep
        // control over key uniqueness.
        for (Iterator<BibtexEntry> i = entries.iterator(); i.hasNext();) {
            BibtexEntry entry = i.next();
            database.removeEntry(entry.getId());
        }
        entries.getReadWriteLock().writeLock().lock();
        glTable.repaint();
    }

    public void insertNodes(JMenu menu, GroupTreeNode node) {
        final AbstractAction action = getAction(node);

        if (node.getChildCount() == 0) {
            menu.add(action);
            if (action.isEnabled())
                menu.setEnabled(true);
            return;
        }

        JMenu submenu = null;
        if (node.getGroup() instanceof AllEntriesGroup) {
            for (int i = 0; i < node.getChildCount(); ++i) {
                insertNodes(menu, (GroupTreeNode) node.getChildAt(i));
            }
        } else {
            submenu = new JMenu("[" + node.getGroup().getName() + "]");
            // setEnabled(true) is done above/below if at least one menu
            // entry (item or submenu) is enabled
            submenu.setEnabled(action.isEnabled());
            submenu.add(action);
            submenu.add(new JPopupMenu.Separator());
            for (int i = 0; i < node.getChildCount(); ++i)
                insertNodes(submenu, (GroupTreeNode) node.getChildAt(i));
            menu.add(submenu);
            if (submenu.isEnabled())
                menu.setEnabled(true);
        }
    }

    protected AbstractAction getAction(GroupTreeNode node) {
        AbstractAction action = new AddToGroupAction(node);
        AbstractGroup group = node.getGroup();
        action.setEnabled(group.supportsAdd());
        return action;
    }

    /**
     * Stores the information about the selected entries being scheduled for
     * addition to this group. The entries are *not* added to the group at this
     * time. <p/> Synchronizes on this.entries to prevent conflict with threads
     * that modify the entry list.
     */
    class AddToGroupAction extends AbstractAction {

        protected GroupTreeNode node;

        public AddToGroupAction(GroupTreeNode node) {
            super(node.getGroup().getName());
            this.node = node;
        }

        public void actionPerformed(ActionEvent event) {

            selectionModel.getSelected().getReadWriteLock().writeLock().lock();
            for (Iterator<BibtexEntry> i = selectionModel.getSelected().iterator(); i.hasNext();) {
                BibtexEntry entry = i.next();
                // We store the groups this entry should be added to in a Set in
                // the Map:
                Set<GroupTreeNode> groups = groupAdditions.get(entry);
                if (groups == null) {
                    // No previous definitions, so we create the Set now:
                    groups = new HashSet<GroupTreeNode>();
                    groupAdditions.put(entry, groups);
                }
                // Add the group:
                groups.add(node);
            }
            selectionModel.getSelected().getReadWriteLock().writeLock().unlock();
        }
    }

    public void addCallBack(CallBack cb) {
        callBacks.add(cb);
    }

    class OkListener implements ActionListener {

        public void actionPerformed(ActionEvent event) {

            // First check if we are supposed to warn about duplicates. If so,
            // see if there
            // are unresolved duplicates, and warn if yes.
            if (Globals.prefs.getBoolean("warnAboutDuplicatesInInspection")) {
                for (Iterator<BibtexEntry> i = entries.iterator(); i.hasNext();) {

                    BibtexEntry entry = i.next();
                    // Only check entries that are to be imported. Keep status
                    // is indicated
                    // through the search hit status of the entry:
                    if (!entry.isSearchHit())
                        continue;

                    // Check if the entry is a suspected, unresolved, duplicate.
                    // This status
                    // is indicated by the entry's group hit status:
                    if (entry.isGroupHit()) {
                        CheckBoxMessage cbm = new CheckBoxMessage(
                            Globals
                                .lang("There are possible duplicates (marked with a 'D' icon) that haven't been resolved. Continue?"),
                            Globals.lang("Disable this confirmation dialog"), false);
                        int answer = JOptionPane.showConfirmDialog(ImportInspectionDialog.this,
                            cbm, Globals.lang("Duplicates found"), JOptionPane.YES_NO_OPTION);
                        if (cbm.isSelected())
                            Globals.prefs.putBoolean("warnAboutDuplicatesInInspection", false);
                        if (answer == JOptionPane.NO_OPTION)
                            return;
                        break;
                    }
                }
            }

            // The compund undo action used to contain all changes made by this
            // dialog.
            NamedCompound ce = new NamedCompound(undoName);

            // See if we should remove any old entries for duplicate resolving:
            if (entriesToDelete.size() > 0) {
                for (Iterator<BibtexEntry> i = entriesToDelete.iterator(); i.hasNext();) {
                    BibtexEntry entry = i.next();
                    ce.addEdit(new UndoableRemoveEntry(panel.database(), entry, panel));
                    panel.database().removeEntry(entry.getId());
                }
            }

            // If "Generate keys" is checked, generate keys unless it's already
            // been done:
            if (autoGenerate.isSelected() && !generatedKeys) {
                generateKeys(false);
            }
            // Remember the choice until next time:
            Globals.prefs.putBoolean("generateKeysAfterInspection", autoGenerate.isSelected());

            final List<BibtexEntry> selected = getSelectedEntries();

            if (selected.size() > 0) {

                if (newDatabase) {
                    // Create a new BasePanel for the entries:
                    BibtexDatabase base = new BibtexDatabase();
                    panel = new BasePanel(frame, base, null, new MetaData(),
                        Globals.prefs.get("defaultEncoding"));
                }

                boolean groupingCanceled = false;

                // Set owner/timestamp if options are enabled:
                Util.setAutomaticFields(selected, Globals.prefs.getBoolean("overwriteOwner"),
                    Globals.prefs.getBoolean("overwriteTimeStamp"), Globals.prefs.getBoolean("markImportedEntries"));


                // Check if we should unmark entries before adding the new ones:
                if (Globals.prefs.getBoolean("unmarkAllEntriesBeforeImporting"))
                    for (BibtexEntry entry : panel.database().getEntries()) {
                        Util.unmarkEntry(entry, true, panel.database(), ce);
                    }

                for (Iterator<BibtexEntry> i = selected.iterator(); i.hasNext();) {
                    BibtexEntry entry = i.next();
                    // entry.clone();

                    // Remove settings to group/search hit status:
                    entry.setSearchHit(false);
                    entry.setGroupHit(false);

                    // If this entry should be added to any groups, do it now:
                    Set<GroupTreeNode> groups = groupAdditions.get(entry);
                    if (!groupingCanceled && (groups != null)) {
                        if (entry.getField(BibtexFields.KEY_FIELD) == null) {
                            // The entry has no key, so it can't be added to the
                            // group.
                            // The best course of action is probably to ask the
                            // user if a key should be generated
                            // immediately.
                            int answer = JOptionPane
                                .showConfirmDialog(
                                    ImportInspectionDialog.this,
                                    Globals
                                        .lang("Cannot add entries to group without generating keys. Generate keys now?"),
                                    Globals.lang("Add to group"), JOptionPane.YES_NO_OPTION);
                            if (answer == JOptionPane.YES_OPTION) {
                                generateKeys(false);
                            } else
                                groupingCanceled = true;
                        }

                        // If the key existed, or exists now, go ahead:
                        if (entry.getField(BibtexFields.KEY_FIELD) != null) {
                            for (Iterator<GroupTreeNode> i2 = groups.iterator(); i2.hasNext();) {
                                GroupTreeNode node = i2.next();
                                if (node.getGroup().supportsAdd()) {
                                    // Add the entry:
                                    AbstractUndoableEdit undo = node.getGroup().add(
                                        new BibtexEntry[] { entry });
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
                        entry.setId(Util.createNeutralId());
                        panel.database().insertEntry(entry);
                        // Let the autocompleters, if any, harvest words from
                        // the entry:
                        Util.updateCompletersForEntry(panel.getAutoCompleters(), entry);
                        ce.addEdit(new UndoableInsertEntry(panel.database(), entry, panel));
                    } catch (KeyCollisionException e) {
                        e.printStackTrace();
                    }
                }

                ce.end();
                panel.undoManager.addEdit(ce);
            }

            dispose();
            SwingUtilities.invokeLater(new Thread() {

                public void run() {
                    if (newDatabase) {
                        frame.addTab(panel, null, true);
                    }
                    panel.markBaseChanged();

                    if (selected.size() > 0) {
                        frame.output(Globals.lang("Number of entries successfully imported") +
                            ": " + selected.size());
                    } else {
                        frame.output(Globals.lang("No entries imported."));
                    }
                }
            });
        }
    }

    protected void signalStopFetching() {
        for (CallBack c : callBacks) {
            c.stopFetching();
        }
    }

    protected void setWidths() {
        TableColumnModel cm = glTable.getColumnModel();
        cm.getColumn(0).setPreferredWidth(55);
        cm.getColumn(0).setMinWidth(55);
        cm.getColumn(0).setMaxWidth(55);
        for (int i = 1; i < PAD; i++) {
        	// Check if the Column is a RankingColumn
        	// If this is the case, set a certain Column-width,
        	// because the RankingIconColumn needs some more width
        	if (frame.basePanel().tableFormat.isRankingColumn(i)) {
        		// Lock the width of ranking icon column.
                cm.getColumn(i).setPreferredWidth(GUIGlobals.WIDTH_ICON_COL_RANKING);
                cm.getColumn(i).setMinWidth(GUIGlobals.WIDTH_ICON_COL_RANKING);
                cm.getColumn(i).setMaxWidth(GUIGlobals.WIDTH_ICON_COL_RANKING);
        	} else {
        		// Lock the width of icon columns.
                cm.getColumn(i).setPreferredWidth(GUIGlobals.WIDTH_ICON_COL);
                cm.getColumn(i).setMinWidth(GUIGlobals.WIDTH_ICON_COL);
                cm.getColumn(i).setMaxWidth(GUIGlobals.WIDTH_ICON_COL);	
        	}
        }

        for (int i = 0; i < fields.length; i++) {
            int width = BibtexFields.getFieldLength(fields[i]);
            glTable.getColumnModel().getColumn(i + PAD).setPreferredWidth(width);
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
            frame.output(Globals.lang("Import canceled by user"));
        }
    }

    class GenerateListener implements ActionListener {

        public void actionPerformed(ActionEvent event) {
            generate.setEnabled(false);
            generatedKeys = true; // To prevent the button from getting
            // enabled again.
            generateKeys(true); // Generate the keys.
        }
    }

    class DeleteListener extends AbstractAction implements ActionListener {

        public DeleteListener() {
            super(Globals.lang("Delete"), GUIGlobals.getImage("delete"));
        }

        public void actionPerformed(ActionEvent event) {
            removeSelectedEntries();
        }
    }

    class MyTable extends JTable {

        public MyTable(TableModel model) {
            super(model);
            // setDefaultRenderer(Boolean.class, );
        }

        public boolean isCellEditable(int row, int col) {
            return col == 0;
        }
    }

    class MyTableModel extends DefaultTableModel {

        public Class<?> getColumnClass(int i) {
            if (i == 0)
                return Boolean.class;
            else
                return String.class;
        }

    }

    class SelectionButton implements ActionListener {

        protected Boolean enable;

        public SelectionButton(boolean enable) {
            this.enable = Boolean.valueOf(enable);
        }

        public void actionPerformed(ActionEvent event) {
            for (int i = 0; i < glTable.getRowCount(); i++) {
                glTable.setValueAt(enable, i, 0);
            }
            glTable.repaint();
        }
    }

    class DeselectDuplicatesButtonListener implements ActionListener {

        public void actionPerformed(ActionEvent event) {
            for (int i = 0; i < glTable.getRowCount(); i++) {
                if (glTable.getValueAt(i, DUPL_COL) != null) {
                    glTable.setValueAt(Boolean.valueOf(false), i, 0);
                }
            }
            glTable.repaint();
        }
    }

    class EntrySelectionListener implements ListEventListener<BibtexEntry> {

        public void listChanged(ListEvent<BibtexEntry> listEvent) {
            if (listEvent.getSourceList().size() == 1) {
                preview.setEntry(listEvent.getSourceList().get(0));
                contentPane.setDividerLocation(0.5f);
                SwingUtilities.invokeLater(new Runnable() {

                    public void run() {
                        preview.scrollRectToVisible(toRect);
                    }
                });
            }
        }
    }

    /**
     * This class handles clicks on the table that should trigger specific
     * events, like opening the popup menu.
     */
    class TableClickListener implements MouseListener {

        public boolean isIconColumn(int col) {
            return (col == FILE_COL) || (col == PDF_COL) || (col == PS_COL) || (col == URL_COL);
        }

        public void mouseClicked(MouseEvent e) {
            final int col = glTable.columnAtPoint(e.getPoint()), row = glTable.rowAtPoint(e
                .getPoint());
            if (isIconColumn(col)) {
                BibtexEntry entry = sortedList.get(row);

                switch (col) {
                case FILE_COL:
                    Object o = entry.getField(GUIGlobals.FILE_FIELD);
                    if (o != null) {
                        FileListTableModel tableModel = new FileListTableModel();
                        tableModel.setContent((String) o);
                        if (tableModel.getRowCount() == 0)
                            return;
                        FileListEntry fl = tableModel.getEntry(0);
                        (new ExternalFileMenuItem(frame, entry, "", fl.getLink(), null, panel
                            .metaData(), fl.getType())).actionPerformed(null);
                    }
                    break;
                case URL_COL:
                    openExternalLink("url", e);
                    break;
                case PDF_COL:
                    openExternalLink("pdf", e);
                    break;
                case PS_COL:
                    openExternalLink("ps", e);
                    break;
                }
            }
        }

        public void mouseEntered(MouseEvent e) {

        }

        public void mouseExited(MouseEvent e) {

        }

        /**
         * Show right-click menu. If the click happened in an icon column that
         * presents its own popup menu, show that. Otherwise, show the ordinary
         * popup menu.
         * 
         * @param e
         *            The mouse event that triggered the popup.
         */
        public void showPopup(MouseEvent e) {
            final int col = glTable.columnAtPoint(e.getPoint());
            switch (col) {
            case FILE_COL:
                showFileFieldMenu(e);
                break;
            default:
                showOrdinaryRightClickMenu(e);
                break;
            }

        }

        public void showOrdinaryRightClickMenu(MouseEvent e) {
            popup.show(glTable, e.getX(), e.getY());
        }

        /**
         * Show the popup menu for the FILE field.
         * 
         * @param e
         *            The mouse event that triggered the popup.
         */
        public void showFileFieldMenu(MouseEvent e) {
            final int row = glTable.rowAtPoint(e.getPoint());
            BibtexEntry entry = sortedList.get(row);
            JPopupMenu menu = new JPopupMenu();
            int count = 0;
            Object o = entry.getField(GUIGlobals.FILE_FIELD);
            FileListTableModel fileList = new FileListTableModel();
            fileList.setContent((String) o);
            // If there are one or more links, open the first one:
            for (int i = 0; i < fileList.getRowCount(); i++) {
                FileListEntry flEntry = fileList.getEntry(i);
                String description = flEntry.getDescription();
                if ((description == null) || (description.trim().length() == 0))
                    description = flEntry.getLink();
                menu.add(new ExternalFileMenuItem(panel.frame(), entry, description, flEntry
                    .getLink(), flEntry.getType().getIcon(), panel.metaData(), flEntry.getType()));
                count++;
            }
            if (count == 0) {
                showOrdinaryRightClickMenu(e);
            } else
                menu.show(glTable, e.getX(), e.getY());
        }

        /**
         * Open old-style external links after user clicks icon.
         * 
         * @param fieldName
         *            The name of the BibTeX field this icon is used for.
         * @param e
         *            The MouseEvent that triggered this operation.
         */
        public void openExternalLink(String fieldName, MouseEvent e) {
            final int row = glTable.rowAtPoint(e.getPoint());
            BibtexEntry entry = sortedList.get(row);

            Object link = entry.getField(fieldName);
            try {
                if (link != null)
                    Util.openExternalViewer(panel.metaData(), (String) link, fieldName);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        public void mouseReleased(MouseEvent e) {
            // Check if the user has right-clicked. If so, open the right-click
            // menu.
            if (e.isPopupTrigger()) {
                showPopup(e);
                return;
            }
        }

        public void mousePressed(MouseEvent e) {
            // Check if the user has right-clicked. If so, open the right-click
            // menu.
            if (e.isPopupTrigger()) {
                showPopup(e);
                return;
            }

            // Check if any other action should be taken:
            final int col = glTable.columnAtPoint(e.getPoint()), row = glTable.rowAtPoint(e
                .getPoint());
            // Is this the duplicate icon column, and is there an icon?
            if ((col == DUPL_COL) && (glTable.getValueAt(row, col) != null)) {
                BibtexEntry first = sortedList.get(row);
                BibtexEntry other = DuplicateCheck.containsDuplicate(panel.database(), first);
                if (other != null) {
                    // This will be true if the duplicate is in the existing
                    // database.
                    DuplicateResolverDialog diag = new DuplicateResolverDialog(
                        ImportInspectionDialog.this, other, first,
                        DuplicateResolverDialog.INSPECTION);
                    Util.placeDialog(diag, ImportInspectionDialog.this);
                    diag.setVisible(true);
                    ImportInspectionDialog.this.toFront();
                    if (diag.getSelected() == DuplicateResolverDialog.KEEP_UPPER) {
                        // Remove old entry. Or... add it to a list of entries
                        // to be deleted. We only delete
                        // it after Ok is clicked.
                        entriesToDelete.add(other);
                        // Clear duplicate icon, which is controlled by the
                        // group hit
                        // field of the entry:
                        entries.getReadWriteLock().writeLock().lock();
                        first.setGroupHit(false);
                        entries.getReadWriteLock().writeLock().unlock();

                    } else if (diag.getSelected() == DuplicateResolverDialog.KEEP_LOWER) {
                        // Remove the entry from the import inspection dialog.
                        entries.getReadWriteLock().writeLock().lock();
                        entries.remove(first);
                        entries.getReadWriteLock().writeLock().unlock();
                    } else if (diag.getSelected() == DuplicateResolverDialog.KEEP_BOTH) {
                        // Do nothing.
                        entries.getReadWriteLock().writeLock().lock();
                        first.setGroupHit(false);
                        entries.getReadWriteLock().writeLock().unlock();
                    }
                }
                // Check if the duplicate is of another entry in the import:
                other = internalDuplicate(entries, first);
                if (other != null) {
                    int answer = DuplicateResolverDialog.resolveDuplicate(
                        ImportInspectionDialog.this, first, other);
                    if (answer == DuplicateResolverDialog.KEEP_UPPER) {
                        entries.remove(other);
                        first.setGroupHit(false);
                    } else if (answer == DuplicateResolverDialog.KEEP_LOWER) {
                        entries.remove(first);
                    } else if (answer == DuplicateResolverDialog.KEEP_BOTH) {
                        first.setGroupHit(false);
                    }
                }
            }
        }
    }

    class AttachUrl extends JMenuItem implements ActionListener {

        public AttachUrl() {
            super(Globals.lang("Attach URL"));
            addActionListener(this);
        }

        public void actionPerformed(ActionEvent event) {
            if (selectionModel.getSelected().size() != 1)
                return;
            BibtexEntry entry = selectionModel.getSelected().get(0);
            String result = JOptionPane.showInputDialog(ImportInspectionDialog.this, Globals
                .lang("Enter URL"), entry.getField("url"));
            entries.getReadWriteLock().writeLock().lock();
            if (result != null) {
                if (result.equals("")) {
                    entry.clearField("url");
                } else {
                    entry.setField("url", result);
                }
            }
            entries.getReadWriteLock().writeLock().unlock();
            glTable.repaint();
        }
    }

    class DownloadFile extends JMenuItem implements ActionListener,
        DownloadExternalFile.DownloadCallback {

        BibtexEntry entry = null;

        public DownloadFile() {
            super(Globals.lang("Download file"));
            addActionListener(this);
        }

        public void actionPerformed(ActionEvent actionEvent) {
            if (selectionModel.getSelected().size() != 1)
                return;
            entry = selectionModel.getSelected().get(0);
            String bibtexKey = entry.getCiteKey();
            if (bibtexKey == null) {
                int answer = JOptionPane.showConfirmDialog(frame, Globals
                    .lang("This entry has no BibTeX key. Generate key now?"), Globals
                    .lang("Download file"), JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE);
                if (answer == JOptionPane.OK_OPTION) {
                    generateKeySelectedEntry();
                    bibtexKey = entry.getCiteKey();
                }
            }
            DownloadExternalFile def = new DownloadExternalFile(frame, metaData, bibtexKey);
            try {
                def.download(this);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        public void downloadComplete(FileListEntry file) {
            ImportInspectionDialog.this.toFront(); // Hack
            FileListTableModel model = new FileListTableModel();
            String oldVal = entry.getField(GUIGlobals.FILE_FIELD);
            if (oldVal != null)
                model.setContent(oldVal);
            model.addEntry(model.getRowCount(), file);
            entries.getReadWriteLock().writeLock().lock();
            entry.setField(GUIGlobals.FILE_FIELD, model.getStringRepresentation());
            entries.getReadWriteLock().writeLock().unlock();
            glTable.repaint();
        }
    }

    class AutoSetLinks extends JMenuItem implements ActionListener {

        public AutoSetLinks() {
            super(Globals.lang("Autoset external links"));
            addActionListener(this);
        }

        public void actionPerformed(ActionEvent actionEvent) {
            if (selectionModel.getSelected().size() != 1)
                return;
            final BibtexEntry entry = selectionModel.getSelected().get(0);
            String bibtexKey = entry.getCiteKey();
            if (bibtexKey == null) {
                int answer = JOptionPane.showConfirmDialog(frame, Globals
                    .lang("This entry has no BibTeX key. Generate key now?"), Globals
                    .lang("Download file"), JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE);
                if (answer == JOptionPane.OK_OPTION) {
                    generateKeySelectedEntry();
                    bibtexKey = entry.getCiteKey();
                } else
                    return; // Can't go on without the bibtex key.
            }
            final FileListTableModel model = new FileListTableModel();
            String oldVal = entry.getField(GUIGlobals.FILE_FIELD);
            if (oldVal != null)
                model.setContent(oldVal);
            // We have a static utility method for searching for all relevant
            // links:
            JDialog diag = new JDialog(ImportInspectionDialog.this, true);
            FileListEditor.autoSetLinks(entry, model, metaData, new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    if (e.getID() > 0) {
                        entries.getReadWriteLock().writeLock().lock();
                        entry.setField(GUIGlobals.FILE_FIELD, model.getStringRepresentation());
                        entries.getReadWriteLock().writeLock().unlock();
                        glTable.repaint();
                    }
                }
            }, diag);

        }
    }

    class LinkLocalFile extends JMenuItem implements ActionListener,
        DownloadExternalFile.DownloadCallback {

        BibtexEntry entry = null;

        public LinkLocalFile() {
            super(Globals.lang("Link local file"));
            addActionListener(this);
        }

        public void actionPerformed(ActionEvent actionEvent) {
            if (selectionModel.getSelected().size() != 1)
                return;
            entry = selectionModel.getSelected().get(0);
            FileListEntry flEntry = new FileListEntry("", "", null);
            FileListEntryEditor editor = new FileListEntryEditor(frame, flEntry, false, true,
                metaData);
            editor.setVisible(true, true);
            if (editor.okPressed()) {
                FileListTableModel model = new FileListTableModel();
                String oldVal = entry.getField(GUIGlobals.FILE_FIELD);
                if (oldVal != null)
                    model.setContent(oldVal);
                model.addEntry(model.getRowCount(), flEntry);
                entries.getReadWriteLock().writeLock().lock();
                entry.setField(GUIGlobals.FILE_FIELD, model.getStringRepresentation());
                entries.getReadWriteLock().writeLock().unlock();
                glTable.repaint();
            }
        }

        public void downloadComplete(FileListEntry file) {
            ImportInspectionDialog.this.toFront(); // Hack
            FileListTableModel model = new FileListTableModel();
            String oldVal = entry.getField(GUIGlobals.FILE_FIELD);
            if (oldVal != null)
                model.setContent(oldVal);
            model.addEntry(model.getRowCount(), file);
            entries.getReadWriteLock().writeLock().lock();
            entry.setField(GUIGlobals.FILE_FIELD, model.getStringRepresentation());
            entries.getReadWriteLock().writeLock().unlock();
            glTable.repaint();
        }
    }

    class AttachFile extends JMenuItem implements ActionListener {

        String fileType;

        public AttachFile(String fileType) {
            super(Globals.lang("Attach %0 file", new String[] { fileType.toUpperCase() }));
            this.fileType = fileType;
            addActionListener(this);
        }

        public void actionPerformed(ActionEvent event) {

            if (selectionModel.getSelected().size() != 1)
                return;
            BibtexEntry entry = selectionModel.getSelected().get(0);
            // Call up a dialog box that provides Browse, Download and auto
            // buttons:
            AttachFileDialog diag = new AttachFileDialog(ImportInspectionDialog.this, metaData,
                entry, fileType);
            Util.placeDialog(diag, ImportInspectionDialog.this);
            diag.setVisible(true);
            // After the dialog has closed, if it wasn't cancelled, list the
            // field:
            if (!diag.cancelled()) {
                entries.getReadWriteLock().writeLock().lock();
                entry.setField(fileType, diag.getValue());
                entries.getReadWriteLock().writeLock().unlock();
                glTable.repaint();
            }

        }
    }

    @SuppressWarnings("unchecked")
    protected void setupComparatorChooser() {
        // First column:
        java.util.List<Comparator> comparators = comparatorChooser
            .getComparatorsForColumn(0);
        comparators.clear();

        comparators = comparatorChooser.getComparatorsForColumn(1);
        comparators.clear();

        // Icon columns:
        for (int i = 2; i < PAD; i++) {
            comparators = comparatorChooser.getComparatorsForColumn(i);
            comparators.clear();
            if (i == FILE_COL)
                comparators.add(new IconComparator(new String[] { GUIGlobals.FILE_FIELD }));
            else if (i == PDF_COL)
                comparators.add(new IconComparator(new String[] { "pdf" }));
            else if (i == PS_COL)
                comparators.add(new IconComparator(new String[] { "ps" }));
            else if (i == URL_COL)
                comparators.add(new IconComparator(new String[] { "url" }));

        }
        // Remaining columns:
        for (int i = PAD; i < PAD + fields.length; i++) {
            comparators = comparatorChooser.getComparatorsForColumn(i);
            comparators.clear();
            comparators.add(new FieldComparator(fields[i - PAD]));
        }

        // Set initial sort columns:

        /*
         * // Default sort order: String[] sortFields = new String[]
         * {Globals.prefs.get("priSort"), Globals.prefs.get("secSort"),
         * Globals.prefs.get("terSort")}; boolean[] sortDirections = new
         * boolean[] {Globals.prefs.getBoolean("priDescending"),
         * Globals.prefs.getBoolean("secDescending"),
         * Globals.prefs.getBoolean("terDescending")}; // descending
         */
        sortedList.getReadWriteLock().writeLock().lock();
        comparatorChooser.appendComparator(PAD, 0, false);
        sortedList.getReadWriteLock().writeLock().unlock();

    }

    class EntryTable extends JTable {

        GeneralRenderer renderer = new GeneralRenderer(Color.white);

        public EntryTable(TableModel model) {
            super(model);
            getTableHeader().setReorderingAllowed(false);
        }

        public TableCellRenderer getCellRenderer(int row, int column) {
            return column == 0 ? getDefaultRenderer(Boolean.class) : renderer;
        }

        /*
         * public TableCellEditor getCellEditor() { return
         * getDefaultEditor(Boolean.class); }
         */

        public Class<?> getColumnClass(int col) {
            if (col == 0)
                return Boolean.class;
            else if (col < PAD)
                return JLabel.class;
            else
                return String.class;
        }

        public boolean isCellEditable(int row, int column) {
            return column == 0;
        }

        public void setValueAt(Object value, int row, int column) {
            // Only column 0, which is controlled by BibtexEntry.searchHit, is
            // editable:
            entries.getReadWriteLock().writeLock().lock();
            BibtexEntry entry = sortedList.get(row);
            entry.setSearchHit(((Boolean) value).booleanValue());
            entries.getReadWriteLock().writeLock().unlock();
        }
    }

    class EntryTableFormat implements TableFormat<BibtexEntry> {

        public int getColumnCount() {
            return PAD + fields.length; 
        }

        public String getColumnName(int i) {
            if (i == 0)
                return Globals.lang("Keep");
            if (i >= PAD) {
                return Util.nCase(fields[i - PAD]);
            }
            return "";
        }

        public Object getColumnValue(BibtexEntry entry, int i) {
            if (i == 0)
                return entry.isSearchHit() ? Boolean.TRUE : Boolean.FALSE;
            else if (i < PAD) {
                Object o;
                switch (i) {
                case DUPL_COL:
                    return entry.isGroupHit() ? duplLabel : null;
                case FILE_COL:
                    o = entry.getField(GUIGlobals.FILE_FIELD);
                    if (o != null) {
                        FileListTableModel model = new FileListTableModel();
                        model.setContent((String) o);
                        fileLabel.setToolTipText(model.getToolTipHTMLRepresentation());
                        if (model.getRowCount() > 0)
                            fileLabel.setIcon(model.getEntry(0).getType().getIcon());
                        return fileLabel;
                    } else
                        return null;
                case PDF_COL:
                    o = entry.getField("pdf");
                    if (o != null) {
                        pdfLabel.setToolTipText((String) o);
                        return pdfLabel;
                    } else
                        return null;

                case PS_COL:
                    o = entry.getField("ps");
                    if (o != null) {
                        psLabel.setToolTipText((String) o);
                        return psLabel;
                    } else
                        return null;
                case URL_COL:
                    o = entry.getField("url");
                    if (o != null) {
                        urlLabel.setToolTipText((String) o);
                        return urlLabel;
                    } else
                        return null;
                default:
                    return null;
                }
            } else {
                String field = fields[i - PAD];
                if (field.equals("author") || field.equals("editor")) {
                    String contents = entry.getField(field);
                    return (contents != null) ? AuthorList.fixAuthor_Natbib(contents) : "";
                } else
                    return entry.getField(field);
            }
        }

    }

    public void toFront() {
        super.toFront();
    }

    public void setStatus(String s) {
        frame.setStatus(s);
    }

    public void showMessage(Object message, String title, int msgType) {
        JOptionPane.showMessageDialog(this, message, title, msgType);
    }

    public void showMessage(String message) {
        JOptionPane.showMessageDialog(this, message);
    }
}
