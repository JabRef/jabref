/*  Copyright (C) 2003-2016 JabRef contributors.
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
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.tree.TreePath;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import net.sf.jabref.BibDatabaseContext;
import net.sf.jabref.Globals;
import net.sf.jabref.HighlightMatchingGroupPreferences;
import net.sf.jabref.JabRefExecutorService;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.collab.ChangeScanner;
import net.sf.jabref.collab.FileUpdateListener;
import net.sf.jabref.collab.FileUpdatePanel;
import net.sf.jabref.exporter.BibDatabaseWriter;
import net.sf.jabref.exporter.ExportToClipboardAction;
import net.sf.jabref.exporter.SaveDatabaseAction;
import net.sf.jabref.exporter.SaveException;
import net.sf.jabref.exporter.SavePreferences;
import net.sf.jabref.exporter.SaveSession;
import net.sf.jabref.external.AttachFileAction;
import net.sf.jabref.external.ExternalFileMenuItem;
import net.sf.jabref.external.ExternalFileType;
import net.sf.jabref.external.ExternalFileTypes;
import net.sf.jabref.external.FindFullTextAction;
import net.sf.jabref.external.RegExpFileSearch;
import net.sf.jabref.external.SynchronizeFileField;
import net.sf.jabref.external.WriteXMPAction;
import net.sf.jabref.groups.GroupSelector;
import net.sf.jabref.groups.GroupTreeNode;
import net.sf.jabref.gui.actions.Actions;
import net.sf.jabref.gui.actions.BaseAction;
import net.sf.jabref.gui.actions.CleanupAction;
import net.sf.jabref.gui.desktop.JabRefDesktop;
import net.sf.jabref.gui.entryeditor.EntryEditor;
import net.sf.jabref.gui.fieldeditors.FieldEditor;
import net.sf.jabref.gui.journals.AbbreviateAction;
import net.sf.jabref.gui.journals.UnabbreviateAction;
import net.sf.jabref.gui.labelpattern.SearchFixDuplicateLabels;
import net.sf.jabref.gui.maintable.MainTable;
import net.sf.jabref.gui.maintable.MainTableDataModel;
import net.sf.jabref.gui.maintable.MainTableFormat;
import net.sf.jabref.gui.maintable.MainTableSelectionListener;
import net.sf.jabref.gui.mergeentries.MergeEntriesDialog;
import net.sf.jabref.gui.mergeentries.MergeEntryDOIDialog;
import net.sf.jabref.gui.plaintextimport.TextInputDialog;
import net.sf.jabref.gui.search.SearchBar;
import net.sf.jabref.gui.undo.CountingUndoManager;
import net.sf.jabref.gui.undo.NamedCompound;
import net.sf.jabref.gui.undo.UndoableChangeType;
import net.sf.jabref.gui.undo.UndoableFieldChange;
import net.sf.jabref.gui.undo.UndoableInsertEntry;
import net.sf.jabref.gui.undo.UndoableKeyChange;
import net.sf.jabref.gui.undo.UndoableRemoveEntry;
import net.sf.jabref.gui.util.FocusRequester;
import net.sf.jabref.gui.util.component.CheckBoxMessage;
import net.sf.jabref.gui.worker.AbstractWorker;
import net.sf.jabref.gui.worker.CallBack;
import net.sf.jabref.gui.worker.MarkEntriesAction;
import net.sf.jabref.gui.worker.SendAsEMailAction;
import net.sf.jabref.gui.worker.Worker;
import net.sf.jabref.importer.AppendDatabaseAction;
import net.sf.jabref.importer.fileformat.BibtexParser;
import net.sf.jabref.logic.FieldChange;
import net.sf.jabref.logic.autocompleter.AutoCompletePreferences;
import net.sf.jabref.logic.autocompleter.AutoCompleter;
import net.sf.jabref.logic.autocompleter.AutoCompleterFactory;
import net.sf.jabref.logic.autocompleter.ContentAutoCompleters;
import net.sf.jabref.logic.l10n.Encodings;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.labelpattern.LabelPatternUtil;
import net.sf.jabref.logic.layout.Layout;
import net.sf.jabref.logic.layout.LayoutHelper;
import net.sf.jabref.logic.util.UpdateField;
import net.sf.jabref.logic.util.io.FileBasedLock;
import net.sf.jabref.logic.util.io.FileUtil;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.database.DatabaseChangeEvent;
import net.sf.jabref.model.database.DatabaseChangeEvent.ChangeType;
import net.sf.jabref.model.database.DatabaseChangeListener;
import net.sf.jabref.model.database.KeyCollisionException;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.EntryType;
import net.sf.jabref.model.entry.IdGenerator;
import net.sf.jabref.specialfields.Printed;
import net.sf.jabref.specialfields.Priority;
import net.sf.jabref.specialfields.Quality;
import net.sf.jabref.specialfields.Rank;
import net.sf.jabref.specialfields.ReadStatus;
import net.sf.jabref.specialfields.Relevance;
import net.sf.jabref.specialfields.SpecialFieldAction;
import net.sf.jabref.specialfields.SpecialFieldDatabaseChangeListener;
import net.sf.jabref.specialfields.SpecialFieldValue;
import net.sf.jabref.sql.DBConnectDialog;
import net.sf.jabref.sql.DBExporterAndImporterFactory;
import net.sf.jabref.sql.DBStrings;
import net.sf.jabref.sql.DbConnectAction;
import net.sf.jabref.sql.SQLUtil;
import net.sf.jabref.sql.exporter.DBExporter;

import ca.odell.glazedlists.event.ListEventListener;
import com.jgoodies.forms.builder.FormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class BasePanel extends JPanel implements ClipboardOwner, FileUpdateListener {

    private static final Log LOGGER = LogFactory.getLog(BasePanel.class);

    private final BibDatabase database;
    private final BibDatabaseContext bibDatabaseContext;
    private final MainTableDataModel tableModel;

    // To contain instantiated entry editors. This is to save time
    // As most enums, this must not be null
    private BasePanelMode mode = BasePanelMode.SHOWING_NOTHING;
    private EntryEditor currentEditor;

    private PreviewPanel currentPreview;
    private MainTableSelectionListener selectionListener;

    private ListEventListener<BibEntry> groupsHighlightListener;

    private JSplitPane splitPane;

    private final JabRefFrame frame;
    private String fileMonitorHandle;
    private boolean saving;
    private boolean updatedExternally;

    private Charset encoding;

    // AutoCompleter used in the search bar
    private AutoCompleter<String> searchAutoCompleter;
    // The undo manager.
    public final CountingUndoManager undoManager = new CountingUndoManager(this);
    private final UndoAction undoAction = new UndoAction();

    private final RedoAction redoAction = new RedoAction();
    private final List<BibEntry> previousEntries = new ArrayList<>();

    private final List<BibEntry> nextEntries = new ArrayList<>();
    private boolean baseChanged;
    private boolean nonUndoableChange;

    // Used to track whether the base has changed since last save.
    public MainTable mainTable;

    public MainTableFormat tableFormat;

    private BibEntry showing;

    // Variable to prevent erroneous update of back/forward histories at the time
    // when a Back or Forward operation is being processed:
    private boolean backOrForwardInProgress;
    // To indicate which entry is currently shown.
    public final Map<String, EntryEditor> entryEditors = new HashMap<>();

    // in switching between entries.
    private PreambleEditor preambleEditor;

    // Keeps track of the preamble dialog if it is open.
    private StringDialog stringDialog;

    // Keeps track of the string dialog if it is open.
    private final Map<String, Object> actions = new HashMap<>();

    private final SidePaneManager sidePaneManager;

    private final SearchBar searchBar;
    private ContentAutoCompleters autoCompleters;

    public BasePanel(JabRefFrame frame, BibDatabaseContext bibDatabaseContext, Charset encoding) {
        Objects.requireNonNull(frame);
        Objects.requireNonNull(encoding);
        Objects.requireNonNull(bibDatabaseContext);

        this.encoding = encoding;
        this.bibDatabaseContext = bibDatabaseContext;

        this.sidePaneManager = frame.getSidePaneManager();
        this.frame = frame;
        this.database = bibDatabaseContext.getDatabase();
        this.tableModel = new MainTableDataModel(getBibDatabaseContext());

        searchBar = new SearchBar(this);

        setupMainPanel();

        setupActions();

        File file = bibDatabaseContext.getDatabaseFile();

        // ensure that at each addition of a new entry, the entry is added to the groups interface
        this.database.addDatabaseChangeListener(new GroupTreeUpdater());

        if (file == null) {
            if (database.hasEntries()) {
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
    }

    // Returns a collection of AutoCompleters, which are populated from the current database
    public ContentAutoCompleters getAutoCompleters() {
        return autoCompleters;
    }

    public String getTabTitle() {
        StringBuilder title = new StringBuilder();

        if (getBibDatabaseContext().getDatabaseFile() == null) {
            title.append(GUIGlobals.UNTITLED_TITLE);

            if (getDatabase().hasEntries()) {
                // if the database is not empty and no file is assigned,
                // the database came from an import and has to be treated somehow
                // -> mark as changed
                // This also happens internally at basepanel to ensure consistency line 224
                title.append('*');
            }
        } else {
            // check if file is modified
            String changeFlag = isModified() ? "*" : "";
            title.append(getBibDatabaseContext().getDatabaseFile().getName()).append(changeFlag);
        }
        return title.toString();
    }

    public boolean isModified() {
        return baseChanged;
    }

    public BasePanelMode getMode() {
        return mode;
    }

    public void setMode(BasePanelMode mode) {
        this.mode = mode;
    }

    public JabRefFrame frame() {
        return frame;
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
        SaveDatabaseAction saveAction = new SaveDatabaseAction(this);
        CleanupAction cleanUpAction = new CleanupAction(this, Globals.prefs);

        actions.put(Actions.UNDO, undoAction);
        actions.put(Actions.REDO, redoAction);

        actions.put(Actions.FOCUS_TABLE, (BaseAction) () -> new FocusRequester(mainTable));

        // The action for opening an entry editor.
        actions.put(Actions.EDIT, (BaseAction) selectionListener::editSignalled);

        // The action for saving a database.
        actions.put(Actions.SAVE, saveAction);

        actions.put(Actions.SAVE_AS, (BaseAction) saveAction::saveAs);

        actions.put(Actions.SAVE_SELECTED_AS, new SaveSelectedAction(SavePreferences.DatabaseSaveType.ALL));

        actions.put(Actions.SAVE_SELECTED_AS_PLAIN,
                new SaveSelectedAction(SavePreferences.DatabaseSaveType.PLAIN_BIBTEX));

        // The action for copying selected entries.
        actions.put(Actions.COPY, (BaseAction) () -> {
            List<BibEntry> bes = mainTable.getSelectedEntries();

            if (bes.isEmpty()) {
                // The user maybe selected a single cell.
                // TODO: Check if this can actually happen
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
            } else {
                TransferableBibtexEntry trbe = new TransferableBibtexEntry(bes);
                // ! look at ClipBoardManager
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(trbe, BasePanel.this);
                output(formatOutputMessage(Localization.lang("Copied"), bes.size()));
            }
        });

        //when you modify this action be sure to adjust Actions.DELETE
        //they are the same except of the Localization, delete confirmation and Actions.COPY call
        actions.put(Actions.CUT, (BaseAction) () -> {
            runCommand(Actions.COPY);
            List<BibEntry> entries = mainTable.getSelectedEntries();
            if (entries.isEmpty()) {
                return;
            }

            NamedCompound compound = new NamedCompound(
                    (entries.size() > 1 ? Localization.lang("cut entries") : Localization.lang("cut entry")));
            for (BibEntry entry : entries) {
                compound.addEdit(new UndoableRemoveEntry(database, entry, BasePanel.this));
                database.removeEntry(entry);
                ensureNotShowingBottomPanel(entry);
            }
            compound.end();
            undoManager.addEdit(compound);

            frame.output(formatOutputMessage(Localization.lang("Cut"), entries.size()));
            markBaseChanged();
        });

        //when you modify this action be sure to adjust Actions.CUT,
        //they are the same except of the Localization, delete confirmation and Actions.COPY call
        actions.put(Actions.DELETE, (BaseAction) () -> {
            List<BibEntry> entries = mainTable.getSelectedEntries();
            if (entries.isEmpty()) {
                return;
            }
            if (!showDeleteConfirmationDialog(entries.size())) {
                return;
            }

            NamedCompound compound = new NamedCompound(
                    (entries.size() > 1 ? Localization.lang("delete entries") : Localization.lang("delete entry")));
            for (BibEntry entry : entries) {
                compound.addEdit(new UndoableRemoveEntry(database, entry, BasePanel.this));
                database.removeEntry(entry);
                ensureNotShowingBottomPanel(entry);
            }
            compound.end();
            undoManager.addEdit(compound);

            markBaseChanged();
            frame.output(formatOutputMessage(Localization.lang("Deleted"), entries.size()));
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
                Collection<BibEntry> bes = null;
                if (content.isDataFlavorSupported(TransferableBibtexEntry.entryFlavor)) {
                    // We have determined that the clipboard data is a set of entries.
                    try {
                        bes = (Collection<BibEntry>) content.getTransferData(TransferableBibtexEntry.entryFlavor);
                    } catch (UnsupportedFlavorException | ClassCastException ex) {
                        LOGGER.warn("Could not paste this type", ex);
                    } catch (IOException ex) {
                        LOGGER.warn("Could not paste", ex);
                    }
                } else if (content.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                    try {
                        BibtexParser bp = new BibtexParser(
                                new StringReader((String) content.getTransferData(DataFlavor.stringFlavor)));
                        BibDatabase db = bp.parse().getDatabase();
                        LOGGER.info("Parsed " + db.getEntryCount() + " entries from clipboard text");
                        if (db.hasEntries()) {
                            bes = db.getEntries();
                        }
                    } catch (UnsupportedFlavorException ex) {
                        LOGGER.warn("Could not paste this type", ex);
                    } catch (Throwable ex) {
                        LOGGER.warn("Could not paste", ex);
                    }

                }

                // finally we paste in the entries (if any), which either came from TransferableBibtexEntries
                // or were parsed from a string
                if ((bes != null) && (!bes.isEmpty())) {

                    NamedCompound ce = new NamedCompound(
                            (bes.size() > 1 ? Localization.lang("paste entries") : Localization.lang("paste entry")));

                    // Store the first inserted bibtexentry.
                    // bes[0] does not work as bes[0] is first clonded,
                    // then inserted.
                    // This entry is used to open up an entry editor
                    // for the first inserted entry.
                    BibEntry firstBE = null;

                    for (BibEntry be1 : bes) {

                        BibEntry be = (BibEntry) be1.clone();
                        if (firstBE == null) {
                            firstBE = be;
                        }
                        UpdateField.setAutomaticFields(be, Globals.prefs.getBoolean(JabRefPreferences.OVERWRITE_OWNER),
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
                    output(formatOutputMessage(Localization.lang("Pasted"), bes.size()));
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
                PreambleEditor form = new PreambleEditor(frame, BasePanel.this, database);
                form.setLocationRelativeTo(frame);
                form.setVisible(true);
                preambleEditor = form;
            } else {
                preambleEditor.setVisible(true);
            }

        });

        // The action for opening the string editor
        actions.put(Actions.EDIT_STRINGS, (BaseAction) () -> {
            if (stringDialog == null) {
                StringDialog form = new StringDialog(frame, BasePanel.this, database);
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

            String errorMessage = "";
            boolean connectedToDB;


            // run first, in EDT:
            @Override
            public void init() {

                DBStrings dbs = bibDatabaseContext.getMetaData().getDBStrings();

                // get DBStrings from user if necessary
                if (dbs.isConfigValid()) {
                    connectedToDB = true;
                } else {
                    // init DB strings if necessary
                    if (!dbs.isInitialized()) {
                        dbs.initialize();
                    }

                    // show connection dialog
                    DBConnectDialog dbd = new DBConnectDialog(frame(), dbs);
                    dbd.setLocationRelativeTo(BasePanel.this);
                    dbd.setVisible(true);

                    connectedToDB = dbd.isConnectedToDB();

                    // store database strings
                    if (connectedToDB) {
                        dbs = dbd.getDBStrings();
                        bibDatabaseContext.getMetaData().setDBStrings(dbs);
                        dbd.dispose();
                    }
                }
            }

            // run second, on a different thread:
            @Override
            public void run() {

                if (connectedToDB) {

                    final DBStrings dbs = bibDatabaseContext.getMetaData().getDBStrings();

                    try {
                        frame.output(Localization.lang("Attempting SQL export..."));
                        final DBExporterAndImporterFactory factory = new DBExporterAndImporterFactory();
                        final DBExporter exporter = factory.getExporter(dbs.getServerType());
                        exporter.exportDatabaseToDBMS(bibDatabaseContext, getDatabase().getEntries(), dbs, frame);
                        dbs.isConfigValid(true);
                    } catch (Exception ex) {
                        final String preamble = Localization
                                .lang("Could not export to SQL database for the following reason:");
                        errorMessage = SQLUtil.getExceptionMessage(ex);
                        LOGGER.info("Could not export to SQL database", ex);
                        dbs.isConfigValid(false);
                        JOptionPane.showMessageDialog(frame, preamble + '\n' + errorMessage,
                                Localization.lang("Export to SQL database"), JOptionPane.ERROR_MESSAGE);
                    }

                    bibDatabaseContext.getMetaData().setDBStrings(dbs);
                }
            }

            // run third, on EDT:
            @Override
            public void update() {

                // if no error, report success
                if (errorMessage.isEmpty()) {
                    if (connectedToDB) {
                        final DBStrings dbs = bibDatabaseContext.getMetaData().getDBStrings();
                        frame.output(Localization.lang("%0 export successful", dbs.getServerType()));
                    }
                } else { // show an error dialog if an error occurred
                    final String preamble = Localization
                            .lang("Could not export to SQL database for the following reason:");
                    frame.output(preamble + "  " + errorMessage);

                    JOptionPane.showMessageDialog(frame, preamble + '\n' + errorMessage,
                            Localization.lang("Export to SQL database"), JOptionPane.ERROR_MESSAGE);

                    errorMessage = "";
                }
            }

        });

        actions.put(FindUnlinkedFilesDialog.ACTION_COMMAND, (BaseAction) () -> {
            final FindUnlinkedFilesDialog dialog = new FindUnlinkedFilesDialog(frame, frame, BasePanel.this);
            dialog.setLocationRelativeTo(frame);
            dialog.setVisible(true);
        });

        // The action for auto-generating keys.
        actions.put(Actions.MAKE_KEY, new AbstractWorker() {

            List<BibEntry> entries;
            int numSelected;
            boolean cancelled;


            // Run first, in EDT:
            @Override
            public void init() {
                entries = getSelectedEntries();
                numSelected = entries.size();

                if (entries.isEmpty()) { // None selected. Inform the user to select entries first.
                    JOptionPane.showMessageDialog(frame,
                            Localization.lang("First select the entries you want keys to be generated for."),
                            Localization.lang("Autogenerate BibTeX keys"), JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
                frame.block();
                output(formatOutputMessage(Localization.lang("Generating BibTeX key for"), numSelected));
            }

            // Run second, on a different thread:
            @Override
            public void run() {
                BibEntry bes;

                // First check if any entries have keys set already. If so, possibly remove
                // them from consideration, or warn about overwriting keys.
                // This is a partial clone of net.sf.jabref.gui.entryeditor.EntryEditor.GenerateKeyAction.actionPerformed(ActionEvent)
                for (final Iterator<BibEntry> i = entries.iterator(); i.hasNext();) {
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
                            final int answer = JOptionPane.showConfirmDialog(frame, cbm,
                                    Localization.lang("Overwrite keys"), JOptionPane.YES_NO_OPTION);
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

                HashMap<BibEntry, Object> oldvals = new HashMap<>();
                // Iterate again, removing already set keys. This is skipped if overwriting
                // is disabled, since all entries with keys set will have been removed.
                if (!Globals.prefs.getBoolean(JabRefPreferences.AVOID_OVERWRITING_KEY)) {
                    for (BibEntry entry : entries) {
                        bes = entry;
                        // Store the old value:
                        oldvals.put(bes, bes.getField(BibEntry.KEY_FIELD));
                        database.setCiteKeyForEntry(bes, null);
                    }
                }

                final NamedCompound ce = new NamedCompound(Localization.lang("Autogenerate BibTeX keys"));

                // Finally, set the new keys:
                for (BibEntry entry : entries) {
                    bes = entry;
                    LabelPatternUtil.makeLabel(bibDatabaseContext.getMetaData(), database, bes);
                    ce.addEdit(new UndoableKeyChange(database, bes, (String) oldvals.get(bes),
                            bes.getField(BibEntry.KEY_FIELD)));
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
                for (final BibEntry bibEntry : entries) {
                    SwingUtilities.invokeLater(() -> {
                        final int row = mainTable.findEntry(bibEntry);
                        if ((row >= 0) && (mainTable.getSelectedRowCount() < entries.size())) {
                            mainTable.addRowSelectionInterval(row, row);
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
            List<BibEntry> bes = mainTable.getSelectedEntries();
            if (!bes.isEmpty()) {
                storeCurrentEdit();
                List<String> keys = new ArrayList<>(bes.size());
                // Collect all non-null keys.
                for (BibEntry be : bes) {
                    if (be.getCiteKey() != null) {
                        keys.add(be.getCiteKey());
                    }
                }
                if (keys.isEmpty()) {
                    output(Localization.lang("None of the selected entries have BibTeX keys."));
                    return;
                }

                StringSelection ss = new StringSelection(String.join(",", keys));
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(ss, BasePanel.this);

                if (keys.size() == bes.size()) {
                    // All entries had keys.
                    output((bes.size() > 1 ? Localization.lang("Copied keys") : Localization.lang("Copied key")) + '.');
                } else {
                    output(Localization.lang("Warning: %0 out of %1 entries have undefined BibTeX key.",
                            Integer.toString(bes.size() - keys.size()), Integer.toString(bes.size())));
                }
            }
        });

        // The action for copying a cite for the selected entry.
        actions.put(Actions.COPY_CITE_KEY, (BaseAction) () -> {
            List<BibEntry> bes = mainTable.getSelectedEntries();
            if (!bes.isEmpty()) {
                storeCurrentEdit();
                List<String> keys = new ArrayList<>(bes.size());
                // Collect all non-null keys.
                for (BibEntry be : bes) {
                    if (be.getCiteKey() != null) {
                        keys.add(be.getCiteKey());
                    }
                }
                if (keys.isEmpty()) {
                    output(Localization.lang("None of the selected entries have BibTeX keys."));
                    return;
                }

                String sb = String.join(",", keys);
                StringSelection ss = new StringSelection("\\cite{" + sb + '}');
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(ss, BasePanel.this);

                if (keys.size() == bes.size()) {
                    // All entries had keys.
                    output(bes.size() > 1 ? Localization.lang("Copied keys") : Localization.lang("Copied key") + '.');
                } else {
                    output(Localization.lang("Warning: %0 out of %1 entries have undefined BibTeX key.",
                            Integer.toString(bes.size() - keys.size()), Integer.toString(bes.size())));
                }
            }
        });

        // The action for copying the BibTeX key and the title for the first selected entry
        actions.put(Actions.COPY_KEY_AND_TITLE, (BaseAction) () -> {
            List<BibEntry> bes = mainTable.getSelectedEntries();
            if (!bes.isEmpty()) {
                storeCurrentEdit();

                // OK: in a future version, this string should be configurable to allow arbitrary exports
                StringReader sr = new StringReader(
                        "\\bibtexkey - \\begin{title}\\format[RemoveBrackets]{\\title}\\end{title}\n");
                Layout layout;
                try {
                    layout = new LayoutHelper(sr, Globals.journalAbbreviationLoader.getRepository())
                            .getLayoutFromText();
                } catch (IOException e) {
                    LOGGER.info("Could not get layout", e);
                    return;
                }

                StringBuilder sb = new StringBuilder();

                int copied = 0;
                // Collect all non-null keys.
                for (BibEntry be : bes) {
                    if (be.getCiteKey() != null) {
                        copied++;
                        sb.append(layout.doLayout(be, database));
                    }
                }

                if (copied == 0) {
                    output(Localization.lang("None of the selected entries have BibTeX keys."));
                    return;
                }

                final StringSelection ss = new StringSelection(sb.toString());
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(ss, BasePanel.this);

                if (copied == bes.size()) {
                    // All entries had keys.
                    output((bes.size() > 1 ? Localization.lang("Copied keys") : Localization.lang("Copied key")) + '.');
                } else {
                    output(Localization.lang("Warning: %0 out of %1 entries have undefined BibTeX key.",
                            Integer.toString(bes.size() - copied), Integer.toString(bes.size())));
                }
            }
        });

        actions.put(Actions.MERGE_DATABASE, new AppendDatabaseAction(frame, this));

        actions.put(Actions.ADD_FILE_LINK, new AttachFileAction(this));

        actions.put(Actions.OPEN_EXTERNAL_FILE,
                (BaseAction) () -> JabRefExecutorService.INSTANCE.execute(() -> {
                    final List<BibEntry> bes = mainTable.getSelectedEntries();
                    if (bes.size() != 1) {
                        output(Localization.lang("This operation requires exactly one item to be selected."));
                        return;
                    }

                    final BibEntry entry = bes.get(0);
                    if (!entry.hasField(Globals.FILE_FIELD)) {
                        // no bibtex field
                        new SearchAndOpenFile(entry, BasePanel.this).searchAndOpen();
                        return;
                    }
                    FileListTableModel tableModel = new FileListTableModel();
                    tableModel.setContent(entry.getField(Globals.FILE_FIELD));
                    if (tableModel.getRowCount() == 0) {
                        // content in bibtex field is not readable
                        new SearchAndOpenFile(entry, BasePanel.this).searchAndOpen();
                        return;
                    }
                    FileListEntry flEntry = tableModel.getEntry(0);
                    ExternalFileMenuItem item = new ExternalFileMenuItem(frame(), entry, "", flEntry.link,
                            flEntry.type.get().getIcon(), bibDatabaseContext, flEntry.type);
                    item.openLink();
                }));

        actions.put(Actions.OPEN_FOLDER, (BaseAction) () -> {
            JabRefExecutorService.INSTANCE.execute(() -> {
                final List<File> files = FileUtil.getListOfLinkedFiles(mainTable.getSelectedEntries(),
                        bibDatabaseContext.getFileDirectory());
                for (final File f : files) {
                    try {
                        JabRefDesktop.openFolderAndSelectFile(f.getAbsolutePath());
                    } catch (IOException e) {
                        LOGGER.info("Could not open folder", e);
                    }
                }
            });
        });

        actions.put(Actions.OPEN_CONSOLE, (BaseAction) () -> JabRefDesktop
                .openConsole(frame.getCurrentBasePanel().getBibDatabaseContext().getDatabaseFile()));

        actions.put(Actions.OPEN_URL, new OpenURLAction());

        actions.put(Actions.MERGE_DOI, (BaseAction) () -> new MergeEntryDOIDialog(BasePanel.this));

        actions.put(Actions.REPLACE_ALL, (BaseAction) () -> {
            final ReplaceStringDialog rsd = new ReplaceStringDialog(frame);
            rsd.setVisible(true);
            if (!rsd.okPressed()) {
                return;
            }
            int counter = 0;
            final NamedCompound ce = new NamedCompound(Localization.lang("Replace string"));
            if (rsd.selOnly()) {
                for (BibEntry be : mainTable.getSelectedEntries()) {
                    counter += rsd.replace(be, ce);
                }
            } else {
                for (BibEntry entry : database.getEntries()) {
                    counter += rsd.replace(entry, ce);
                }
            }

            output(Localization.lang("Replaced") + ' ' + counter + ' '
                    + (counter == 1 ? Localization.lang("occurrence") : Localization.lang("occurrences")) + '.');
            if (counter > 0) {
                ce.end();
                undoManager.addEdit(ce);
                markBaseChanged();
            }
        });

        actions.put(Actions.DUPLI_CHECK,
                (BaseAction) () -> JabRefExecutorService.INSTANCE.execute(new DuplicateSearch(BasePanel.this)));

        actions.put(Actions.PLAIN_TEXT_IMPORT, (BaseAction) () -> {
            // get Type of new entry
            EntryTypeDialog etd = new EntryTypeDialog(frame);
            etd.setLocationRelativeTo(BasePanel.this);
            etd.setVisible(true);
            EntryType tp = etd.getChoice();
            if (tp == null) {
                return;
            }

            String id = IdGenerator.next();
            BibEntry bibEntry = new BibEntry(id, tp.getName());
            TextInputDialog tidialog = new TextInputDialog(frame, bibEntry);
            tidialog.setLocationRelativeTo(BasePanel.this);
            tidialog.setVisible(true);

            if (tidialog.okPressed()) {
                UpdateField.setAutomaticFields(Collections.singletonList(bibEntry), false, false);
                insertEntry(bibEntry);
            }
        });

        actions.put(Actions.MARK_ENTRIES, new MarkEntriesAction(frame, 0));

        actions.put(Actions.UNMARK_ENTRIES, (BaseAction) () -> {
            try {
                List<BibEntry> bes = mainTable.getSelectedEntries();
                if (bes.isEmpty()) {
                    output(Localization.lang("This operation requires one or more entries to be selected."));
                    return;
                }
                NamedCompound ce = new NamedCompound(Localization.lang("Unmark entries"));
                for (BibEntry be : bes) {
                    EntryMarker.unmarkEntry(be, false, database, ce);
                }
                ce.end();
                undoManager.addEdit(ce);
                markBaseChanged();
                String outputStr;
                if (bes.size() == 1) {
                    outputStr = Localization.lang("Unmarked selected entry");
                } else {
                    outputStr = Localization.lang("Unmarked all %0 selected entries", Integer.toString(bes.size()));
                }
                output(outputStr);
            } catch (Throwable ex) {
                LOGGER.warn("Could not unmark", ex);
            }
        });

        actions.put(Actions.UNMARK_ALL, (BaseAction) () -> {
            NamedCompound ce = new NamedCompound(Localization.lang("Unmark all"));

            for (BibEntry be : database.getEntries()) {
                EntryMarker.unmarkEntry(be, false, database, ce);
            }
            ce.end();
            undoManager.addEdit(ce);
            markBaseChanged();
            output(Localization.lang("Unmarked all entries"));
        });

        // Note that we can't put the number of entries that have been reverted into the undoText as the concrete number cannot be injected
        actions.put(Relevance.getInstance().getValues().get(0).getActionName(),
                new SpecialFieldAction(frame, Relevance.getInstance(),
                        Relevance.getInstance().getValues().get(0).getFieldValue().get(), true,
                        Localization.lang("Toggle relevance"), Localization.lang("Toggled relevance for %0 entries")));
        actions.put(Quality.getInstance().getValues().get(0).getActionName(),
                new SpecialFieldAction(frame, Quality.getInstance(),
                        Quality.getInstance().getValues().get(0).getFieldValue().get(), true,
                        Localization.lang("Toggle quality assured"),
                        Localization.lang("Toggled quality for %0 entries")));
        actions.put(Printed.getInstance().getValues().get(0).getActionName(), new SpecialFieldAction(frame,
                Printed.getInstance(), Printed.getInstance().getValues().get(0).getFieldValue().get(), true,
                Localization.lang("Toggle print status"), Localization.lang("Toggled print status for %0 entries")));

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
            setPreviewActiveBasePanels(enabled);
            frame.setPreviewToggle(enabled);
        });

        actions.put(Actions.TOGGLE_HIGHLIGHTS_GROUPS_MATCHING_ANY, (BaseAction) () -> {
            new HighlightMatchingGroupPreferences(Globals.prefs).setToAny();
            // ping the listener so it updates:
            groupsHighlightListener.listChanged(null);
        });

        actions.put(Actions.TOGGLE_HIGHLIGHTS_GROUPS_MATCHING_ALL, (BaseAction) () -> {
            new HighlightMatchingGroupPreferences(Globals.prefs).setToAll();
            // ping the listener so it updates:
            groupsHighlightListener.listChanged(null);
        });

        actions.put(Actions.TOGGLE_HIGHLIGHTS_GROUPS_MATCHING_DISABLE, (BaseAction) () -> {
            new HighlightMatchingGroupPreferences(Globals.prefs).setToDisabled();
            // ping the listener so it updates:
            groupsHighlightListener.listChanged(null);
        });

        actions.put(Actions.SWITCH_PREVIEW, (BaseAction) selectionListener::switchPreview);

        actions.put(Actions.MANAGE_SELECTORS, (BaseAction) () -> {
            ContentSelectorDialog2 csd = new ContentSelectorDialog2(frame, frame, BasePanel.this, false,
                    bibDatabaseContext.getMetaData(), null);
            csd.setLocationRelativeTo(frame);
            csd.setVisible(true);
        });

        actions.put(Actions.EXPORT_TO_CLIPBOARD, new ExportToClipboardAction(frame, getDatabase()));
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
     * This method is called from JabRefFrame if a database specific action is requested by the user. Runs the command
     * if it is defined, or prints an error message to the standard error stream.
     *
     * @param _command The name of the command to run.
     */
    public void runCommand(final String _command) {
        if (!actions.containsKey(_command)) {
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
            LOGGER.error("runCommand error: " + ex.getMessage(), ex);
        }
    }

    private boolean saveDatabase(File file, boolean selectedOnly, Charset enc,
            SavePreferences.DatabaseSaveType saveType) throws SaveException {
        SaveSession session;
        frame.block();
        final String SAVE_DATABASE = Localization.lang("Save database");
        try {
            SavePreferences prefs = SavePreferences.loadForSaveFromPreferences(Globals.prefs).withEncoding(enc)
                    .withSaveType(saveType);
            BibDatabaseWriter databaseWriter = new BibDatabaseWriter();
            if (selectedOnly) {
                session = databaseWriter.savePartOfDatabase(bibDatabaseContext, prefs, mainTable.getSelectedEntries());
            } else {
                session = databaseWriter.saveDatabase(bibDatabaseContext, prefs);
            }

            registerUndoableChanges(session);

        } catch (UnsupportedCharsetException ex2) {
            JOptionPane.showMessageDialog(frame,
                    Localization.lang("Could not save file.") + ' '
                            + Localization.lang("Character encoding '%0' is not supported.", enc.displayName()),
                    SAVE_DATABASE, JOptionPane.ERROR_MESSAGE);
            throw new SaveException("rt");
        } catch (SaveException ex) {
            if (ex.specificEntry()) {
                // Error occurred during processing of
                // be. Highlight it:
                final int row = mainTable.findEntry(ex.getEntry());
                final int topShow = Math.max(0, row - 3);
                mainTable.setRowSelectionInterval(row, row);
                mainTable.scrollTo(topShow);
                showEntry(ex.getEntry());
            } else {
                LOGGER.warn("Could not save", ex);
            }

            JOptionPane.showMessageDialog(frame, Localization.lang("Could not save file.") + "\n" + ex.getMessage(),
                    SAVE_DATABASE, JOptionPane.ERROR_MESSAGE);
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
            builder.add(Localization.lang("The chosen encoding '%0' could not encode the following characters:",
                    session.getEncoding().displayName())).xy(1, 1);
            builder.add(ta).xy(3, 1);
            builder.add(Localization.lang("What do you want to do?")).xy(1, 3);
            String tryDiff = Localization.lang("Try different encoding");
            int answer = JOptionPane.showOptionDialog(frame, builder.getPanel(), SAVE_DATABASE,
                    JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE, null,
                    new String[] {Localization.lang("Save"), tryDiff, Localization.lang("Cancel")}, tryDiff);

            if (answer == JOptionPane.NO_OPTION) {
                // The user wants to use another encoding.
                Object choice = JOptionPane.showInputDialog(frame, Localization.lang("Select encoding"), SAVE_DATABASE,
                        JOptionPane.QUESTION_MESSAGE, null, Encodings.ENCODINGS_DISPLAYNAMES, enc);
                if (choice == null) {
                    commit = false;
                } else {
                    Charset newEncoding = Charset.forName((String) choice);
                    return saveDatabase(file, selectedOnly, newEncoding, saveType);

                }
            } else if (answer == JOptionPane.CANCEL_OPTION) {
                commit = false;
            }

        }

        if (commit) {
            session.commit(file);
            this.encoding = enc; // Make sure to remember which encoding we used.
        } else {
            session.cancel();
        }

        return commit;
    }

    public void registerUndoableChanges(SaveSession session) {
        NamedCompound ce = new NamedCompound(Localization.lang("Save actions"));
        for (FieldChange change : session.getFieldChanges()) {
            ce.addEdit(new UndoableFieldChange(change));
        }
        ce.end();
        if (ce.hasEdits()) {
            undoManager.addEdit(ce);
        }
    }

    /**
     * This method is called from JabRefFrame when the user wants to create a new entry. If the argument is null, the
     * user is prompted for an entry type.
     *
     * @param type The type of the entry to create.
     * @return The newly created BibEntry or null the operation was canceled by the user.
     */
    public BibEntry newEntry(EntryType type) {
        EntryType actualType = type;
        if (actualType == null) {
            // Find out what type is wanted.
            final EntryTypeDialog etd = new EntryTypeDialog(frame);
            // We want to center the dialog, to make it look nicer.
            etd.setLocationRelativeTo(frame);
            etd.setVisible(true);
            actualType = etd.getChoice();
        }
        if (actualType != null) { // Only if the dialog was not cancelled.
            String id = IdGenerator.next();
            final BibEntry be = new BibEntry(id, actualType.getName());
            try {
                database.insertEntry(be);
                // Set owner/timestamp if options are enabled:
                List<BibEntry> list = new ArrayList<>();
                list.add(be);
                UpdateField.setAutomaticFields(list, true, true);

                // Create an UndoableInsertEntry object.
                undoManager.addEdit(new UndoableInsertEntry(database, be, BasePanel.this));
                output(Localization.lang("Added new '%0' entry.", actualType.getName().toLowerCase()));

                // We are going to select the new entry. Before that, make sure that we are in
                // show-entry mode. If we aren't already in that mode, enter the WILL_SHOW_EDITOR
                // mode which makes sure the selection will trigger display of the entry editor
                // and adjustment of the splitter.
                if (mode != BasePanelMode.SHOWING_EDITOR) {
                    mode = BasePanelMode.WILL_SHOW_EDITOR;
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
        public void databaseChanged(final DatabaseChangeEvent e) {
            if ((e.getType() == ChangeType.ADDED_ENTRY) && Globals.prefs.getBoolean(JabRefPreferences.AUTO_ASSIGN_GROUP)
                    && frame.groupToggle.isSelected()) {
                final List<BibEntry> entries = Collections.singletonList(e.getEntry());
                final TreePath[] selection = frame.getGroupSelector().getGroupsTree().getSelectionPaths();
                if (selection != null) {
                    // it is possible that the user selected nothing. Therefore, checked for "!= null"
                    for (final TreePath tree : selection) {
                        ((GroupTreeNode) tree.getLastPathComponent()).addToGroup(entries);
                    }
                }
                SwingUtilities.invokeLater(() -> BasePanel.this.getGroupSelector().valueChanged(null));
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
    public void insertEntry(final BibEntry bibEntry) {
        if (bibEntry != null) {
            try {
                database.insertEntry(bibEntry);
                if (Globals.prefs.getBoolean(JabRefPreferences.USE_OWNER)) {
                    // Set owner field to default value
                    UpdateField.setAutomaticFields(bibEntry, true, true);
                }
                // Create an UndoableInsertEntry object.
                undoManager.addEdit(new UndoableInsertEntry(database, bibEntry, BasePanel.this));
                output(Localization.lang("Added new '%0' entry.", bibEntry.getType()));

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

    public void editEntryByKeyAndFocusField(final String bibtexKey, final String fieldName) {
        final List<BibEntry> entries = database.getEntriesByKey(bibtexKey);
        if (entries.size() == 1) {
            mainTable.setSelected(mainTable.findEntry(entries.get(0)));
            selectionListener.editSignalled();
            final EntryEditor editor = getEntryEditor(entries.get(0));
            editor.setFocusToField(fieldName);
            new FocusRequester(editor);
        }
    }

    public void updateTableFont() {
        mainTable.updateFont();
    }

    private void createMainTable() {
        database.addDatabaseChangeListener(tableModel.getEventList());
        database.addDatabaseChangeListener(SpecialFieldDatabaseChangeListener.getInstance());

        tableFormat = new MainTableFormat(database);
        tableFormat.updateTableFormat();
        mainTable = new MainTable(tableFormat, tableModel, frame, this);

        selectionListener = new MainTableSelectionListener(this, mainTable);
        mainTable.updateFont();
        mainTable.addSelectionListener(selectionListener);
        mainTable.addMouseListener(selectionListener);
        mainTable.addKeyListener(selectionListener);
        mainTable.addFocusListener(selectionListener);

        // Add the listener that will take care of highlighting groups as the selection changes:
        groupsHighlightListener = listEvent -> {
            HighlightMatchingGroupPreferences highlightMatchingGroupPreferences = new HighlightMatchingGroupPreferences(
                    Globals.prefs);
            if (highlightMatchingGroupPreferences.isAny()) {
                getGroupSelector().showMatchingGroups(mainTable.getSelectedEntries(), false);
            } else if (highlightMatchingGroupPreferences.isAll()) {
                getGroupSelector().showMatchingGroups(mainTable.getSelectedEntries(), true);
            } else {
                // no highlight
                getGroupSelector().showMatchingGroups(null, true);
            }
        };
        mainTable.addSelectionListener(groupsHighlightListener);

        mainTable.getActionMap().put(Actions.CUT, new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    runCommand(Actions.CUT);
                } catch (Throwable ex) {
                    LOGGER.warn("Could not cut", ex);
                }
            }
        });
        mainTable.getActionMap().put(Actions.COPY, new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    runCommand(Actions.COPY);
                } catch (Throwable ex) {
                    LOGGER.warn("Could not copy", ex);
                }
            }
        });
        mainTable.getActionMap().put(Actions.PASTE, new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    runCommand(Actions.PASTE);
                } catch (Throwable ex) {
                    LOGGER.warn("Could not paste", ex);
                }
            }
        });

        mainTable.addKeyListener(new KeyAdapter() {

            @Override
            public void keyPressed(KeyEvent e) {
                final int keyCode = e.getKeyCode();
                final TreePath path = frame.getGroupSelector().getSelectionPath();
                final GroupTreeNode node = path == null ? null : (GroupTreeNode) path.getLastPathComponent();

                if (e.isControlDown()) {
                    switch (keyCode) {
                    // The up/down/left/rightkeystrokes are displayed in the
                    // GroupSelector's popup menu, so if they are to be changed,
                    // edit GroupSelector.java accordingly!
                    case KeyEvent.VK_UP:
                        e.consume();
                        if (node != null) {
                            frame.getGroupSelector().moveNodeUp(node, true);
                        }
                        break;
                    case KeyEvent.VK_DOWN:
                        e.consume();
                        if (node != null) {
                            frame.getGroupSelector().moveNodeDown(node, true);
                        }
                        break;
                    case KeyEvent.VK_LEFT:
                        e.consume();
                        if (node != null) {
                            frame.getGroupSelector().moveNodeLeft(node, true);
                        }
                        break;
                    case KeyEvent.VK_RIGHT:
                        e.consume();
                        if (node != null) {
                            frame.getGroupSelector().moveNodeRight(node, true);
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
                    default:
                        break;
                    }
                } else if (keyCode == KeyEvent.VK_ENTER) {
                    e.consume();
                    try {
                        runCommand(Actions.EDIT);
                    } catch (Throwable ex) {
                        LOGGER.warn("Could not run action based on key press", ex);
                    }
                }
            }
        });
    }

    public void setupMainPanel() {
        splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setDividerSize(GUIGlobals.SPLIT_PANE_DIVIDER_SIZE);

        // check whether a mainTable already existed and a floatSearch was active
        boolean floatSearchActive = (mainTable != null) && (this.tableModel.getSearchState() == MainTableDataModel.DisplayOption.FLOAT);

        createMainTable();

        for (EntryEditor ee : entryEditors.values()) {
            ee.validateAllFields();
        }

        splitPane.setTopComponent(mainTable.getPane());

        // Remove borders
        splitPane.setBorder(BorderFactory.createEmptyBorder());
        setBorder(BorderFactory.createEmptyBorder());

        // If an entry is currently being shown, make sure it stays shown,
        // otherwise set the bottom component to null.
        if (mode == BasePanelMode.SHOWING_PREVIEW) {
            mode = BasePanelMode.SHOWING_NOTHING;
            int row = mainTable.findEntry(currentPreview.getEntry());
            if (row >= 0) {
                mainTable.setRowSelectionInterval(row, row);
            }

        } else if (mode == BasePanelMode.SHOWING_EDITOR) {
            mode = BasePanelMode.SHOWING_NOTHING;
        } else {
            splitPane.setBottomComponent(null);
        }

        setLayout(new BorderLayout());
        removeAll();
        add(searchBar, BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);

        // Set up name autocompleter for search:
        instantiateSearchAutoCompleter();
        this.getDatabase().addDatabaseChangeListener(new SearchAutoCompleterUpdater());

        AutoCompletePreferences autoCompletePreferences = new AutoCompletePreferences(Globals.prefs);
        // Set up AutoCompleters for this panel:
        if (Globals.prefs.getBoolean(JabRefPreferences.AUTO_COMPLETE)) {
            autoCompleters = new ContentAutoCompleters(getDatabase(), bibDatabaseContext.getMetaData(),
                    autoCompletePreferences, Globals.journalAbbreviationLoader);
            // ensure that the autocompleters are in sync with entries
            this.getDatabase().addDatabaseChangeListener(new AutoCompletersUpdater());
        } else {
            // create empty ContentAutoCompleters() if autoCompletion is deactivated
            autoCompleters = new ContentAutoCompleters(Globals.journalAbbreviationLoader);
        }

        // restore floating search result
        // (needed if preferences have been changed which causes a recreation of the main table)
        if (floatSearchActive) {
            mainTable.showFloatSearch();
        }

        splitPane.revalidate();
        revalidate();
        repaint();
    }

    public void updateSearchManager() {
        searchBar.setAutoCompleter(searchAutoCompleter);
    }

    private void instantiateSearchAutoCompleter() {
        AutoCompletePreferences autoCompletePreferences = new AutoCompletePreferences(Globals.prefs);
        AutoCompleterFactory autoCompleterFactory = new AutoCompleterFactory(autoCompletePreferences);
        searchAutoCompleter = autoCompleterFactory.getPersonAutoCompleter();
        for (BibEntry entry : database.getEntries()) {
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
        if (mode == BasePanelMode.SHOWING_PREVIEW) {
            splitPane.setDividerLocation(
                    splitPane.getHeight() - Globals.prefs.getInt(JabRefPreferences.PREVIEW_PANEL_HEIGHT));
        } else {
            splitPane.setDividerLocation(
                    splitPane.getHeight() - Globals.prefs.getInt(JabRefPreferences.ENTRY_EDITOR_HEIGHT));

        }
    }

    private boolean isShowingEditor() {
        return (splitPane.getBottomComponent() != null) && (splitPane.getBottomComponent() instanceof EntryEditor);
    }

    public void showEntry(final BibEntry be) {

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
        if ((getShowing() != null) && isShowingEditor()) {
            visName = ((EntryEditor) splitPane.getBottomComponent()).getVisiblePanelName();
        }
        if (getShowing() != null) {
            divLoc = splitPane.getDividerLocation();
        }

        if (entryEditors.containsKey(be.getType())) {
            // We already have an editor for this entry type.
            form = entryEditors.get(be.getType());
            form.switchTo(be);
            if (visName != null) {
                form.setVisiblePanel(visName);
            }
            splitPane.setBottomComponent(form);
        } else {
            // We must instantiate a new editor for this type.
            form = new EntryEditor(frame, BasePanel.this, be);
            if (visName != null) {
                form.setVisiblePanel(visName);
            }
            splitPane.setBottomComponent(form);

            entryEditors.put(be.getType(), form);

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
    public EntryEditor getEntryEditor(BibEntry entry) {
        EntryEditor form;
        if (entryEditors.containsKey(entry.getType())) {
            EntryEditor visibleNow = currentEditor;

            // We already have an editor for this entry type.
            form = entryEditors.get(entry.getType());

            // If the cached editor is not the same as the currently shown one,
            // make sure the current one stores its current edit:
            if ((visibleNow != null) && (!(form.equals(visibleNow)))) {
                visibleNow.storeCurrentEdit();
            }

            form.switchTo(entry);
        } else {
            // We must instantiate a new editor for this type. First make sure the old one
            // stores its last edit:
            storeCurrentEdit();
            // Then start the new one:
            form = new EntryEditor(frame, BasePanel.this, entry);

            entryEditors.put(entry.getType(), form);
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
        if (mode == BasePanelMode.SHOWING_EDITOR) {
            Globals.prefs.putInt(JabRefPreferences.ENTRY_EDITOR_HEIGHT,
                    splitPane.getHeight() - splitPane.getDividerLocation());
        } else if (mode == BasePanelMode.SHOWING_PREVIEW) {
            Globals.prefs.putInt(JabRefPreferences.PREVIEW_PANEL_HEIGHT,
                    splitPane.getHeight() - splitPane.getDividerLocation());
        }
        mode = BasePanelMode.SHOWING_EDITOR;
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
        mode = BasePanelMode.SHOWING_PREVIEW;
        currentPreview = preview;
        splitPane.setBottomComponent(preview);
    }

    /**
     * Removes the bottom component.
     */
    public void hideBottomComponent() {
        mode = BasePanelMode.SHOWING_NOTHING;
        splitPane.setBottomComponent(null);
    }

    /**
     * This method selects the given entry, and scrolls it into view in the table. If an entryEditor is shown, it is
     * given focus afterwards.
     */
    public void highlightEntry(final BibEntry be) {
        final int row = mainTable.findEntry(be);
        if (row >= 0) {
            mainTable.setRowSelectionInterval(row, row);
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
     * Closes the entry editor or preview panel if it is showing the given entry.
     */
    public void ensureNotShowingBottomPanel(BibEntry entry) {
        if ((mode == BasePanelMode.SHOWING_EDITOR && currentEditor.getEntry() == entry) ||
                (mode == BasePanelMode.SHOWING_PREVIEW && currentPreview.getEntry() == entry)) {
            hideBottomComponent();
        }
    }

    public void updateEntryEditorIfShowing() {
        if (mode == BasePanelMode.SHOWING_EDITOR) {
            if (currentEditor.getDisplayedBibEntryType().equals(currentEditor.getEntry().getType())) {
                currentEditor.updateAllFields();
                currentEditor.updateSource();
            } else {
                // The entry has changed type, so we must get a new editor.
                newEntryShowing(null);
                final EntryEditor newEditor = getEntryEditor(currentEditor.getEntry());
                showEntryEditor(newEditor);
            }
        }
    }

    /**
     * If an entry editor is showing, make sure its currently focused field stores its changes, if any.
     */
    public void storeCurrentEdit() {
        if (isShowingEditor()) {
            final EntryEditor editor = (EntryEditor) splitPane.getBottomComponent();
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

        // Put an asterisk behind the filename to indicate the database has changed.
        frame.setWindowTitle();
        frame.updateAllTabTitles();
        // If the status line states that the base has been saved, we
        // remove this message, since it is no longer relevant. If a
        // different message is shown, we leave it.
        if (frame.getStatusLineText().startsWith(Localization.lang("Saved database"))) {
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
            if (getBibDatabaseContext().getDatabaseFile() == null) {
                frame.setTabTitle(this, GUIGlobals.UNTITLED_TITLE, null);
            } else {
                frame.setTabTitle(this, getTabTitle(), getBibDatabaseContext().getDatabaseFile().getAbsolutePath());
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

    public BibDatabase getDatabase() {
        return database;
    }

    public void preambleEditorClosing() {
        preambleEditor = null;
    }

    public void stringsClosing() {
        stringDialog = null;
    }

    public void changeTypeOfSelectedEntries(String newType) {
        List<BibEntry> bes = mainTable.getSelectedEntries();
        changeType(bes, newType);
    }

    private void changeType(List<BibEntry> entries, String newType) {
        if ((entries == null) || (entries.isEmpty())) {
            LOGGER.error("At least one entry must be selected to be able to change the type.");
            return;
        }

        if (entries.size() > 1) {
            int choice = JOptionPane.showConfirmDialog(this,
                    Localization.lang("Multiple entries selected. Do you want to change the type of all these to '%0'?",
                            newType),
                    Localization.lang("Change entry type"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (choice == JOptionPane.NO_OPTION) {
                return;
            }
        }

        NamedCompound compound = new NamedCompound(Localization.lang("Change entry type"));
        for (BibEntry entry : entries) {
            compound.addEdit(new UndoableChangeType(entry, entry.getType(), newType));
            entry.setType(newType);
        }

        output(formatOutputMessage(Localization.lang("Changed type to '%0' for", newType), entries.size()));
        compound.end();
        undoManager.addEdit(compound);
        markBaseChanged();
        updateEntryEditorIfShowing();
    }

    public boolean showDeleteConfirmationDialog(int numberOfEntries) {
        if (Globals.prefs.getBoolean(JabRefPreferences.CONFIRM_DELETE)) {
            String msg;
            msg = Localization.lang("Really delete the selected entry?");
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
            NamedCompound ce = new NamedCompound(Localization.lang("Autogenerate BibTeX keys"));
            boolean any = false;

            for (BibEntry bes : database.getEntries()) {
                String oldKey = bes.getCiteKey();
                if ((oldKey == null) || oldKey.isEmpty()) {
                    LabelPatternUtil.makeLabel(bibDatabaseContext.getMetaData(), database, bes);
                    ce.addEdit(new UndoableKeyChange(database, bes, null, bes.getCiteKey()));
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
    private void setPreviewActive(boolean enabled) {
        selectionListener.setPreviewActive(enabled);
    }

    /**
     * Depending on whether a preview or an entry editor is showing, save the current divider location in the correct
     * preference setting.
     */
    public void saveDividerLocation() {
        if (mode == BasePanelMode.SHOWING_PREVIEW) {
            Globals.prefs.putInt(JabRefPreferences.PREVIEW_PANEL_HEIGHT,
                    splitPane.getHeight() - splitPane.getDividerLocation());
        } else if (mode == BasePanelMode.SHOWING_EDITOR) {
            Globals.prefs.putInt(JabRefPreferences.ENTRY_EDITOR_HEIGHT,
                    splitPane.getHeight() - splitPane.getDividerLocation());
        }
    }


    private class UndoAction implements BaseAction {

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
                LOGGER.warn("Nothing to undo", ex);
                frame.output(Localization.lang("Nothing to undo") + '.');
            }

            markChangedOrUnChanged();
        }
    }

    private class OpenURLAction implements BaseAction {

        private static final String URL_FIELD = "url";
        private static final String DOI_FIELD = "doi";
        private static final String PS_FIELD = "ps";
        private static final String PDF_FIELD = "pdf";


        @Override
        public void action() {
            final List<BibEntry> bes = mainTable.getSelectedEntries();
            String field = DOI_FIELD;
            if (bes.size() == 1) {
                Object link = bes.get(0).getField(DOI_FIELD);
                if (bes.get(0).hasField(URL_FIELD)) {
                    link = bes.get(0).getField(URL_FIELD);
                    field = URL_FIELD;
                }
                if (link == null) {
                    // No URL or DOI found in the "url" and "doi" fields.
                    // Look for web links in the "file" field as a fallback:
                    FileListEntry entry = null;
                    FileListTableModel tm = new FileListTableModel();
                    tm.setContent(bes.get(0).getField(Globals.FILE_FIELD));
                    for (int i = 0; i < tm.getRowCount(); i++) {
                        FileListEntry flEntry = tm.getEntry(i);
                        if (URL_FIELD.equalsIgnoreCase(flEntry.type.get().getName())
                                || PS_FIELD.equalsIgnoreCase(flEntry.type.get().getName())
                                || PDF_FIELD.equalsIgnoreCase(flEntry.type.get().getName())) {
                            entry = flEntry;
                            break;
                        }
                    }
                    if (entry == null) {
                        output(Localization.lang("No url defined") + '.');
                    } else {
                        try {
                            JabRefDesktop.openExternalFileAnyFormat(bibDatabaseContext, entry.link, entry.type);
                            output(Localization.lang("External viewer called") + '.');
                        } catch (IOException e) {
                            output(Localization.lang("Could not open link"));
                            LOGGER.info("Could not open link", e);
                        }
                    }
                } else {
                    try {
                        JabRefDesktop.openExternalViewer(bibDatabaseContext, link.toString(), field);
                        output(Localization.lang("External viewer called") + '.');
                    } catch (IOException ex) {
                        output(Localization.lang("Error") + ": " + ex.getMessage());
                    }
                }
            } else {
                output(Localization.lang("This operation requires exactly one item to be selected."));
            }

        }
    }

    private class RedoAction implements BaseAction {

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
            // We are just saving the file, so this message is most likely due to bad timing.
            // If not, we'll handle it on the next polling.
            return;
        }

        updatedExternally = true;

        final ChangeScanner scanner = new ChangeScanner(frame, BasePanel.this,
                getBibDatabaseContext().getDatabaseFile());

        // Test: running scan automatically in background
        if ((getBibDatabaseContext().getDatabaseFile() != null)
                && !FileBasedLock.waitForFileLock(getBibDatabaseContext().getDatabaseFile(), 10)) {
            // The file is locked even after the maximum wait. Do nothing.
            LOGGER.error("File updated externally, but change scan failed because the file is locked.");
            // Perturb the stored timestamp so successive checks are made:
            Globals.fileUpdateMonitor.perturbTimestamp(getFileMonitorHandle());
            return;
        }

        JabRefExecutorService.INSTANCE.executeWithLowPriorityInOwnThreadAndWait(scanner);

        // Adding the sidepane component is Swing work, so we must do this in the Swing
        // thread:
        Runnable t = () -> {

            // Check if there is already a notification about external
            // changes:
            boolean hasAlready = sidePaneManager.hasComponent(FileUpdatePanel.NAME);
            if (hasAlready) {
                sidePaneManager.hideComponent(FileUpdatePanel.NAME);
                sidePaneManager.unregisterComponent(FileUpdatePanel.NAME);
            }
            FileUpdatePanel pan = new FileUpdatePanel(BasePanel.this, sidePaneManager,
                    getBibDatabaseContext().getDatabaseFile(), scanner);
            sidePaneManager.register(FileUpdatePanel.NAME, pan);
            sidePaneManager.show(FileUpdatePanel.NAME);
        };

        if (scanner.changesFound()) {
            SwingUtilities.invokeLater(t);
        } else {
            setUpdatedExternally(false);
        }
    }

    @Override
    public void fileRemoved() {
        LOGGER.info("File '" + getBibDatabaseContext().getDatabaseFile().getPath() + "' has been deleted.");
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
     * @return A list containing the selected entries. Is never null.
     */
    public List<BibEntry> getSelectedEntries() {
        return mainTable.getSelectedEntries();
    }

    public BibDatabaseContext getBibDatabaseContext() {
        return this.bibDatabaseContext;
    }

    public GroupSelector getGroupSelector() {
        return frame.getGroupSelector();
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

    private BibEntry getShowing() {
        return showing;
    }

    /**
     * Update the pointer to the currently shown entry in all cases where the user has moved to a new entry, except when
     * using Back and Forward commands. Also updates history for Back command, and clears history for Forward command.
     *
     * @param entry The entry that is now to be shown.
     */
    public void newEntryShowing(BibEntry entry) {
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
        if (!Objects.equals(entry, showing)) {
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
            BibEntry toShow = previousEntries.get(previousEntries.size() - 1);
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
            BibEntry toShow = nextEntries.get(nextEntries.size() - 1);
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
        frame.getBackAction().setEnabled(!previousEntries.isEmpty());
        frame.getForwardAction().setEnabled(!nextEntries.isEmpty());
    }

    private String formatOutputMessage(String start, int count) {
        return String.format("%s %d %s.", start, count,
                (count > 1 ? Localization.lang("entries") : Localization.lang("entry")));
    }


    private class SaveSelectedAction implements BaseAction {

        private final SavePreferences.DatabaseSaveType saveType;


        public SaveSelectedAction(SavePreferences.DatabaseSaveType saveType) {
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
                    saveDatabase(expFile, true, Globals.prefs.getDefaultEncoding(), saveType);
                    frame.getFileHistory().newFile(expFile.getPath());
                    frame.output(Localization.lang("Saved selected to '%0'.", expFile.getPath()));
                }
            }
        }
    }

    private static class SearchAndOpenFile {

        private final BibEntry entry;
        private final BasePanel basePanel;


        public SearchAndOpenFile(final BibEntry entry, final BasePanel basePanel) {
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
            final Collection<BibEntry> entries = Collections.singleton(entry);

            final Collection<ExternalFileType> types = ExternalFileTypes.getInstance().getExternalFileTypeSelection();
            final List<File> dirs = new ArrayList<>();
            if (basePanel.getBibDatabaseContext().getFileDirectory().size() > 0) {
                final List<String> mdDirs = basePanel.getBibDatabaseContext().getFileDirectory();
                for (final String mdDir : mdDirs) {
                    dirs.add(new File(mdDir));

                }
            }
            final List<String> extensions = new ArrayList<>();
            for (final ExternalFileType type : types) {
                extensions.add(type.getExtension());
            }
            // Run the search operation:
            Map<BibEntry, List<File>> result;
            if (Globals.prefs.getBoolean(JabRefPreferences.AUTOLINK_USE_REG_EXP_SEARCH_KEY)) {
                String regExp = Globals.prefs.get(JabRefPreferences.REG_EXP_SEARCH_EXPRESSION_KEY);
                result = RegExpFileSearch.findFilesForSet(entries, extensions, dirs, regExp);
            } else {
                result = FileUtil.findAssociatedFiles(entries, extensions, dirs);
            }
            if (result.containsKey(entry)) {
                final List<File> res = result.get(entry);
                if (!res.isEmpty()) {
                    final String filepath = res.get(0).getPath();
                    final Optional<String> extension = FileUtil.getFileExtension(filepath);
                    if (extension.isPresent()) {
                        Optional<ExternalFileType> type = ExternalFileTypes.getInstance()
                                .getExternalFileTypeByExt(extension.get());
                        if (type.isPresent()) {
                            try {
                                JabRefDesktop.openExternalFileAnyFormat(basePanel.getBibDatabaseContext(), filepath,
                                        type);
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

    /**
     * Set the preview active state for all BasePanel instances.
     *
     * @param enabled
     */
    private void setPreviewActiveBasePanels(boolean enabled) {
        for (int i = 0; i < frame.getTabbedPane().getTabCount(); i++) {
            frame.getBasePanelAt(i).setPreviewActive(enabled);
        }
    }
}
