/*  Copyright (C) 2003-2015 JabRef contributors.
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
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import javax.swing.undo.AbstractUndoableEdit;

import net.sf.jabref.gui.renderer.GeneralRenderer;
import net.sf.jabref.model.entry.AuthorList;
import net.sf.jabref.model.database.BibtexDatabase;
import net.sf.jabref.model.entry.BibtexEntry;
import net.sf.jabref.bibtex.DuplicateCheck;
import net.sf.jabref.bibtex.comparator.FieldComparator;
import net.sf.jabref.Globals;
import net.sf.jabref.model.entry.EntryUtil;
import net.sf.jabref.model.entry.IdGenerator;
import net.sf.jabref.JabRefExecutorService;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.MetaData;
import net.sf.jabref.importer.OutputPrinter;
import net.sf.jabref.external.DownloadExternalFile;
import net.sf.jabref.external.ExternalFileMenuItem;
import net.sf.jabref.groups.structure.AbstractGroup;
import net.sf.jabref.groups.structure.AllEntriesGroup;
import net.sf.jabref.groups.GroupTreeNode;
import net.sf.jabref.groups.UndoableChangeAssignment;
import net.sf.jabref.gui.help.HelpAction;
import net.sf.jabref.gui.keyboard.KeyBinds;
import net.sf.jabref.importer.ImportInspector;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.labelPattern.LabelPatternUtil;
import net.sf.jabref.gui.undo.NamedCompound;
import net.sf.jabref.gui.undo.UndoableInsertEntry;
import net.sf.jabref.gui.undo.UndoableRemoveEntry;
import net.sf.jabref.gui.util.IconComparator;
import net.sf.jabref.gui.util.PositionWindow;
import net.sf.jabref.gui.desktop.JabRefDesktop;
import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;
import ca.odell.glazedlists.gui.AbstractTableComparatorChooser;
import ca.odell.glazedlists.gui.TableFormat;
import ca.odell.glazedlists.swing.DefaultEventSelectionModel;
import ca.odell.glazedlists.swing.DefaultEventTableModel;
import ca.odell.glazedlists.swing.GlazedListsSwing;
import ca.odell.glazedlists.swing.TableComparatorChooser;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.ButtonStackBuilder;

/**
 * Dialog to allow the selection of entries as part of an Import.
 * <p>
 * The usual way to use this class is to pass it to an Importer which will do
 * the following:
 * <ul>
 * <li>Register itself as a callback to get notified if the user wants to stop
 * the import.</li>
 * <li>Call setVisible(true) to display the dialog</li>
 * <li>For each entry that has been found call addEntry(...)</li>
 * <li>Call entryListComplete() after all entries have been fetched</li>
 * </ul>
 * <p>
 * If the importer wants to cancel the import, it should call the dispose()
 * method.
 * <p>
 * If the importer receives the stopFetching-call, it should stop fetching as
 * soon as possible (it is not really critical, but good style to not contribute
 * any more results via addEntry, call entryListComplete() or dispose(), after
 * receiving this call).
 *
 * @author alver
 */
public class ImportInspectionDialog extends JDialog implements ImportInspector, OutputPrinter {

    public interface CallBack {

        /**
         * This method is called by the dialog when the user has cancelled or
         * signalled a stop. It is expected that any long-running fetch
         * operations will stop after this method is called.
         */
        void stopFetching();
    }


    protected ImportInspectionDialog ths = this;

    private BasePanel panel;

    private final JabRefFrame frame;

    private final MetaData metaData;

    private final JSplitPane contentPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

    private final JTable glTable;

    private final TableComparatorChooser<BibtexEntry> comparatorChooser;

    private final DefaultEventSelectionModel<BibtexEntry> selectionModel;

    private final String[] fields;

    private final JProgressBar progressBar = new JProgressBar(SwingConstants.HORIZONTAL);

    private final JButton ok = new JButton(Localization.lang("Ok"));
    private final JButton generate = new JButton(Localization.lang("Generate now"));

    private final EventList<BibtexEntry> entries = new BasicEventList<>();

    private final SortedList<BibtexEntry> sortedList;

    /**
     * Duplicate resolving may require deletion of old entries.
     */
    private final List<BibtexEntry> entriesToDelete = new ArrayList<>();

    private final String undoName;

    private final ArrayList<CallBack> callBacks = new ArrayList<>();

    private final boolean newDatabase;

    private final JPopupMenu popup = new JPopupMenu();

    private final JButton deselectAllDuplicates = new JButton(Localization.lang("Deselect all duplicates"));

    private final JButton stop = new JButton(Localization.lang("Stop"));

    private final PreviewPanel preview;

    private boolean generatedKeys; // Set to true after keys have
    // been

    // generated.

    private boolean defaultSelected = true;

    private final Rectangle toRect = new Rectangle(0, 0, 1, 1);

    private final Map<BibtexEntry, Set<GroupTreeNode>> groupAdditions = new HashMap<>();

    private final JCheckBox autoGenerate = new JCheckBox(Localization.lang("Generate keys"), Globals.prefs
            .getBoolean(JabRefPreferences.GENERATE_KEYS_AFTER_INSPECTION));

    private final JLabel duplLabel = new JLabel(IconTheme.JabRefIcon.DUPLICATE.getSmallIcon());
    private final JLabel fileLabel = new JLabel(IconTheme.JabRefIcon.FILE.getSmallIcon());
    private final JLabel urlLabel = new JLabel(IconTheme.JabRefIcon.WWW.getSmallIcon());

    private static final int DUPL_COL = 1;
    private static final int FILE_COL = 2;
    private static final int URL_COL = 3;
    private static final int PAD = 4;


    /**
     * The "defaultSelected" boolean value determines if new entries added are
     * selected for import or not. This value is true by default.
     *
     * @param defaultSelected The desired value.
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
        preview = new PreviewPanel(null, metaData, Globals.prefs.get(JabRefPreferences.PREVIEW_0));

        duplLabel.setToolTipText(Localization.lang("Possible duplicate of existing entry. Click to resolve."));

        sortedList = new SortedList<>(entries);
        DefaultEventTableModel<BibtexEntry> tableModelGl = (DefaultEventTableModel<BibtexEntry>) GlazedListsSwing
                .eventTableModelWithThreadProxyList(sortedList, new EntryTableFormat());
        glTable = new EntryTable(tableModelGl);
        GeneralRenderer renderer = new GeneralRenderer(Color.white);
        glTable.setDefaultRenderer(JLabel.class, renderer);
        glTable.setDefaultRenderer(String.class, renderer);
        glTable.getInputMap().put(Globals.prefs.getKey(KeyBinds.DELETE_ENTRY), "delete");
        DeleteListener deleteListener = new DeleteListener();
        glTable.getActionMap().put("delete", deleteListener);

        selectionModel = (DefaultEventSelectionModel<BibtexEntry>) GlazedListsSwing
                .eventSelectionModelWithThreadProxyList(sortedList);
        glTable.setSelectionModel(selectionModel);
        selectionModel.getSelected().addListEventListener(new EntrySelectionListener());
        comparatorChooser = TableComparatorChooser.install(glTable, sortedList,
                AbstractTableComparatorChooser.MULTIPLE_COLUMN_KEYBOARD);
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
            JMenu groupsAdd = new JMenu(Localization.lang("Add to group"));
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
        JButton cancel = new JButton(
                Localization.lang("Cancel"));
        bb.addButton(cancel);
        bb.addRelatedGap();
        JButton help = new HelpAction(frame.helpDiag, GUIGlobals.importInspectionHelp).getIconButton();
        bb.addButton(help);
        bb.addGlue();
        bb.getPanel().setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

        ButtonStackBuilder builder = new ButtonStackBuilder();
        JButton selectAll = new JButton(Localization.lang("Select all"));
        builder.addButton(selectAll);
        JButton deselectAll = new JButton(Localization.lang("Deselect all"));
        builder.addButton(deselectAll);
        builder.addButton(deselectAllDuplicates);
        builder.addRelatedGap();
        JButton delete = new JButton(Localization.lang("Delete"));
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
        getContentPane().add(bb.getPanel(), BorderLayout.SOUTH);

        // Remember and default to last size:
        setSize(new Dimension(Globals.prefs.getInt(JabRefPreferences.IMPORT_INSPECTION_DIALOG_WIDTH), Globals.prefs
                .getInt(JabRefPreferences.IMPORT_INSPECTION_DIALOG_HEIGHT)));
        addWindowListener(new WindowAdapter() {

            @Override
            public void windowOpened(WindowEvent e) {
                contentPane.setDividerLocation(0.5f);
            }

            @Override
            public void windowClosed(WindowEvent e) {
                Globals.prefs.putInt(JabRefPreferences.IMPORT_INSPECTION_DIALOG_WIDTH, getSize().width);
                Globals.prefs.putInt(JabRefPreferences.IMPORT_INSPECTION_DIALOG_HEIGHT, getSize().height);
            }
        });
        // Key bindings:
        AbstractAction closeAction = new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        };
        ActionMap am = contentPane.getActionMap();
        InputMap im = contentPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        im.put(Globals.prefs.getKey(KeyBinds.CLOSE_DIALOG), "close");
        am.put("close", closeAction);

    }

    /* (non-Javadoc)
     * @see net.sf.jabref.gui.ImportInspection#setProgress(int, int)
     */
    @Override
    public void setProgress(int current, int max) {
        progressBar.setIndeterminate(false);
        progressBar.setMinimum(0);
        progressBar.setMaximum(max);
        progressBar.setValue(current);
    }

    /* (non-Javadoc)
     * @see net.sf.jabref.gui.ImportInspection#addEntry(net.sf.jabref.BibtexEntry)
     */
    @Override
    public void addEntry(BibtexEntry entry) {
        List<BibtexEntry> list = new ArrayList<>();
        list.add(entry);
        addEntries(list);
    }

    /* (non-Javadoc)
     * @see net.sf.jabref.gui.ImportInspection#addEntries(java.util.Collection)
     */
    public void addEntries(Collection<BibtexEntry> entriesToAdd) {

        for (BibtexEntry entry : entriesToAdd) {
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
     * @param entries A Collection of BibtexEntry instances.
     * @param entry   The entry to search for duplicates of.
     * @return A possible duplicate, if any, or null if none were found.
     */
    private static BibtexEntry internalDuplicate(Collection<BibtexEntry> entriesDupe, BibtexEntry entry) {
        for (BibtexEntry othEntry : entriesDupe) {
            if (othEntry == entry) {
                continue; // Don't compare the entry to itself
            }
            if (DuplicateCheck.isDuplicate(entry, othEntry)) {
                return othEntry;
            }
        }
        return null;
    }

    /**
     * Removes all selected entries from the table. Synchronizes on this.entries
     * to prevent conflict with addition of new entries.
     */
    private void removeSelectedEntries() {
        int row = glTable.getSelectedRow();
        List<Object> toRemove = new ArrayList<>();
        toRemove.addAll(selectionModel.getSelected());
        entries.getReadWriteLock().writeLock().lock();
        for (Object o : toRemove) {
            entries.remove(o);
        }
        entries.getReadWriteLock().writeLock().unlock();
        glTable.clearSelection();
        if ((row >= 0) && (!entries.isEmpty())) {
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
        if (!generatedKeys) {
            generate.setEnabled(true);
        }
        stop.setEnabled(false);

        //This is for selecting and displaying the first entry in the glTable
        this.glTable.repaint();
        if (this.glTable.getSelectedRowCount() == 0) {
            if (this.glTable.getRowCount() > 0) {
                this.glTable.setRowSelectionInterval(0, 0); //Select first row in the table
            }
        }
    }

    /**
     * This method returns a List containing all entries that are selected
     * (checkbox checked).
     *
     * @return a List containing the selected entries.
     */
    private List<BibtexEntry> getSelectedEntries() {
        List<BibtexEntry> selected = new ArrayList<>();
        for (BibtexEntry entry : entries) {
            if (entry.isSearchHit()) {
                selected.add(entry);
            }
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
    private void generateKeySelectedEntry() {
        if (selectionModel.getSelected().size() != 1) {
            return;
        }
        BibtexEntry entry = selectionModel.getSelected().get(0);
        entries.getReadWriteLock().writeLock().lock();

        BibtexDatabase database;
        MetaData localMetaData;

        // Relate to existing database, if any:
        if (panel != null) {
            database = panel.database();
            localMetaData = panel.metaData();
        } else {
            database = new BibtexDatabase();
            localMetaData = new MetaData();
        }

        entry.setId(IdGenerator.next());
        // Add the entry to the database we are working with:
        database.insertEntry(entry);

        // Generate a unique key:
        LabelPatternUtil.makeLabel(localMetaData, database, entry);
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
    private void generateKeys() {
        entries.getReadWriteLock().writeLock().lock();

        BibtexDatabase database;
        MetaData localMetaData;

        // Relate to existing database, if any:
        if (panel != null) {
            database = panel.database();
            localMetaData = panel.metaData();
        } else {
            database = new BibtexDatabase();
            localMetaData = new MetaData();
        }

        List<String> keys = new ArrayList<>(entries.size());
        // Iterate over the entries, add them to the database we are working
        // with,
        // and generate unique keys:
        for (BibtexEntry entry : entries) {

            entry.setId(IdGenerator.next());
            database.insertEntry(entry);

            LabelPatternUtil.makeLabel(localMetaData, database, entry);
            // Add the generated key to our list:
            keys.add(entry.getCiteKey());
        }
        // Remove the entries from the database again, since they are not
        // supposed to
        // added yet. They only needed to be in it while we generated the keys,
        // to keep
        // control over key uniqueness.
        for (BibtexEntry entry : entries) {
            database.removeEntry(entry.getId());
        }
        entries.getReadWriteLock().writeLock().lock();
        glTable.repaint();
    }

    private void insertNodes(JMenu menu, GroupTreeNode node) {
        final AbstractAction action = getAction(node);

        if (node.getChildCount() == 0) {
            menu.add(action);
            if (action.isEnabled()) {
                menu.setEnabled(true);
            }
            return;
        }

        JMenu submenu;
        if (node.getGroup() instanceof AllEntriesGroup) {
            for (int i = 0; i < node.getChildCount(); ++i) {
                insertNodes(menu, (GroupTreeNode) node.getChildAt(i));
            }
        } else {
            submenu = new JMenu('[' + node.getGroup().getName() + ']');
            // setEnabled(true) is done above/below if at least one menu
            // entry (item or submenu) is enabled
            submenu.setEnabled(action.isEnabled());
            submenu.add(action);
            submenu.add(new JPopupMenu.Separator());
            for (int i = 0; i < node.getChildCount(); ++i) {
                insertNodes(submenu, (GroupTreeNode) node.getChildAt(i));
            }
            menu.add(submenu);
            if (submenu.isEnabled()) {
                menu.setEnabled(true);
            }
        }
    }

    private AbstractAction getAction(GroupTreeNode node) {
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

        final GroupTreeNode node;


        public AddToGroupAction(GroupTreeNode node) {
            super(node.getGroup().getName());
            this.node = node;
        }

        @Override
        public void actionPerformed(ActionEvent event) {

            selectionModel.getSelected().getReadWriteLock().writeLock().lock();
            for (BibtexEntry entry : selectionModel.getSelected()) {
                // We store the groups this entry should be added to in a Set in
                // the Map:
                Set<GroupTreeNode> groups = groupAdditions.get(entry);
                if (groups == null) {
                    // No previous definitions, so we create the Set now:
                    groups = new HashSet<>();
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


    private class OkListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent event) {

            // First check if we are supposed to warn about duplicates. If so,
            // see if there
            // are unresolved duplicates, and warn if yes.
            if (Globals.prefs.getBoolean(JabRefPreferences.WARN_ABOUT_DUPLICATES_IN_INSPECTION)) {
                for (BibtexEntry entry : entries) {

                    // Only check entries that are to be imported. Keep status
                    // is indicated
                    // through the search hit status of the entry:
                    if (!entry.isSearchHit()) {
                        continue;
                    }

                    // Check if the entry is a suspected, unresolved, duplicate.
                    // This status
                    // is indicated by the entry's group hit status:
                    if (entry.isGroupHit()) {
                        CheckBoxMessage cbm = new CheckBoxMessage(
                                Localization.lang("There are possible duplicates (marked with a 'D' icon) that haven't been resolved. Continue?"),
                                Localization.lang("Disable this confirmation dialog"), false);
                        int answer = JOptionPane.showConfirmDialog(ImportInspectionDialog.this,
                                cbm, Localization.lang("Duplicates found"), JOptionPane.YES_NO_OPTION);
                        if (cbm.isSelected()) {
                            Globals.prefs.putBoolean(JabRefPreferences.WARN_ABOUT_DUPLICATES_IN_INSPECTION, false);
                        }
                        if (answer == JOptionPane.NO_OPTION) {
                            return;
                        }
                        break;
                    }
                }
            }

            // The compund undo action used to contain all changes made by this
            // dialog.
            NamedCompound ce = new NamedCompound(undoName);

            // See if we should remove any old entries for duplicate resolving:
            if (!entriesToDelete.isEmpty()) {
                for (BibtexEntry entry : entriesToDelete) {
                    ce.addEdit(new UndoableRemoveEntry(panel.database(), entry, panel));
                    panel.database().removeEntry(entry.getId());
                }
            }

            // If "Generate keys" is checked, generate keys unless it's already
            // been done:
            if (autoGenerate.isSelected() && !generatedKeys) {
                generateKeys();
            }
            // Remember the choice until next time:
            Globals.prefs.putBoolean(JabRefPreferences.GENERATE_KEYS_AFTER_INSPECTION, autoGenerate.isSelected());

            final List<BibtexEntry> selected = getSelectedEntries();

            if (!selected.isEmpty()) {

                if (newDatabase) {
                    // Create a new BasePanel for the entries:
                    BibtexDatabase base = new BibtexDatabase();
                    panel = new BasePanel(frame, base, null, new MetaData(), Globals.prefs.getDefaultEncoding());
                }

                boolean groupingCanceled = false;

                // Set owner/timestamp if options are enabled:
                net.sf.jabref.util.Util.setAutomaticFields(selected, Globals.prefs.getBoolean(JabRefPreferences.OVERWRITE_OWNER),
                        Globals.prefs.getBoolean(JabRefPreferences.OVERWRITE_TIME_STAMP), Globals.prefs.getBoolean(JabRefPreferences.MARK_IMPORTED_ENTRIES));

                // Check if we should unmark entries before adding the new ones:
                if (Globals.prefs.getBoolean(JabRefPreferences.UNMARK_ALL_ENTRIES_BEFORE_IMPORTING)) {
                    for (BibtexEntry entry : panel.database().getEntries()) {
                        EntryMarker.unmarkEntry(entry, true, panel.database(), ce);
                    }
                }

                for (BibtexEntry entry : selected) {
                    // entry.clone();

                    // Remove settings to group/search hit status:
                    entry.setSearchHit(false);
                    entry.setGroupHit(false);

                    // If this entry should be added to any groups, do it now:
                    Set<GroupTreeNode> groups = groupAdditions.get(entry);
                    if (!groupingCanceled && (groups != null)) {
                        if (entry.getCiteKey() == null) {
                            // The entry has no key, so it can't be added to the
                            // group.
                            // The best course of action is probably to ask the
                            // user if a key should be generated
                            // immediately.
                            int answer = JOptionPane
                                    .showConfirmDialog(
                                            ImportInspectionDialog.this,
                                            Localization.lang("Cannot add entries to group without generating keys. Generate keys now?"),
                                            Localization.lang("Add to group"), JOptionPane.YES_NO_OPTION);
                            if (answer == JOptionPane.YES_OPTION) {
                                generateKeys();
                            } else {
                                groupingCanceled = true;
                            }
                        }

                        // If the key existed, or exists now, go ahead:
                        if (entry.getCiteKey() != null) {
                            for (GroupTreeNode node : groups) {
                                if (node.getGroup().supportsAdd()) {
                                    // Add the entry:
                                    AbstractUndoableEdit undo = node.getGroup().add(
                                            new BibtexEntry[]{entry});
                                    if (undo instanceof UndoableChangeAssignment) {
                                        ((UndoableChangeAssignment) undo).setEditedNode(node);
                                    }
                                    ce.addEdit(undo);

                                } else {
                                    // Shouldn't happen...
                                }
                            }
                        }
                    }

                    entry.setId(IdGenerator.next());
                    panel.database().insertEntry(entry);
                    ce.addEdit(new UndoableInsertEntry(panel.database(), entry, panel));

                }

                ce.end();
                panel.undoManager.addEdit(ce);
            }

            dispose();
            SwingUtilities.invokeLater(new Runnable() {

                @Override
                public void run() {
                    if (newDatabase) {
                        frame.addTab(panel, null, true);
                    }
                    panel.markBaseChanged();

                    if (!selected.isEmpty()) {
                        frame.output(Localization.lang("Number of entries successfully imported") +
                                ": " + selected.size());
                    } else {
                        frame.output(Localization.lang("No entries imported."));
                    }
                }
            });
        }
    }


    private void signalStopFetching() {
        for (CallBack c : callBacks) {
            c.stopFetching();
        }
    }

    private void setWidths() {
        TableColumnModel cm = glTable.getColumnModel();
        cm.getColumn(0).setPreferredWidth(55);
        cm.getColumn(0).setMinWidth(55);
        cm.getColumn(0).setMaxWidth(55);
        for (int i = 1; i < PAD; i++) {
            // Lock the width of icon columns.
            cm.getColumn(i).setPreferredWidth(GUIGlobals.WIDTH_ICON_COL);
            cm.getColumn(i).setMinWidth(GUIGlobals.WIDTH_ICON_COL);
            cm.getColumn(i).setMaxWidth(GUIGlobals.WIDTH_ICON_COL);
        }

        for (int i = 0; i < fields.length; i++) {
            int width = BibtexFields.getFieldLength(fields[i]);
            glTable.getColumnModel().getColumn(i + PAD).setPreferredWidth(width);
        }
    }


    private class StopListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent event) {
            signalStopFetching();
            entryListComplete();
        }
    }

    private class CancelListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent event) {
            signalStopFetching();
            dispose();
            frame.output(Localization.lang("Import canceled by user"));
        }
    }

    private class GenerateListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent event) {
            generate.setEnabled(false);
            generatedKeys = true; // To prevent the button from getting
            // enabled again.
            generateKeys(); // Generate the keys.
        }
    }

    class DeleteListener extends AbstractAction {

        public DeleteListener() {
            super(Localization.lang("Delete"), IconTheme.JabRefIcon.DELETE_ENTRY.getSmallIcon());
        }

        @Override
        public void actionPerformed(ActionEvent event) {
            removeSelectedEntries();
        }
    }

    class SelectionButton implements ActionListener {

        final Boolean enable;


        public SelectionButton(boolean enable) {
            this.enable = enable;
        }

        @Override
        public void actionPerformed(ActionEvent event) {
            for (int i = 0; i < glTable.getRowCount(); i++) {
                glTable.setValueAt(enable, i, 0);
            }
            glTable.repaint();
        }
    }

    private class DeselectDuplicatesButtonListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent event) {
            for (int i = 0; i < glTable.getRowCount(); i++) {
                if (glTable.getValueAt(i, DUPL_COL) != null) {
                    glTable.setValueAt(false, i, 0);
                }
            }
            glTable.repaint();
        }
    }

    private class EntrySelectionListener implements ListEventListener<BibtexEntry> {

        @Override
        public void listChanged(ListEvent<BibtexEntry> listEvent) {
            if (listEvent.getSourceList().size() == 1) {
                preview.setEntry(listEvent.getSourceList().get(0));
                contentPane.setDividerLocation(0.5f);
                SwingUtilities.invokeLater(new Runnable() {

                    @Override
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
            return (col == FILE_COL) || (col == URL_COL);
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            final int col = glTable.columnAtPoint(e.getPoint());
            final int row = glTable.rowAtPoint(e
                    .getPoint());
            if (isIconColumn(col)) {
                BibtexEntry entry = sortedList.get(row);

                switch (col) {
                    case FILE_COL:
                        Object o = entry.getField(Globals.FILE_FIELD);
                        if (o != null) {
                            FileListTableModel tableModel = new FileListTableModel();
                            tableModel.setContent((String) o);
                            if (tableModel.getRowCount() == 0) {
                                return;
                            }
                            FileListEntry fl = tableModel.getEntry(0);
                            (new ExternalFileMenuItem(frame, entry, "", fl.getLink(), null, panel
                                    .metaData(), fl.getType())).actionPerformed(null);
                        }
                        break;
                    case URL_COL:
                        openExternalLink("url", e);
                        break;
                }
            }
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            // Nothing
        }

        @Override
        public void mouseExited(MouseEvent e) {
            // Nothing
        }

        /**
         * Show right-click menu. If the click happened in an icon column that
         * presents its own popup menu, show that. Otherwise, show the ordinary
         * popup menu.
         *
         * @param e The mouse event that triggered the popup.
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
         * @param e The mouse event that triggered the popup.
         */
        public void showFileFieldMenu(MouseEvent e) {
            final int row = glTable.rowAtPoint(e.getPoint());
            BibtexEntry entry = sortedList.get(row);
            JPopupMenu menu = new JPopupMenu();
            int count = 0;
            Object o = entry.getField(Globals.FILE_FIELD);
            FileListTableModel fileList = new FileListTableModel();
            fileList.setContent((String) o);
            // If there are one or more links, open the first one:
            for (int i = 0; i < fileList.getRowCount(); i++) {
                FileListEntry flEntry = fileList.getEntry(i);
                String description = flEntry.getDescription();
                if ((description == null) || (description.trim().isEmpty())) {
                    description = flEntry.getLink();
                }
                menu.add(new ExternalFileMenuItem(panel.frame(), entry, description, flEntry
                        .getLink(), flEntry.getType().getIcon(), panel.metaData(), flEntry.getType()));
                count++;
            }
            if (count == 0) {
                showOrdinaryRightClickMenu(e);
            } else {
                menu.show(glTable, e.getX(), e.getY());
            }
        }

        /**
         * Open old-style external links after user clicks icon.
         *
         * @param fieldName The name of the BibTeX field this icon is used for.
         * @param e         The MouseEvent that triggered this operation.
         */
        public void openExternalLink(String fieldName, MouseEvent e) {
            final int row = glTable.rowAtPoint(e.getPoint());
            BibtexEntry entry = sortedList.get(row);

            Object link = entry.getField(fieldName);
            try {
                if (link != null) {
                    JabRefDesktop.openExternalViewer(panel.metaData(), (String) link, fieldName);
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            // Check if the user has right-clicked. If so, open the right-click
            // menu.
            if (e.isPopupTrigger()) {
                showPopup(e);
            }
        }

        @Override
        public void mousePressed(MouseEvent e) {
            // Check if the user has right-clicked. If so, open the right-click
            // menu.
            if (e.isPopupTrigger()) {
                showPopup(e);
                return;
            }

            // Check if any other action should be taken:
            final int col = glTable.columnAtPoint(e.getPoint());
            final int row = glTable.rowAtPoint(e.getPoint());
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
                    PositionWindow.placeDialog(diag, ImportInspectionDialog.this);
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
                    } else if (diag.getSelected() == DuplicateResolverDialog.KEEP_MERGE) {
                        // Remove old entry. Or... add it to a list of entries
                        // to be deleted. We only delete
                        // it after Ok is clicked.
                        entriesToDelete.add(other);
                        // Store merged entry for later adding
                        // Clear duplicate icon, which is controlled by the
                        // group hit
                        // field of the entry:
                        entries.getReadWriteLock().writeLock().lock();
                        diag.getMergedEntry().setGroupHit(false);
                        diag.getMergedEntry().setSearchHit(true);
                        entries.add(diag.getMergedEntry());
                        entries.remove(first);
                        first = new BibtexEntry(); // Reset first so the next duplicate doesn't trigger
                        entries.getReadWriteLock().writeLock().unlock();
                    }
                }
                // Check if the duplicate is of another entry in the import:
                other = internalDuplicate(entries, first);
                if (other != null) {
                    DuplicateResolverDialog diag = new DuplicateResolverDialog(
                            ImportInspectionDialog.this, first, other, DuplicateResolverDialog.DUPLICATE_SEARCH);
                    PositionWindow.placeDialog(diag, ImportInspectionDialog.this);
                    diag.setVisible(true);
                    ImportInspectionDialog.this.toFront();
                    int answer = diag.getSelected();
                    if (answer == DuplicateResolverDialog.KEEP_UPPER) {
                        entries.remove(other);
                        first.setGroupHit(false);
                    } else if (answer == DuplicateResolverDialog.KEEP_LOWER) {
                        entries.remove(first);
                    } else if (answer == DuplicateResolverDialog.KEEP_BOTH) {
                        first.setGroupHit(false);
                    } else if (answer == DuplicateResolverDialog.KEEP_MERGE) {
                        diag.getMergedEntry().setGroupHit(false);
                        diag.getMergedEntry().setSearchHit(true);
                        entries.add(diag.getMergedEntry());
                        entries.remove(first);
                        entries.remove(other);
                    }
                }
            }
        }
    }

    class AttachUrl extends JMenuItem implements ActionListener {

        public AttachUrl() {
            super(Localization.lang("Attach URL"));
            addActionListener(this);
        }

        @Override
        public void actionPerformed(ActionEvent event) {
            if (selectionModel.getSelected().size() != 1) {
                return;
            }
            BibtexEntry entry = selectionModel.getSelected().get(0);
            String result = JOptionPane.showInputDialog(ImportInspectionDialog.this,
                    Localization.lang("Enter URL"), entry.getField("url"));
            entries.getReadWriteLock().writeLock().lock();
            if (result != null) {
                if (result.isEmpty()) {
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

        BibtexEntry entry;


        public DownloadFile() {
            super(Localization.lang("Download file"));
            addActionListener(this);
        }

        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            if (selectionModel.getSelected().size() != 1) {
                return;
            }
            entry = selectionModel.getSelected().get(0);
            String bibtexKey = entry.getCiteKey();
            if (bibtexKey == null) {
                int answer = JOptionPane.showConfirmDialog(frame,
                        Localization.lang("This entry has no BibTeX key. Generate key now?"),
                        Localization.lang("Download file"), JOptionPane.OK_CANCEL_OPTION,
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

        @Override
        public void downloadComplete(FileListEntry file) {
            ImportInspectionDialog.this.toFront(); // Hack
            FileListTableModel localModel = new FileListTableModel();
            String oldVal = entry.getField(Globals.FILE_FIELD);
            if (oldVal != null) {
                localModel.setContent(oldVal);
            }
            localModel.addEntry(localModel.getRowCount(), file);
            entries.getReadWriteLock().writeLock().lock();
            entry.setField(Globals.FILE_FIELD, localModel.getStringRepresentation());
            entries.getReadWriteLock().writeLock().unlock();
            glTable.repaint();
        }
    }

    class AutoSetLinks extends JMenuItem implements ActionListener {

        public AutoSetLinks() {
            super(Localization.lang("Autoset external links"));
            addActionListener(this);
        }

        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            if (selectionModel.getSelected().size() != 1) {
                return;
            }
            final BibtexEntry entry = selectionModel.getSelected().get(0);
            if (entry.getCiteKey() == null) {
                int answer = JOptionPane.showConfirmDialog(frame,
                        Localization.lang("This entry has no BibTeX key. Generate key now?"),
                        Localization.lang("Download file"), JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.QUESTION_MESSAGE);
                if (answer == JOptionPane.OK_OPTION) {
                    generateKeySelectedEntry();
                } else {
                    return; // Can't go on without the bibtex key.
                }
            }
            final FileListTableModel localModel = new FileListTableModel();
            String oldVal = entry.getField(Globals.FILE_FIELD);
            if (oldVal != null) {
                localModel.setContent(oldVal);
            }
            // We have a static utility method for searching for all relevant
            // links:
            JDialog diag = new JDialog(ImportInspectionDialog.this, true);
            JabRefExecutorService.INSTANCE.execute(net.sf.jabref.util.Util.autoSetLinks(entry, localModel, metaData, new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    if (e.getID() > 0) {
                        entries.getReadWriteLock().writeLock().lock();
                        entry.setField(Globals.FILE_FIELD, localModel.getStringRepresentation());
                        entries.getReadWriteLock().writeLock().unlock();
                        glTable.repaint();
                    }
                }
            }, diag));

        }
    }

    class LinkLocalFile extends JMenuItem implements ActionListener,
            DownloadExternalFile.DownloadCallback {

        BibtexEntry entry;


        public LinkLocalFile() {
            super(Localization.lang("Link local file"));
            addActionListener(this);
        }

        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            if (selectionModel.getSelected().size() != 1) {
                return;
            }
            entry = selectionModel.getSelected().get(0);
            FileListEntry flEntry = new FileListEntry("", "", null);
            FileListEntryEditor editor = new FileListEntryEditor(frame, flEntry, false, true,
                    metaData);
            editor.setVisible(true, true);
            if (editor.okPressed()) {
                FileListTableModel localModel = new FileListTableModel();
                String oldVal = entry.getField(Globals.FILE_FIELD);
                if (oldVal != null) {
                    localModel.setContent(oldVal);
                }
                localModel.addEntry(localModel.getRowCount(), flEntry);
                entries.getReadWriteLock().writeLock().lock();
                entry.setField(Globals.FILE_FIELD, localModel.getStringRepresentation());
                entries.getReadWriteLock().writeLock().unlock();
                glTable.repaint();
            }
        }

        @Override
        public void downloadComplete(FileListEntry file) {
            ImportInspectionDialog.this.toFront(); // Hack
            FileListTableModel localModel = new FileListTableModel();
            String oldVal = entry.getField(Globals.FILE_FIELD);
            if (oldVal != null) {
                localModel.setContent(oldVal);
            }
            localModel.addEntry(localModel.getRowCount(), file);
            entries.getReadWriteLock().writeLock().lock();
            entry.setField(Globals.FILE_FIELD, localModel.getStringRepresentation());
            entries.getReadWriteLock().writeLock().unlock();
            glTable.repaint();
        }
    }

    class AttachFile extends JMenuItem implements ActionListener {

        final String fileType;


        public AttachFile(String fileType) {
            super(Localization.lang("Attach %0 file", new String[]{fileType.toUpperCase()}));
            this.fileType = fileType;
            addActionListener(this);
        }

        @Override
        public void actionPerformed(ActionEvent event) {

            if (selectionModel.getSelected().size() != 1) {
                return;
            }
            BibtexEntry entry = selectionModel.getSelected().get(0);
            // Call up a dialog box that provides Browse, Download and auto
            // buttons:
            AttachFileDialog diag = new AttachFileDialog(ImportInspectionDialog.this, metaData,
                    entry, fileType);
            PositionWindow.placeDialog(diag, ImportInspectionDialog.this);
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


    private void setupComparatorChooser() {
        // First column:
        java.util.List<Comparator> comparators = comparatorChooser.getComparatorsForColumn(0);
        comparators.clear();

        comparators = comparatorChooser.getComparatorsForColumn(1);
        comparators.clear();

        // Icon columns:
        for (int i = 2; i < PAD; i++) {
            comparators = comparatorChooser.getComparatorsForColumn(i);
            comparators.clear();
            if (i == FILE_COL) {
                comparators.add(new IconComparator(new String[]{Globals.FILE_FIELD}));
            } else if (i == URL_COL) {
                comparators.add(new IconComparator(new String[]{"url"}));
            }

        }
        // Remaining columns:
        for (int i = PAD; i < (PAD + fields.length); i++) {
            comparators = comparatorChooser.getComparatorsForColumn(i);
            comparators.clear();
            comparators.add(new FieldComparator(fields[i - PAD]));
        }

        // Set initial sort columns:

        /*
         * // Default sort order: String[] sortFields = new String[]
         * {Globals.prefs.get(JabRefPreferences.PRIMARY_SORT_FIELD), Globals.prefs.get(JabRefPreferences.SECONDARY_SORT_FIELD),
         * Globals.prefs.get(JabRefPreferences.TERTIARY_SORT_FIELD)}; boolean[] sortDirections = new
         * boolean[] {Globals.prefs.getBoolean(JabRefPreferences.PRIMARY_SORT_DESCENDING),
         * Globals.prefs.getBoolean(JabRefPreferences.SECONDARY_SORT_DESCENDING),
         * Globals.prefs.getBoolean(JabRefPreferences.TERTIARY_SORT_DESCENDING)}; // descending
         */
        sortedList.getReadWriteLock().writeLock().lock();
        comparatorChooser.appendComparator(PAD, 0, false);
        sortedList.getReadWriteLock().writeLock().unlock();

    }


    class EntryTable extends JTable {

        final GeneralRenderer renderer = new GeneralRenderer(Color.white);


        public EntryTable(TableModel model) {
            super(model);
            getTableHeader().setReorderingAllowed(false);
        }

        @Override
        public TableCellRenderer getCellRenderer(int row, int column) {
            return column == 0 ? getDefaultRenderer(Boolean.class) : renderer;
        }

        /*
         * public TableCellEditor getCellEditor() { return
         * getDefaultEditor(Boolean.class); }
         */

        @Override
        public Class<?> getColumnClass(int col) {
            if (col == 0) {
                return Boolean.class;
            } else if (col < PAD) {
                return JLabel.class;
            } else {
                return String.class;
            }
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return column == 0;
        }

        @Override
        public void setValueAt(Object value, int row, int column) {
            // Only column 0, which is controlled by BibtexEntry.searchHit, is
            // editable:
            entries.getReadWriteLock().writeLock().lock();
            BibtexEntry entry = sortedList.get(row);
            entry.setSearchHit((Boolean) value);
            entries.getReadWriteLock().writeLock().unlock();
        }
    }

    private class EntryTableFormat implements TableFormat<BibtexEntry> {

        @Override
        public int getColumnCount() {
            return PAD + fields.length;
        }

        @Override
        public String getColumnName(int i) {
            if (i == 0) {
                return Localization.lang("Keep");
            }
            if (i >= PAD) {
                return EntryUtil.capitalizeFirst(fields[i - PAD]);
            }
            return "";
        }

        @Override
        public Object getColumnValue(BibtexEntry entry, int i) {
            if (i == 0) {
                return entry.isSearchHit() ? Boolean.TRUE : Boolean.FALSE;
            } else if (i < PAD) {
                Object o;
                switch (i) {
                    case DUPL_COL:
                        return entry.isGroupHit() ? duplLabel : null;
                    case FILE_COL:
                        o = entry.getField(Globals.FILE_FIELD);
                        if (o != null) {
                            FileListTableModel model = new FileListTableModel();
                            model.setContent((String) o);
                            fileLabel.setToolTipText(model.getToolTipHTMLRepresentation());
                            if (model.getRowCount() > 0) {
                                fileLabel.setIcon(model.getEntry(0).getType().getIcon());
                            }
                            return fileLabel;
                        } else {
                            return null;
                        }
                    case URL_COL:
                        o = entry.getField("url");
                        if (o != null) {
                            urlLabel.setToolTipText((String) o);
                            return urlLabel;
                        } else {
                            return null;
                        }
                    default:
                        return null;
                }
            } else {
                String field = fields[i - PAD];
                if ("author".equals(field) || "editor".equals(field)) {
                    String contents = entry.getField(field);
                    return (contents != null) ? AuthorList.fixAuthor_Natbib(contents) : "";
                } else {
                    return entry.getField(field);
                }
            }
        }

    }


    @Override
    public void toFront() {
        super.toFront();
    }

    @Override
    public void setStatus(String s) {
        frame.setStatus(s);
    }

    @Override
    public void showMessage(Object message, String title, int msgType) {
        JOptionPane.showMessageDialog(this, message, title, msgType);
    }

    @Override
    public void showMessage(String message) {
        JOptionPane.showMessageDialog(this, message);
    }
}
