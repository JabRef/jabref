package net.sf.jabref.gui;

import java.awt.BorderLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TimerTask;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.tree.TreePath;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefExecutorService;
import net.sf.jabref.collab.ChangeScanner;
import net.sf.jabref.collab.FileUpdateListener;
import net.sf.jabref.collab.FileUpdatePanel;
import net.sf.jabref.gui.actions.Actions;
import net.sf.jabref.gui.actions.BaseAction;
import net.sf.jabref.gui.actions.CleanupAction;
import net.sf.jabref.gui.actions.CopyBibTeXKeyAndLinkAction;
import net.sf.jabref.gui.bibtexkeypattern.SearchFixDuplicateLabels;
import net.sf.jabref.gui.desktop.JabRefDesktop;
import net.sf.jabref.gui.entryeditor.EntryEditor;
import net.sf.jabref.gui.exporter.ExportToClipboardAction;
import net.sf.jabref.gui.exporter.SaveDatabaseAction;
import net.sf.jabref.gui.externalfiles.FindFullTextAction;
import net.sf.jabref.gui.externalfiles.SynchronizeFileField;
import net.sf.jabref.gui.externalfiles.WriteXMPAction;
import net.sf.jabref.gui.externalfiletype.ExternalFileMenuItem;
import net.sf.jabref.gui.externalfiletype.ExternalFileType;
import net.sf.jabref.gui.externalfiletype.ExternalFileTypes;
import net.sf.jabref.gui.fieldeditors.FieldEditor;
import net.sf.jabref.gui.filelist.AttachFileAction;
import net.sf.jabref.gui.filelist.FileListEntry;
import net.sf.jabref.gui.filelist.FileListTableModel;
import net.sf.jabref.gui.groups.GroupAddRemoveDialog;
import net.sf.jabref.gui.groups.GroupSelector;
import net.sf.jabref.gui.groups.GroupTreeNodeViewModel;
import net.sf.jabref.gui.importer.actions.AppendDatabaseAction;
import net.sf.jabref.gui.journals.AbbreviateAction;
import net.sf.jabref.gui.journals.UnabbreviateAction;
import net.sf.jabref.gui.keyboard.KeyBinding;
import net.sf.jabref.gui.maintable.MainTable;
import net.sf.jabref.gui.maintable.MainTableDataModel;
import net.sf.jabref.gui.maintable.MainTableFormat;
import net.sf.jabref.gui.maintable.MainTableSelectionListener;
import net.sf.jabref.gui.mergeentries.FetchAndMergeEntry;
import net.sf.jabref.gui.mergeentries.MergeEntriesDialog;
import net.sf.jabref.gui.plaintextimport.TextInputDialog;
import net.sf.jabref.gui.specialfields.SpecialFieldDatabaseChangeListener;
import net.sf.jabref.gui.specialfields.SpecialFieldValueViewModel;
import net.sf.jabref.gui.specialfields.SpecialFieldViewModel;
import net.sf.jabref.gui.undo.CountingUndoManager;
import net.sf.jabref.gui.undo.NamedCompound;
import net.sf.jabref.gui.undo.UndoableChangeType;
import net.sf.jabref.gui.undo.UndoableFieldChange;
import net.sf.jabref.gui.undo.UndoableInsertEntry;
import net.sf.jabref.gui.undo.UndoableKeyChange;
import net.sf.jabref.gui.undo.UndoableRemoveEntry;
import net.sf.jabref.gui.util.component.CheckBoxMessage;
import net.sf.jabref.gui.worker.AbstractWorker;
import net.sf.jabref.gui.worker.CallBack;
import net.sf.jabref.gui.worker.MarkEntriesAction;
import net.sf.jabref.gui.worker.SendAsEMailAction;
import net.sf.jabref.gui.worker.Worker;
import net.sf.jabref.logic.autocompleter.AutoCompletePreferences;
import net.sf.jabref.logic.autocompleter.AutoCompleter;
import net.sf.jabref.logic.autocompleter.AutoCompleterFactory;
import net.sf.jabref.logic.autocompleter.ContentAutoCompleters;
import net.sf.jabref.logic.bibtexkeypattern.BibtexKeyPatternUtil;
import net.sf.jabref.logic.citationstyle.CitationStyleCache;
import net.sf.jabref.logic.exporter.BibtexDatabaseWriter;
import net.sf.jabref.logic.exporter.FileSaveSession;
import net.sf.jabref.logic.exporter.SaveException;
import net.sf.jabref.logic.exporter.SavePreferences;
import net.sf.jabref.logic.exporter.SaveSession;
import net.sf.jabref.logic.l10n.Encodings;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.layout.Layout;
import net.sf.jabref.logic.layout.LayoutHelper;
import net.sf.jabref.logic.search.SearchQuery;
import net.sf.jabref.logic.util.FileExtensions;
import net.sf.jabref.logic.util.UpdateField;
import net.sf.jabref.logic.util.io.FileBasedLock;
import net.sf.jabref.logic.util.io.FileUtil;
import net.sf.jabref.logic.util.io.RegExpFileSearch;
import net.sf.jabref.model.FieldChange;
import net.sf.jabref.model.bibtexkeypattern.AbstractBibtexKeyPattern;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.database.BibDatabaseContext;
import net.sf.jabref.model.database.DatabaseLocation;
import net.sf.jabref.model.database.KeyCollisionException;
import net.sf.jabref.model.database.event.EntryAddedEvent;
import net.sf.jabref.model.database.event.EntryRemovedEvent;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.EntryType;
import net.sf.jabref.model.entry.FieldName;
import net.sf.jabref.model.entry.IdGenerator;
import net.sf.jabref.model.entry.event.EntryChangedEvent;
import net.sf.jabref.model.entry.event.EntryEventSource;
import net.sf.jabref.model.entry.specialfields.SpecialField;
import net.sf.jabref.model.entry.specialfields.SpecialFieldValue;
import net.sf.jabref.preferences.HighlightMatchingGroupPreferences;
import net.sf.jabref.preferences.JabRefPreferences;
import net.sf.jabref.preferences.PreviewPreferences;
import net.sf.jabref.shared.DBMSSynchronizer;

import ca.odell.glazedlists.event.ListEventListener;
import com.google.common.eventbus.Subscribe;
import com.jgoodies.forms.builder.FormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class BasePanel extends JPanel implements ClipboardOwner, FileUpdateListener {
    private static final Log LOGGER = LogFactory.getLog(BasePanel.class);

    // Divider size for BaseFrame split pane. 0 means non-resizable.
    private static final int SPLIT_PANE_DIVIDER_SIZE = 4;

    private final BibDatabaseContext bibDatabaseContext;
    private final MainTableDataModel tableModel;

    private final CitationStyleCache citationStyleCache;

    // To contain instantiated entry editors. This is to save time
    // As most enums, this must not be null
    private BasePanelMode mode = BasePanelMode.SHOWING_NOTHING;
    private EntryEditor currentEditor;

    private MainTableSelectionListener selectionListener;

    private ListEventListener<BibEntry> groupsHighlightListener;

    private JSplitPane splitPane;

    private final JabRefFrame frame;
    private String fileMonitorHandle;
    private boolean saving;
    private boolean updatedExternally;

    // AutoCompleter used in the search bar
    private AutoCompleter<String> searchAutoCompleter;
    // The undo manager.
    private final UndoAction undoAction = new UndoAction();
    private final RedoAction redoAction = new RedoAction();
    private final CountingUndoManager undoManager = new CountingUndoManager();

    private final List<BibEntry> previousEntries = new ArrayList<>();

    private final List<BibEntry> nextEntries = new ArrayList<>();
    private boolean baseChanged;
    private boolean nonUndoableChange;

    // Used to track whether the base has changed since last save.
    private MainTable mainTable;

    private MainTableFormat tableFormat;

    private BibEntry showing;

    // Variable to prevent erroneous update of back/forward histories at the time
    // when a Back or Forward operation is being processed:
    private boolean backOrForwardInProgress;

    // in switching between entries.
    private PreambleEditor preambleEditor;

    // Keeps track of the preamble dialog if it is open.
    private StringDialog stringDialog;

    // Keeps track of the string dialog if it is open.
    private final Map<String, Object> actions = new HashMap<>();

    private final SidePaneManager sidePaneManager;

    private ContentAutoCompleters autoCompleters;

    private SearchQuery currentSearchQuery;


    public BasePanel(JabRefFrame frame, BibDatabaseContext bibDatabaseContext) {
        Objects.requireNonNull(frame);
        Objects.requireNonNull(bibDatabaseContext);

        this.bibDatabaseContext = bibDatabaseContext;

        this.sidePaneManager = frame.getSidePaneManager();
        this.frame = frame;
        this.tableModel = new MainTableDataModel(getBibDatabaseContext());

        citationStyleCache = new CitationStyleCache(bibDatabaseContext);

        setupMainPanel();

        setupActions();

        this.getDatabase().registerListener(new SearchListener());

        // ensure that at each addition of a new entry, the entry is added to the groups interface
        this.bibDatabaseContext.getDatabase().registerListener(new GroupTreeListener());

        Optional<File> file = bibDatabaseContext.getDatabaseFile();
        if (file.isPresent()) {
            // Register so we get notifications about outside changes to the file.
            try {
                fileMonitorHandle = Globals.getFileUpdateMonitor().addUpdateListener(this, file.get());
            } catch (IOException ex) {
                LOGGER.warn("Could not register FileUpdateMonitor", ex);
            }
        } else {
            if (bibDatabaseContext.getDatabase().hasEntries()) {
                // if the database is not empty and no file is assigned,
                // the database came from an import and has to be treated somehow
                // -> mark as changed
                this.baseChanged = true;
            }
        }
    }

    // Returns a collection of AutoCompleters, which are populated from the current database
    public ContentAutoCompleters getAutoCompleters() {
        return autoCompleters;
    }

    public String getTabTitle() {
        StringBuilder title = new StringBuilder();
        DatabaseLocation databaseLocation = this.bibDatabaseContext.getLocation();
        boolean isAutosaveEnabled = Globals.prefs.getBoolean(JabRefPreferences.LOCAL_AUTO_SAVE);

        if (databaseLocation == DatabaseLocation.LOCAL) {
            if (this.bibDatabaseContext.getDatabaseFile().isPresent()) {
                // check if file is modified
                String changeFlag = isModified() && !isAutosaveEnabled ? "*" : "";
                title.append(this.bibDatabaseContext.getDatabaseFile().get().getName()).append(changeFlag);
            } else {
                title.append(GUIGlobals.UNTITLED_TITLE);

                if (getDatabase().hasEntries()) {
                    // if the database is not empty and no file is assigned,
                    // the database came from an import and has to be treated somehow
                    // -> mark as changed
                    // This also happens internally at basepanel to ensure consistency line 224
                    title.append('*');
                }
            }
        } else if (databaseLocation == DatabaseLocation.SHARED) {
            title.append(
                    this.bibDatabaseContext.getDBMSSynchronizer().getDBName() + " [" + Localization.lang("shared") + "]");
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

    public void output(String s) {
        frame.output(s);
    }

    private void setupActions() {
        SaveDatabaseAction saveAction = new SaveDatabaseAction(this);
        CleanupAction cleanUpAction = new CleanupAction(this, Globals.prefs);

        actions.put(Actions.UNDO, undoAction);
        actions.put(Actions.REDO, redoAction);

        actions.put(Actions.FOCUS_TABLE, (BaseAction) () -> {
            mainTable.requestFocus();
        });

        // The action for opening an entry editor.
        actions.put(Actions.EDIT, (BaseAction) selectionListener::editSignalled);

        // The action for saving a database.
        actions.put(Actions.SAVE, saveAction);

        actions.put(Actions.SAVE_AS, (BaseAction) saveAction::saveAs);

        actions.put(Actions.SAVE_SELECTED_AS, new SaveSelectedAction(SavePreferences.DatabaseSaveType.ALL));

        actions.put(Actions.SAVE_SELECTED_AS_PLAIN,
                new SaveSelectedAction(SavePreferences.DatabaseSaveType.PLAIN_BIBTEX));

        // The action for copying selected entries.
        actions.put(Actions.COPY, (BaseAction) () -> copy());

        actions.put(Actions.PRINT_PREVIEW, new PrintPreviewAction());

        actions.put(Actions.CUT, (BaseAction) this::cut);

        //when you modify this action be sure to adjust Actions.CUT,
        //they are the same except of the Localization, delete confirmation and Actions.COPY call
        actions.put(Actions.DELETE, (BaseAction) () -> delete(false));

        // The action for pasting entries or cell contents.
        //  - more robust detection of available content flavors (doesn't only look at first one offered)
        //  - support for parsing string-flavor clipboard contents which are bibtex entries.
        //    This allows you to (a) paste entire bibtex entries from a text editor, web browser, etc
        //                       (b) copy and paste entries between multiple instances of JabRef (since
        //         only the text representation seems to get as far as the X clipboard, at least on my system)
        actions.put(Actions.PASTE, (BaseAction) () -> paste());

        actions.put(Actions.SELECT_ALL, (BaseAction) mainTable::selectAll);

        // The action for opening the preamble editor
        actions.put(Actions.EDIT_PREAMBLE, (BaseAction) () -> {
            if (preambleEditor == null) {
                PreambleEditor form = new PreambleEditor(frame, BasePanel.this, bibDatabaseContext.getDatabase());
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
                StringDialog form = new StringDialog(frame, BasePanel.this, bibDatabaseContext.getDatabase());
                form.setVisible(true);
                stringDialog = form;
            } else {
                stringDialog.setVisible(true);
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
            boolean canceled;


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
                // We don't want to generate keys for entries which already have one thus remove the entries
                if (Globals.prefs.getBoolean(JabRefPreferences.AVOID_OVERWRITING_KEY)) {
                    entries.removeIf(BibEntry::hasCiteKey);

                // if we're going to override some cite keys warn the user about it
                } else if (Globals.prefs.getBoolean(JabRefPreferences.WARN_BEFORE_OVERWRITING_KEY)) {
                    if (entries.parallelStream().anyMatch(BibEntry::hasCiteKey)) {
                        CheckBoxMessage cbm = new CheckBoxMessage(
                                Localization.lang("One or more keys will be overwritten. Continue?"),
                                Localization.lang("Disable this confirmation dialog"), false);
                        final int answer = JOptionPane.showConfirmDialog(frame, cbm,
                                Localization.lang("Overwrite keys"), JOptionPane.YES_NO_OPTION);
                        Globals.prefs.putBoolean(JabRefPreferences.WARN_BEFORE_OVERWRITING_KEY, !cbm.isSelected());

                        // The user doesn't want to overide cite keys
                        if (answer == JOptionPane.NO_OPTION) {
                            canceled = true;
                            return;
                        }
                    }
                }

                // generate the new cite keys for each entry
                final NamedCompound ce = new NamedCompound(Localization.lang("Autogenerate BibTeX keys"));
                AbstractBibtexKeyPattern citeKeyPattern = bibDatabaseContext.getMetaData()
                        .getCiteKeyPattern(Globals.prefs.getBibtexKeyPatternPreferences().getKeyPattern());
                for (BibEntry entry : entries) {
                    String oldCiteKey = entry.getCiteKeyOptional().orElse("");
                    BibtexKeyPatternUtil.makeLabel(citeKeyPattern, bibDatabaseContext.getDatabase(),
                            entry, Globals.prefs.getBibtexKeyPatternPreferences());
                    String newCiteKey = entry.getCiteKeyOptional().orElse("");
                    if (!oldCiteKey.equals(newCiteKey)) {
                        ce.addEdit(new UndoableKeyChange(entry, oldCiteKey, newCiteKey));
                    }
                }
                ce.end();

                // register the undo event only if new cite keys were generated
                if (ce.hasEdits()) {
                    getUndoManager().addEdit(ce);
                }
            }

            // Run third, on EDT:
            @Override
            public void update() {
                if (canceled) {
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

        actions.put(Actions.SEARCH, (BaseAction) frame.getGlobalSearchBar()::focus);
        actions.put(Actions.GLOBAL_SEARCH, (BaseAction) frame.getGlobalSearchBar()::performGlobalSearch);

        // The action for copying the selected entry's key.
        actions.put(Actions.COPY_KEY, (BaseAction) () -> copyKey());

        // The action for copying a cite for the selected entry.
        actions.put(Actions.COPY_CITE_KEY, (BaseAction) () -> copyCiteKey());

        // The action for copying the BibTeX key and the title for the first selected entry
        actions.put(Actions.COPY_KEY_AND_TITLE, (BaseAction) () -> copyKeyAndTitle());

        // The action for copying the BibTeX keys as hyperlinks to the urls of the selected entries
        actions.put(Actions.COPY_KEY_AND_LINK, new CopyBibTeXKeyAndLinkAction(mainTable));

        actions.put(Actions.MERGE_DATABASE, new AppendDatabaseAction(frame, this));

        actions.put(Actions.ADD_FILE_LINK, new AttachFileAction(this));

        actions.put(Actions.OPEN_EXTERNAL_FILE, (BaseAction) () -> openExternalFile());

        actions.put(Actions.OPEN_FOLDER, (BaseAction) () -> JabRefExecutorService.INSTANCE.execute(() -> {
            final List<File> files = FileUtil.getListOfLinkedFiles(mainTable.getSelectedEntries(),
                    bibDatabaseContext.getFileDirectory(Globals.prefs.getFileDirectoryPreferences()));
            for (final File f : files) {
                try {
                    JabRefDesktop.openFolderAndSelectFile(f.getAbsolutePath());
                } catch (IOException e) {
                    LOGGER.info("Could not open folder", e);
                }
            }
        }));

        actions.put(Actions.OPEN_CONSOLE, (BaseAction) () -> JabRefDesktop
                .openConsole(frame.getCurrentBasePanel().getBibDatabaseContext().getDatabaseFile().orElse(null)));

        actions.put(Actions.PULL_CHANGES_FROM_SHARED_DATABASE, (BaseAction) () -> {
            DBMSSynchronizer dbmsSynchronizer = frame.getCurrentBasePanel().getBibDatabaseContext().getDBMSSynchronizer();
            dbmsSynchronizer.pullChanges();
        });

        actions.put(Actions.OPEN_URL, new OpenURLAction());

        actions.put(Actions.MERGE_WITH_FETCHED_ENTRY, (BaseAction) () -> {
            if (mainTable.getSelectedEntries().size() == 1) {
                BibEntry originalEntry = mainTable.getSelectedEntries().get(0);
                new FetchAndMergeEntry(originalEntry, this, FetchAndMergeEntry.SUPPORTED_FIELDS);
            } else {
                JOptionPane.showMessageDialog(frame(),
                        Localization.lang("This operation requires exactly one item to be selected."),
                        Localization.lang("Merge entry with %0 information",
                                FieldName.orFields(FieldName.getDisplayName(FieldName.DOI),
                                        FieldName.getDisplayName(FieldName.ISBN),
                                        FieldName.getDisplayName(FieldName.EPRINT))),
                        JOptionPane.INFORMATION_MESSAGE);
            }
        });

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
                for (BibEntry entry : bibDatabaseContext.getDatabase().getEntries()) {
                    counter += rsd.replace(entry, ce);
                }
            }

            output(Localization.lang("Replaced") + ' ' + counter + ' '
                    + (counter == 1 ? Localization.lang("occurrence") : Localization.lang("occurrences")) + '.');
            if (counter > 0) {
                ce.end();
                getUndoManager().addEdit(ce);
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
                UpdateField.setAutomaticFields(Collections.singletonList(bibEntry), false, false,
                        Globals.prefs.getUpdateFieldPreferences());
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
                    EntryMarker.unmarkEntry(be, false, bibDatabaseContext.getDatabase(), ce);
                }
                ce.end();
                getUndoManager().addEdit(ce);
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

            for (BibEntry be : bibDatabaseContext.getDatabase().getEntries()) {
                EntryMarker.unmarkEntry(be, false, bibDatabaseContext.getDatabase(), ce);
            }
            ce.end();
            getUndoManager().addEdit(ce);
            markBaseChanged();
            output(Localization.lang("Unmarked all entries"));
        });

        // Note that we can't put the number of entries that have been reverted into the undoText as the concrete number cannot be injected
        actions.put(new SpecialFieldValueViewModel(SpecialField.RELEVANCE.getValues().get(0)).getActionName(),
                new SpecialFieldViewModel(SpecialField.RELEVANCE).getSpecialFieldAction(
                        SpecialField.RELEVANCE.getValues().get(0), frame));
        actions.put(new SpecialFieldValueViewModel(SpecialField.QUALITY.getValues().get(0)).getActionName(),
                new SpecialFieldViewModel(SpecialField.QUALITY).getSpecialFieldAction(SpecialField.QUALITY.getValues().get(0), frame));
        actions.put(new SpecialFieldValueViewModel(SpecialField.PRINTED.getValues().get(0)).getActionName(),
                new SpecialFieldViewModel(SpecialField.PRINTED).getSpecialFieldAction(
                        SpecialField.PRINTED.getValues().get(0), frame));

        for (SpecialFieldValue prio : SpecialField.PRIORITY.getValues()) {
            actions.put(new SpecialFieldValueViewModel(prio).getActionName(), new SpecialFieldViewModel(SpecialField.PRIORITY).getSpecialFieldAction(prio, this.frame));
        }
        for (SpecialFieldValue rank : SpecialField.RANKING.getValues()) {
            actions.put(new SpecialFieldValueViewModel(rank).getActionName(), new SpecialFieldViewModel(SpecialField.RANKING).getSpecialFieldAction(rank, this.frame));
        }
        for (SpecialFieldValue status : SpecialField.READ_STATUS.getValues()) {
            actions.put(new SpecialFieldValueViewModel(status).getActionName(), new SpecialFieldViewModel(SpecialField.READ_STATUS).getSpecialFieldAction(status, this.frame));
        }

        actions.put(Actions.TOGGLE_PREVIEW, (BaseAction) () -> {
            PreviewPreferences previewPreferences = Globals.prefs.getPreviewPreferences();
            boolean enabled = !previewPreferences.isPreviewPanelEnabled();
            PreviewPreferences newPreviewPreferences = previewPreferences
                    .getBuilder()
                    .withPreviewPanelEnabled(enabled)
                    .build();
            Globals.prefs.storePreviewPreferences(newPreviewPreferences);
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

        actions.put(Actions.NEXT_PREVIEW_STYLE, (BaseAction) selectionListener::nextPreviewStyle);
        actions.put(Actions.PREVIOUS_PREVIEW_STYLE, (BaseAction) selectionListener::previousPreviewStyle);

        actions.put(Actions.EXPORT_TO_CLIPBOARD, new ExportToClipboardAction(frame));
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

    private void copy() {
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
    }

    private void cut() {
        runCommand(Actions.COPY);
        // cannot call runCommand(Actions.DELETE), b/c it will call delete(false) with the wrong parameter
        delete(true);
    }

    /**
     * Removes the selected entries from the database
     * @param cut If false the user will get asked if he really wants to delete the entries, and it will be localized
     *            as "deleted".
     *            If true the action will be localized as "cut"
     */
    private void delete(boolean cut) {
        List<BibEntry> entries = mainTable.getSelectedEntries();
        if (entries.isEmpty()) {
            return;
        }
        if (!cut && !showDeleteConfirmationDialog(entries.size())) {
            return;
        }

        // select the next entry to stay at the same place as before (or the previous if we're already at the end)
        if (mainTable.getSelectedRow() != mainTable.getRowCount() -1){
            selectNextEntry();
        } else {
            selectPreviousEntry();
        }

        NamedCompound compound;
        if (cut) {
            compound = new NamedCompound(
                    (entries.size() > 1 ? Localization.lang("cut entries") : Localization.lang("cut entry")));
        } else {
            compound = new NamedCompound(
                    (entries.size() > 1 ? Localization.lang("delete entries") : Localization.lang("delete entry")));
        }
        for (BibEntry entry : entries) {
            compound.addEdit(new UndoableRemoveEntry(bibDatabaseContext.getDatabase(), entry, BasePanel.this));
            bibDatabaseContext.getDatabase().removeEntry(entry);
            ensureNotShowingBottomPanel(entry);
        }
        compound.end();
        getUndoManager().addEdit(compound);

        markBaseChanged();
        frame.output(formatOutputMessage(cut ? Localization.lang("Cut") : Localization.lang("Deleted"), entries.size()));

        // prevent the main table from loosing focus
        mainTable.requestFocus();
    }

    private void paste() {
        Collection<BibEntry> bes = new ClipBoardManager().extractBibEntriesFromClipboard();

        // finally we paste in the entries (if any), which either came from TransferableBibtexEntries
        // or were parsed from a string
        if (!bes.isEmpty()) {

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
                UpdateField.setAutomaticFields(be, Globals.prefs.getUpdateFieldPreferences());

                // We have to clone the
                // entries, since the pasted
                // entries must exist
                // independently of the copied
                // ones.
                be.setId(IdGenerator.next());
                bibDatabaseContext.getDatabase().insertEntry(be);

                ce.addEdit(new UndoableInsertEntry(bibDatabaseContext.getDatabase(), be, BasePanel.this));

            }
            ce.end();
            getUndoManager().addEdit(ce);
            output(formatOutputMessage(Localization.lang("Pasted"), bes.size()));
            markBaseChanged();

            highlightEntry(firstBE);
            mainTable.requestFocus();

            if (Globals.prefs.getBoolean(JabRefPreferences.AUTO_OPEN_FORM)) {
                selectionListener.editSignalled(firstBE);
            }
        }
    }

    private void copyCiteKey() {
        List<BibEntry> bes = mainTable.getSelectedEntries();
        if (!bes.isEmpty()) {
            storeCurrentEdit();
            List<String> keys = new ArrayList<>(bes.size());
            // Collect all non-null keys.
            for (BibEntry be : bes) {
                be.getCiteKeyOptional().ifPresent(keys::add);
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
    }

    private void copyKey() {
        List<BibEntry> bes = mainTable.getSelectedEntries();
        if (!bes.isEmpty()) {
            storeCurrentEdit();
            List<String> keys = new ArrayList<>(bes.size());
            // Collect all non-null keys.
            for (BibEntry be : bes) {
                be.getCiteKeyOptional().ifPresent(keys::add);
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
    }

    private void copyKeyAndTitle() {
        List<BibEntry> bes = mainTable.getSelectedEntries();
        if (!bes.isEmpty()) {
            storeCurrentEdit();

            // OK: in a future version, this string should be configurable to allow arbitrary exports
            StringReader sr = new StringReader(
                    "\\bibtexkey - \\begin{title}\\format[RemoveBrackets]{\\title}\\end{title}\n");
            Layout layout;
            try {
                layout = new LayoutHelper(sr,
                        Globals.prefs.getLayoutFormatterPreferences(Globals.journalAbbreviationLoader))
                                .getLayoutFromText();
            } catch (IOException e) {
                LOGGER.info("Could not get layout", e);
                return;
            }

            StringBuilder sb = new StringBuilder();

            int copied = 0;
            // Collect all non-null keys.
            for (BibEntry be : bes) {
                if (be.hasCiteKey()) {
                    copied++;
                    sb.append(layout.doLayout(be, bibDatabaseContext.getDatabase()));
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
    }

    private void openExternalFile() {
        JabRefExecutorService.INSTANCE.execute(() -> {
            final List<BibEntry> bes = mainTable.getSelectedEntries();
            if (bes.size() != 1) {
                output(Localization.lang("This operation requires exactly one item to be selected."));
                return;
            }

            final BibEntry entry = bes.get(0);
            if (!entry.hasField(FieldName.FILE)) {
                // no bibtex field
                new SearchAndOpenFile(entry, BasePanel.this).searchAndOpen();
                return;
            }
            FileListTableModel fileListTableModel = new FileListTableModel();
            entry.getField(FieldName.FILE).ifPresent(fileListTableModel::setContent);
            if (fileListTableModel.getRowCount() == 0) {
                // content in BibTeX field is not readable
                new SearchAndOpenFile(entry, BasePanel.this).searchAndOpen();
                return;
            }
            FileListEntry flEntry = fileListTableModel.getEntry(0);
            ExternalFileMenuItem item = new ExternalFileMenuItem(frame(), entry, "", flEntry.link,
                    flEntry.type.get().getIcon(), bibDatabaseContext, flEntry.type);
            item.openLink();
        });
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
            BibtexDatabaseWriter<SaveSession> databaseWriter = new BibtexDatabaseWriter<>(
                    FileSaveSession::new);
            if (selectedOnly) {
                session = databaseWriter.savePartOfDatabase(bibDatabaseContext, mainTable.getSelectedEntries(), prefs);
            } else {
                session = databaseWriter.saveDatabase(bibDatabaseContext, prefs);
            }

            registerUndoableChanges(session);
        }
        // FIXME: not sure if this is really thrown anywhere
        catch (UnsupportedCharsetException ex) {
            JOptionPane.showMessageDialog(frame,
                    Localization.lang("Could not save file.") + ' '
                            + Localization.lang("Character encoding '%0' is not supported.", enc.displayName()),
                    SAVE_DATABASE, JOptionPane.ERROR_MESSAGE);
            throw new SaveException("rt");
        } catch (SaveException ex) {
            if (ex.specificEntry()) {
                // Error occurred during processing of the entry. Highlight it:
                highlightEntry(ex.getEntry());
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
            session.commit(file.toPath());
            this.bibDatabaseContext.getMetaData().setEncoding(enc); // Make sure to remember which encoding we used.
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
            getUndoManager().addEdit(ce);
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
        if (actualType != null) { // Only if the dialog was not canceled.
            String id = IdGenerator.next();
            final BibEntry be = new BibEntry(id, actualType.getName());
            try {
                bibDatabaseContext.getDatabase().insertEntry(be);
                // Set owner/timestamp if options are enabled:
                List<BibEntry> list = new ArrayList<>();
                list.add(be);
                UpdateField.setAutomaticFields(list, true, true, Globals.prefs.getUpdateFieldPreferences());

                // Create an UndoableInsertEntry object.
                getUndoManager().addEdit(new UndoableInsertEntry(bibDatabaseContext.getDatabase(), be, BasePanel.this));
                output(Localization.lang("Added new '%0' entry.", actualType.getName().toLowerCase()));

                // We are going to select the new entry. Before that, make sure that we are in
                // show-entry mode. If we aren't already in that mode, enter the WILL_SHOW_EDITOR
                // mode which makes sure the selection will trigger display of the entry editor
                // and adjustment of the splitter.
                if (mode != BasePanelMode.SHOWING_EDITOR) {
                    mode = BasePanelMode.WILL_SHOW_EDITOR;
                }

                highlightEntry(be);

                // The database just changed.
                markBaseChanged();

                final EntryEditor entryEditor = getEntryEditor(be);
                this.showEntryEditor(entryEditor);
                entryEditor.requestFocus();

                return be;
            } catch (KeyCollisionException ex) {
                LOGGER.info(ex.getMessage(), ex);
            }
        }
        return null;
    }

    private class GroupTreeListener {

        private final Runnable task = new Runnable() {

            @Override
            public void run() {
                // Update group display (for example to reflect that the number of contained entries has changed)
                frame.getGroupSelector().revalidateGroups();
            }

        };

        /**
         * Only access when you have the lock of the task instance
         *
         * Guarded by "task"
         */
        private TimerTask timerTask = new TimerTask() {

            @Override
            public void run() {
                task.run();
            }
        };


        @Subscribe
        public void listen(EntryAddedEvent addedEntryEvent) {
            // if the added entry is an undo don't add it to the current group
            if (addedEntryEvent.getEntryEventSource() == EntryEventSource.UNDO) {
                scheduleUpdate();
                return;
            }

            // Automatically add new entry to the selected group (or set of groups)
            if (Globals.prefs.getBoolean(JabRefPreferences.AUTO_ASSIGN_GROUP) && frame.getGroupSelector().getToggleAction().isSelected()) {
                final List<BibEntry> entries = Collections.singletonList(addedEntryEvent.getBibEntry());
                final TreePath[] selection = frame.getGroupSelector().getGroupsTree().getSelectionPaths();
                if (selection != null) {
                    // it is possible that the user selected nothing. Therefore, checked for "!= null"
                    for (final TreePath tree : selection) {
                        ((GroupTreeNodeViewModel) tree.getLastPathComponent()).addEntriesToGroup(entries);
                    }
                }
                SwingUtilities.invokeLater(() -> BasePanel.this.getGroupSelector().valueChanged(null));
            }

            scheduleUpdate();
        }

        private void scheduleUpdate() {
            // This is a quickfix/dirty hack.
            // a better solution would be using RxJava or something reactive instead
            // nevertheless it works correctly
            synchronized (task) {
                timerTask.cancel();
                timerTask = new TimerTask() {

                    @Override
                    public void run() {
                        task.run();
                    }
                };
                JabRefExecutorService.INSTANCE.submit(timerTask, 200);
            }
        }
    }

    /**
     * Ensures that the search auto completer is up to date when entries are changed AKA Let the auto completer, if any,
     * harvest words from the entry
     */
    private class SearchAutoCompleteListener {

        @Subscribe
        public void listen(EntryAddedEvent addedEntryEvent) {
            searchAutoCompleter.addBibtexEntry(addedEntryEvent.getBibEntry());
        }

        @Subscribe
        public void listen(EntryChangedEvent entryChangedEvent) {
            searchAutoCompleter.addBibtexEntry(entryChangedEvent.getBibEntry());
        }
    }

    /**
     * Ensures that auto completers are up to date when entries are changed AKA Let the auto completer, if any, harvest
     * words from the entry
     */
    private class AutoCompleteListener {

        @Subscribe
        public void listen(EntryAddedEvent addedEntryEvent) {
            BasePanel.this.autoCompleters.addEntry(addedEntryEvent.getBibEntry());
        }

        @Subscribe
        public void listen(EntryChangedEvent entryChangedEvent) {
            BasePanel.this.autoCompleters.addEntry(entryChangedEvent.getBibEntry());
        }
    }

    /**
     * Ensures that the results of the current search are updated when a new entry is inserted into the database
     */
    private class SearchListener {
        @Subscribe
        public void listen(EntryAddedEvent addedEntryEvent) {
            frame.getGlobalSearchBar().performSearch();
        }

        @Subscribe
        public void listen(EntryChangedEvent entryChangedEvent) {
            frame.getGlobalSearchBar().setDontSelectSearchBar(true);
            frame.getGlobalSearchBar().performSearch();
        }

        @Subscribe
        public void listen(EntryRemovedEvent removedEntryEvent) {
            // IMO only used to update the status (found X entries)
            frame.getGlobalSearchBar().performSearch();
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
                bibDatabaseContext.getDatabase().insertEntry(bibEntry);
                if (Globals.prefs.getBoolean(JabRefPreferences.USE_OWNER)) {
                    // Set owner field to default value
                    UpdateField.setAutomaticFields(bibEntry, true, true, Globals.prefs.getUpdateFieldPreferences());
                }
                // Create an UndoableInsertEntry object.
                getUndoManager()
                        .addEdit(new UndoableInsertEntry(bibDatabaseContext.getDatabase(), bibEntry, BasePanel.this));
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
        final List<BibEntry> entries = bibDatabaseContext.getDatabase().getEntriesByKey(bibtexKey);
        if (entries.size() == 1) {
            mainTable.setSelected(mainTable.findEntry(entries.get(0)));
            selectionListener.editSignalled();
            final EntryEditor editor = getEntryEditor(entries.get(0));
            editor.setFocusToField(fieldName);
            this.showEntryEditor(editor);
            editor.requestFocus();
        }
    }

    public void updateTableFont() {
        mainTable.updateFont();
    }

    private void createMainTable() {
        bibDatabaseContext.getDatabase().registerListener(tableModel.getListSynchronizer());
        bibDatabaseContext.getDatabase().registerListener(SpecialFieldDatabaseChangeListener.getInstance());

        tableFormat = new MainTableFormat(bibDatabaseContext.getDatabase());
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

        String clearSearch = "clearSearch";
        mainTable.getInputMap().put(Globals.getKeyPrefs().getKey(KeyBinding.CLEAR_SEARCH), clearSearch);
        mainTable.getActionMap().put(clearSearch, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // need to close these here, b/c this action overshadows the responsible actions when the main table is selected
                switch (mode) {
                    case SHOWING_NOTHING:
                        frame.getGlobalSearchBar().endSearch();
                        break;
                    case SHOWING_PREVIEW:
                        getPreviewPanel().close();
                        break;
                    case SHOWING_EDITOR:
                    case WILL_SHOW_EDITOR:
                        getCurrentEditor().close();
                        break;
                    default:
                        LOGGER.warn("unknown BasePanelMode: '" + mode + "', doing nothing");
                        break;
                }
            }
        });

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
                final GroupTreeNodeViewModel node = path == null ? null : (GroupTreeNodeViewModel) path
                        .getLastPathComponent();

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
        splitPane.setDividerSize(SPLIT_PANE_DIVIDER_SIZE);
        adjustSplitter(); // restore last splitting state (before mainTable is created as creation affects the stored size of the entryEditors)

        // check whether a mainTable already existed and a floatSearch was active
        boolean floatSearchActive = (mainTable != null)
                && (this.tableModel.getSearchState() == MainTableDataModel.DisplayOption.FLOAT);

        createMainTable();

        splitPane.setTopComponent(mainTable.getPane());

        // Remove borders
        splitPane.setBorder(BorderFactory.createEmptyBorder());
        setBorder(BorderFactory.createEmptyBorder());

        // If an entry is currently being shown, make sure it stays shown,
        // otherwise set the bottom component to null.
        if (mode == BasePanelMode.SHOWING_PREVIEW) {
            mode = BasePanelMode.SHOWING_NOTHING;
            highlightEntry(selectionListener.getPreview().getEntry());
        } else if (mode == BasePanelMode.SHOWING_EDITOR) {
            mode = BasePanelMode.SHOWING_NOTHING;
        } else {
            splitPane.setBottomComponent(null);
        }

        setLayout(new BorderLayout());
        removeAll();
        add(splitPane, BorderLayout.CENTER);

        // Set up name autocompleter for search:
        instantiateSearchAutoCompleter();
        this.getDatabase().registerListener(new SearchAutoCompleteListener());

        AutoCompletePreferences autoCompletePreferences = new AutoCompletePreferences(Globals.prefs);
        // Set up AutoCompleters for this panel:
        if (Globals.prefs.getBoolean(JabRefPreferences.AUTO_COMPLETE)) {
            autoCompleters = new ContentAutoCompleters(getDatabase(), autoCompletePreferences, Globals.journalAbbreviationLoader);
            // ensure that the autocompleters are in sync with entries
            this.getDatabase().registerListener(new AutoCompleteListener());
        } else {
            // create empty ContentAutoCompleters() if autoCompletion is deactivated
            autoCompleters = new ContentAutoCompleters();
        }

        // restore floating search result
        // (needed if preferences have been changed which causes a recreation of the main table)
        if (floatSearchActive) {
            mainTable.showFloatSearch();
        }

        splitPane.revalidate();
        revalidate();
        repaint();

        // saves the divider position as soon as it changes
        splitPane.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, event -> saveDividerLocation());
    }

    public void updateSearchManager() {
        frame.getGlobalSearchBar().setAutoCompleter(searchAutoCompleter);
    }

    private void instantiateSearchAutoCompleter() {
        AutoCompletePreferences autoCompletePreferences = new AutoCompletePreferences(Globals.prefs);
        AutoCompleterFactory autoCompleterFactory = new AutoCompleterFactory(autoCompletePreferences,
                Globals.journalAbbreviationLoader);
        searchAutoCompleter = autoCompleterFactory.getPersonAutoCompleter();
        for (BibEntry entry : bibDatabaseContext.getDatabase().getEntries()) {
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
                    splitPane.getHeight() - Globals.prefs.getPreviewPreferences().getPreviewPanelHeight());
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

        String visName = null;
        if ((getShowing() != null) && isShowingEditor()) {
            visName = ((EntryEditor) splitPane.getBottomComponent()).getVisiblePanelName();
        }

        // We must instantiate a new editor.
        EntryEditor entryEditor = new EntryEditor(frame, BasePanel.this, be);
        if (visName != null) {
            entryEditor.setVisiblePanel(visName);
        }
        showEntryEditor(entryEditor);

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
        // We must instantiate a new editor. First make sure the old one stores its last edit:
        storeCurrentEdit();
        // Then start the new one:
        return new EntryEditor(frame, BasePanel.this, entry);
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
        }
        mode = BasePanelMode.SHOWING_EDITOR;
        if (currentEditor != null) {
            currentEditor.setMovingToDifferentEntry();
        }
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
        splitPane.setBottomComponent(preview);
        adjustSplitter();
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
    public void highlightEntry(final BibEntry bibEntry) {
        highlightEntry(mainTable.findEntry(bibEntry));
    }

    /**
     * This method selects the entry on the given position, and scrolls it into view in the table.
     * If an entryEditor is shown, it is given focus afterwards.
     */
    public void highlightEntry(int pos) {
        if ((pos >= 0) && (pos < mainTable.getRowCount())) {
            mainTable.setRowSelectionInterval(pos, pos);
            mainTable.ensureVisible(pos);
        }
    }

    public void selectPreviousEntry() {
        highlightEntry(((mainTable.getSelectedRow() - 1) + mainTable.getRowCount()) % mainTable.getRowCount());
    }

    public void selectNextEntry() {
        highlightEntry((mainTable.getSelectedRow() + 1) % mainTable.getRowCount());
    }

    public void selectFirstEntry() {
        highlightEntry(0);
    }

    public void selectLastEntry() {
        highlightEntry(mainTable.getRowCount() - 1);
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
        if (((mode == BasePanelMode.SHOWING_EDITOR) && (currentEditor.getEntry() == entry))
                || ((mode == BasePanelMode.SHOWING_PREVIEW) && (selectionListener.getPreview().getEntry() == entry))) {
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

    public void markBaseChanged() {
        baseChanged = true;

        if (SwingUtilities.isEventDispatchThread()) {
            markBasedChangedInternal();
        } else {
            try {
                SwingUtilities.invokeAndWait(() -> markBasedChangedInternal());
            } catch (InvocationTargetException | InterruptedException e) {
                LOGGER.info("Problem marking database as changed", e);
            }
        }

    }

    private void markBasedChangedInternal() {
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
        if (getUndoManager().hasChanged()) {
            if (!baseChanged) {
                markBaseChanged();
            }
        } else if (baseChanged && !nonUndoableChange) {
            baseChanged = false;
            if (getBibDatabaseContext().getDatabaseFile().isPresent()) {
                frame.setTabTitle(this, getTabTitle(),
                        getBibDatabaseContext().getDatabaseFile().get().getAbsolutePath());
            } else {
                frame.setTabTitle(this, GUIGlobals.UNTITLED_TITLE, null);
            }
        }
        frame.setWindowTitle();
    }

    public BibDatabase getDatabase() {
        return bibDatabaseContext.getDatabase();
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
        getUndoManager().addEdit(compound);
        markBaseChanged();
        updateEntryEditorIfShowing();
    }

    public boolean showDeleteConfirmationDialog(int numberOfEntries) {
        if (Globals.prefs.getBoolean(JabRefPreferences.CONFIRM_DELETE)) {
            String msg;
            msg = Localization.lang("Really delete the selected entry?");
            String title = Localization.lang("Delete entry");
            if (numberOfEntries > 1) {
                msg = Localization.lang("Really delete the %0 selected entries?", Integer.toString(numberOfEntries));
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

            for (BibEntry bes : bibDatabaseContext.getDatabase().getEntries()) {
                Optional<String> oldKey = bes.getCiteKeyOptional();
                if (!(oldKey.isPresent()) || oldKey.get().isEmpty()) {
                    BibtexKeyPatternUtil.makeLabel(bibDatabaseContext.getMetaData()
                            .getCiteKeyPattern(Globals.prefs.getBibtexKeyPatternPreferences().getKeyPattern()),
                            bibDatabaseContext.getDatabase(),
                            bes, Globals.prefs.getBibtexKeyPatternPreferences());
                    bes.getCiteKeyOptional().ifPresent(
                            newKey -> ce.addEdit(new UndoableKeyChange(bes, oldKey.orElse(""), newKey)));
                }
            }

            // Store undo information, if any:
            if (ce.hasEdits()) {
                ce.end();
                getUndoManager().addEdit(ce);
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
            int previewPanelHeight = splitPane.getHeight() - splitPane.getDividerLocation();
            PreviewPreferences previewPreferences = Globals.prefs.getPreviewPreferences()
                    .getBuilder()
                    .withPreviewPanelHeight(previewPanelHeight)
                    .build();
            Globals.prefs.storePreviewPreferences(previewPreferences);
        } else if (mode == BasePanelMode.SHOWING_EDITOR) {
            Globals.prefs.putInt(JabRefPreferences.ENTRY_EDITOR_HEIGHT,
                    splitPane.getHeight() - splitPane.getDividerLocation());
        }
    }


    private class UndoAction implements BaseAction {

        @Override
        public void action() {
            try {
                JComponent focused = Globals.getFocusListener().getFocused();
                if ((focused != null) && (focused instanceof FieldEditor) && focused.hasFocus()) {
                    // User is currently editing a field:
                    // Check if it is the preamble:
                    if ((preambleEditor != null) && (focused == preambleEditor.getFieldEditor())) {
                        preambleEditor.storeCurrentEdit();
                    } else {
                        storeCurrentEdit();
                    }
                }
                getUndoManager().undo();
                markBaseChanged();
                frame.output(Localization.lang("Undo"));
            } catch (CannotUndoException ex) {
                LOGGER.warn("Nothing to undo", ex);
                frame.output(Localization.lang("Nothing to undo") + '.');
            }

            markChangedOrUnChanged();
        }
    }

    private class OpenURLAction implements BaseAction {

        @Override
        public void action() {
            final List<BibEntry> bes = mainTable.getSelectedEntries();
            if (bes.size() == 1) {
                String field = FieldName.DOI;
                Optional<String> link = bes.get(0).getField(FieldName.DOI);
                if (bes.get(0).hasField(FieldName.URL)) {
                    link = bes.get(0).getField(FieldName.URL);
                    field = FieldName.URL;
                }
                if (link.isPresent()) {
                    try {
                        JabRefDesktop.openExternalViewer(bibDatabaseContext, link.get(), field);
                        output(Localization.lang("External viewer called") + '.');
                    } catch (IOException ex) {
                        output(Localization.lang("Error") + ": " + ex.getMessage());
                    }
                } else {
                    // No URL or DOI found in the "url" and "doi" fields.
                    // Look for web links in the "file" field as a fallback:
                    FileListEntry entry = null;
                    FileListTableModel tm = new FileListTableModel();
                    bes.get(0).getField(FieldName.FILE).ifPresent(tm::setContent);
                    for (int i = 0; i < tm.getRowCount(); i++) {
                        FileListEntry flEntry = tm.getEntry(i);
                        if (FieldName.URL.equalsIgnoreCase(flEntry.type.get().getName())
                                || FieldName.PS.equalsIgnoreCase(flEntry.type.get().getName())
                                || FieldName.PDF.equalsIgnoreCase(flEntry.type.get().getName())) {
                            entry = flEntry;
                            break;
                        }
                    }
                    if (entry == null) {
                        output(Localization.lang("No URL defined") + '.');
                    } else {
                        try {
                            JabRefDesktop.openExternalFileAnyFormat(bibDatabaseContext, entry.link, entry.type);
                            output(Localization.lang("External viewer called") + '.');
                        } catch (IOException e) {
                            output(Localization.lang("Could not open link"));
                            LOGGER.info("Could not open link", e);
                        }
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

                JComponent focused = Globals.getFocusListener().getFocused();
                if ((focused != null) && (focused instanceof FieldEditor) && focused.hasFocus()) {
                    // User is currently editing a field:
                    storeCurrentEdit();
                }

                getUndoManager().redo();
                markBaseChanged();
                frame.output(Localization.lang("Redo"));
            } catch (CannotRedoException ex) {
                frame.output(Localization.lang("Nothing to redo") + '.');
            }

            markChangedOrUnChanged();
        }
    }

    private class PrintPreviewAction implements BaseAction {

        @Override
        public void action() throws Exception {
            selectionListener.setPreviewActive(true);
            showPreview(selectionListener.getPreview());
            selectionListener.getPreview().getPrintAction().actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, null));
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
                getBibDatabaseContext().getDatabaseFile().orElse(null));

        // Test: running scan automatically in background
        if ((getBibDatabaseContext().getDatabaseFile().isPresent())
                && !FileBasedLock.waitForFileLock(getBibDatabaseContext().getDatabaseFile().get().toPath())) {
            // The file is locked even after the maximum wait. Do nothing.
            LOGGER.error("File updated externally, but change scan failed because the file is locked.");
            // Perturb the stored timestamp so successive checks are made:
            Globals.getFileUpdateMonitor().perturbTimestamp(getFileMonitorHandle());
            return;
        }

        JabRefExecutorService.INSTANCE.executeWithLowPriorityInOwnThreadAndWait(scanner);

        // Adding the sidepane component is Swing work, so we must do this in the Swing
        // thread:
        Runnable t = () -> {

            // Check if there is already a notification about external
            // changes:
            boolean hasAlready = sidePaneManager.hasComponent(FileUpdatePanel.class);
            if (hasAlready) {
                sidePaneManager.hideComponent(FileUpdatePanel.class);
                sidePaneManager.unregisterComponent(FileUpdatePanel.class);
            }
            FileUpdatePanel pan = new FileUpdatePanel(BasePanel.this, sidePaneManager,
                    getBibDatabaseContext().getDatabaseFile().orElse(null), scanner);
            sidePaneManager.register(pan);
            sidePaneManager.show(FileUpdatePanel.class);
        };

        if (scanner.changesFound()) {
            SwingUtilities.invokeLater(t);
        } else {
            setUpdatedExternally(false);
        }
    }

    @Override
    public void fileRemoved() {
        LOGGER.info("File '" + getBibDatabaseContext().getDatabaseFile().get().getPath() + "' has been deleted.");
    }

    /**
     * Perform necessary cleanup when this BasePanel is closed.
     */
    public void cleanUp() {
        if (fileMonitorHandle != null) {
            Globals.getFileUpdateMonitor().removeUpdateListener(fileMonitorHandle);
        }
        // Check if there is a FileUpdatePanel for this BasePanel being shown. If so,
        // remove it:
        if (sidePaneManager.hasComponent(FileUpdatePanel.class)) {
            FileUpdatePanel fup = (FileUpdatePanel) sidePaneManager.getComponent(FileUpdatePanel.class);
            if (fup.getPanel() == this) {
                sidePaneManager.hideComponent(FileUpdatePanel.class);
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
            FileDialog dialog = new FileDialog(frame).withExtension(FileExtensions.BIBTEX_DB);
            dialog.setDefaultExtension(FileExtensions.BIBTEX_DB);
            Optional<Path> chosenFile = dialog.saveNewFile();

            if (chosenFile.isPresent()) {
                Path path = chosenFile.get();
                saveDatabase(path.toFile(), true, Globals.prefs.getDefaultEncoding(), saveType);
                frame.getFileHistory().newFile(path.toString());
                frame.output(Localization.lang("Saved selected to '%0'.", path.toString()));
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
            final List<BibEntry> entries = Collections.singletonList(entry);

            final Set<ExternalFileType> types = ExternalFileTypes.getInstance().getExternalFileTypeSelection();
            final List<File> dirs = new ArrayList<>();
            final List<String> mdDirs = basePanel.getBibDatabaseContext()
                    .getFileDirectory(Globals.prefs.getFileDirectoryPreferences());
            for (final String mdDir : mdDirs) {
                dirs.add(new File(mdDir));
            }
            final List<String> extensions = new ArrayList<>();
            for (final ExternalFileType type : types) {
                extensions.add(type.getExtension());
            }
            // Run the search operation:
            Map<BibEntry, List<File>> result;
            if (Globals.prefs.getBoolean(JabRefPreferences.AUTOLINK_USE_REG_EXP_SEARCH_KEY)) {
                String regExp = Globals.prefs.get(JabRefPreferences.REG_EXP_SEARCH_EXPRESSION_KEY);
                result = RegExpFileSearch.findFilesForSet(entries, extensions, dirs, regExp,
                        Globals.prefs.getKeywordDelimiter());
            } else {
                boolean autoLinkExactKeyOnly = Globals.prefs.getBoolean(JabRefPreferences.AUTOLINK_EXACT_KEY_ONLY);
                result = FileUtil.findAssociatedFiles(entries, extensions, dirs, autoLinkExactKeyOnly);
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

    public CountingUndoManager getUndoManager() {
        return undoManager;
    }

    public MainTable getMainTable() {
        return mainTable;
    }

    public BibDatabaseContext getDatabaseContext() {
        return bibDatabaseContext;
    }

    public SearchQuery getCurrentSearchQuery() {
        return currentSearchQuery;
    }

    public void setCurrentSearchQuery(SearchQuery currentSearchQuery) {
        this.currentSearchQuery = currentSearchQuery;
    }

    public CitationStyleCache getCitationStyleCache() {
        return citationStyleCache;
    }

    public PreviewPanel getPreviewPanel() {
        if (selectionListener == null) {
            // only occurs if this is called while instantiating this BasePanel
            return null;
        }
        return selectionListener.getPreview();
    }

}
