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

import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;
import ca.odell.glazedlists.matchers.Matcher;
import com.jgoodies.forms.builder.FormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefExecutorService;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.MetaData;
import net.sf.jabref.collab.ChangeScanner;
import net.sf.jabref.collab.FileUpdateListener;
import net.sf.jabref.collab.FileUpdatePanel;
import net.sf.jabref.exporter.*;
import net.sf.jabref.exporter.FileActions.DatabaseSaveType;
import net.sf.jabref.exporter.layout.Layout;
import net.sf.jabref.exporter.layout.LayoutHelper;
import net.sf.jabref.external.*;
import net.sf.jabref.groups.GroupMatcher;
import net.sf.jabref.groups.GroupSelector;
import net.sf.jabref.groups.GroupTreeNode;
import net.sf.jabref.gui.actions.Actions;
import net.sf.jabref.gui.actions.BaseAction;
import net.sf.jabref.gui.actions.CleanUpAction;
import net.sf.jabref.gui.desktop.JabRefDesktop;
import net.sf.jabref.gui.entryeditor.EntryEditor;
import net.sf.jabref.gui.fieldeditors.FieldEditor;
import net.sf.jabref.gui.journals.AbbreviateAction;
import net.sf.jabref.gui.journals.UnabbreviateAction;
import net.sf.jabref.gui.labelPattern.SearchFixDuplicateLabels;
import net.sf.jabref.gui.mergeentries.MergeEntriesDialog;
import net.sf.jabref.gui.mergeentries.MergeEntryDOIDialog;
import net.sf.jabref.gui.search.SearchBar;
import net.sf.jabref.gui.undo.*;
import net.sf.jabref.gui.util.FocusRequester;
import net.sf.jabref.gui.util.PositionWindow;
import net.sf.jabref.gui.worker.*;
import net.sf.jabref.importer.AppendDatabaseAction;
import net.sf.jabref.importer.fileformat.BibtexParser;
import net.sf.jabref.logic.autocompleter.AutoCompleter;
import net.sf.jabref.logic.autocompleter.AutoCompleterFactory;
import net.sf.jabref.logic.autocompleter.ContentAutoCompleters;
import net.sf.jabref.logic.l10n.Encodings;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.labelPattern.LabelPatternUtil;
import net.sf.jabref.logic.search.matchers.EverythingMatcher;
import net.sf.jabref.logic.search.matchers.SearchMatcher;
import net.sf.jabref.logic.util.io.FileBasedLock;
import net.sf.jabref.model.database.BibtexDatabase;
import net.sf.jabref.model.database.DatabaseChangeEvent;
import net.sf.jabref.model.database.DatabaseChangeEvent.ChangeType;
import net.sf.jabref.model.database.DatabaseChangeListener;
import net.sf.jabref.model.database.KeyCollisionException;
import net.sf.jabref.model.entry.BibtexEntry;
import net.sf.jabref.model.entry.EntryType;
import net.sf.jabref.model.entry.IdGenerator;
import net.sf.jabref.specialfields.*;
import net.sf.jabref.sql.*;
import net.sf.jabref.sql.exporter.DBExporter;
import net.sf.jabref.util.Util;
import net.sf.jabref.wizard.text.gui.TextInputDialog;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.swing.*;
import javax.swing.tree.TreePath;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.*;
import java.util.List;

public class BasePanel extends JPanel implements ClipboardOwner, FileUpdateListener {

    private static final Log LOGGER = LogFactory.getLog(BasePanel.class);

    public static final int SHOWING_NOTHING = 0;
    private static final int SHOWING_PREVIEW = 1;
    public static final int SHOWING_EDITOR = 2;
    public static final int WILL_SHOW_EDITOR = 3;

    /*
     * The database shown in this panel.
     */
    private final BibtexDatabase database;

    private int mode;
    private EntryEditor currentEditor;
    private PreviewPanel currentPreview;

    private MainTableSelectionListener selectionListener;
    private ListEventListener<BibtexEntry> groupsHighlightListener;

    private JSplitPane splitPane;

    JabRefFrame frame;

    private String fileMonitorHandle;
    private boolean saving;
    private boolean updatedExternally;
    private Charset encoding;

    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints con = new GridBagConstraints();

    // AutoCompleter used in the search bar
    private AutoCompleter<String> searchAutoCompleter;

    // The undo manager.
    public final CountingUndoManager undoManager = new CountingUndoManager(this);
    private final UndoAction undoAction = new UndoAction();
    private final RedoAction redoAction = new RedoAction();

    private final List<BibtexEntry> previousEntries = new ArrayList<>();
    private final List<BibtexEntry> nextEntries = new ArrayList<>();

    private boolean baseChanged;
    private boolean nonUndoableChange;
    // Used to track whether the base has changed since last save.

    public MainTable mainTable;
    public MainTableFormat tableFormat;
    private FilterList<BibtexEntry> searchFilterList;
    private FilterList<BibtexEntry> groupFilterList;

    public RightClickMenu rcm;

    private BibtexEntry showing;

    // Variable to prevent erroneous update of back/forward histories at the time
    // when a Back or Forward operation is being processed:
    private boolean backOrForwardInProgress;

    // To indicate which entry is currently shown.
    public final HashMap<String, EntryEditor> entryEditors = new HashMap<>();
    // To contain instantiated entry editors. This is to save time
    // in switching between entries.

    private PreambleEditor preambleEditor;
    // Keeps track of the preamble dialog if it is open.

    private StringDialog stringDialog;
    // Keeps track of the string dialog if it is open.

    private SaveDatabaseAction saveAction;

    public boolean sortingByGroup;
    public boolean sortingByCiteSeerResults;
    public boolean coloringByGroup;

    // MetaData parses, keeps and writes meta data.
    public final MetaData metaData;

    private final HashMap<String, Object> actions = new HashMap<>();
    private final SidePaneManager sidePaneManager;

    private final SearchBar searchBar;

    private final StartStopListAction<BibtexEntry> filterSearchToggle;

    private final StartStopListAction<BibtexEntry> filterGroupToggle;

    // Returns a collection of AutoCompleters, which are populated from the current database
    public ContentAutoCompleters getAutoCompleters() {
        return autoCompleters;
    }

    private ContentAutoCompleters autoCompleters;


    public BasePanel(JabRefFrame frame, BibtexDatabase db, File file, MetaData metaData, Charset encoding) {
        Objects.requireNonNull(frame);
        Objects.requireNonNull(db);
        //file may be null
        Objects.requireNonNull(encoding);
        Objects.requireNonNull(metaData);

        this.encoding = encoding;
        this.metaData = metaData;

        this.sidePaneManager = GUIGlobals.sidePaneManager;
        this.frame = frame;
        database = db;

        searchBar = new SearchBar(this);

        setupMainPanel();

        setupActions();

        metaData.setFile(file);

        // ensure that at each addition of a new entry, the entry is added to the groups interface
        db.addDatabaseChangeListener(new GroupTreeUpdater());


        if (file == null) {
            if (!database.getEntries().isEmpty()) {
                // if the database is not empty and no file is assigned,
                // the database came from an import and has to be treated somehow
                // -> mark as changed
                this.baseChanged = true;
            }
        } else {
            // Register so we get notifications about outside changes to the file.
            try {
                fileMonitorHandle = Globals.fileUpdateMonitor.addUpdateListener(this, file);
            } catch (IOException ex) {
                LOGGER.warn("Could not register FileUpdateMonitor", ex);
            }
        }

        filterSearchToggle = new StartStopListAction<>(searchFilterList, SearchMatcher.INSTANCE, EverythingMatcher.INSTANCE);
        filterGroupToggle = new StartStopListAction<>(groupFilterList, GroupMatcher.INSTANCE, EverythingMatcher.INSTANCE);
    }

    public String getTabTitle() {
        String title;

        if (getDatabaseFile() == null) {
            title = GUIGlobals.untitledTitle;

            if (!database().getEntries().isEmpty()) {
                // if the database is not empty and no file is assigned,
                // the database came from an import and has to be treated somehow
                // -> mark as changed
                // This also happens internally at basepanel to ensure consistency line 224
                title = title + '*';
            }
        } else {
            title = getDatabaseFile().getName();
        }

        return title;
    }

    public boolean isBaseChanged() {
        return baseChanged;
    }

    public int getMode() {
        return mode;
    }

    public void setMode(int mode) {
        this.mode = mode;
    }

    public BibtexDatabase database() {
        return database;
    }

    public MetaData metaData() {
        return metaData;
    }

    public JabRefFrame frame() {
        return frame;
    }

    public JabRefPreferences prefs() {
        return Globals.prefs;
    }

    public Charset getEncoding() {
        return encoding;
    }

    public void setEncoding(Charset encoding) {
        this.encoding = encoding;
    }

    public void output(String s) {
        frame.output(s);
    }

    private void setupActions() {
        saveAction = new SaveDatabaseAction(this);
        CleanUpAction cleanUpAction = new CleanUpAction(this);

        actions.put(Actions.UNDO, undoAction);
        actions.put(Actions.REDO, redoAction);

        actions.put(Actions.FOCUS_TABLE, (BaseAction) () -> new FocusRequester(mainTable));

        // The action for opening an entry editor.
        actions.put(Actions.EDIT, (BaseAction) selectionListener::editSignalled);

        // The action for saving a database.
        actions.put(Actions.SAVE, saveAction);

        actions.put(Actions.SAVE_AS, (BaseAction) saveAction::saveAs);

        actions.put(Actions.SAVE_SELECTED_AS, new SaveSelectedAction(FileActions.DatabaseSaveType.DEFAULT));

        actions.put(Actions.SAVE_SELECTED_AS_PLAIN, new SaveSelectedAction(FileActions.DatabaseSaveType.PLAIN_BIBTEX));

        // The action for copying selected entries.
        actions.put(Actions.COPY, (BaseAction) () -> {
            BibtexEntry[] bes = mainTable.getSelectedEntries();

            if ((bes != null) && (bes.length > 0)) {
                TransferableBibtexEntry trbe = new TransferableBibtexEntry(bes);
                // ! look at ClipBoardManager
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(trbe, BasePanel.this);
                output(formatOutputMessage(Localization.lang("Copied"), bes.length));
            } else {
                // The user maybe selected a single cell.
                int[] rows = mainTable.getSelectedRows();
                int[] cols = mainTable.getSelectedColumns();
                if ((cols.length == 1) && (rows.length == 1)) {
                    // Copy single value.
                    Object o = mainTable.getValueAt(rows[0], cols[0]);
                    if (o != null) {
                        StringSelection ss = new StringSelection(o.toString());
                        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(ss, BasePanel.this);

                        output(Localization.lang("Copied cell contents") + '.');
                    }
                }
            }
        });

        actions.put(Actions.CUT, (BaseAction) () -> {
            runCommand(Actions.COPY);
            BibtexEntry[] bes = mainTable.getSelectedEntries();
            //int row0 = mainTable.getSelectedRow();
            if ((bes != null) && (bes.length > 0)) {
                // Create a CompoundEdit to make the action undoable.
                NamedCompound ce = new NamedCompound((bes.length > 1 ?
                        // @formatter:off
                    Localization.lang("cut entries") :
                    Localization.lang("cut entry")));
                // @formatter:on
                // Loop through the array of entries, and delete them.
                for (BibtexEntry be : bes) {
                    database.removeEntry(be.getId());
                    ensureNotShowing(be);
                    ce.addEdit(new UndoableRemoveEntry(database, be, BasePanel.this));
                }
                //entryTable.clearSelection();
                frame.output(formatOutputMessage(Localization.lang("Cut"), bes.length));
                ce.end();
                undoManager.addEdit(ce);
                markBaseChanged();
            }
        });

        actions.put(Actions.DELETE, (BaseAction) () -> {
            BibtexEntry[] bes = mainTable.getSelectedEntries();
            if ((bes != null) && (bes.length > 0)) {

                boolean goOn = showDeleteConfirmationDialog(bes.length);
                if (goOn) {
                    // Create a CompoundEdit to make the action undoable.
                    NamedCompound ce = new NamedCompound((bes.length > 1 ?
                            // @formatter:off
                        Localization.lang("delete entries") :
                        Localization.lang("delete entry")));
                    // @formatter:on
                    // Loop through the array of entries, and delete them.
                    for (BibtexEntry be : bes) {
                        database.removeEntry(be.getId());
                        ensureNotShowing(be);
                        ce.addEdit(new UndoableRemoveEntry(database, be, BasePanel.this));
                    }
                    markBaseChanged();
                    frame.output(formatOutputMessage(Localization.lang("Deleted"), bes.length));
                    ce.end();
                    undoManager.addEdit(ce);
                }
            }
        });

        // The action for pasting entries or cell contents.
        // Edited by Seb Wills <saw27@mrao.cam.ac.uk> on 14-Apr-04:
        //  - more robust detection of available content flavors (doesn't only look at first one offered)
        //  - support for parsing string-flavor clipboard contents which are bibtex entries.
        //    This allows you to (a) paste entire bibtex entries from a text editor, web browser, etc
        //                       (b) copy and paste entries between multiple instances of JabRef (since
        //         only the text representation seems to get as far as the X clipboard, at least on my system)
        actions.put(Actions.PASTE, (BaseAction) () -> {
            // Get clipboard contents, and see if TransferableBibtexEntry is among the content flavors offered
            Transferable content = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
            if (content != null) {
                BibtexEntry[] bes = null;
                if (content.isDataFlavorSupported(TransferableBibtexEntry.entryFlavor)) {
                    // We have determined that the clipboard data is a set of entries.
                    try {
                        bes = (BibtexEntry[]) content.getTransferData(TransferableBibtexEntry.entryFlavor);

                    } catch (UnsupportedFlavorException ex) {
                        ex.printStackTrace();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                } else if (content.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                    try {
                        BibtexParser bp = new BibtexParser(
                                new StringReader((String) content.getTransferData(DataFlavor.stringFlavor)));
                        BibtexDatabase db = bp.parse().getDatabase();
                        LOGGER.info("Parsed " + db.getEntryCount() + " entries from clipboard text");
                        if (db.getEntryCount() > 0) {
                            bes = db.getEntries().toArray(new BibtexEntry[db.getEntryCount()]);
                        }
                    } catch (UnsupportedFlavorException ex) {
                        ex.printStackTrace();
                    } catch (Throwable ex) {
                        ex.printStackTrace();
                    }

                }

                // finally we paste in the entries (if any), which either came from TransferableBibtexEntries
                // or were parsed from a string
                if ((bes != null) && (bes.length > 0)) {

                    NamedCompound ce = new NamedCompound((bes.length > 1 ?
                            // @formatter:off
                        Localization.lang("paste entries") :
                        Localization.lang("paste entry")));
                    // @formatter:on

                    // Store the first inserted bibtexentry.
                    // bes[0] does not work as bes[0] is first clonded,
                    // then inserted.
                    // This entry is used to open up an entry editor
                    // for the first inserted entry.
                    BibtexEntry firstBE = null;

                    for (BibtexEntry be1 : bes) {

                        BibtexEntry be = (BibtexEntry) be1.clone();
                        if (firstBE == null) {
                            firstBE = be;
                        }
                        Util.setAutomaticFields(be, Globals.prefs.getBoolean(JabRefPreferences.OVERWRITE_OWNER),
                                Globals.prefs.getBoolean(JabRefPreferences.OVERWRITE_TIME_STAMP));

                        // We have to clone the
                        // entries, since the pasted
                        // entries must exist
                        // independently of the copied
                        // ones.
                        be.setId(IdGenerator.next());
                        database.insertEntry(be);

                        ce.addEdit(new UndoableInsertEntry(database, be, BasePanel.this));

                    }
                    ce.end();
                    undoManager.addEdit(ce);
                    //entryTable.clearSelection();
                    //entryTable.revalidate();
                    output(formatOutputMessage(Localization.lang("Pasted"), bes.length));
                    markBaseChanged();

                    if (Globals.prefs.getBoolean(JabRefPreferences.AUTO_OPEN_FORM)) {
                        selectionListener.editSignalled(firstBE);
                    }
                    highlightEntry(firstBE);
                }
            }

        });

        actions.put(Actions.SELECT_ALL, (BaseAction) mainTable::selectAll);

        // The action for opening the preamble editor
        actions.put(Actions.EDIT_PREAMBLE, (BaseAction) () -> {
            if (preambleEditor == null) {
                PreambleEditor form = new PreambleEditor
                        (frame, BasePanel.this, database, Globals.prefs);
                PositionWindow.placeDialog(form, frame);
                form.setVisible(true);
                preambleEditor = form;
            } else {
                preambleEditor.setVisible(true);
            }

        });

        // The action for opening the string editor
        actions.put(Actions.EDIT_STRINGS, (BaseAction) () -> {
            if (stringDialog == null) {
                StringDialog form = new StringDialog(frame, BasePanel.this, database, Globals.prefs);
                form.setVisible(true);
                stringDialog = form;
            } else {
                stringDialog.setVisible(true);
            }

        });

        // The action for toggling the groups interface
        actions.put(Actions.TOGGLE_GROUPS, (BaseAction) () -> {
            sidePaneManager.toggle("groups");
            frame.groupToggle.setSelected(sidePaneManager.isComponentVisible("groups"));
        });

        // action for collecting database strings from user
        actions.put(Actions.DB_CONNECT, new DbConnectAction(this));

        // action for exporting database to external SQL database
        actions.put(Actions.DB_EXPORT, new AbstractWorker() {

            String errorMessage;
            boolean connectToDB;

            // run first, in EDT:
            @Override
            public void init() {

                DBStrings dbs = metaData.getDBStrings();

                // get DBStrings from user if necessary
                if (!dbs.isConfigValid()) {

                    // init DB strings if necessary
                    if (!dbs.isInitialized()) {
                        dbs.initialize();
                    }

                    // show connection dialog
                    DBConnectDialog dbd = new DBConnectDialog(frame(), dbs);
                    PositionWindow.placeDialog(dbd, BasePanel.this);
                    dbd.setVisible(true);

                    connectToDB = dbd.getConnectToDB();

                    // store database strings
                    if (connectToDB) {
                        dbs = dbd.getDBStrings();
                        metaData.setDBStrings(dbs);
                        dbd.dispose();
                    }

                } else {
                    connectToDB = true;
                }

            }

            // run second, on a different thread:
            @Override
            public void run() {

                if (connectToDB) {

                    DBStrings dbs = metaData.getDBStrings();

                    try {
                        frame.output(Localization.lang("Attempting SQL export..."));
                        DBExporterAndImporterFactory factory = new DBExporterAndImporterFactory();
                        DBExporter exporter = factory.getExporter(dbs.getServerType());
                        exporter.exportDatabaseToDBMS(database, metaData, null, dbs, frame);
                        dbs.isConfigValid(true);
                    } catch (Exception ex) {
                        // @formatter:off
                        String preamble = Localization.lang("Could not export to SQL database for the following reason:");
                        // @formatter:on
                        errorMessage = SQLUtil.getExceptionMessage(ex);
                        ex.printStackTrace();
                        dbs.isConfigValid(false);
                        JOptionPane.showMessageDialog(frame, preamble + '\n' + errorMessage,
                                Localization.lang("Export to SQL database"), JOptionPane.ERROR_MESSAGE);
                    }

                    metaData.setDBStrings(dbs);
                }
            }

            // run third, on EDT:
            @Override
            public void update() {

                // if no error, report success
                if (errorMessage == null) {
                    if (connectToDB) {
                        frame.output(Localization.lang("%0 export successful"));
                    }
                } else { // show an error dialog if an error occurred
                    // @formatter:off
                    String preamble = Localization.lang("Could not export to SQL database for the following reason:");
                    // @formatter:on
                    frame.output(preamble + "  " + errorMessage);

                    JOptionPane.showMessageDialog(frame, preamble + '\n' + errorMessage,
                            Localization.lang("Export to SQL database"), JOptionPane.ERROR_MESSAGE);

                    errorMessage = null;
                }
            }

        });

        actions.put(FindUnlinkedFilesDialog.ACTION_COMMAND, (BaseAction) () -> {
            FindUnlinkedFilesDialog dialog = new FindUnlinkedFilesDialog(frame, frame, BasePanel.this);
            PositionWindow.placeDialog(dialog, frame);
            dialog.setVisible(true);
        });

        // The action for auto-generating keys.
        actions.put(Actions.MAKE_KEY, new AbstractWorker() {

            //int[] rows;
            List<BibtexEntry> entries;
            int numSelected;
            boolean cancelled;

            // Run first, in EDT:
            @Override
            public void init() {
                entries = new ArrayList<>(Arrays.asList(getSelectedEntries()));
                numSelected = entries.size();

                if (entries.isEmpty()) { // None selected. Inform the user to select entries first.
                    JOptionPane.showMessageDialog(frame,
                            Localization.lang("First select the entries you want keys to be generated for."),
                            Localization.lang("Autogenerate BibTeX key"), JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
                frame.block();
                output(formatOutputMessage(Localization.lang("Generating BibTeX key for"), numSelected));
            }

            // Run second, on a different thread:
            @Override
            public void run() {
                BibtexEntry bes;
                NamedCompound ce = new NamedCompound(Localization.lang("autogenerate keys"));

                // First check if any entries have keys set already. If so, possibly remove
                // them from consideration, or warn about overwriting keys.
                // This is a partial clone of net.sf.jabref.gui.entryeditor.EntryEditor.GenerateKeyAction.actionPerformed(ActionEvent)
                for (Iterator<BibtexEntry> i = entries.iterator(); i.hasNext(); ) {
                    bes = i.next();
                    if (bes.getCiteKey() != null) {
                        if (Globals.prefs.getBoolean(JabRefPreferences.AVOID_OVERWRITING_KEY)) {
                            // Remove the entry, because its key is already set:
                            i.remove();
                        } else if (Globals.prefs.getBoolean(JabRefPreferences.WARN_BEFORE_OVERWRITING_KEY)) {
                            // Ask if the user wants to cancel the operation:
                            CheckBoxMessage cbm = new CheckBoxMessage(
                                    Localization.lang("One or more keys will be overwritten. Continue?"),
                                    Localization.lang("Disable this confirmation dialog"), false);
                            int answer = JOptionPane.showConfirmDialog(frame, cbm, Localization.lang("Overwrite keys"),
                                    JOptionPane.YES_NO_OPTION);
                            if (cbm.isSelected()) {
                                Globals.prefs.putBoolean(JabRefPreferences.WARN_BEFORE_OVERWRITING_KEY, false);
                            }
                            if (answer == JOptionPane.NO_OPTION) {
                                // Ok, break off the operation.
                                cancelled = true;
                                return;
                            }
                            // No need to check more entries, because the user has already confirmed
                            // that it's ok to overwrite keys:
                            break;
                        }
                    }
                }

                HashMap<BibtexEntry, Object> oldvals = new HashMap<>();
                // Iterate again, removing already set keys. This is skipped if overwriting
                // is disabled, since all entries with keys set will have been removed.
                if (!Globals.prefs.getBoolean(JabRefPreferences.AVOID_OVERWRITING_KEY)) {
                    for (BibtexEntry entry : entries) {
                        bes = entry;
                        // Store the old value:
                        oldvals.put(bes, bes.getField(BibtexEntry.KEY_FIELD));
                        database.setCiteKeyForEntry(bes.getId(), null);
                    }
                }

                // Finally, set the new keys:
                for (BibtexEntry entry : entries) {
                    bes = entry;
                    LabelPatternUtil.makeLabel(metaData, database, bes);
                    ce.addEdit(new UndoableKeyChange(database, bes.getId(), (String) oldvals.get(bes),
                            bes.getField(BibtexEntry.KEY_FIELD)));
                }
                ce.end();
                undoManager.addEdit(ce);
            }

            // Run third, on EDT:
            @Override
            public void update() {
                database.setFollowCrossrefs(true);
                if (cancelled) {
                    frame.unblock();
                    return;
                }
                markBaseChanged();
                numSelected = entries.size();

                ////////////////////////////////////////////////////////////////////////////////
                //          Prevent selection loss for autogenerated BibTeX-Keys
                ////////////////////////////////////////////////////////////////////////////////
                for (final BibtexEntry bibEntry : entries) {
                    SwingUtilities.invokeLater(new Runnable() {

                        @Override
                        public void run() {
                            final int row = mainTable.findEntry(bibEntry);
                            if ((row >= 0) && (mainTable.getSelectedRowCount() < entries.size())) {
                                mainTable.addRowSelectionInterval(row, row);
                            }
                        }
                    });
                }
                ////////////////////////////////////////////////////////////////////////////////
                output(formatOutputMessage(Localization.lang("Generated BibTeX key for"), numSelected));
                frame.unblock();
            }
        });

        // The action for cleaning up entry.
        actions.put(Actions.CLEANUP, cleanUpAction);

        actions.put(Actions.MERGE_ENTRIES, (BaseAction) () -> new MergeEntriesDialog(BasePanel.this));

        actions.put(Actions.SEARCH, (BaseAction) searchBar::focus);

        // The action for copying the selected entry's key.
        actions.put(Actions.COPY_KEY, (BaseAction) () -> {
            BibtexEntry[] bes = mainTable.getSelectedEntries();
            if ((bes != null) && (bes.length > 0)) {
                storeCurrentEdit();
                List<String> keys = new ArrayList<>(bes.length);
                // Collect all non-null keys.
                for (BibtexEntry be : bes) {
                    if (be.getCiteKey() != null) {
                        keys.add(be.getCiteKey());
                    }
                }
                if (keys.isEmpty()) {
                    output("None of the selected entries have BibTeX keys.");
                    return;
                }
                StringBuilder sb = new StringBuilder(keys.get(0));
                for (int i = 1; i < keys.size(); i++) {
                    sb.append(',');
                    sb.append(keys.get(i));
                }

                StringSelection ss = new StringSelection(sb.toString());
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(ss, BasePanel.this);

                if (keys.size() == bes.length) {
                    // All entries had keys.
                    output((bes.length > 1 ?
                            // @formatter:off
                        Localization.lang("Copied keys") :
                        Localization.lang("Copied key")) + '.');
                    // @formatter:on
                } else {
                    output(Localization.lang("Warning: %0 out of %1 entries have undefined BibTeX key.",
                            Integer.toString(bes.length - keys.size()), Integer.toString(bes.length)));
                }
            }
        });

        // The action for copying a cite for the selected entry.
        actions.put(Actions.COPY_CITE_KEY, new BaseAction() {

            @Override
            public void action() {
                BibtexEntry[] bes = mainTable.getSelectedEntries();
                if ((bes != null) && (bes.length > 0)) {
                    storeCurrentEdit();
                    List<String> keys = new ArrayList<>(bes.length);
                    // Collect all non-null keys.
                    for (BibtexEntry be : bes) {
                        if (be.getCiteKey() != null) {
                            keys.add(be.getCiteKey());
                        }
                    }
                    if (keys.isEmpty()) {
                        output("None of the selected entries have BibTeX keys.");
                        return;
                    }
                    StringBuilder sb = new StringBuilder(keys.get(0));
                    for (int i = 1; i < keys.size(); i++) {
                        sb.append(',');
                        sb.append(keys.get(i));
                    }

                    StringSelection ss = new StringSelection("\\cite{" + sb + '}');
                    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(ss, BasePanel.this);

                    if (keys.size() == bes.length) {
                        // All entries had keys.
                        // @formatter:off
                        output(bes.length > 1 ? Localization.lang("Copied keys") :
                            Localization.lang("Copied key") + '.');
                        // @formatter:on
                    } else {
                        output(Localization.lang("Warning: %0 out of %1 entries have undefined BibTeX key.",
                                Integer.toString(bes.length - keys.size()), Integer.toString(bes.length)));
                    }
                }
            }
        });

        // The action for copying the BibTeX key and the title for the first selected entry
        actions.put(Actions.COPY_KEY_AND_TITLE, new BaseAction() {

            @Override
            public void action() {
                BibtexEntry[] bes = mainTable.getSelectedEntries();
                if ((bes != null) && (bes.length > 0)) {
                    storeCurrentEdit();

                    // OK: in a future version, this string should be configurable to allow arbitrary exports
                    StringReader sr = new StringReader(
                            "\\bibtexkey - \\begin{title}\\format[RemoveBrackets]{\\title}\\end{title}\n");
                    Layout layout;
                    try {
                        layout = new LayoutHelper(sr).getLayoutFromText(Globals.FORMATTER_PACKAGE);
                    } catch (Exception e) {
                        e.printStackTrace();
                        return;
                    }

                    StringBuilder sb = new StringBuilder();

                    int copied = 0;
                    // Collect all non-null keys.
                    for (BibtexEntry be : bes) {
                        if (be.getCiteKey() != null) {
                            copied++;
                            sb.append(layout.doLayout(be, database));
                        }
                    }

                    if (copied == 0) {
                        output("None of the selected entries have BibTeX keys.");
                        return;
                    }

                    StringSelection ss = new StringSelection(sb.toString());
                    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(ss, BasePanel.this);

                    if (copied == bes.length) {
                        // All entries had keys.
                        output((bes.length > 1 ?
                                // @formatter:off
                            Localization.lang("Copied keys") :
                            Localization.lang("Copied key")) + '.');
                        // @formatter:on
                    } else {
                        // @formatter:off
                        output(Localization.lang("Warning: %0 out of %1 entries have undefined BibTeX key.", Integer.toString(copied), Integer.toString(bes.length)));
                        // @formatter:on
                    }
                }
            }
        });

        actions.put(Actions.MERGE_DATABASE, new AppendDatabaseAction(frame, this));

        actions.put(Actions.ADD_FILE_LINK, new AttachFileAction(this));

        actions.put(Actions.OPEN_EXTERNAL_FILE, new BaseAction() {

            @Override
            public void action() {
                JabRefExecutorService.INSTANCE.execute(new Runnable() {

                    @Override
                    public void run() {
                        BibtexEntry[] bes = mainTable.getSelectedEntries();
                        if (bes.length != 1) {
                            output(Localization.lang("No entries or multiple entries selected."));
                            return;
                        }

                        BibtexEntry entry = bes[0];
                        String file = entry.getField(Globals.FILE_FIELD);
                        if (file == null) {
                            // no bibtex field
                            new SearchAndOpenFile(entry, BasePanel.this).searchAndOpen();
                            return;
                        }
                        FileListTableModel tableModel = new FileListTableModel();
                        tableModel.setContent(file);
                        if (tableModel.getRowCount() == 0) {
                            // content in bibtex field is not readable
                            new SearchAndOpenFile(entry, BasePanel.this).searchAndOpen();
                            return;
                        }
                        FileListEntry flEntry = tableModel.getEntry(0);
                        ExternalFileMenuItem item = new ExternalFileMenuItem(frame(), entry, "", flEntry.getLink(),
                                flEntry.getType().getIcon(), metaData(), flEntry.getType());
                        item.openLink();
                    }
                });
            }
        });

        actions.put(Actions.OPEN_FOLDER, new BaseAction() {

            @Override
            public void action() {
                JabRefExecutorService.INSTANCE.execute(new Runnable() {

                    @Override
                    public void run() {
                        BibtexEntry[] bes = mainTable.getSelectedEntries();
                        List<File> files = Util.getListOfLinkedFiles(bes,
                                metaData().getFileDirectory(Globals.FILE_FIELD));
                        for (File f : files) {
                            try {
                                JabRefDesktop.openFolderAndSelectFile(f.getAbsolutePath());
                            } catch (IOException e) {
                                LOGGER.info("Could not open folder", e);
                            }
                        }
                    }
                });
            }
        });

        actions.put(Actions.OPEN_URL, new BaseAction() {

            @Override
            public void action() {
                BibtexEntry[] bes = mainTable.getSelectedEntries();
                String field = "doi";
                if ((bes != null) && (bes.length == 1)) {
                    Object link = bes[0].getField("doi");
                    if (bes[0].getField("url") != null) {
                        link = bes[0].getField("url");
                        field = "url";
                    }
                    if (link != null) {
                        try {
                            JabRefDesktop.openExternalViewer(metaData(), link.toString(), field);
                            output(Localization.lang("External viewer called") + '.');
                        } catch (IOException ex) {
                            output(Localization.lang("Error") + ": " + ex.getMessage());
                        }
                    } else {
                        // No URL or DOI found in the "url" and "doi" fields.
                        // Look for web links in the "file" field as a fallback:
                        FileListEntry entry = null;
                        FileListTableModel tm = new FileListTableModel();
                        tm.setContent(bes[0].getField("file"));
                        for (int i = 0; i < tm.getRowCount(); i++) {
                            FileListEntry flEntry = tm.getEntry(i);
                            if ("url".equals(flEntry.getType().getName().toLowerCase())
                                    || "ps".equals(flEntry.getType().getName().toLowerCase())) {
                                entry = flEntry;
                                break;
                            }
                        }
                        if (entry != null) {
                            try {
                                JabRefDesktop.openExternalFileAnyFormat(metaData, entry.getLink(), entry.getType());
                                output(Localization.lang("External viewer called") + '.');
                            } catch (IOException e) {
                                output(Localization.lang("Could not open link"));
                                e.printStackTrace();
                            }
                        } else {
                            output(Localization.lang("No url defined") + '.');
                        }
                    }
                } else {
                    output(Localization.lang("No entries or multiple entries selected."));
                }
            }
        });

        actions.put(Actions.MERGE_DOI, (BaseAction) () -> new MergeEntryDOIDialog(BasePanel.this));

        actions.put(Actions.REPLACE_ALL, new BaseAction() {

            @Override
            public void action() {
                ReplaceStringDialog rsd = new ReplaceStringDialog(frame);
                rsd.setVisible(true);
                if (!rsd.okPressed()) {
                    return;
                }
                int counter = 0;
                NamedCompound ce = new NamedCompound(Localization.lang("Replace string"));
                if (!rsd.selOnly()) {
                    for (BibtexEntry entry : database.getEntries()) {
                        counter += rsd.replace(entry, ce);
                    }
                } else {
                    BibtexEntry[] bes = mainTable.getSelectedEntries();
                    for (BibtexEntry be : bes) {
                        counter += rsd.replace(be, ce);
                    }
                }

                output(Localization.lang("Replaced") + ' ' + counter + ' ' + (counter == 1 ?
                        // @formatter:off
                     Localization.lang("occurrence") :
                     Localization.lang("occurrences")) + '.');
                // @formatter:on
                if (counter > 0) {
                    ce.end();
                    undoManager.addEdit(ce);
                    markBaseChanged();
                }
            }
        });

        actions.put(Actions.DUPLI_CHECK,
                (BaseAction) () -> JabRefExecutorService.INSTANCE.execute(new DuplicateSearch(BasePanel.this)));

        actions.put(Actions.PLAIN_TEXT_IMPORT, new BaseAction() {

            @Override
            public void action() {
                // get Type of new entry
                EntryTypeDialog etd = new EntryTypeDialog(frame);
                PositionWindow.placeDialog(etd, BasePanel.this);
                etd.setVisible(true);
                EntryType tp = etd.getChoice();
                if (tp == null) {
                    return;
                }

                String id = IdGenerator.next();
                BibtexEntry bibEntry = new BibtexEntry(id, tp);
                TextInputDialog tidialog = new TextInputDialog(frame, BasePanel.this, "import", true, bibEntry);
                PositionWindow.placeDialog(tidialog, BasePanel.this);
                tidialog.setVisible(true);

                if (tidialog.okPressed()) {
                    Util.setAutomaticFields(Collections.singletonList(bibEntry), false, false, false);
                    insertEntry(bibEntry);
                }
            }
        });

        actions.put(Actions.MARK_ENTRIES, new MarkEntriesAction(frame, 0));

        actions.put(Actions.UNMARK_ENTRIES, new BaseAction() {

            @Override
            public void action() {
                try {
                    BibtexEntry[] bes = mainTable.getSelectedEntries();
                    if (bes.length == 0) {
                        output(Localization.lang("No entries selected."));
                        return;
                    }
                    NamedCompound ce = new NamedCompound(Localization.lang("Unmark entries"));
                    for (BibtexEntry be : bes) {
                        EntryMarker.unmarkEntry(be, false, database, ce);
                    }
                    ce.end();
                    undoManager.addEdit(ce);
                    markBaseChanged();
                    String outputStr;
                    if (bes.length == 1) {
                        outputStr = Localization.lang("Unmarked selected entry");
                    } else {
                        outputStr = Localization.lang("Unmarked all %0 selected entries", Integer.toString(bes.length));
                    }
                    output(outputStr);
                } catch (Throwable ex) {
                    ex.printStackTrace();
                }
            }
        });

        actions.put(Actions.UNMARK_ALL, (BaseAction) () -> {
            NamedCompound ce = new NamedCompound(Localization.lang("Unmark all"));

            for (BibtexEntry be : database.getEntries()) {
                EntryMarker.unmarkEntry(be, false, database, ce);
            }
            ce.end();
            undoManager.addEdit(ce);
            markBaseChanged();
            output(Localization.lang("Unmarked all entries"));
        });

        // Note that we can't put the number of entries that have been reverted into the undoText as the concrete number cannot be injected
        // @formatter:off
        actions.put(Relevance.getInstance().getValues().get(0).getActionName(),
                new SpecialFieldAction(frame, Relevance.getInstance(),
                        Relevance.getInstance().getValues().get(0).getFieldValue(), true,
                        Localization.lang("Toggle relevance"),
                        Localization.lang("Toggled relevance for %0 entries")));
        actions.put(Quality.getInstance().getValues().get(0).getActionName(),
                new SpecialFieldAction(frame, Quality.getInstance(),
                        Quality.getInstance().getValues().get(0).getFieldValue(), true,
                        Localization.lang("Toggle quality"),
                        Localization.lang("Toggled quality for %0 entries")));
        actions.put(Printed.getInstance().getValues().get(0).getActionName(), new SpecialFieldAction(frame,
                Printed.getInstance(), Printed.getInstance().getValues().get(0).getFieldValue(), true,
                Localization.lang("Toggle print status"),
                Localization.lang("Toggled print status for %0 entries")));
        // @formatter:on

        for (SpecialFieldValue prio : Priority.getInstance().getValues()) {
            actions.put(prio.getActionName(), prio.getAction(this.frame));
        }
        for (SpecialFieldValue rank : Rank.getInstance().getValues()) {
            actions.put(rank.getActionName(), rank.getAction(this.frame));
        }
        for (SpecialFieldValue status : ReadStatus.getInstance().getValues()) {
            actions.put(status.getActionName(), status.getAction(this.frame));
        }

        actions.put(Actions.TOGGLE_PREVIEW, (BaseAction) () -> {
            boolean enabled = !Globals.prefs.getBoolean(JabRefPreferences.PREVIEW_ENABLED);
            Globals.prefs.putBoolean(JabRefPreferences.PREVIEW_ENABLED, enabled);
            frame.setPreviewActive(enabled);
            frame.previewToggle.setSelected(enabled);
        });

        actions.put(Actions.TOGGLE_HIGHLIGHTS_GROUPS_MATCHING_ANY, (BaseAction) () -> {
            boolean enabled = !Globals.prefs.getBoolean(JabRefPreferences.HIGHLIGHT_GROUPS_MATCHING_ANY);
            Globals.prefs.putBoolean(JabRefPreferences.HIGHLIGHT_GROUPS_MATCHING_ANY, enabled);
            if (enabled) {
                Globals.prefs.putBoolean(JabRefPreferences.HIGHLIGHT_GROUPS_MATCHING_ALL, false);
            }
            // ping the listener so it updates:
            groupsHighlightListener.listChanged(null);
        });

        actions.put(Actions.TOGGLE_HIGHLIGHTS_GROUPS_MATCHING_ALL, (BaseAction) () -> {
            boolean enabled = !Globals.prefs.getBoolean(JabRefPreferences.HIGHLIGHT_GROUPS_MATCHING_ALL);
            Globals.prefs.putBoolean(JabRefPreferences.HIGHLIGHT_GROUPS_MATCHING_ALL, enabled);
            if (enabled) {
                Globals.prefs.putBoolean(JabRefPreferences.HIGHLIGHT_GROUPS_MATCHING_ANY, false);
            }
            // ping the listener so it updates:
            groupsHighlightListener.listChanged(null);
        });

        actions.put(Actions.SWITCH_PREVIEW, (BaseAction) selectionListener::switchPreview);

        actions.put(Actions.MANAGE_SELECTORS, (BaseAction) () -> {
            ContentSelectorDialog2 csd = new ContentSelectorDialog2
                    (frame, frame, BasePanel.this, false, metaData, null);
            PositionWindow.placeDialog(csd, frame);
            csd.setVisible(true);
        });

        actions.put(Actions.EXPORT_TO_CLIPBOARD, new ExportToClipboardAction(frame, database()));
        actions.put(Actions.SEND_AS_EMAIL, new SendAsEMailAction(frame));

        actions.put(Actions.WRITE_XMP, new WriteXMPAction(this));

        actions.put(Actions.ABBREVIATE_ISO, new AbbreviateAction(this, true));
        actions.put(Actions.ABBREVIATE_MEDLINE, new AbbreviateAction(this, false));
        actions.put(Actions.UNABBREVIATE, new UnabbreviateAction(this));
        actions.put(Actions.AUTO_SET_FILE, new SynchronizeFileField(this));

        actions.put(Actions.BACK, (BaseAction) BasePanel.this::back);
        actions.put(Actions.FORWARD, (BaseAction) BasePanel.this::forward);

        actions.put(Actions.RESOLVE_DUPLICATE_KEYS, new SearchFixDuplicateLabels(this));

        actions.put(Actions.ADD_TO_GROUP, new GroupAddRemoveDialog(this, true, false));
        actions.put(Actions.REMOVE_FROM_GROUP, new GroupAddRemoveDialog(this, false, false));
        actions.put(Actions.MOVE_TO_GROUP, new GroupAddRemoveDialog(this, true, true));

        actions.put(Actions.DOWNLOAD_FULL_TEXT, new FindFullTextAction(this));
    }

    /**
     * This method is called from JabRefFrame is a database specific action is requested by the user. Runs the command
     * if it is defined, or prints an error message to the standard error stream.
     *
     * @param _command The name of the command to run.
     */
    public void runCommand(String _command) {
        if (actions.get(_command) == null) {
            LOGGER.info("No action defined for '" + _command + '\'');
            return;
        }

        Object o = actions.get(_command);
        try {
            if (o instanceof BaseAction) {
                ((BaseAction) o).action();
            } else {
                // This part uses Spin's features:
                Worker wrk = ((AbstractWorker) o).getWorker();
                // The Worker returned by getWorker() has been wrapped
                // by Spin.off(), which makes its methods be run in
                // a different thread from the EDT.
                CallBack clb = ((AbstractWorker) o).getCallBack();

                ((AbstractWorker) o).init(); // This method runs in this same thread, the EDT.
                // Useful for initial GUI actions, like printing a message.

                // The CallBack returned by getCallBack() has been wrapped
                // by Spin.over(), which makes its methods be run on
                // the EDT.
                wrk.run(); // Runs the potentially time-consuming action
                // without freezing the GUI. The magic is that THIS line
                // of execution will not continue until run() is finished.
                clb.update(); // Runs the update() method on the EDT.
            }
        } catch (Throwable ex) {
            // If the action has blocked the JabRefFrame before crashing, we need to unblock it.
            // The call to unblock will simply hide the glasspane, so there is no harm in calling
            // it even if the frame hasn't been blocked.
            frame.unblock();
            LOGGER.error("runCommand error: " + ex.getMessage());
        }
    }

    private boolean saveDatabase(File file, boolean selectedOnly, Charset enc, FileActions.DatabaseSaveType saveType)
            throws SaveException {
        SaveSession session;
        frame.block();
        try {
            if (!selectedOnly) {
                session = FileActions.saveDatabase(database, metaData, file, Globals.prefs, false, false, enc, false);
            } else {
                session = FileActions.savePartOfDatabase(database, metaData, file, Globals.prefs,
                        mainTable.getSelectedEntries(), enc, saveType);
            }

        } catch (UnsupportedCharsetException ex2) {
            // @formatter:off
            JOptionPane.showMessageDialog(frame, Localization.lang("Could not save file.") + ' '
                            + Localization.lang("Character encoding '%0' is not supported.", enc.displayName()),
                    Localization.lang("Save database"), JOptionPane.ERROR_MESSAGE);
            // @formatter:on
            throw new SaveException("rt");
        } catch (SaveException ex) {
            if (ex.specificEntry()) {
                // Error occured during processing of
                // be. Highlight it:
                int row = mainTable.findEntry(ex.getEntry());
                int topShow = Math.max(0, row - 3);
                mainTable.setRowSelectionInterval(row, row);
                mainTable.scrollTo(topShow);
                showEntry(ex.getEntry());
            } else {
                ex.printStackTrace();
            }

            JOptionPane.showMessageDialog(frame, Localization.lang("Could not save file.") + "\n" + ex.getMessage(),
                    Localization.lang("Save database"), JOptionPane.ERROR_MESSAGE);
            throw new SaveException("rt");

        } finally {
            frame.unblock();
        }

        boolean commit = true;
        if (!session.getWriter().couldEncodeAll()) {
            FormBuilder builder = FormBuilder.create()
                    .layout(new FormLayout("left:pref, 4dlu, fill:pref", "pref, 4dlu, pref"));
            JTextArea ta = new JTextArea(session.getWriter().getProblemCharacters());
            ta.setEditable(false);
            builder.add(Localization.lang("The chosen encoding '%0' could not encode the following characters: ",
                    session.getEncoding().displayName())).xy(1, 1);
            builder.add(ta).xy(3, 1);
            builder.add(Localization.lang("What do you want to do?")).xy(1, 3);
            String tryDiff = Localization.lang("Try different encoding");
            int answer = JOptionPane.showOptionDialog(frame, builder.getPanel(), Localization.lang("Save database"),
                    JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE, null,
                    // @formatter:off
                    new String[] {Localization.lang("Save"), tryDiff,
                            Localization.lang("Cancel")}, tryDiff);
                    // @formatter:on

            if (answer == JOptionPane.NO_OPTION) {
                // The user wants to use another encoding.
                Object choice = JOptionPane.showInputDialog(frame, Localization.lang("Select encoding"),
                        Localization.lang("Save database"), JOptionPane.QUESTION_MESSAGE, null,
                        Encodings.ENCODINGS_DISPLAYNAMES,
                        enc);
                if (choice != null) {
                    Charset newEncoding = Charset.forName((String) choice);
                    return saveDatabase(file, selectedOnly, newEncoding, saveType);
                } else {
                    commit = false;
                }
            } else if (answer == JOptionPane.CANCEL_OPTION) {
                commit = false;
            }

        }

        if (commit) {
            session.commit();
            this.encoding = enc; // Make sure to remember which encoding we used.
        } else {
            session.cancel();
        }

        return commit;
    }

    /**
     * This method is called from JabRefFrame when the user wants to create a new entry. If the argument is null, the
     * user is prompted for an entry type.
     *
     * @param type The type of the entry to create.
     * @return The newly created BibtexEntry or null the operation was canceled by the user.
     */
    public BibtexEntry newEntry(EntryType type) {
        if (type == null) {
            // Find out what type is wanted.
            EntryTypeDialog etd = new EntryTypeDialog(frame);
            // We want to center the dialog, to make it look nicer.
            PositionWindow.placeDialog(etd, frame);
            etd.setVisible(true);
            type = etd.getChoice();
        }
        if (type != null) { // Only if the dialog was not cancelled.
            String id = IdGenerator.next();
            final BibtexEntry be = new BibtexEntry(id, type);
            try {
                database.insertEntry(be);
                // Set owner/timestamp if options are enabled:
                ArrayList<BibtexEntry> list = new ArrayList<>();
                list.add(be);
                Util.setAutomaticFields(list, true, true, false);

                // Create an UndoableInsertEntry object.
                undoManager.addEdit(new UndoableInsertEntry(database, be, BasePanel.this));
                output(Localization.lang("Added new '%0' entry.", type.getName().toLowerCase()));

                // We are going to select the new entry. Before that, make sure that we are in
                // show-entry mode. If we aren't already in that mode, enter the WILL_SHOW_EDITOR
                // mode which makes sure the selection will trigger display of the entry editor
                // and adjustment of the splitter.
                if (mode != BasePanel.SHOWING_EDITOR) {
                    mode = BasePanel.WILL_SHOW_EDITOR;
                }

                int row = mainTable.findEntry(be);
                if (row >= 0) {
                    highlightEntry(be); // Selects the entry. The selection listener will open the editor.
                } else {
                    // The entry is not visible in the table, perhaps due to a filtering search
                    // or group selection. Show the entry editor anyway:
                    showEntry(be);
                }

                markBaseChanged(); // The database just changed.
                new FocusRequester(getEntryEditor(be));

                return be;
            } catch (KeyCollisionException ex) {
                LOGGER.info(ex.getMessage(), ex);
            }
        }
        return null;
    }

    public SearchBar getSearchBar() {
        return searchBar;
    }

    /**
     * This listener is used to add a new entry to a group (or a set of groups) in case the Group View is selected and
     * one or more groups are marked
     */
    private class GroupTreeUpdater implements DatabaseChangeListener {

        @Override
        public void databaseChanged(DatabaseChangeEvent e) {
            if ((e.getType() == ChangeType.ADDED_ENTRY) && Globals.prefs.getBoolean(JabRefPreferences.AUTO_ASSIGN_GROUP)
                    && frame.groupToggle.isSelected()) {
                BibtexEntry[] entries = {e.getEntry()};
                TreePath[] selection = frame.groupSelector.getGroupsTree().getSelectionPaths();
                if (selection != null) {
                    // it is possible that the user selected nothing. Therefore, checked for "!= null"
                    for (TreePath tree : selection) {
                        ((GroupTreeNode) tree.getLastPathComponent()).addToGroup(entries);
                    }
                }
                //BasePanel.this.updateEntryEditorIfShowing(); // doesn't seem to be necessary
                SwingUtilities.invokeLater(new Runnable() {

                    @Override
                    public void run() {
                        BasePanel.this.getGroupSelector().valueChanged(null);
                    }
                });
            }
        }
    }

    /**
     * Ensures that the search auto completer is up to date when entries are changed AKA Let the auto completer, if any,
     * harvest words from the entry
     */
    private class SearchAutoCompleterUpdater implements DatabaseChangeListener {

        @Override
        public void databaseChanged(DatabaseChangeEvent e) {
            if ((e.getType() == ChangeType.CHANGED_ENTRY) || (e.getType() == ChangeType.ADDED_ENTRY)) {
                searchAutoCompleter.addBibtexEntry(e.getEntry());
            }
        }
    }

    /**
     * Ensures that auto completers are up to date when entries are changed AKA Let the auto completer, if any, harvest
     * words from the entry
     */
    private class AutoCompletersUpdater implements DatabaseChangeListener {

        @Override
        public void databaseChanged(DatabaseChangeEvent e) {
            if ((e.getType() == ChangeType.CHANGED_ENTRY) || (e.getType() == ChangeType.ADDED_ENTRY)) {
                BasePanel.this.autoCompleters.addEntry(e.getEntry());
            }
        }
    }

    /**
     * This method is called from JabRefFrame when the user wants to create a new entry.
     *
     * @param bibEntry The new entry.
     */
    public void insertEntry(BibtexEntry bibEntry) {
        if (bibEntry != null) {
            try {
                database.insertEntry(bibEntry);
                if (Globals.prefs.getBoolean(JabRefPreferences.USE_OWNER)) {
                    // Set owner field to default value
                    Util.setAutomaticFields(bibEntry, true, true);
                }
                // Create an UndoableInsertEntry object.
                undoManager.addEdit(new UndoableInsertEntry(database, bibEntry, BasePanel.this));
                output(Localization.lang("Added new '%0' entry.", bibEntry.getType().getName().toLowerCase()));

                markBaseChanged(); // The database just changed.
                if (Globals.prefs.getBoolean(JabRefPreferences.AUTO_OPEN_FORM)) {
                    selectionListener.editSignalled(bibEntry);
                }
                highlightEntry(bibEntry);
            } catch (KeyCollisionException ex) {
                LOGGER.info("Collision for bibtex key" + bibEntry.getId(), ex);
            }
        }
    }

    public void editEntryByKeyAndFocusField(String bibtexKey, String fieldName) {
        BibtexEntry[] entries = database.getEntriesByKey(bibtexKey);
        if (entries.length == 1) {
            mainTable.setSelected(mainTable.findEntry(entries[0]));
            selectionListener.editSignalled();
            EntryEditor editor = getEntryEditor(entries[0]);
            editor.setFocusToField(fieldName);
            new FocusRequester(editor);
        }
    }

    public void updateTableFont() {
        mainTable.updateFont();
    }

    private void createMainTable() {
        //Comparator comp = new FieldComparator("author");

        GlazedEntrySorter eventList = new GlazedEntrySorter(database.getEntryMap());
        // Must initialize sort columns somehow:

        database.addDatabaseChangeListener(eventList);
        database.addDatabaseChangeListener(SpecialFieldDatabaseChangeListener.getInstance());
        groupFilterList = new FilterList<>(eventList.getTheList(), EverythingMatcher.INSTANCE);
        if (filterGroupToggle != null) {
            filterGroupToggle.updateFilterList(groupFilterList);
        }
        searchFilterList = new FilterList<>(groupFilterList, EverythingMatcher.INSTANCE);
        if (filterSearchToggle != null) {
            filterSearchToggle.updateFilterList(searchFilterList);
        }
        tableFormat = new MainTableFormat(this);
        tableFormat.updateTableFormat();
        mainTable = new MainTable(tableFormat, searchFilterList, frame, this);

        selectionListener = new MainTableSelectionListener(this, mainTable);
        mainTable.updateFont();
        mainTable.addSelectionListener(selectionListener);
        mainTable.addMouseListener(selectionListener);
        mainTable.addKeyListener(selectionListener);
        mainTable.addFocusListener(selectionListener);

        // Add the listener that will take care of highlighting groups as the selection changes:
        groupsHighlightListener = new ListEventListener<BibtexEntry>() {

            @Override
            public void listChanged(ListEvent<BibtexEntry> listEvent) {
                if (Globals.prefs.getBoolean(JabRefPreferences.HIGHLIGHT_GROUPS_MATCHING_ANY)) {
                    getGroupSelector().showMatchingGroups(mainTable.getSelectedEntries(), false);
                } else if (Globals.prefs.getBoolean(JabRefPreferences.HIGHLIGHT_GROUPS_MATCHING_ALL)) {
                    getGroupSelector().showMatchingGroups(mainTable.getSelectedEntries(), true);
                } else {
                    // no highlight
                    getGroupSelector().showMatchingGroups(null, true);
                }
            }
        };
        mainTable.addSelectionListener(groupsHighlightListener);

        mainTable.getActionMap().put(Actions.CUT, new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    runCommand(Actions.CUT);
                } catch (Throwable ex) {
                    ex.printStackTrace();
                }
            }
        });
        mainTable.getActionMap().put(Actions.COPY, new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    runCommand(Actions.COPY);
                } catch (Throwable ex) {
                    ex.printStackTrace();
                }
            }
        });
        mainTable.getActionMap().put(Actions.PASTE, new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    runCommand(Actions.PASTE);
                } catch (Throwable ex) {
                    ex.printStackTrace();
                }
            }
        });

        mainTable.addKeyListener(new KeyAdapter() {

            @Override
            public void keyPressed(KeyEvent e) {
                final int keyCode = e.getKeyCode();
                final TreePath path = frame.groupSelector.getSelectionPath();
                final GroupTreeNode node = path == null ? null : (GroupTreeNode) path.getLastPathComponent();

                if (e.isControlDown()) {
                    switch (keyCode) {
                    // The up/down/left/rightkeystrokes are displayed in the
                    // GroupSelector's popup menu, so if they are to be changed,
                    // edit GroupSelector.java accordingly!
                    case KeyEvent.VK_UP:
                        e.consume();
                        if (node != null) {
                            frame.groupSelector.moveNodeUp(node, true);
                        }
                        break;
                    case KeyEvent.VK_DOWN:
                        e.consume();
                        if (node != null) {
                            frame.groupSelector.moveNodeDown(node, true);
                        }
                        break;
                    case KeyEvent.VK_LEFT:
                        e.consume();
                        if (node != null) {
                            frame.groupSelector.moveNodeLeft(node, true);
                        }
                        break;
                    case KeyEvent.VK_RIGHT:
                        e.consume();
                        if (node != null) {
                            frame.groupSelector.moveNodeRight(node, true);
                        }
                        break;
                    case KeyEvent.VK_PAGE_DOWN:
                        frame.nextTab.actionPerformed(null);
                        e.consume();
                        break;
                    case KeyEvent.VK_PAGE_UP:
                        frame.prevTab.actionPerformed(null);
                        e.consume();
                        break;
                    }
                } else if (keyCode == KeyEvent.VK_ENTER) {
                    e.consume();
                    try {
                        runCommand(Actions.EDIT);
                    } catch (Throwable ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });
    }

    public void setupMainPanel() {
        //System.out.println("setupMainPanel");
        //splitPane = new com.jgoodies.uif_lite.component.UIFSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setDividerSize(GUIGlobals.SPLIT_PANE_DIVIDER_SIZE);
        // We replace the default FocusTraversalPolicy with a subclass
        // that only allows FieldEditor components to gain keyboard focus,
        // if there is an entry editor open.
        /*splitPane.setFocusTraversalPolicy(new LayoutFocusTraversalPolicy() {
                protected boolean accept(Component c) {
                    if (showing == null)
                        return super.accept(c);
                    else
                        return (super.accept(c) &&
                                (c instanceof FieldEditor));
                }
                });*/

        // check whether a mainTable already existed and a floatSearch was active
        boolean floatSearchActive = (mainTable != null) && isShowingFloatSearch();

        createMainTable();

        for (EntryEditor ee : entryEditors.values()) {
            ee.validateAllFields();
        }

        splitPane.setTopComponent(mainTable.getPane());

        // Remove borders
        splitPane.setBorder(BorderFactory.createEmptyBorder());
        setBorder(BorderFactory.createEmptyBorder());

        //setupTable();
        // If an entry is currently being shown, make sure it stays shown,
        // otherwise set the bottom component to null.
        if (mode == BasePanel.SHOWING_PREVIEW) {
            mode = BasePanel.SHOWING_NOTHING;
            int row = mainTable.findEntry(currentPreview.getEntry());
            if (row >= 0) {
                mainTable.setRowSelectionInterval(row, row);
            }

        } else if (mode == BasePanel.SHOWING_EDITOR) {
            mode = BasePanel.SHOWING_NOTHING;
            /*int row = mainTable.findEntry(currentEditor.entry);
            if (row >= 0)
                mainTable.setRowSelectionInterval(row, row);
            */
            //showEntryEditor(currentEditor);
        } else {
            splitPane.setBottomComponent(null);
        }

        setLayout(new BorderLayout());
        removeAll();
        add(searchBar, BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);

        // Set up name autocompleter for search:
        //if (!Globals.prefs.getBoolean("searchAutoComplete")) {
        instantiateSearchAutoCompleter();
        this.getDatabase().addDatabaseChangeListener(new SearchAutoCompleterUpdater());

        // Set up AutoCompleters for this panel:
        if (Globals.prefs.getBoolean(JabRefPreferences.AUTO_COMPLETE)) {
            autoCompleters = new ContentAutoCompleters(getDatabase(), metaData);
            // ensure that the autocompleters are in sync with entries
            this.getDatabase().addDatabaseChangeListener(new AutoCompletersUpdater());
        } else {
            // create empty ContentAutoCompleters() if autoCompletion is deactivated
            autoCompleters = new ContentAutoCompleters();
        }

        // restore floating search result
        // (needed if preferences have been changed which causes a recreation of the main table)
        if (floatSearchActive) {
            startShowingFloatSearch();
        }

        splitPane.revalidate();
        revalidate();
        repaint();
    }

    public void updateSearchManager() {
        searchBar.setAutoCompleter(searchAutoCompleter);
    }

    private void instantiateSearchAutoCompleter() {
        searchAutoCompleter = AutoCompleterFactory.getFor("author", "editor");
        for (BibtexEntry entry : database.getEntries()) {
            searchAutoCompleter.addBibtexEntry(entry);
        }
    }

    public void updatePreamble() {
        if (preambleEditor != null) {
            preambleEditor.updatePreamble();
        }
    }

    public void assureStringDialogNotEditing() {
        if (stringDialog != null) {
            stringDialog.assureNotEditing();
        }
    }

    public void updateStringDialog() {
        if (stringDialog != null) {
            stringDialog.refreshTable();
        }
    }

    public void adjustSplitter() {
        if (getMode() == BasePanel.SHOWING_PREVIEW) {
            splitPane.setDividerLocation(
                    splitPane.getHeight() - Globals.prefs.getInt(JabRefPreferences.PREVIEW_PANEL_HEIGHT));
        } else {
            splitPane.setDividerLocation(
                    splitPane.getHeight() - Globals.prefs.getInt(JabRefPreferences.ENTRY_EDITOR_HEIGHT));

        }
    }

    /**
     * Stores the source view in the entry editor, if one is open, has the source view selected and the source has been
     * edited.
     *
     * @return boolean false if there is a validation error in the source panel, true otherwise.
     */
    public boolean entryEditorAllowsChange() {
        Component c = splitPane.getBottomComponent();
        if (c instanceof EntryEditor) {
            return ((EntryEditor) c).lastSourceAccepted();
        } else {
            return true;
        }
    }

    private boolean isShowingEditor() {
        return (splitPane.getBottomComponent() != null) && (splitPane.getBottomComponent() instanceof EntryEditor);
    }

    public void showEntry(final BibtexEntry be) {

        if (getShowing() == be) {
            if (splitPane.getBottomComponent() == null) {
                // This is the special occasion when showing is set to an
                // entry, but no entry editor is in fact shown. This happens
                // after Preferences dialog is closed, and it means that we
                // must make sure the same entry is shown again. We do this by
                // setting showing to null, and recursively calling this method.
                newEntryShowing(null);
                showEntry(be);
            } else {
                // The correct entry is already being shown. Make sure the editor
                // is updated.
                ((EntryEditor) splitPane.getBottomComponent()).updateAllFields();

            }
            return;

        }

        EntryEditor form;
        int divLoc = -1;
        String visName = null;
        if (getShowing() != null) {
            if (isShowingEditor()) {
                visName = ((EntryEditor) splitPane.getBottomComponent()).getVisiblePanelName();
            }
        }
        if (getShowing() != null) {
            divLoc = splitPane.getDividerLocation();
        }

        if (entryEditors.containsKey(be.getType().getName())) {
            // We already have an editor for this entry type.
            form = entryEditors.get(be.getType().getName());
            form.switchTo(be);
            if (visName != null) {
                form.setVisiblePanel(visName);
            }
            splitPane.setBottomComponent(form);
            //highlightEntry(be);
        } else {
            // We must instantiate a new editor for this type.
            form = new EntryEditor(frame, BasePanel.this, be);
            if (visName != null) {
                form.setVisiblePanel(visName);
            }
            splitPane.setBottomComponent(form);

            //highlightEntry(be);
            entryEditors.put(be.getType().getName(), form);

        }
        if (divLoc > 0) {
            splitPane.setDividerLocation(divLoc);
        } else {
            splitPane.setDividerLocation(
                    splitPane.getHeight() - Globals.prefs.getInt(JabRefPreferences.ENTRY_EDITOR_HEIGHT));
        }

        newEntryShowing(be);
        setEntryEditorEnabled(true); // Make sure it is enabled.
    }

    /**
     * Get an entry editor ready to edit the given entry. If an appropriate editor is already cached, it will be updated
     * and returned.
     *
     * @param entry The entry to be edited.
     * @return A suitable entry editor.
     */
    public EntryEditor getEntryEditor(BibtexEntry entry) {
        EntryEditor form;
        if (entryEditors.containsKey(entry.getType().getName())) {
            EntryEditor visibleNow = currentEditor;

            // We already have an editor for this entry type.
            form = entryEditors.get(entry.getType().getName());

            // If the cached editor is not the same as the currently shown one,
            // make sure the current one stores its current edit:
            if ((visibleNow != null) && (form != visibleNow)) {
                visibleNow.storeCurrentEdit();
            }

            form.switchTo(entry);
            //if (visName != null)
            //    form.setVisiblePanel(visName);
        } else {
            // We must instantiate a new editor for this type. First make sure the old one
            // stores its last edit:
            storeCurrentEdit();
            // Then start the new one:
            form = new EntryEditor(frame, BasePanel.this, entry);
            //if (visName != null)
            //    form.setVisiblePanel(visName);

            entryEditors.put(entry.getType().getName(), form);
        }
        return form;
    }

    public EntryEditor getCurrentEditor() {
        return currentEditor;
    }

    /**
     * Sets the given entry editor as the bottom component in the split pane. If an entry editor already was shown,
     * makes sure that the divider doesn't move. Updates the mode to SHOWING_EDITOR.
     *
     * @param editor The entry editor to add.
     */
    public void showEntryEditor(EntryEditor editor) {
        if (mode == BasePanel.SHOWING_EDITOR) {
            Globals.prefs.putInt(JabRefPreferences.ENTRY_EDITOR_HEIGHT,
                    splitPane.getHeight() - splitPane.getDividerLocation());
        } else if (mode == BasePanel.SHOWING_PREVIEW) {
            Globals.prefs.putInt(JabRefPreferences.PREVIEW_PANEL_HEIGHT,
                    splitPane.getHeight() - splitPane.getDividerLocation());
        }
        mode = BasePanel.SHOWING_EDITOR;
        currentEditor = editor;
        splitPane.setBottomComponent(editor);
        if (editor.getEntry() != getShowing()) {
            newEntryShowing(editor.getEntry());
        }
        adjustSplitter();

    }

    /**
     * Sets the given preview panel as the bottom component in the split panel. Updates the mode to SHOWING_PREVIEW.
     *
     * @param preview The preview to show.
     */
    public void showPreview(PreviewPanel preview) {
        mode = BasePanel.SHOWING_PREVIEW;
        currentPreview = preview;
        splitPane.setBottomComponent(preview);
    }

    /**
     * Removes the bottom component.
     */
    public void hideBottomComponent() {
        mode = BasePanel.SHOWING_NOTHING;
        splitPane.setBottomComponent(null);
    }

    /**
     * This method selects the given entry, and scrolls it into view in the table. If an entryEditor is shown, it is
     * given focus afterwards.
     */
    public void highlightEntry(final BibtexEntry be) {
        final int row = mainTable.findEntry(be);
        if (row >= 0) {
            mainTable.setRowSelectionInterval(row, row);
            //entryTable.setActiveRow(row);
            mainTable.ensureVisible(row);
        }
    }

    /**
     * This method is called from an EntryEditor when it should be closed. We relay to the selection listener, which
     * takes care of the rest.
     *
     * @param editor The entry editor to close.
     */
    public void entryEditorClosing(EntryEditor editor) {
        // Store divider location for next time:
        Globals.prefs.putInt(JabRefPreferences.ENTRY_EDITOR_HEIGHT,
                splitPane.getHeight() - splitPane.getDividerLocation());
        selectionListener.entryEditorClosing(editor);
    }

    /**
     * Closes the entry editor if it is showing the given entry.
     *
     * @param be a <code>BibtexEntry</code> value
     */
    public void ensureNotShowing(BibtexEntry be) {
        if ((mode == BasePanel.SHOWING_EDITOR) && (currentEditor.getEntry() == be)) {
            selectionListener.entryEditorClosing(currentEditor);
        }
    }

    public void updateEntryEditorIfShowing() {
        if (mode == BasePanel.SHOWING_EDITOR) {
            if (currentEditor.getType() != currentEditor.getEntry().getType()) {
                // The entry has changed type, so we must get a new editor.
                newEntryShowing(null);
                EntryEditor newEditor = getEntryEditor(currentEditor.getEntry());
                showEntryEditor(newEditor);
            } else {
                currentEditor.updateAllFields();
                currentEditor.updateSource();
            }
        }
    }

    /**
     * If an entry editor is showing, make sure its currently focused field stores its changes, if any.
     */
    public void storeCurrentEdit() {
        if (isShowingEditor()) {
            EntryEditor editor = (EntryEditor) splitPane.getBottomComponent();
            editor.storeCurrentEdit();
        }

    }

    /**
     * This method iterates through all existing entry editors in this BasePanel, telling each to update all its
     * instances of FieldContentSelector. This is done to ensure that the list of words in each selector is up-to-date
     * after the user has made changes in the Manage dialog.
     */
    public void updateAllContentSelectors() {
        for (Map.Entry<String, EntryEditor> stringEntryEditorEntry : entryEditors.entrySet()) {
            EntryEditor ed = stringEntryEditorEntry.getValue();
            ed.updateAllContentSelectors();
        }
    }

    public void rebuildAllEntryEditors() {
        for (Map.Entry<String, EntryEditor> stringEntryEditorEntry : entryEditors.entrySet()) {
            EntryEditor ed = stringEntryEditorEntry.getValue();
            ed.rebuildPanels();
        }

    }

    public void markBaseChanged() {
        baseChanged = true;

        // Put an asterix behind the filename to indicate the
        // database has changed.
        String oldTitle = frame.getTabTitle(this);
        if (!oldTitle.endsWith("*")) {
            frame.setTabTitle(this, oldTitle + '*', frame.getTabTooltip(this));
            frame.setWindowTitle();
        }
        // If the status line states that the base has been saved, we
        // remove this message, since it is no longer relevant. If a
        // different message is shown, we leave it.
        if (frame.statusLine.getText().startsWith(Localization.lang("Saved database"))) {
            frame.output(" ");
        }

    }

    public void markNonUndoableBaseChanged() {
        nonUndoableChange = true;
        markBaseChanged();
    }

    private synchronized void markChangedOrUnChanged() {
        if (undoManager.hasChanged()) {
            if (!baseChanged) {
                markBaseChanged();
            }
        } else if (baseChanged && !nonUndoableChange) {
            baseChanged = false;
            if (getDatabaseFile() != null) {
                frame.setTabTitle(BasePanel.this, getDatabaseFile().getName(), getDatabaseFile().getAbsolutePath());
            } else {
                frame.setTabTitle(BasePanel.this, GUIGlobals.untitledTitle, null);
            }
        }
        frame.setWindowTitle();
    }

    /**
     * Selects a single entry, and scrolls the table to center it.
     *
     * @param pos Current position of entry to select.
     */
    public void selectSingleEntry(int pos) {
        mainTable.clearSelection();
        mainTable.addRowSelectionInterval(pos, pos);
        mainTable.scrollToCenter(pos, 0);
    }

    public StartStopListAction<BibtexEntry> getFilterSearchToggle() {
        return filterSearchToggle;
    }

    public StartStopListAction<BibtexEntry> getFilterGroupToggle() {
        return filterGroupToggle;
    }

    public static class StartStopListAction<E> {
        private FilterList<E> list;
        private final Matcher<E> active;
        private final Matcher<E> inactive;

        private boolean isActive = false;

        private StartStopListAction(FilterList<E> list, Matcher<E> active, Matcher<E> inactive) {
            this.list = list;
            this.active = active;
            this.inactive = inactive;
        }

        public void start() {
            if (!isActive) {
                list.setMatcher(active);
                isActive = true;
            }
        }

        public void stop() {
            if (isActive) {
                list.setMatcher(inactive);
                isActive = false;
            }
        }

        public boolean isActive() {
            return isActive;
        }

        public void updateFilterList(FilterList<E> filterList) {
            list = filterList;
            if(isActive) {
                list.setMatcher(active);
            } else {
                list.setMatcher(inactive);
            }
        }
    }


    /**
     * Query whether this BasePanel is in the mode where a float search result is shown.
     *
     * @return true if showing float search, false otherwise.
     */
    public boolean isShowingFloatSearch() {
        return mainTable.isFloatSearchActive();
    }

    public void stopShowingFloatSearch() {
        mainTable.stopShowingFloatSearch();
    }

    public void startShowingFloatSearch() {
        mainTable.showFloatSearch();
    }

    public BibtexDatabase getDatabase() {
        return database;
    }

    public void preambleEditorClosing() {
        preambleEditor = null;
    }

    public void stringsClosing() {
        stringDialog = null;
    }

    public void changeType(BibtexEntry entry, EntryType type) {
        changeType(new BibtexEntry[] {entry}, type);
    }

    public void changeType(EntryType type) {
        BibtexEntry[] bes = mainTable.getSelectedEntries();
        changeType(bes, type);
    }

    private void changeType(BibtexEntry[] bes, EntryType type) {

        if ((bes == null) || (bes.length == 0)) {
            LOGGER.error("At least one entry must be selected to be able to change the type.");
            return;
        }
        if (bes.length > 1) {
            int choice = JOptionPane.showConfirmDialog(this,
                    // @formatter:off
                    Localization.lang("Multiple entries selected. Do you want to change\nthe type of all these to '%0'?", type.getName()),
                    Localization.lang("Change entry type"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                    // @formatter:on
            if (choice == JOptionPane.NO_OPTION) {
                return;
            }
        }

        NamedCompound ce = new NamedCompound(Localization.lang("Change entry type"));
        for (BibtexEntry be : bes) {
            ce.addEdit(new UndoableChangeType(be, be.getType(), type));
            be.setType(type);
        }

        // @formatter:off
        output(formatOutputMessage(Localization.lang("Changed type to '%0' for", type.getName()), bes.length));
        // @formatter:on
        ce.end();
        undoManager.addEdit(ce);
        markBaseChanged();
        updateEntryEditorIfShowing();
    }

    public boolean showDeleteConfirmationDialog(int numberOfEntries) {
        if (Globals.prefs.getBoolean(JabRefPreferences.CONFIRM_DELETE)) {
            String msg;
            // @formatter:off
            msg = Localization.lang("Really delete the selected entry?");
            // @formatter:on
            String title = Localization.lang("Delete entry");
            if (numberOfEntries > 1) {
                msg = Localization.lang("Really delete the selected %0 entries?", Integer.toString(numberOfEntries));
                title = Localization.lang("Delete multiple entries");
            }

            CheckBoxMessage cb = new CheckBoxMessage(msg, Localization.lang("Disable this confirmation dialog"), false);

            int answer = JOptionPane.showConfirmDialog(frame, cb, title, JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE);
            if (cb.isSelected()) {
                Globals.prefs.putBoolean(JabRefPreferences.CONFIRM_DELETE, false);
            }
            return answer == JOptionPane.YES_OPTION;
        } else {
            return true;
        }

    }

    /**
     * If the relevant option is set, autogenerate keys for all entries that are lacking keys.
     */
    public void autoGenerateKeysBeforeSaving() {
        if (Globals.prefs.getBoolean(JabRefPreferences.GENERATE_KEYS_BEFORE_SAVING)) {
            NamedCompound ce = new NamedCompound(Localization.lang("autogenerate keys"));
            boolean any = false;

            for (BibtexEntry bes : database.getEntries()) {
                String oldKey = bes.getCiteKey();
                if ((oldKey == null) || oldKey.isEmpty()) {
                    LabelPatternUtil.makeLabel(metaData, database, bes);
                    ce.addEdit(new UndoableKeyChange(database, bes.getId(), null, bes.getCiteKey()));
                    any = true;
                }
            }
            // Store undo information, if any:
            if (any) {
                ce.end();
                undoManager.addEdit(ce);
            }
        }
    }

    /**
     * Activates or deactivates the entry preview, depending on the argument. When deactivating, makes sure that any
     * visible preview is hidden.
     *
     * @param enabled
     */
    public void setPreviewActive(boolean enabled) {
        selectionListener.setPreviewActive(enabled);
    }

    public void setSelectionListenerEnabled(boolean enabled) {
        selectionListener.setEnabled(enabled);
    }

    /**
     * Depending on whether a preview or an entry editor is showing, save the current divider location in the correct
     * preference setting.
     */
    public void saveDividerLocation() {
        if (mode == BasePanel.SHOWING_PREVIEW) {
            Globals.prefs.putInt(JabRefPreferences.PREVIEW_PANEL_HEIGHT,
                    splitPane.getHeight() - splitPane.getDividerLocation());
        } else if (mode == BasePanel.SHOWING_EDITOR) {
            Globals.prefs.putInt(JabRefPreferences.ENTRY_EDITOR_HEIGHT,
                    splitPane.getHeight() - splitPane.getDividerLocation());
        }
    }

    class UndoAction implements BaseAction {

        @Override
        public void action() {
            try {
                JComponent focused = Globals.focusListener.getFocused();
                if ((focused != null) && (focused instanceof FieldEditor) && focused.hasFocus()) {
                    // User is currently editing a field:
                    // Check if it is the preamble:
                    if ((preambleEditor != null) && (focused == preambleEditor.getFieldEditor())) {
                        preambleEditor.storeCurrentEdit();
                    } else {
                        storeCurrentEdit();
                    }
                }
                String name = undoManager.getUndoPresentationName();
                undoManager.undo();
                markBaseChanged();
                frame.output(name);
            } catch (CannotUndoException ex) {
                ex.printStackTrace();
                frame.output(Localization.lang("Nothing to undo") + '.');
            }
            // After everything, enable/disable the undo/redo actions
            // appropriately.
            //updateUndoState();
            //redoAction.updateRedoState();
            markChangedOrUnChanged();
        }
    }

    class RedoAction implements BaseAction {

        @Override
        public void action() {
            try {

                JComponent focused = Globals.focusListener.getFocused();
                if ((focused != null) && (focused instanceof FieldEditor) && focused.hasFocus()) {
                    // User is currently editing a field:
                    storeCurrentEdit();
                }

                String name = undoManager.getRedoPresentationName();
                undoManager.redo();
                markBaseChanged();
                frame.output(name);
            } catch (CannotRedoException ex) {
                frame.output(Localization.lang("Nothing to redo") + '.');
            }
            // After everything, enable/disable the undo/redo actions
            // appropriately.
            //updateRedoState();
            //undoAction.updateUndoState();
            markChangedOrUnChanged();
        }
    }

    // Method pertaining to the ClipboardOwner interface.
    @Override
    public void lostOwnership(Clipboard clipboard, Transferable contents) {
        // Nothing
    }

    private void setEntryEditorEnabled(boolean enabled) {
        if ((getShowing() != null) && (splitPane.getBottomComponent() instanceof EntryEditor)) {
            EntryEditor ed = (EntryEditor) splitPane.getBottomComponent();
            if (ed.isEnabled() != enabled) {
                ed.setEnabled(enabled);
            }
        }
    }

    public String fileMonitorHandle() {
        return fileMonitorHandle;
    }

    @Override
    public void fileUpdated() {
        if (saving) {
            return; // We are just saving the file, so this message is most likely due
        }
        //if (updatedExternally) {
        //  return;
        //}
        // to bad timing. If not, we'll handle it on the next polling.
        //LOGGER.debug("File '"+file.getPath()+"' has been modified.");
        updatedExternally = true;

        final ChangeScanner scanner = new ChangeScanner(frame, BasePanel.this, BasePanel.this.getDatabaseFile());

        // Adding the sidepane component is Swing work, so we must do this in the Swing
        // thread:
        Runnable t = new Runnable() {

            @Override
            public void run() {

                // Check if there is already a notification about external
                // changes:
                boolean hasAlready = sidePaneManager.hasComponent(FileUpdatePanel.NAME);
                if (hasAlready) {
                    sidePaneManager.hideComponent(FileUpdatePanel.NAME);
                    sidePaneManager.unregisterComponent(FileUpdatePanel.NAME);
                }
                FileUpdatePanel pan = new FileUpdatePanel(frame, BasePanel.this, sidePaneManager, getDatabaseFile(), scanner);
                sidePaneManager.register(FileUpdatePanel.NAME, pan);
                sidePaneManager.show(FileUpdatePanel.NAME);
                //setUpdatedExternally(false);
                //scanner.displayResult();
            }
        };

        // Test: running scan automatically in background
        if ((BasePanel.this.getDatabaseFile() != null) && !FileBasedLock.waitForFileLock(BasePanel.this.getDatabaseFile(), 10)) {
            // The file is locked even after the maximum wait. Do nothing.
            LOGGER.error("File updated externally, but change scan failed because the file is locked.");
            // Perturb the stored timestamp so successive checks are made:
            Globals.fileUpdateMonitor.perturbTimestamp(getFileMonitorHandle());
            return;
        }

        JabRefExecutorService.INSTANCE.executeWithLowPriorityInOwnThreadAndWait(scanner);

        if (scanner.changesFound()) {
            SwingUtilities.invokeLater(t);
        } else {
            setUpdatedExternally(false);
            //System.out.println("No changes found.");
        }
    }

    @Override
    public void fileRemoved() {
        LOGGER.info("File '" + getDatabaseFile().getPath() + "' has been deleted.");
    }

    /**
     * Perform necessary cleanup when this BasePanel is closed.
     */
    public void cleanUp() {
        if (fileMonitorHandle != null) {
            Globals.fileUpdateMonitor.removeUpdateListener(fileMonitorHandle);
        }
        // Check if there is a FileUpdatePanel for this BasePanel being shown. If so,
        // remove it:
        if (sidePaneManager.hasComponent("fileUpdate")) {
            FileUpdatePanel fup = (FileUpdatePanel) sidePaneManager.getComponent("fileUpdate");
            if (fup.getPanel() == this) {
                sidePaneManager.hideComponent("fileUpdate");
            }
        }
    }

    public void setUpdatedExternally(boolean b) {
        updatedExternally = b;
    }

    /**
     * Get an array containing the currently selected entries. The array is stable and not changed if the selection
     * changes
     *
     * @return An array containing the selected entries. Is never null.
     */
    public BibtexEntry[] getSelectedEntries() {
        return mainTable.getSelectedEntries();
    }

    /**
     * Get the file where this database was last saved to or loaded from, if any.
     *
     * @return The relevant File, or null if none is defined.
     */
    public File getDatabaseFile() {
        return metaData.getFile();
    }

    /**
     * Get a String containing a comma-separated list of the bibtex keys of the selected entries.
     *
     * @return A comma-separated list of the keys of the selected entries.
     */
    public String getKeysForSelection() {
        StringBuilder result = new StringBuilder();
        String citeKey;//, message = "";
        boolean first = true;
        for (BibtexEntry bes : mainTable.getSelected()) {
            citeKey = bes.getCiteKey();
            // if the key is empty we give a warning and ignore this entry
            if ((citeKey == null) || citeKey.isEmpty()) {
                continue;
            }
            if (first) {
                result.append(citeKey);
                first = false;
            } else {
                result.append(',').append(citeKey);
            }
        }
        return result.toString();
    }

    public GroupSelector getGroupSelector() {
        return frame.groupSelector;
    }

    public boolean isUpdatedExternally() {
        return updatedExternally;
    }

    public String getFileMonitorHandle() {
        return fileMonitorHandle;
    }

    public void setFileMonitorHandle(String fileMonitorHandle) {
        this.fileMonitorHandle = fileMonitorHandle;
    }

    public SidePaneManager getSidePaneManager() {
        return sidePaneManager;
    }

    public void setNonUndoableChange(boolean nonUndoableChange) {
        this.nonUndoableChange = nonUndoableChange;
    }

    public void setBaseChanged(boolean baseChanged) {
        this.baseChanged = baseChanged;
    }

    public void setSaving(boolean saving) {
        this.saving = saving;
    }

    public boolean isSaving() {
        return saving;
    }

    private BibtexEntry getShowing() {
        return showing;
    }

    /**
     * Update the pointer to the currently shown entry in all cases where the user has moved to a new entry, except when
     * using Back and Forward commands. Also updates history for Back command, and clears history for Forward command.
     *
     * @param entry The entry that is now to be shown.
     */
    public void newEntryShowing(BibtexEntry entry) {
        // If this call is the result of a Back or Forward operation, we must take
        // care not to make any history changes, since the necessary changes will
        // already have been done in the back() or forward() method:
        if (backOrForwardInProgress) {
            showing = entry;
            backOrForwardInProgress = false;
            setBackAndForwardEnabledState();
            return;
        }
        nextEntries.clear();
        if (entry != showing) {
            // Add the entry we are leaving to the history:
            if (showing != null) {
                previousEntries.add(showing);
                if (previousEntries.size() > GUIGlobals.MAX_BACK_HISTORY_SIZE) {
                    previousEntries.remove(0);
                }
            }
            showing = entry;
            setBackAndForwardEnabledState();
        }

    }

    /**
     * Go back (if there is any recorded history) and update the histories for the Back and Forward commands.
     */
    private void back() {
        if (!previousEntries.isEmpty()) {
            BibtexEntry toShow = previousEntries.get(previousEntries.size() - 1);
            previousEntries.remove(previousEntries.size() - 1);
            // Add the entry we are going back from to the Forward history:
            if (showing != null) {
                nextEntries.add(showing);
            }
            backOrForwardInProgress = true; // to avoid the history getting updated erroneously
            highlightEntry(toShow);
        }
    }

    private void forward() {
        if (!nextEntries.isEmpty()) {
            BibtexEntry toShow = nextEntries.get(nextEntries.size() - 1);
            nextEntries.remove(nextEntries.size() - 1);
            // Add the entry we are going forward from to the Back history:
            if (showing != null) {
                previousEntries.add(showing);
            }
            backOrForwardInProgress = true; // to avoid the history getting updated erroneously
            highlightEntry(toShow);
        }
    }

    public void setBackAndForwardEnabledState() {
        frame.back.setEnabled(!previousEntries.isEmpty());
        frame.forward.setEnabled(!nextEntries.isEmpty());
    }

    private String formatOutputMessage(String start, int count) {
        // @formatter:off
        return String.format("%s %d %s.", start, count, (count > 1 ? Localization.lang("entries") :
            Localization.lang("entry")));
        // @formatter:on
    }

    private class SaveSelectedAction implements BaseAction {

        private final DatabaseSaveType saveType;

        public SaveSelectedAction(DatabaseSaveType saveType) {
            this.saveType = saveType;
        }

        @Override
        public void action() throws SaveException {

            String chosenFile = FileDialogs.getNewFile(frame,
                    new File(Globals.prefs.get(JabRefPreferences.WORKING_DIRECTORY)), ".bib", JFileChooser.SAVE_DIALOG,
                    false);
            if (chosenFile != null) {
                File expFile = new File(chosenFile);
                if (!expFile.exists() || (JOptionPane.showConfirmDialog(frame,
                        Localization.lang("'%0' exists. Overwrite file?", expFile.getName()),
                        Localization.lang("Save database"), JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION)) {
                    saveDatabase(expFile, true, Globals.prefs.getDefaultEncoding(),
                            saveType);
                    frame.getFileHistory().newFile(expFile.getPath());
                    frame.output(Localization.lang("Saved selected to '%0'.", expFile.getPath()));
                }
            }
        }
    }

    private static class SearchAndOpenFile {

        private final BibtexEntry entry;
        private final BasePanel basePanel;

        public SearchAndOpenFile(BibtexEntry entry, BasePanel basePanel) {
            this.entry = entry;
            this.basePanel = basePanel;
        }

        public Optional<String> searchAndOpen() {
            if (!Globals.prefs.getBoolean(JabRefPreferences.RUN_AUTOMATIC_FILE_SEARCH)) {
                return Optional.empty();
            }

            /*  The search can lead to an unexpected 100% CPU usage which is perceived
                as a bug, if the search incidentally starts at a directory with lots
                of stuff below. It is now disabled by default. */

            // see if we can fall back to a filename based on the bibtex key
            final Collection<BibtexEntry> entries = Collections.singleton(entry);

            ExternalFileType[] types = Globals.prefs.getExternalFileTypeSelection();
            ArrayList<File> dirs = new ArrayList<>();
            if (basePanel.metaData.getFileDirectory(Globals.FILE_FIELD).length > 0) {
                String[] mdDirs = basePanel.metaData.getFileDirectory(Globals.FILE_FIELD);
                for (String mdDir : mdDirs) {
                    dirs.add(new File(mdDir));

                }
            }
            Collection<String> extensions = new ArrayList<>();
            for (final ExternalFileType type : types) {
                extensions.add(type.getExtension());
            }
            // Run the search operation:
            Map<BibtexEntry, List<File>> result;
            if (Globals.prefs.getBoolean(JabRefPreferences.AUTOLINK_USE_REG_EXP_SEARCH_KEY)) {
                String regExp = Globals.prefs.get(JabRefPreferences.REG_EXP_SEARCH_EXPRESSION_KEY);
                result = RegExpFileSearch.findFilesForSet(entries, extensions, dirs, regExp);
            } else {
                result = Util.findAssociatedFiles(entries, extensions, dirs);
            }
            if (result.get(entry) != null) {
                List<File> res = result.get(entry);
                if (!res.isEmpty()) {
                    String filepath = res.get(0).getPath();
                    int index = filepath.lastIndexOf('.');
                    if ((index >= 0) && (index < (filepath.length() - 1))) {
                        String extension = filepath.substring(index + 1);
                        ExternalFileType type = Globals.prefs.getExternalFileTypeByExt(extension);
                        if (type != null) {
                            try {
                                JabRefDesktop.openExternalFileAnyFormat(basePanel.metaData, filepath, type);
                                basePanel.output(Localization.lang("External viewer called") + '.');
                                return Optional.of(filepath);
                            } catch (IOException ex) {
                                basePanel.output(Localization.lang("Error") + ": " + ex.getMessage());
                            }
                        }
                    }
                }
            }

        return Optional.empty();
    }
}
}
