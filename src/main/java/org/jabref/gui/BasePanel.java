package org.jabref.gui;

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
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;

import org.jabref.Globals;
import org.jabref.JabRefExecutorService;
import org.jabref.gui.actions.Actions;
import org.jabref.gui.actions.BaseAction;
import org.jabref.gui.actions.CleanupAction;
import org.jabref.gui.actions.CopyBibTeXKeyAndLinkAction;
import org.jabref.gui.autocompleter.AutoCompletePreferences;
import org.jabref.gui.autocompleter.AutoCompleteUpdater;
import org.jabref.gui.autocompleter.PersonNameSuggestionProvider;
import org.jabref.gui.autocompleter.SuggestionProviders;
import org.jabref.gui.bibtexkeypattern.SearchFixDuplicateLabels;
import org.jabref.gui.collab.DatabaseChangeMonitor;
import org.jabref.gui.collab.FileUpdatePanel;
import org.jabref.gui.contentselector.ContentSelectorDialog;
import org.jabref.gui.customjfx.CustomJFXPanel;
import org.jabref.gui.desktop.JabRefDesktop;
import org.jabref.gui.entryeditor.EntryEditor;
import org.jabref.gui.exporter.ExportToClipboardAction;
import org.jabref.gui.exporter.SaveDatabaseAction;
import org.jabref.gui.externalfiles.FindFullTextAction;
import org.jabref.gui.externalfiles.SynchronizeFileField;
import org.jabref.gui.externalfiles.WriteXMPAction;
import org.jabref.gui.externalfiletype.ExternalFileMenuItem;
import org.jabref.gui.externalfiletype.ExternalFileType;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.fieldeditors.FieldEditor;
import org.jabref.gui.filelist.AttachFileAction;
import org.jabref.gui.filelist.FileListEntry;
import org.jabref.gui.filelist.FileListTableModel;
import org.jabref.gui.groups.GroupAddRemoveDialog;
import org.jabref.gui.importer.actions.AppendDatabaseAction;
import org.jabref.gui.journals.AbbreviateAction;
import org.jabref.gui.journals.UnabbreviateAction;
import org.jabref.gui.keyboard.KeyBinding;
import org.jabref.gui.maintable.MainTable;
import org.jabref.gui.maintable.MainTableDataModel;
import org.jabref.gui.maintable.MainTableFormat;
import org.jabref.gui.maintable.MainTableSelectionListener;
import org.jabref.gui.mergeentries.MergeEntriesDialog;
import org.jabref.gui.mergeentries.MergeWithFetchedEntryAction;
import org.jabref.gui.plaintextimport.TextInputDialog;
import org.jabref.gui.specialfields.SpecialFieldDatabaseChangeListener;
import org.jabref.gui.specialfields.SpecialFieldValueViewModel;
import org.jabref.gui.specialfields.SpecialFieldViewModel;
import org.jabref.gui.undo.CountingUndoManager;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.undo.UndoableChangeType;
import org.jabref.gui.undo.UndoableFieldChange;
import org.jabref.gui.undo.UndoableInsertEntry;
import org.jabref.gui.undo.UndoableKeyChange;
import org.jabref.gui.undo.UndoableRemoveEntry;
import org.jabref.gui.util.DefaultTaskExecutor;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.gui.util.component.CheckBoxMessage;
import org.jabref.gui.worker.AbstractWorker;
import org.jabref.gui.worker.CallBack;
import org.jabref.gui.worker.CitationStyleToClipboardWorker;
import org.jabref.gui.worker.MarkEntriesAction;
import org.jabref.gui.worker.SendAsEMailAction;
import org.jabref.logic.bibtexkeypattern.BibtexKeyGenerator;
import org.jabref.logic.citationstyle.CitationStyleCache;
import org.jabref.logic.citationstyle.CitationStyleOutputFormat;
import org.jabref.logic.exporter.BibtexDatabaseWriter;
import org.jabref.logic.exporter.FileSaveSession;
import org.jabref.logic.exporter.SaveException;
import org.jabref.logic.exporter.SavePreferences;
import org.jabref.logic.exporter.SaveSession;
import org.jabref.logic.l10n.Encodings;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.layout.Layout;
import org.jabref.logic.layout.LayoutHelper;
import org.jabref.logic.pdf.FileAnnotationCache;
import org.jabref.logic.search.SearchQuery;
import org.jabref.logic.util.FileType;
import org.jabref.logic.util.UpdateField;
import org.jabref.logic.util.io.FileFinder;
import org.jabref.logic.util.io.FileFinders;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.FieldChange;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.KeyCollisionException;
import org.jabref.model.database.event.BibDatabaseContextChangedEvent;
import org.jabref.model.database.event.CoarseChangeFilter;
import org.jabref.model.database.event.EntryAddedEvent;
import org.jabref.model.database.event.EntryRemovedEvent;
import org.jabref.model.database.shared.DatabaseLocation;
import org.jabref.model.database.shared.DatabaseSynchronizer;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.EntryType;
import org.jabref.model.entry.FieldName;
import org.jabref.model.entry.InternalBibtexFields;
import org.jabref.model.entry.event.EntryChangedEvent;
import org.jabref.model.entry.event.EntryEventSource;
import org.jabref.model.entry.specialfields.SpecialField;
import org.jabref.model.entry.specialfields.SpecialFieldValue;
import org.jabref.model.strings.StringUtil;
import org.jabref.preferences.JabRefPreferences;
import org.jabref.preferences.PreviewPreferences;

import com.google.common.eventbus.Subscribe;
import com.jgoodies.forms.builder.FormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BasePanel extends JPanel implements ClipboardOwner {

    private static final Logger LOGGER = LoggerFactory.getLogger(BasePanel.class);

    // Divider size for BaseFrame split pane. 0 means non-resizable.
    private static final int SPLIT_PANE_DIVIDER_SIZE = 4;

    private final BibDatabaseContext bibDatabaseContext;
    private final MainTableDataModel tableModel;

    private final CitationStyleCache citationStyleCache;
    private final FileAnnotationCache annotationCache;

    private final JabRefFrame frame;
    // The undo manager.
    private final UndoAction undoAction = new UndoAction();
    private final RedoAction redoAction = new RedoAction();
    private final CountingUndoManager undoManager = new CountingUndoManager();
    private final List<BibEntry> previousEntries = new ArrayList<>();
    private final List<BibEntry> nextEntries = new ArrayList<>();
    // Keeps track of the string dialog if it is open.
    private final Map<String, Object> actions = new HashMap<>();
    private final SidePaneManager sidePaneManager;
    private final PreviewPanel preview;
    private final JFXPanel previewContainer;

    // To contain instantiated entry editors. This is to save time
    // As most enums, this must not be null
    private BasePanelMode mode = BasePanelMode.SHOWING_NOTHING;
    private final EntryEditor entryEditor;
    private final JFXPanel entryEditorContainer;
    private MainTableSelectionListener selectionListener;
    private JSplitPane splitPane;
    private boolean saving;

    // AutoCompleter used in the search bar
    private PersonNameSuggestionProvider searchAutoCompleter;
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
    private SuggestionProviders suggestionProviders;

    // the query the user searches when this BasePanel is active
    private Optional<SearchQuery> currentSearchQuery = Optional.empty();

    private Optional<DatabaseChangeMonitor> changeMonitor = Optional.empty();

    public BasePanel(JabRefFrame frame, BibDatabaseContext bibDatabaseContext) {
        Objects.requireNonNull(frame);
        Objects.requireNonNull(bibDatabaseContext);

        this.bibDatabaseContext = bibDatabaseContext;
        bibDatabaseContext.getDatabase().registerListener(this);
        bibDatabaseContext.getMetaData().registerListener(this);

        this.sidePaneManager = frame.getSidePaneManager();
        this.frame = frame;
        this.tableModel = new MainTableDataModel(getBibDatabaseContext());

        citationStyleCache = new CitationStyleCache(bibDatabaseContext);
        annotationCache = new FileAnnotationCache(bibDatabaseContext, Globals.prefs.getFileDirectoryPreferences());

        this.preview = new PreviewPanel(this, getBibDatabaseContext());
        DefaultTaskExecutor.runInJavaFXThread(() -> frame().getGlobalSearchBar().getSearchQueryHighlightObservable().addSearchListener(preview));
        this.previewContainer = CustomJFXPanel.wrap(new Scene(preview));

        setupMainPanel();

        setupActions();

        this.getDatabase().registerListener(new SearchListener());
        this.getDatabase().registerListener(new EntryRemovedListener());

        // ensure that at each addition of a new entry, the entry is added to the groups interface
        this.bibDatabaseContext.getDatabase().registerListener(new GroupTreeListener());

        Optional<File> file = bibDatabaseContext.getDatabaseFile();
        if (file.isPresent()) {
            // Register so we get notifications about outside changes to the file.
            changeMonitor = Optional.of(new DatabaseChangeMonitor(bibDatabaseContext, Globals.getFileUpdateMonitor(), this));
        } else {
            if (bibDatabaseContext.getDatabase().hasEntries()) {
                // if the database is not empty and no file is assigned,
                // the database came from an import and has to be treated somehow
                // -> mark as changed
                this.baseChanged = true;
            }
        }

        this.getDatabase().registerListener(new UpdateTimestampListener(Globals.prefs));

        entryEditor = new EntryEditor(this);
        entryEditorContainer = setupEntryEditor(entryEditor);

    }

    private static JFXPanel setupEntryEditor(EntryEditor entryEditor) {
        JFXPanel container = CustomJFXPanel.wrap(new Scene(entryEditor));
        container.addKeyListener(new KeyAdapter() {

            @Override
            public void keyPressed(KeyEvent e) {

                //We need to consume this event here to prevent the propgation of keybinding events back to the JFrame
                Optional<KeyBinding> keyBinding = Globals.getKeyPrefs().mapToKeyBinding(e);
                if (keyBinding.isPresent()) {
                    switch (keyBinding.get()) {
                        case CUT:
                        case COPY:
                        case PASTE:
                        case DELETE_ENTRY:
                        case SELECT_ALL:
                            e.consume();
                            break;
                        default:
                            //do nothing
                    }
                }
            }
        });
        return container;
    }

    public static void runWorker(AbstractWorker worker) throws Exception {
        // This part uses Spin's features:
        Runnable wrk = worker.getWorker();
        // The Worker returned by getWorker() has been wrapped
        // by Spin.off(), which makes its methods be run in
        // a different thread from the EDT.
        CallBack clb = worker.getCallBack();

        worker.init(); // This method runs in this same thread, the EDT.
        // Useful for initial GUI actions, like printing a message.

        // The CallBack returned by getCallBack() has been wrapped
        // by Spin.over(), which makes its methods be run on
        // the EDT.
        wrk.run(); // Runs the potentially time-consuming action
        // without freezing the GUI. The magic is that THIS line
        // of execution will not continue until run() is finished.
        clb.update(); // Runs the update() method on the EDT.
    }

    @Subscribe
    public void listen(BibDatabaseContextChangedEvent event) {
        SwingUtilities.invokeLater(() -> this.markBaseChanged());

    }

    /**
     * Returns a collection of suggestion providers, which are populated from the current library.
     */
    public SuggestionProviders getSuggestionProviders() {
        return suggestionProviders;
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
                    this.bibDatabaseContext.getDBMSSynchronizer().getDBName() + " [" + Localization.lang("shared")
                            + "]");
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
                BibtexKeyGenerator keyGenerator = new BibtexKeyGenerator(bibDatabaseContext, Globals.prefs.getBibtexKeyPatternPreferences());
                for (BibEntry entry : entries) {
                    Optional<FieldChange> change = keyGenerator.generateAndSetKey(entry);
                    change.ifPresent(fieldChange -> ce.addEdit(new UndoableKeyChange(fieldChange)));
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

        // The action for copying the selected entry's title.
        actions.put(Actions.COPY_TITLE, (BaseAction) () -> copyTitle());

        // The action for copying a cite for the selected entry.
        actions.put(Actions.COPY_CITE_KEY, (BaseAction) () -> copyCiteKey());

        // The action for copying the BibTeX key and the title for the first selected entry
        actions.put(Actions.COPY_KEY_AND_TITLE, (BaseAction) () -> copyKeyAndTitle());

        actions.put(Actions.COPY_CITATION_ASCII_DOC,
                (BaseAction) () -> copyCitationToClipboard(CitationStyleOutputFormat.ASCII_DOC));
        actions.put(Actions.COPY_CITATION_XSLFO,
                (BaseAction) () -> copyCitationToClipboard(CitationStyleOutputFormat.XSL_FO));
        actions.put(Actions.COPY_CITATION_HTML,
                (BaseAction) () -> copyCitationToClipboard(CitationStyleOutputFormat.HTML));
        actions.put(Actions.COPY_CITATION_RTF,
                (BaseAction) () -> copyCitationToClipboard(CitationStyleOutputFormat.RTF));
        actions.put(Actions.COPY_CITATION_TEXT,
                (BaseAction) () -> copyCitationToClipboard(CitationStyleOutputFormat.TEXT));

        // The action for copying the BibTeX keys as hyperlinks to the urls of the selected entries
        actions.put(Actions.COPY_KEY_AND_LINK, new CopyBibTeXKeyAndLinkAction(mainTable));

        actions.put(Actions.MERGE_DATABASE, new AppendDatabaseAction(frame, this));

        actions.put(Actions.ADD_FILE_LINK, new AttachFileAction(this));

        actions.put(Actions.OPEN_EXTERNAL_FILE, (BaseAction) () -> openExternalFile());

        actions.put(Actions.OPEN_FOLDER, (BaseAction) () -> JabRefExecutorService.INSTANCE.execute(() -> {
            final List<Path> files = FileUtil.getListOfLinkedFiles(mainTable.getSelectedEntries(),
                    bibDatabaseContext.getFileDirectoriesAsPaths(Globals.prefs.getFileDirectoryPreferences()));
            for (final Path f : files) {
                try {
                    JabRefDesktop.openFolderAndSelectFile(f.toAbsolutePath());
                } catch (IOException e) {
                    LOGGER.info("Could not open folder", e);
                }
            }
        }));

        actions.put(Actions.OPEN_CONSOLE, (BaseAction) () -> JabRefDesktop
                .openConsole(frame.getCurrentBasePanel().getBibDatabaseContext().getDatabaseFile().orElse(null)));

        actions.put(Actions.PULL_CHANGES_FROM_SHARED_DATABASE, (BaseAction) () -> {
            DatabaseSynchronizer dbmsSynchronizer = frame.getCurrentBasePanel().getBibDatabaseContext().getDBMSSynchronizer();
            dbmsSynchronizer.pullChanges();
        });

        actions.put(Actions.OPEN_URL, new OpenURLAction());

        actions.put(Actions.MERGE_WITH_FETCHED_ENTRY, new MergeWithFetchedEntryAction(this));

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

            BibEntry bibEntry = new BibEntry(tp.getName());
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
                new SpecialFieldViewModel(SpecialField.QUALITY)
                        .getSpecialFieldAction(SpecialField.QUALITY.getValues().get(0), frame));
        actions.put(new SpecialFieldValueViewModel(SpecialField.PRINTED.getValues().get(0)).getActionName(),
                new SpecialFieldViewModel(SpecialField.PRINTED).getSpecialFieldAction(
                        SpecialField.PRINTED.getValues().get(0), frame));

        for (SpecialFieldValue prio : SpecialField.PRIORITY.getValues()) {
            actions.put(new SpecialFieldValueViewModel(prio).getActionName(),
                    new SpecialFieldViewModel(SpecialField.PRIORITY).getSpecialFieldAction(prio, this.frame));
        }
        for (SpecialFieldValue rank : SpecialField.RANKING.getValues()) {
            actions.put(new SpecialFieldValueViewModel(rank).getActionName(),
                    new SpecialFieldViewModel(SpecialField.RANKING).getSpecialFieldAction(rank, this.frame));
        }
        for (SpecialFieldValue status : SpecialField.READ_STATUS.getValues()) {
            actions.put(new SpecialFieldValueViewModel(status).getActionName(),
                    new SpecialFieldViewModel(SpecialField.READ_STATUS).getSpecialFieldAction(status, this.frame));
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

        actions.put(Actions.NEXT_PREVIEW_STYLE, (BaseAction) this::nextPreviewStyle);
        actions.put(Actions.PREVIOUS_PREVIEW_STYLE, (BaseAction) this::previousPreviewStyle);

        actions.put(Actions.MANAGE_SELECTORS, (BaseAction) () -> {
            ContentSelectorDialog csd = new ContentSelectorDialog(frame, frame, BasePanel.this, false, null);
            csd.setLocationRelativeTo(frame);
            csd.setVisible(true);
        });

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

    /**
     * Generates and copies citations based on the selected entries to the clipboard
     *
     * @param outputFormat the desired {@link CitationStyleOutputFormat}
     */
    private void copyCitationToClipboard(CitationStyleOutputFormat outputFormat) {
        new CitationStyleToClipboardWorker(this, outputFormat).execute();
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
     *
     * @param cut If false the user will get asked if he really wants to delete the entries, and it will be localized as
     *            "deleted". If true the action will be localized as "cut"
     */
    private void delete(boolean cut) {
        delete(cut, mainTable.getSelectedEntries());
    }

    /**
     * Removes the selected entries from the database
     *
     * @param cut If false the user will get asked if he really wants to delete the entries, and it will be localized as
     *            "deleted". If true the action will be localized as "cut"
     */
    private void delete(boolean cut, List<BibEntry> entries) {
        if (entries.isEmpty()) {
            return;
        }
        if (!cut && !showDeleteConfirmationDialog(entries.size())) {
            return;
        }

        // select the next entry to stay at the same place as before (or the previous if we're already at the end)
        if (mainTable.getSelectedRow() != (mainTable.getRowCount() - 1)) {
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
        frame.output(
                formatOutputMessage(cut ? Localization.lang("Cut") : Localization.lang("Deleted"), entries.size()));

        // prevent the main table from loosing focus
        mainTable.requestFocus();
    }

    public void delete(BibEntry entry) {
        delete(false, Collections.singletonList(entry));
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

    private void copyTitle() {
        List<BibEntry> selectedBibEntries = mainTable.getSelectedEntries();
        if (!selectedBibEntries.isEmpty()) {
            // Collect all non-null titles.
            List<String> titles = selectedBibEntries.stream()
                    .filter(bibEntry -> bibEntry.getTitle().isPresent())
                    .map(bibEntry -> bibEntry.getTitle().get())
                    .collect(Collectors.toList());

            if (titles.isEmpty()) {
                output(Localization.lang("None of the selected entries have titles."));
                return;
            }
            StringSelection ss = new StringSelection(String.join("\n", titles));
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(ss, BasePanel.this);

            if (titles.size() == selectedBibEntries.size()) {
                // All entries had titles.
                output((selectedBibEntries.size() > 1 ? Localization.lang("Copied titles") : Localization
                        .lang("Copied title")) + '.');
            } else {
                output(Localization.lang("Warning: %0 out of %1 entries have undefined title.",
                        Integer.toString(selectedBibEntries.size() - titles.size()),
                        Integer.toString(selectedBibEntries.size())));
            }
        }
    }

    private void copyCiteKey() {
        List<BibEntry> bes = mainTable.getSelectedEntries();
        if (!bes.isEmpty()) {
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
            String citeCommand = Optional.ofNullable(Globals.prefs.get(JabRefPreferences.CITE_COMMAND))
                    .filter(cite -> cite.contains("\\")) // must contain \
                    .orElse("\\cite");
            StringSelection ss = new StringSelection(citeCommand + "{" + sb + '}');
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
            final List<BibEntry> selectedEntries = mainTable.getSelectedEntries();
            if (selectedEntries.size() != 1) {
                output(Localization.lang("This operation requires exactly one item to be selected."));
                return;
            }

            final BibEntry entry = selectedEntries.get(0);
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
            ExternalFileMenuItem item = new ExternalFileMenuItem(frame(), entry, "", flEntry.getLink(),
                    flEntry.getType().get().getIcon(), bibDatabaseContext, flEntry.getType());
            item.doClick();
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
                runWorker((AbstractWorker) o);
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
                                 SavePreferences.DatabaseSaveType saveType)
            throws SaveException {
        SaveSession session;
        frame.block();
        final String SAVE_DATABASE = Localization.lang("Save library");
        try {
            SavePreferences prefs = Globals.prefs.loadForSaveFromPreferences()
                    .withEncoding(enc)
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
                showAndEdit(ex.getEntry());
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
            final BibEntry be = new BibEntry(actualType.getName());
            try {
                bibDatabaseContext.getDatabase().insertEntry(be);
                // Set owner/timestamp if options are enabled:
                List<BibEntry> list = new ArrayList<>();
                list.add(be);
                UpdateField.setAutomaticFields(list, true, true, Globals.prefs.getUpdateFieldPreferences());

                // Create an UndoableInsertEntry object.
                getUndoManager().addEdit(new UndoableInsertEntry(bibDatabaseContext.getDatabase(), be, BasePanel.this));
                output(Localization.lang("Added new '%0' entry.", actualType.getName().toLowerCase(Locale.ROOT)));

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

                this.showAndEdit(be);

                return be;
            } catch (KeyCollisionException ex) {
                LOGGER.info(ex.getMessage(), ex);
            }
        }
        return null;
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
                getUndoManager().addEdit(new UndoableInsertEntry(bibDatabaseContext.getDatabase(), bibEntry, BasePanel.this));
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

    public void editEntryByIdAndFocusField(final String entryId, final String fieldName) {
        bibDatabaseContext.getDatabase().getEntryById(entryId).ifPresent(entry -> {
            mainTable.setSelected(mainTable.findEntry(entry));
            selectionListener.editSignalled();
            showAndEdit(entry);
            entryEditor.setFocusToField(fieldName);
        });
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

        // Add the listener that binds selection to state manager (TODO: should be replaced by proper JavaFX binding as soon as table is implemented in JavaFX)
        mainTable.addSelectionListener(listEvent -> Platform
                .runLater(() -> Globals.stateManager.setSelectedEntries(mainTable.getSelectedEntries())));

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
                        entryEditorClosing(getEntryEditor());
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

                if (e.isControlDown()) {
                    switch (keyCode) {
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
        boolean floatSearchActive = (mainTable != null) && (this.tableModel.getSearchState() == MainTableDataModel.DisplayOption.FLOAT);

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

        setupAutoCompletion();

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

    /**
     * Set up auto completion for this database
     */
    private void setupAutoCompletion() {
        AutoCompletePreferences autoCompletePreferences = Globals.prefs.getAutoCompletePreferences();
        if (autoCompletePreferences.shouldAutoComplete()) {
            suggestionProviders = new SuggestionProviders(autoCompletePreferences, Globals.journalAbbreviationLoader);
            suggestionProviders.indexDatabase(getDatabase());
            // Ensure that the suggestion providers are in sync with entries
            CoarseChangeFilter changeFilter = new CoarseChangeFilter(bibDatabaseContext);
            changeFilter.registerListener(new AutoCompleteUpdater(suggestionProviders));
        } else {
            // Create empty suggestion providers if auto completion is deactivated
            suggestionProviders = new SuggestionProviders();
        }
    }

    public void updateSearchManager() {
        frame.getGlobalSearchBar().setAutoCompleter(searchAutoCompleter);
    }

    private void instantiateSearchAutoCompleter() {
        searchAutoCompleter = new PersonNameSuggestionProvider(InternalBibtexFields.getPersonNameFields());
        for (BibEntry entry : bibDatabaseContext.getDatabase().getEntries()) {
            searchAutoCompleter.indexEntry(entry);
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

    public EntryEditor getEntryEditor() {
        return entryEditor;
    }

    /**
     * Sets the entry editor as the bottom component in the split pane. If an entry editor already was shown,
     * makes sure that the divider doesn't move. Updates the mode to SHOWING_EDITOR.
     * Then shows the given entry.
     *
     * @param entry The entry to edit.
     */
    public void showAndEdit(BibEntry entry) {

        if (mode == BasePanelMode.SHOWING_EDITOR) {
            Globals.prefs.putInt(JabRefPreferences.ENTRY_EDITOR_HEIGHT, splitPane.getHeight() - splitPane.getDividerLocation());
        }
        mode = BasePanelMode.SHOWING_EDITOR;
        splitPane.setBottomComponent(entryEditorContainer);
        DefaultTaskExecutor.runInJavaFXThread(() -> {
            if (entry != getShowing()) {
                entryEditor.setEntry(entry);
                newEntryShowing(entry);
            }
            entryEditor.requestFocus();

        });
        adjustSplitter();
    }

    /**
     * Sets the given preview panel as the bottom component in the split panel. Updates the mode to SHOWING_PREVIEW.
     *
     * @param entry The entry to show in the preview.
     */
    public void showPreview(BibEntry entry) {
        preview.setEntry(entry);
        mode = BasePanelMode.SHOWING_PREVIEW;
        splitPane.setBottomComponent(previewContainer);
        adjustSplitter();
    }

    private void showPreview() {
        if (!mainTable.getSelected().isEmpty()) {
            showPreview(mainTable.getSelected().get(0));
        }
    }

    public void nextPreviewStyle() {
        cyclePreview(Globals.prefs.getPreviewPreferences().getPreviewCyclePosition() + 1);
    }

    public void previousPreviewStyle() {
        cyclePreview(Globals.prefs.getPreviewPreferences().getPreviewCyclePosition() - 1);
    }

    private void cyclePreview(int newPosition) {
        PreviewPreferences previewPreferences = Globals.prefs.getPreviewPreferences()
                .getBuilder()
                .withPreviewCyclePosition(newPosition)
                .build();
        Globals.prefs.storePreviewPreferences(previewPreferences);

        preview.updateLayout(previewPreferences);
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
        if (((mode == BasePanelMode.SHOWING_EDITOR) && (entryEditor.getEntry() == entry))
                || ((mode == BasePanelMode.SHOWING_PREVIEW) && (selectionListener.getPreview().getEntry() == entry))) {
            hideBottomComponent();
        }
    }

    public void updateEntryEditorIfShowing() {
        if (mode == BasePanelMode.SHOWING_EDITOR) {
            BibEntry currentEntry = entryEditor.getEntry();
            showAndEdit(currentEntry);
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
        if (frame.getStatusLineText().startsWith(Localization.lang("Saved library"))) {
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
                frame.setTabTitle(this, getTabTitle(), getBibDatabaseContext().getDatabaseFile().get().getAbsolutePath());
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
                    Localization.lang("Multiple entries selected. Do you want to change the type of all these to '%0'?", newType),
                    Localization.lang("Change entry type"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (choice == JOptionPane.NO_OPTION) {
                return;
            }
        }

        NamedCompound compound = new NamedCompound(Localization.lang("Change entry type"));
        for (BibEntry entry : entries) {
            compound.addEdit(new UndoableChangeType(entry, entry.getType(), newType));
            DefaultTaskExecutor.runInJavaFXThread(() -> {
                entry.setType(newType);
            });
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

            BibtexKeyGenerator keyGenerator = new BibtexKeyGenerator(bibDatabaseContext, Globals.prefs.getBibtexKeyPatternPreferences());
            for (BibEntry bes : bibDatabaseContext.getDatabase().getEntries()) {
                Optional<String> oldKey = bes.getCiteKeyOptional();
                if (StringUtil.isBlank(oldKey)) {
                    Optional<FieldChange> change = keyGenerator.generateAndSetKey(bes);
                    change.ifPresent(fieldChange -> ce.addEdit(new UndoableKeyChange(fieldChange)));
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

    // Method pertaining to the ClipboardOwner interface.
    @Override
    public void lostOwnership(Clipboard clipboard, Transferable contents) {
        // Nothing
    }

    /**
     * Perform necessary cleanup when this BasePanel is closed.
     */
    public void cleanUp() {
        changeMonitor.ifPresent(DatabaseChangeMonitor::unregister);

        // Check if there is a FileUpdatePanel for this BasePanel being shown. If so,
        // remove it:
        if (sidePaneManager.hasComponent(FileUpdatePanel.class)) {
            FileUpdatePanel fup = (FileUpdatePanel) sidePaneManager.getComponent(FileUpdatePanel.class);
            if (fup.getPanel() == this) {
                sidePaneManager.hideComponent(FileUpdatePanel.class);
            }
        }
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

    public boolean isUpdatedExternally() {
        return changeMonitor.map(DatabaseChangeMonitor::hasBeenModifiedExternally).orElse(false);
    }

    public void markExternalChangesAsResolved() {
        changeMonitor.ifPresent(DatabaseChangeMonitor::markExternalChangesAsResolved);
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

    public boolean isSaving() {
        return saving;
    }

    public void setSaving(boolean saving) {
        this.saving = saving;
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
    private void newEntryShowing(BibEntry entry) {

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

    /**
     * Set the preview active state for all BasePanel instances.
     */
    private void setPreviewActiveBasePanels(boolean enabled) {
        for (int i = 0; i < frame.getTabbedPane().getTabCount(); i++) {
            frame.getBasePanelAt(i).setPreviewActive(enabled);
        }
    }

    private void setPreviewActive(boolean enabled) {
        if (enabled) {
            showPreview();
        } else {
            preview.close();
        }
    }

    public CountingUndoManager getUndoManager() {
        return undoManager;
    }

    public MainTable getMainTable() {
        return mainTable;
    }

    public Optional<SearchQuery> getCurrentSearchQuery() {
        return currentSearchQuery;
    }

    /**
     * Set the query the user currently searches while this basepanel is active
     *
     * @param currentSearchQuery can be null
     */
    public void setCurrentSearchQuery(SearchQuery currentSearchQuery) {
        this.currentSearchQuery = Optional.ofNullable(currentSearchQuery);
    }

    public CitationStyleCache getCitationStyleCache() {
        return citationStyleCache;
    }

    public PreviewPanel getPreviewPanel() {
        return preview;
    }

    public FileAnnotationCache getAnnotationCache() {
        return annotationCache;
    }

    public void resetChangeMonitor() {
        changeMonitor.ifPresent(DatabaseChangeMonitor::unregister);
        changeMonitor = Optional.of(new DatabaseChangeMonitor(bibDatabaseContext, Globals.getFileUpdateMonitor(), this));
    }

    public void updateTimeStamp() {
        changeMonitor.ifPresent(DatabaseChangeMonitor::markAsSaved);
    }

    public Path getTempFile() {
        return changeMonitor.map(DatabaseChangeMonitor::getTempFile).orElse(null);
    }

    private static class SearchAndOpenFile {

        private final BibEntry entry;
        private final BasePanel basePanel;

        public SearchAndOpenFile(final BibEntry entry, final BasePanel basePanel) {
            this.entry = entry;
            this.basePanel = basePanel;
        }

        public void searchAndOpen() {
            if (!Globals.prefs.getBoolean(JabRefPreferences.RUN_AUTOMATIC_FILE_SEARCH)) {
                /*  The search can lead to an unexpected 100% CPU usage which is perceived
                    as a bug, if the search incidentally starts at a directory with lots
                    of stuff below. It is now disabled by default. */
                return;
            }

            final Set<ExternalFileType> types = ExternalFileTypes.getInstance().getExternalFileTypeSelection();
            final List<Path> dirs = basePanel.getBibDatabaseContext().getFileDirectoriesAsPaths(Globals.prefs.getFileDirectoryPreferences());
            final List<String> extensions = types.stream().map(ExternalFileType::getExtension).collect(Collectors.toList());

            // Run the search operation:
            FileFinder fileFinder = FileFinders.constructFromConfiguration(Globals.prefs.getAutoLinkPreferences());
            try {
                List<Path> files = fileFinder.findAssociatedFiles(entry, dirs, extensions);
                if (!files.isEmpty()) {
                    Path file = files.get(0);
                    Optional<ExternalFileType> type = ExternalFileTypes.getInstance().getExternalFileTypeByFile(file);
                    if (type.isPresent()) {
                        JabRefDesktop.openExternalFileAnyFormat(file, basePanel.getBibDatabaseContext(), type);
                        basePanel.output(Localization.lang("External viewer called") + '.');
                    }
                }
            } catch (IOException ex) {
                LOGGER.error("Problems with finding/or opening files ", ex);
                basePanel.output(Localization.lang("Error") + ": " + ex.getMessage());
            }
        }
    }

    private class GroupTreeListener {

        @Subscribe
        public void listen(EntryAddedEvent addedEntryEvent) {
            // if the added entry is an undo don't add it to the current group
            if (addedEntryEvent.getEntryEventSource() == EntryEventSource.UNDO) {
                return;
            }

            // Automatically add new entry to the selected group (or set of groups)
            if (Globals.prefs.getBoolean(JabRefPreferences.AUTO_ASSIGN_GROUP)) {
                final List<BibEntry> entries = Collections.singletonList(addedEntryEvent.getBibEntry());
                Globals.stateManager.getSelectedGroup(bibDatabaseContext).forEach(
                        selectedGroup -> selectedGroup.addEntriesToGroup(entries));
            }
        }
    }

    private class EntryRemovedListener {

        @Subscribe
        public void listen(EntryRemovedEvent entryRemovedEvent) {
            // if the entry that is displayed in the current entry editor is removed, close the entry editor
            if ((mode == BasePanelMode.SHOWING_EDITOR) && entryEditor.getEntry().equals(entryRemovedEvent.getBibEntry())) {
                entryEditor.close();
            }

            BibEntry previewEntry = selectionListener.getPreview().getEntry();
            if ((previewEntry != null) && previewEntry.equals(entryRemovedEvent.getBibEntry())) {
                preview.close();
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
            searchAutoCompleter.indexEntry(addedEntryEvent.getBibEntry());
        }

        @Subscribe
        public void listen(EntryChangedEvent entryChangedEvent) {
            searchAutoCompleter.indexEntry(entryChangedEvent.getBibEntry());
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
            frame.getGlobalSearchBar().setDontSelectSearchBar();
            frame.getGlobalSearchBar().performSearch();
        }

        @Subscribe
        public void listen(EntryRemovedEvent removedEntryEvent) {
            // IMO only used to update the status (found X entries)
            frame.getGlobalSearchBar().performSearch();
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

                    FieldEditor fieldEditor = (FieldEditor) focused;
                    if ((preambleEditor != null) && (fieldEditor.equals(preambleEditor.getFieldEditor()))) {
                        preambleEditor.storeCurrentEdit();
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
                        if (FieldName.URL.equalsIgnoreCase(flEntry.getType().get().getName())
                                || FieldName.PS.equalsIgnoreCase(flEntry.getType().get().getName())
                                || FieldName.PDF.equalsIgnoreCase(flEntry.getType().get().getName())) {
                            entry = flEntry;
                            break;
                        }
                    }
                    if (entry == null) {
                        output(Localization.lang("No URL defined") + '.');
                    } else {
                        try {
                            JabRefDesktop.openExternalFileAnyFormat(bibDatabaseContext, entry.getLink(),
                                    entry.getType());
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
            showPreview();
            preview.print();
        }
    }

    private class SaveSelectedAction implements BaseAction {

        private final SavePreferences.DatabaseSaveType saveType;

        public SaveSelectedAction(SavePreferences.DatabaseSaveType saveType) {
            this.saveType = saveType;
        }

        @Override
        public void action() throws SaveException {
            FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                    .withDefaultExtension(FileType.BIBTEX_DB)
                    .addExtensionFilter(FileType.BIBTEX_DB)
                    .withInitialDirectory(Globals.prefs.get(JabRefPreferences.WORKING_DIRECTORY))
                    .build();

            DialogService ds = new FXDialogService();

            Optional<Path> chosenFile = DefaultTaskExecutor
                    .runInJavaFXThread(() -> ds.showFileSaveDialog(fileDialogConfiguration));

            if (chosenFile.isPresent()) {
                Path path = chosenFile.get();
                saveDatabase(path.toFile(), true, Globals.prefs.getDefaultEncoding(), saveType);
                frame.getFileHistory().newFile(path.toString());
                frame.output(Localization.lang("Saved selected to '%0'.", path.toString()));
            }
        }
    }
}
