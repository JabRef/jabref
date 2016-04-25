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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
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

import net.sf.jabref.BibDatabaseContext;
import net.sf.jabref.Defaults;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefExecutorService;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.MetaData;
import net.sf.jabref.bibtex.FieldProperties;
import net.sf.jabref.bibtex.InternalBibtexFields;
import net.sf.jabref.bibtex.comparator.FieldComparator;
import net.sf.jabref.external.DownloadExternalFile;
import net.sf.jabref.external.ExternalFileMenuItem;
import net.sf.jabref.gui.DuplicateResolverDialog.DuplicateResolverResult;
import net.sf.jabref.gui.desktop.JabRefDesktop;
import net.sf.jabref.gui.groups.GroupTreeNodeViewModel;
import net.sf.jabref.gui.groups.UndoableChangeEntriesOfGroup;
import net.sf.jabref.gui.help.HelpAction;
import net.sf.jabref.gui.help.HelpFiles;
import net.sf.jabref.gui.keyboard.KeyBinding;
import net.sf.jabref.gui.renderer.GeneralRenderer;
import net.sf.jabref.gui.undo.NamedCompound;
import net.sf.jabref.gui.undo.UndoableInsertEntry;
import net.sf.jabref.gui.undo.UndoableRemoveEntry;
import net.sf.jabref.gui.util.comparator.IconComparator;
import net.sf.jabref.gui.util.component.CheckBoxMessage;
import net.sf.jabref.importer.ImportInspector;
import net.sf.jabref.importer.OutputPrinter;
import net.sf.jabref.logic.groups.AllEntriesGroup;
import net.sf.jabref.logic.groups.EntriesGroupChange;
import net.sf.jabref.logic.groups.GroupTreeNode;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.labelpattern.LabelPatternUtil;
import net.sf.jabref.logic.util.UpdateField;
import net.sf.jabref.model.DuplicateCheck;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.database.BibDatabaseMode;
import net.sf.jabref.model.entry.AuthorList;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.EntryUtil;
import net.sf.jabref.model.entry.IdGenerator;

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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
 */
public class ImportInspectionDialog extends JDialog implements ImportInspector, OutputPrinter {
    private static final Log LOGGER = LogFactory.getLog(ImportInspectionDialog.class);

    private BasePanel panel;

    public final JabRefFrame frame;

    private final BibDatabaseContext bibDatabaseContext;

    private final JSplitPane contentPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

    private final JTable glTable;

    private final TableComparatorChooser<BibEntry> comparatorChooser;

    private final DefaultEventSelectionModel<BibEntry> selectionModel;

    private final JProgressBar progressBar = new JProgressBar(SwingConstants.HORIZONTAL);

    private final JButton ok = new JButton(Localization.lang("OK"));
    private final JButton generate = new JButton(Localization.lang("Generate now"));

    private final EventList<BibEntry> entries = new BasicEventList<>();

    private final SortedList<BibEntry> sortedList;

    /**
     * Duplicate resolving may require deletion of old entries.
     */
    private final List<BibEntry> entriesToDelete = new ArrayList<>();

    private final String undoName;

    private final List<CallBack> callBacks = new ArrayList<>();

    private final boolean newDatabase;

    private final JPopupMenu popup = new JPopupMenu();

    private final JButton deselectAllDuplicates = new JButton(Localization.lang("Deselect all duplicates"));

    private final JButton stop = new JButton(Localization.lang("Stop"));

    private final PreviewPanel preview;

    private boolean generatedKeys; // Set to true after keys have been generated.

    private boolean defaultSelected = true;

    private final Rectangle toRect = new Rectangle(0, 0, 1, 1);

    private final Map<BibEntry, Set<GroupTreeNode>> groupAdditions = new HashMap<>();

    private final JCheckBox autoGenerate = new JCheckBox(Localization.lang("Generate keys"),
            Globals.prefs.getBoolean(JabRefPreferences.GENERATE_KEYS_AFTER_INSPECTION));

    private final JLabel duplLabel = new JLabel(IconTheme.JabRefIcon.DUPLICATE.getSmallIcon());
    private final JLabel fileLabel = new JLabel(IconTheme.JabRefIcon.FILE.getSmallIcon());
    private final JLabel urlLabel = new JLabel(IconTheme.JabRefIcon.WWW.getSmallIcon());

    private static final List<String> INSPECTION_FIELDS = Arrays.asList("author", "title", "year", BibEntry.KEY_FIELD);

    private static final int DUPL_COL = 1;
    private static final int FILE_COL = 2;
    private static final int URL_COL = 3;
    private static final int PAD = 4;

    private static final String URL_FIELD = "url";


    /**
     * Creates a dialog that displays the given list of fields in the table. The
     * dialog allows another process to add entries dynamically while the dialog
     * is shown.
     *
     * @param frame
     * @param panel
     */
    public ImportInspectionDialog(JabRefFrame frame, BasePanel panel, String undoName, boolean newDatabase) {
        this.frame = frame;
        this.panel = panel;
        this.bibDatabaseContext = (panel == null) ? null : panel.getBibDatabaseContext();
        this.undoName = undoName;
        this.newDatabase = newDatabase;
        setIconImage(new ImageIcon(IconTheme.getIconUrl("jabrefIcon48")).getImage());
        preview = new PreviewPanel(null, bibDatabaseContext, Globals.prefs.get(JabRefPreferences.PREVIEW_0));

        duplLabel.setToolTipText(Localization.lang("Possible duplicate of existing entry. Click to resolve."));

        sortedList = new SortedList<>(entries);
        DefaultEventTableModel<BibEntry> tableModelGl = (DefaultEventTableModel<BibEntry>) GlazedListsSwing
                .eventTableModelWithThreadProxyList(sortedList, new EntryTableFormat());
        glTable = new EntryTable(tableModelGl);
        GeneralRenderer renderer = new GeneralRenderer(Color.white);
        glTable.setDefaultRenderer(JLabel.class, renderer);
        glTable.setDefaultRenderer(String.class, renderer);
        glTable.getInputMap().put(Globals.getKeyPrefs().getKey(KeyBinding.DELETE_ENTRY), "delete");
        DeleteListener deleteListener = new DeleteListener();
        glTable.getActionMap().put("delete", deleteListener);

        selectionModel = (DefaultEventSelectionModel<BibEntry>) GlazedListsSwing
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
        if (!newDatabase && (bibDatabaseContext != null)) {
            GroupTreeNode node = bibDatabaseContext.getMetaData().getGroups();
            JMenu groupsAdd = new JMenu(Localization.lang("Add to group"));
            groupsAdd.setEnabled(false); // Will get enabled if there are groups that can be added to.
            insertNodes(groupsAdd, node);
            popup.add(groupsAdd);
        }

        // Add "Attach file" menu choices to right click menu:
        popup.add(new LinkLocalFile());
        popup.add(new DownloadFile());
        popup.add(new AutoSetLinks());
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
        JButton help = new HelpAction(HelpFiles.IMPORT_INSPECTION).getHelpButton();
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
        cancel.addActionListener(e -> {
            signalStopFetching();
            dispose();
            frame.output(Localization.lang("Import canceled by user"));
        });
        generate.addActionListener(e -> {
            generate.setEnabled(false);
            generatedKeys = true; // To prevent the button from getting
            // enabled again.
            generateKeys(); // Generate the keys.
        });
        stop.addActionListener(e -> {
            signalStopFetching();
            entryListComplete();
        });
        selectAll.addActionListener(new SelectionButton(true));
        deselectAll.addActionListener(new SelectionButton(false));
        deselectAllDuplicates.addActionListener(e -> {
            for (int i = 0; i < glTable.getRowCount(); i++) {
                if (glTable.getValueAt(i, DUPL_COL) != null) {
                    glTable.setValueAt(false, i, 0);
                }
            }
            glTable.repaint();
        });
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
        Action closeAction = new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        };
        ActionMap am = contentPane.getActionMap();
        InputMap im = contentPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        im.put(Globals.getKeyPrefs().getKey(KeyBinding.CLOSE_DIALOG), "close");
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
     * @see net.sf.jabref.gui.ImportInspection#addEntry(net.sf.jabref.BibEntry)
     */
    @Override
    public void addEntry(BibEntry entry) {
        List<BibEntry> list = new ArrayList<>();
        list.add(entry);
        addEntries(list);
    }

    /* (non-Javadoc)
     * @see net.sf.jabref.gui.ImportInspection#addEntries(java.util.Collection)
     */
    public void addEntries(Collection<BibEntry> entriesToAdd) {

        for (BibEntry entry : entriesToAdd) {
            // We exploit the entry's search status for indicating "Keep"
            // status:
            entry.setSearchHit(defaultSelected);
            // We exploit the entry's group status for indicating duplicate
            // status.
            // Checking duplicates means both checking against the background
            // database (if
            // applicable) and against entries already in the table.
            if ((panel != null) && (DuplicateCheck
                    .containsDuplicate(panel.getDatabase(), entry, panel.getBibDatabaseContext().getMode()).isPresent()
                    || (internalDuplicate(this.entries, entry).isPresent()))) {
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
     * @param entriesDupe A Collection of BibEntry instances.
     * @param entry       The entry to search for duplicates of.
     * @return A possible duplicate, if any, or null if none were found.
     */
    private Optional<BibEntry> internalDuplicate(Collection<BibEntry> entriesDupe, BibEntry entry) {
        for (BibEntry othEntry : entriesDupe) {
            if (othEntry.equals(entry)) {
                continue; // Don't compare the entry to itself
            }
            if (DuplicateCheck.isDuplicate(entry, othEntry, panel.getBibDatabaseContext().getMode())) {
                return Optional.of(othEntry);
            }
        }
        return Optional.empty();
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
        if ((this.glTable.getSelectedRowCount() == 0) && (this.glTable.getRowCount() > 0)) {
            this.glTable.setRowSelectionInterval(0, 0); //Select first row in the table
        }
    }


    /**
     * Generate key for the selected entry only.
     */
    private void generateKeySelectedEntry() {
        if (selectionModel.getSelected().size() != 1) {
            return;
        }
        BibEntry entry = selectionModel.getSelected().get(0);
        entries.getReadWriteLock().writeLock().lock();

        BibDatabase database;
        MetaData localMetaData;

        // Relate to existing database, if any:
        if (panel == null) {
            database = new BibDatabase();
            localMetaData = new MetaData();
        } else {
            database = panel.getDatabase();
            localMetaData = panel.getBibDatabaseContext().getMetaData();
        }

        entry.setId(IdGenerator.next());
        // Add the entry to the database we are working with:
        database.insertEntry(entry);

        // Generate a unique key:
        LabelPatternUtil.makeLabel(localMetaData, database, entry);
        // Remove the entry from the database again, since we only added it in
        // order to
        // make sure the key was unique:
        database.removeEntry(entry);

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

        BibDatabase database;
        MetaData localMetaData;

        // Relate to existing database, if any:
        if (panel == null) {
            database = new BibDatabase();
            localMetaData = new MetaData();
        } else {
            database = panel.getDatabase();
            localMetaData = panel.getBibDatabaseContext().getMetaData();
        }

        List<String> keys = new ArrayList<>(entries.size());
        // Iterate over the entries, add them to the database we are working
        // with,
        // and generate unique keys:
        for (BibEntry entry : entries) {

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
        for (BibEntry entry : entries) {
            database.removeEntry(entry);
        }
        entries.getReadWriteLock().writeLock().lock();
        glTable.repaint();
    }

    private void insertNodes(JMenu menu, GroupTreeNode node) {
        final AbstractAction action = getAction(node);

        if (node.getNumberOfChildren() == 0) {
            menu.add(action);
            if (action.isEnabled()) {
                menu.setEnabled(true);
            }
            return;
        }

        JMenu submenu;
        if (node.getGroup() instanceof AllEntriesGroup) {
            for (GroupTreeNode child : node.getChildren()) {
                insertNodes(menu, child);
            }
        } else {
            submenu = new JMenu('[' + node.getName() + ']');
            // setEnabled(true) is done above/below if at least one menu
            // entry (item or submenu) is enabled
            submenu.setEnabled(action.isEnabled());
            submenu.add(action);
            submenu.add(new JPopupMenu.Separator());
            for (GroupTreeNode child : node.getChildren()) {
                insertNodes(submenu, child);
            }
            menu.add(submenu);
            if (submenu.isEnabled()) {
                menu.setEnabled(true);
            }
        }
    }

    private AbstractAction getAction(GroupTreeNode node) {
        AbstractAction action = new AddToGroupAction(node);
        action.setEnabled(node.supportsAddingEntries());
        return action;
    }

    /**
     * Stores the information about the selected entries being scheduled for
     * addition to this group. The entries are *not* added to the group at this
     * time. <p/> Synchronizes on this.entries to prevent conflict with threads
     * that modify the entry list.
     */
    class AddToGroupAction extends AbstractAction {

        private final GroupTreeNode node;

        public AddToGroupAction(GroupTreeNode node) {
            super(node.getName());
            this.node = node;
        }

        @Override
        public void actionPerformed(ActionEvent event) {

            selectionModel.getSelected().getReadWriteLock().writeLock().lock();
            for (BibEntry entry : selectionModel.getSelected()) {
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
                for (BibEntry entry : entries) {

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
                                Localization.lang("There are possible duplicates (marked with an icon) that haven't been resolved. Continue?"),
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
                removeEntriesToDelete(ce);
            }

            // If "Generate keys" is checked, generate keys unless it's already
            // been done:
            if (autoGenerate.isSelected() && !generatedKeys) {
                generateKeys();
            }
            // Remember the choice until next time:
            Globals.prefs.putBoolean(JabRefPreferences.GENERATE_KEYS_AFTER_INSPECTION, autoGenerate.isSelected());

            final List<BibEntry> selected = getSelectedEntries();

            if (!selected.isEmpty()) {
                addSelectedEntries(ce, selected);
            }

            dispose();
            SwingUtilities.invokeLater(() -> updateGUI(selected.size()));
        }

        private void updateGUI(int entryCount) {
            if (newDatabase) {
                frame.addTab(panel, true);
            }
            panel.markBaseChanged();

            if (entryCount == 0) {
                frame.output(Localization.lang("No entries imported."));
            } else {
                frame.output(Localization.lang("Number of entries successfully imported") + ": " + entryCount);
            }
        }

        private void removeEntriesToDelete(NamedCompound ce) {
            for (BibEntry entry : entriesToDelete) {
                ce.addEdit(new UndoableRemoveEntry(panel.getDatabase(), entry, panel));
                panel.getDatabase().removeEntry(entry);
            }
        }

        private void addSelectedEntries(NamedCompound ce, final List<BibEntry> selected) {
            if (newDatabase) {
                // Create a new BasePanel for the entries:
                Defaults defaults = new Defaults(BibDatabaseMode.fromPreference(Globals.prefs.getBoolean(JabRefPreferences.BIBLATEX_DEFAULT_MODE)));
                panel = new BasePanel(frame, new BibDatabaseContext(defaults), Globals.prefs.getDefaultEncoding());
            }

            boolean groupingCanceled = false;

            // Set owner/timestamp if options are enabled:
            UpdateField.setAutomaticFields(selected, Globals.prefs.getBoolean(JabRefPreferences.OVERWRITE_OWNER),
                    Globals.prefs.getBoolean(JabRefPreferences.OVERWRITE_TIME_STAMP));

            // Mark entries if we should
            if (EntryMarker.shouldMarkEntries()) {
                for (BibEntry entry : selected) {
                    EntryMarker.markEntry(entry, EntryMarker.IMPORT_MARK_LEVEL, false, new NamedCompound(""));
                }
            }
            // Check if we should unmark entries before adding the new ones:
            if (Globals.prefs.getBoolean(JabRefPreferences.UNMARK_ALL_ENTRIES_BEFORE_IMPORTING)) {
                for (BibEntry entry : panel.getDatabase().getEntries()) {
                    EntryMarker.unmarkEntry(entry, true, panel.getDatabase(), ce);
                }
            }

            for (BibEntry entry : selected) {
                // Remove settings to group/search hit status:
                entry.setSearchHit(false);
                entry.setGroupHit(false);

                // If this entry should be added to any groups, do it now:
                Set<GroupTreeNode> groups = groupAdditions.get(entry);
                if (!groupingCanceled && (groups != null)) {
                    groupingCanceled = addToGroups(ce, entry, groups);
                }


                entry.setId(IdGenerator.next());
                panel.getDatabase().insertEntry(entry);
                ce.addEdit(new UndoableInsertEntry(panel.getDatabase(), entry, panel));

            }

            ce.end();
            panel.undoManager.addEdit(ce);
        }

        private boolean addToGroups(NamedCompound ce, BibEntry entry, Set<GroupTreeNode> groups) {
            boolean groupingCanceled = false;
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
                    if (node.supportsAddingEntries()) {
                        // Add the entry:

                        Optional<EntriesGroupChange> undo = node.getGroup().add(Collections.singletonList(entry));
                        if (undo.isPresent()) {
                            ce.addEdit(UndoableChangeEntriesOfGroup.getUndoableEdit(new GroupTreeNodeViewModel(node),
                                    undo.get()));
                        }
                    }
                }
            }
            return groupingCanceled;
        }

        /**
         * This method returns a List containing all entries that are selected
         * (checkbox checked).
         *
         * @return a List containing the selected entries.
         */
        private List<BibEntry> getSelectedEntries() {
            List<BibEntry> selected = new ArrayList<>();
            for (BibEntry entry : entries) {
                if (entry.isSearchHit()) {
                    selected.add(entry);
                }
            }

            return selected;
        }

    }

    private void signalStopFetching() {
        callBacks.forEach(c -> c.stopFetching());
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

        for (int i = 0; i < INSPECTION_FIELDS.size(); i++) {
            int width = InternalBibtexFields.getFieldLength(INSPECTION_FIELDS.get(i));
            glTable.getColumnModel().getColumn(i + PAD).setPreferredWidth(width);
        }
    }

    private class DeleteListener extends AbstractAction {

        public DeleteListener() {
            super(Localization.lang("Delete"), IconTheme.JabRefIcon.DELETE_ENTRY.getSmallIcon());
        }

        @Override
        public void actionPerformed(ActionEvent event) {
            removeSelectedEntries();
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
    }

    private class SelectionButton implements ActionListener {

        private final Boolean enable;

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

    private class EntrySelectionListener implements ListEventListener<BibEntry> {

        @Override
        public void listChanged(ListEvent<BibEntry> listEvent) {
            if (listEvent.getSourceList().size() == 1) {
                preview.setEntry(listEvent.getSourceList().get(0));
                contentPane.setDividerLocation(0.5f);
                SwingUtilities.invokeLater(() -> preview.scrollRectToVisible(toRect));
            }
        }
    }

    /**
     * This class handles clicks on the table that should trigger specific
     * events, like opening the popup menu.
     */
    private class TableClickListener implements MouseListener {

        private boolean isIconColumn(int col) {
            return (col == FILE_COL) || (col == URL_COL);
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            final int col = glTable.columnAtPoint(e.getPoint());
            final int row = glTable.rowAtPoint(e.getPoint());
            if (isIconColumn(col)) {
                BibEntry entry = sortedList.get(row);

                if (col == FILE_COL) {
                    if (entry.hasField(Globals.FILE_FIELD)) {
                        FileListTableModel tableModel = new FileListTableModel();
                        tableModel.setContent(entry.getField(Globals.FILE_FIELD));
                        if (tableModel.getRowCount() == 0) {
                            return;
                        }
                        FileListEntry fl = tableModel.getEntry(0);
                        (new ExternalFileMenuItem(frame, entry, "", fl.link, null,
                                panel.getBibDatabaseContext(), fl.type))
                                .actionPerformed(null);
                    }
                } else { // Must be URL_COL
                    openExternalLink(URL_FIELD, e);
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
        private void showPopup(MouseEvent e) {
            final int col = glTable.columnAtPoint(e.getPoint());
            if (col == FILE_COL) {
                showFileFieldMenu(e);
            } else {
                showOrdinaryRightClickMenu(e);
            }
        }

        private void showOrdinaryRightClickMenu(MouseEvent e) {
            popup.show(glTable, e.getX(), e.getY());
        }

        /**
         * Show the popup menu for the FILE field.
         *
         * @param e The mouse event that triggered the popup.
         */
        private void showFileFieldMenu(MouseEvent e) {
            final int row = glTable.rowAtPoint(e.getPoint());
            BibEntry entry = sortedList.get(row);
            JPopupMenu menu = new JPopupMenu();
            int count = 0;
            FileListTableModel fileList = new FileListTableModel();
            entry.getFieldOptional(Globals.FILE_FIELD).ifPresent(fileList::setContent);
            // If there are one or more links, open the first one:
            for (int i = 0; i < fileList.getRowCount(); i++) {
                FileListEntry flEntry = fileList.getEntry(i);
                String description = flEntry.description;
                if ((description == null) || (description.trim().isEmpty())) {
                    description = flEntry.link;
                }
                menu.add(new ExternalFileMenuItem(panel.frame(), entry, description, flEntry.link,
                        flEntry.type.get().getIcon(), panel.getBibDatabaseContext(), flEntry.type));
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
        private void openExternalLink(String fieldName, MouseEvent e) {
            final int row = glTable.rowAtPoint(e.getPoint());
            BibEntry entry = sortedList.get(row);

            entry.getFieldOptional(fieldName).ifPresent(link -> {
                try {
                    JabRefDesktop.openExternalViewer(panel.getBibDatabaseContext(), link, fieldName);
                } catch (IOException ex) {
                    LOGGER.warn("Could not open link", ex);
                }
            });
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
                BibEntry first = sortedList.get(row);
                Optional<BibEntry> other = DuplicateCheck.containsDuplicate(panel.getDatabase(), first, panel.getBibDatabaseContext().getMode());
                if (other.isPresent()) {
                    // This will be true if the duplicate is in the existing
                    // database.
                    DuplicateResolverDialog diag = new DuplicateResolverDialog(ImportInspectionDialog.this, other.get(),
                            first, DuplicateResolverDialog.DuplicateResolverType.INSPECTION);
                    diag.setLocationRelativeTo(ImportInspectionDialog.this);
                    diag.setVisible(true);
                    ImportInspectionDialog.this.toFront();
                    if (diag.getSelected() == DuplicateResolverResult.KEEP_UPPER) {
                        // Remove old entry. Or... add it to a list of entries
                        // to be deleted. We only delete
                        // it after Ok is clicked.
                        entriesToDelete.add(other.get());
                        // Clear duplicate icon, which is controlled by the
                        // group hit
                        // field of the entry:
                        entries.getReadWriteLock().writeLock().lock();
                        first.setGroupHit(false);
                        entries.getReadWriteLock().writeLock().unlock();

                    } else if (diag.getSelected() == DuplicateResolverResult.KEEP_LOWER) {
                        // Remove the entry from the import inspection dialog.
                        entries.getReadWriteLock().writeLock().lock();
                        entries.remove(first);
                        entries.getReadWriteLock().writeLock().unlock();
                    } else if (diag.getSelected() == DuplicateResolverResult.KEEP_BOTH) {
                        // Do nothing.
                        entries.getReadWriteLock().writeLock().lock();
                        first.setGroupHit(false);
                        entries.getReadWriteLock().writeLock().unlock();
                    } else if (diag.getSelected() == DuplicateResolverResult.KEEP_MERGE) {
                        // Remove old entry. Or... add it to a list of entries
                        // to be deleted. We only delete
                        // it after Ok is clicked.
                        entriesToDelete.add(other.get());
                        // Store merged entry for later adding
                        // Clear duplicate icon, which is controlled by the
                        // group hit
                        // field of the entry:
                        entries.getReadWriteLock().writeLock().lock();
                        diag.getMergedEntry().setGroupHit(false);
                        diag.getMergedEntry().setSearchHit(true);
                        entries.add(diag.getMergedEntry());
                        entries.remove(first);
                        first = new BibEntry(); // Reset first so the next duplicate doesn't trigger
                        entries.getReadWriteLock().writeLock().unlock();
                    }
                }
                // Check if the duplicate is of another entry in the import:
                other = internalDuplicate(entries, first);
                if (other.isPresent()) {
                    DuplicateResolverDialog diag = new DuplicateResolverDialog(ImportInspectionDialog.this, first,
                            other.get(), DuplicateResolverDialog.DuplicateResolverType.DUPLICATE_SEARCH);
                    diag.setLocationRelativeTo(ImportInspectionDialog.this);
                    diag.setVisible(true);
                    ImportInspectionDialog.this.toFront();
                    DuplicateResolverResult answer = diag.getSelected();
                    if (answer == DuplicateResolverResult.KEEP_UPPER) {
                        entries.remove(other.get());
                        first.setGroupHit(false);
                    } else if (answer == DuplicateResolverResult.KEEP_LOWER) {
                        entries.remove(first);
                    } else if (answer == DuplicateResolverResult.KEEP_BOTH) {
                        first.setGroupHit(false);
                    } else if (answer == DuplicateResolverResult.KEEP_MERGE) {
                        diag.getMergedEntry().setGroupHit(false);
                        diag.getMergedEntry().setSearchHit(true);
                        entries.add(diag.getMergedEntry());
                        entries.remove(first);
                        entries.remove(other.get());
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
            BibEntry entry = selectionModel.getSelected().get(0);
            String result = JOptionPane.showInputDialog(ImportInspectionDialog.this, Localization.lang("Enter URL"),
                    entry.getField(URL_FIELD));
            entries.getReadWriteLock().writeLock().lock();
            if (result != null) {
                if (result.isEmpty()) {
                    entry.clearField(URL_FIELD);
                } else {
                    entry.setField(URL_FIELD, result);
                }
            }
            entries.getReadWriteLock().writeLock().unlock();
            glTable.repaint();
        }
    }

    class DownloadFile extends JMenuItem implements ActionListener,
            DownloadExternalFile.DownloadCallback {

        private BibEntry entry;

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
            DownloadExternalFile def = new DownloadExternalFile(frame, bibDatabaseContext, bibtexKey);
            try {
                def.download(this);
            } catch (IOException ex) {
                LOGGER.warn("Could not downlod file", ex);
            }
        }

        @Override
        public void downloadComplete(FileListEntry file) {
            ImportInspectionDialog.this.toFront(); // Hack
            FileListTableModel localModel = new FileListTableModel();
            entry.getFieldOptional(Globals.FILE_FIELD).ifPresent(localModel::setContent);
            localModel.addEntry(localModel.getRowCount(), file);
            entries.getReadWriteLock().writeLock().lock();
            entry.setField(Globals.FILE_FIELD, localModel.getStringRepresentation());
            entries.getReadWriteLock().writeLock().unlock();
            glTable.repaint();
        }
    }

    private class AutoSetLinks extends JMenuItem implements ActionListener {

        public AutoSetLinks() {
            super(Localization.lang("Automatically set file links"));
            addActionListener(this);
        }

        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            if (selectionModel.getSelected().size() != 1) {
                return;
            }
            final BibEntry entry = selectionModel.getSelected().get(0);
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
            entry.getFieldOptional(Globals.FILE_FIELD).ifPresent(localModel::setContent);
            // We have a static utility method for searching for all relevant
            // links:
            JDialog diag = new JDialog(ImportInspectionDialog.this, true);
            JabRefExecutorService.INSTANCE.execute(
                    net.sf.jabref.external.AutoSetLinks.autoSetLinks(entry, localModel, bibDatabaseContext, e -> {
                        if (e.getID() > 0) {
                            entries.getReadWriteLock().writeLock().lock();
                            entry.setField(Globals.FILE_FIELD, localModel.getStringRepresentation());
                            entries.getReadWriteLock().writeLock().unlock();
                            glTable.repaint();
                        }
                    } , diag));

        }
    }

    private class LinkLocalFile extends JMenuItem implements ActionListener,
            DownloadExternalFile.DownloadCallback {

        private BibEntry entry;

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
            FileListEntry flEntry = new FileListEntry("", "");
            FileListEntryEditor editor = new FileListEntryEditor(frame, flEntry, false, true, bibDatabaseContext);
            editor.setVisible(true, true);
            if (editor.okPressed()) {
                FileListTableModel localModel = new FileListTableModel();
                entry.getFieldOptional(Globals.FILE_FIELD).ifPresent(localModel::setContent);
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
            entry.getFieldOptional(Globals.FILE_FIELD).ifPresent(localModel::setContent);
            localModel.addEntry(localModel.getRowCount(), file);
            entries.getReadWriteLock().writeLock().lock();
            entry.setField(Globals.FILE_FIELD, localModel.getStringRepresentation());
            entries.getReadWriteLock().writeLock().unlock();
            glTable.repaint();
        }
    }

    private void setupComparatorChooser() {
        // First column:

        List<Comparator> comparators = comparatorChooser.getComparatorsForColumn(0);
        comparators.clear();

        comparators = comparatorChooser.getComparatorsForColumn(1);
        comparators.clear();

        // Icon columns:
        for (int i = 2; i < PAD; i++) {
            comparators = comparatorChooser.getComparatorsForColumn(i);
            comparators.clear();
            if (i == FILE_COL) {
                comparators.add(new IconComparator(Collections.singletonList(Globals.FILE_FIELD)));
            } else if (i == URL_COL) {
                comparators.add(new IconComparator(Collections.singletonList(URL_FIELD)));
            }

        }
        // Remaining columns:
        for (int i = PAD; i < (PAD + INSPECTION_FIELDS.size()); i++) {
            comparators = comparatorChooser.getComparatorsForColumn(i);
            comparators.clear();
            comparators.add(new FieldComparator(INSPECTION_FIELDS.get(i - PAD)));
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


    @FunctionalInterface
    public interface CallBack {

        /**
         * This method is called by the dialog when the user has canceled or
         * signaled a stop. It is expected that any long-running fetch
         * operations will stop after this method is called.
         */
        void stopFetching();
    }


    /**
     * The "defaultSelected" boolean value determines if new entries added are
     * selected for import or not. This value is true by default.
     *
     * @param defaultSelected The desired value.
     */
    public void setDefaultSelected(boolean defaultSelected) {
        this.defaultSelected = defaultSelected;
    }


    class EntryTable extends JTable {

        private final GeneralRenderer renderer = new GeneralRenderer(Color.white);

        public EntryTable(TableModel model) {
            super(model);
            getTableHeader().setReorderingAllowed(false);
        }

        @Override
        public TableCellRenderer getCellRenderer(int row, int column) {
            return column == 0 ? getDefaultRenderer(Boolean.class) : renderer;
        }

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
            // Only column 0, which is controlled by BibEntry.searchHit, is
            // editable:
            entries.getReadWriteLock().writeLock().lock();
            BibEntry entry = sortedList.get(row);
            entry.setSearchHit((Boolean) value);
            entries.getReadWriteLock().writeLock().unlock();
        }
    }

    private class EntryTableFormat implements TableFormat<BibEntry> {

        @Override
        public int getColumnCount() {
            return PAD + INSPECTION_FIELDS.size();
        }

        @Override
        public String getColumnName(int i) {
            if (i == 0) {
                return Localization.lang("Keep");
            }
            if (i >= PAD) {
                return EntryUtil.capitalizeFirst(INSPECTION_FIELDS.get(i - PAD));
            }
            return "";
        }

        @Override
        public Object getColumnValue(BibEntry entry, int i) {
            if (i == 0) {
                return entry.isSearchHit() ? Boolean.TRUE : Boolean.FALSE;
            } else if (i < PAD) {
                switch (i) {
                case DUPL_COL:
                    return entry.isGroupHit() ? duplLabel : null;
                case FILE_COL:
                    if (entry.hasField(Globals.FILE_FIELD)) {
                        FileListTableModel model = new FileListTableModel();
                        model.setContent(entry.getField(Globals.FILE_FIELD));
                        fileLabel.setToolTipText(model.getToolTipHTMLRepresentation());
                        if (model.getRowCount() > 0) {
                            fileLabel.setIcon(model.getEntry(0).type.get().getIcon());
                        }
                        return fileLabel;
                    } else {
                        return null;
                    }
                case URL_COL:
                    if (entry.hasField(URL_FIELD)) {
                        urlLabel.setToolTipText(entry.getField(URL_FIELD));
                        return urlLabel;
                    } else {
                        return null;
                    }
                default:
                    return null;
                }
            } else {
                String field = INSPECTION_FIELDS.get(i - PAD);
                if (InternalBibtexFields.getFieldExtras(field).contains(FieldProperties.PERSON_NAMES)) {
                    return entry.getFieldOptional(field).map(AuthorList::fixAuthorNatbib)
                            .orElse("");
                } else {
                    return entry.getField(field);
                }
            }
        }

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
