package org.jabref.gui;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.SwingUtilities;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;

import org.jabref.Globals;
import org.jabref.JabRefExecutorService;
import org.jabref.gui.actions.Actions;
import org.jabref.gui.actions.BaseAction;
import org.jabref.gui.actions.CleanupAction;
import org.jabref.gui.actions.CopyBibTeXKeyAndLinkAction;
import org.jabref.gui.actions.GenerateBibtexKeyAction;
import org.jabref.gui.actions.WriteXMPAction;
import org.jabref.gui.autocompleter.AutoCompletePreferences;
import org.jabref.gui.autocompleter.AutoCompleteUpdater;
import org.jabref.gui.autocompleter.PersonNameSuggestionProvider;
import org.jabref.gui.autocompleter.SuggestionProviders;
import org.jabref.gui.collab.DatabaseChangeMonitor;
import org.jabref.gui.collab.DatabaseChangePane;
import org.jabref.gui.desktop.JabRefDesktop;
import org.jabref.gui.edit.ReplaceStringAction;
import org.jabref.gui.entryeditor.EntryEditor;
import org.jabref.gui.exporter.SaveDatabaseAction;
import org.jabref.gui.externalfiles.FindFullTextAction;
import org.jabref.gui.externalfiletype.ExternalFileMenuItem;
import org.jabref.gui.externalfiletype.ExternalFileType;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.filelist.FileListEntry;
import org.jabref.gui.filelist.FileListTableModel;
import org.jabref.gui.icon.JabRefIcon;
import org.jabref.gui.importer.actions.AppendDatabaseAction;
import org.jabref.gui.journals.AbbreviateAction;
import org.jabref.gui.journals.UnabbreviateAction;
import org.jabref.gui.maintable.MainTable;
import org.jabref.gui.maintable.MainTableDataModel;
import org.jabref.gui.mergeentries.MergeEntriesAction;
import org.jabref.gui.mergeentries.MergeWithFetchedEntryAction;
import org.jabref.gui.specialfields.SpecialFieldDatabaseChangeListener;
import org.jabref.gui.specialfields.SpecialFieldValueViewModel;
import org.jabref.gui.specialfields.SpecialFieldViewModel;
import org.jabref.gui.undo.CountingUndoManager;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.undo.UndoableChangeType;
import org.jabref.gui.undo.UndoableFieldChange;
import org.jabref.gui.undo.UndoableInsertEntry;
import org.jabref.gui.undo.UndoableRemoveEntry;
import org.jabref.gui.util.DefaultTaskExecutor;
import org.jabref.gui.worker.CitationStyleToClipboardWorker;
import org.jabref.gui.worker.SendAsEMailAction;
import org.jabref.logic.citationstyle.CitationStyleCache;
import org.jabref.logic.citationstyle.CitationStyleOutputFormat;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.layout.Layout;
import org.jabref.logic.layout.LayoutHelper;
import org.jabref.logic.pdf.FileAnnotationCache;
import org.jabref.logic.search.SearchQuery;
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
import org.jabref.model.entry.FieldName;
import org.jabref.model.entry.InternalBibtexFields;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.event.EntryChangedEvent;
import org.jabref.model.entry.event.EntryEventSource;
import org.jabref.model.entry.specialfields.SpecialField;
import org.jabref.model.entry.specialfields.SpecialFieldValue;
import org.jabref.preferences.JabRefPreferences;
import org.jabref.preferences.PreviewPreferences;

import com.google.common.eventbus.Subscribe;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BasePanel extends StackPane {

    private static final Logger LOGGER = LoggerFactory.getLogger(BasePanel.class);

    private final BibDatabaseContext bibDatabaseContext;
    private final MainTableDataModel tableModel;
    private final DatabaseChangePane changePane;

    private final CitationStyleCache citationStyleCache;
    private final FileAnnotationCache annotationCache;

    private final JabRefFrame frame;
    // The undo manager.
    private final UndoAction undoAction = new UndoAction();
    private final RedoAction redoAction = new RedoAction();
    private final CountingUndoManager undoManager;
    // Keeps track of the string dialog if it is open.
    private final Map<Actions, BaseAction> actions = new HashMap<>();
    private final SidePaneManager sidePaneManager;
    private final PreviewPanel preview;
    private final BasePanelPreferences preferences;
    private final ExternalFileTypes externalFileTypes;

    private final EntryEditor entryEditor;
    private MainTable mainTable;
    // To contain instantiated entry editors. This is to save time
    // As most enums, this must not be null
    private BasePanelMode mode = BasePanelMode.SHOWING_NOTHING;
    private SplitPane splitPane;
    private boolean saving;

    // AutoCompleter used in the search bar
    private PersonNameSuggestionProvider searchAutoCompleter;
    private boolean baseChanged;
    private boolean nonUndoableChange;
    // Used to track whether the base has changed since last save.
    private BibEntry showing;

    private SuggestionProviders suggestionProviders;

    @SuppressWarnings({"FieldCanBeLocal", "unused"}) private Subscription dividerPositionSubscription;

    // the query the user searches when this BasePanel is active
    private Optional<SearchQuery> currentSearchQuery = Optional.empty();

    private Optional<DatabaseChangeMonitor> changeMonitor = Optional.empty();
    private final DialogService dialogService;

    public BasePanel(JabRefFrame frame, BasePanelPreferences preferences, BibDatabaseContext bibDatabaseContext, ExternalFileTypes externalFileTypes) {
        this.preferences = Objects.requireNonNull(preferences);
        this.frame = Objects.requireNonNull(frame);
        this.bibDatabaseContext = Objects.requireNonNull(bibDatabaseContext);
        this.externalFileTypes = Objects.requireNonNull(externalFileTypes);
        this.undoManager = frame.getUndoManager();
        this.dialogService = frame.getDialogService();

        bibDatabaseContext.getDatabase().registerListener(this);
        bibDatabaseContext.getMetaData().registerListener(this);

        this.sidePaneManager = frame.getSidePaneManager();
        this.tableModel = new MainTableDataModel(getBibDatabaseContext());

        citationStyleCache = new CitationStyleCache(bibDatabaseContext);
        annotationCache = new FileAnnotationCache(bibDatabaseContext, Globals.prefs.getFilePreferences());

        setupMainPanel();

        setupActions();

        this.getDatabase().registerListener(new SearchListener());
        this.getDatabase().registerListener(new EntryRemovedListener());

        // ensure that at each addition of a new entry, the entry is added to the groups interface
        this.bibDatabaseContext.getDatabase().registerListener(new GroupTreeListener());
        // ensure that all entry changes mark the panel as changed
        this.bibDatabaseContext.getDatabase().registerListener(this);

        Optional<File> file = bibDatabaseContext.getDatabaseFile();
        if (file.isPresent()) {
            // Register so we get notifications about outside changes to the file.
            changeMonitor = Optional.of(new DatabaseChangeMonitor(bibDatabaseContext, Globals.getFileUpdateMonitor(), Globals.TASK_EXECUTOR));
            changePane = new DatabaseChangePane(mainTable.getPane(), bibDatabaseContext, changeMonitor.get());
            getChildren().add(changePane);
        } else {
            if (bibDatabaseContext.getDatabase().hasEntries()) {
                // if the database is not empty and no file is assigned,
                // the database came from an import and has to be treated somehow
                // -> mark as changed
                this.baseChanged = true;
            }
            changePane = null;
        }

        this.getDatabase().registerListener(new UpdateTimestampListener(Globals.prefs));

        this.entryEditor = new EntryEditor(this, preferences.getEntryEditorPreferences(), Globals.getFileUpdateMonitor(), dialogService, externalFileTypes, Globals.TASK_EXECUTOR);

        this.preview = new PreviewPanel(this, getBibDatabaseContext(), preferences.getKeyBindings(), preferences.getPreviewPreferences(), dialogService, externalFileTypes);
        frame().getGlobalSearchBar().getSearchQueryHighlightObservable().addSearchListener(preview);

    }

    @Subscribe
    public void listen(BibDatabaseContextChangedEvent event) {
        this.markBaseChanged();
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
            title.append(this.bibDatabaseContext.getDBMSSynchronizer().getDBName() + " [" + Localization.lang("shared") + "]");
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
        SaveDatabaseAction saveAction = new SaveDatabaseAction(this, Globals.prefs);
        CleanupAction cleanUpAction = new CleanupAction(this, Globals.prefs);

        actions.put(Actions.UNDO, undoAction);
        actions.put(Actions.REDO, redoAction);

        // The action for opening an entry editor.
        actions.put(Actions.EDIT, this::showAndEdit);

        // The action for saving a database.
        actions.put(Actions.SAVE, saveAction::save);

        actions.put(Actions.SAVE_AS, saveAction::saveAs);

        actions.put(Actions.SAVE_SELECTED_AS_PLAIN, saveAction::saveSelectedAsPlain);

        // The action for copying selected entries.
        actions.put(Actions.COPY, this::copy);

        actions.put(Actions.PRINT_PREVIEW, new PrintPreviewAction());

        actions.put(Actions.CUT, this::cut);

        actions.put(Actions.DELETE, () -> delete(false));

        // The action for pasting entries or cell contents.
        //  - more robust detection of available content flavors (doesn't only look at first one offered)
        //  - support for parsing string-flavor clipboard contents which are bibtex entries.
        //    This allows you to (a) paste entire bibtex entries from a text editor, web browser, etc
        //                       (b) copy and paste entries between multiple instances of JabRef (since
        //         only the text representation seems to get as far as the X clipboard, at least on my system)
        actions.put(Actions.PASTE, this::paste);

        actions.put(Actions.SELECT_ALL, mainTable.getSelectionModel()::selectAll);

        // The action for auto-generating keys.
        actions.put(Actions.MAKE_KEY, new GenerateBibtexKeyAction(this, frame.getDialogService()));

        // The action for cleaning up entry.
        actions.put(Actions.CLEANUP, cleanUpAction);

        actions.put(Actions.MERGE_ENTRIES, () -> new MergeEntriesAction(frame).execute());

        // The action for copying the selected entry's key.
        actions.put(Actions.COPY_KEY, this::copyKey);

        // The action for copying the selected entry's title.
        actions.put(Actions.COPY_TITLE, this::copyTitle);

        // The action for copying a cite for the selected entry.
        actions.put(Actions.COPY_CITE_KEY, this::copyCiteKey);

        // The action for copying the BibTeX key and the title for the first selected entry
        actions.put(Actions.COPY_KEY_AND_TITLE, this::copyKeyAndTitle);

        actions.put(Actions.COPY_CITATION_ASCII_DOC, () -> copyCitationToClipboard(CitationStyleOutputFormat.ASCII_DOC));
        actions.put(Actions.COPY_CITATION_XSLFO, () -> copyCitationToClipboard(CitationStyleOutputFormat.XSL_FO));
        actions.put(Actions.COPY_CITATION_HTML, () -> copyCitationToClipboard(CitationStyleOutputFormat.HTML));
        actions.put(Actions.COPY_CITATION_RTF, () -> copyCitationToClipboard(CitationStyleOutputFormat.RTF));
        actions.put(Actions.COPY_CITATION_TEXT, () -> copyCitationToClipboard(CitationStyleOutputFormat.TEXT));

        // The action for copying the BibTeX keys as hyperlinks to the urls of the selected entries
        actions.put(Actions.COPY_KEY_AND_LINK, new CopyBibTeXKeyAndLinkAction(mainTable, Globals.clipboardManager));

        actions.put(Actions.MERGE_DATABASE, new AppendDatabaseAction(frame, this));

        actions.put(Actions.OPEN_EXTERNAL_FILE, this::openExternalFile);

        actions.put(Actions.OPEN_FOLDER, () -> JabRefExecutorService.INSTANCE.execute(() -> {
            final List<Path> files = FileUtil.getListOfLinkedFiles(mainTable.getSelectedEntries(), bibDatabaseContext.getFileDirectoriesAsPaths(Globals.prefs.getFilePreferences()));
            for (final Path f : files) {
                try {
                    JabRefDesktop.openFolderAndSelectFile(f.toAbsolutePath());
                } catch (IOException e) {
                    LOGGER.info("Could not open folder", e);
                }
            }
        }));

        actions.put(Actions.OPEN_CONSOLE, () -> JabRefDesktop.openConsole(frame.getCurrentBasePanel().getBibDatabaseContext().getDatabaseFile().orElse(null)));

        actions.put(Actions.PULL_CHANGES_FROM_SHARED_DATABASE, () -> {
            DatabaseSynchronizer dbmsSynchronizer = frame.getCurrentBasePanel().getBibDatabaseContext().getDBMSSynchronizer();
            dbmsSynchronizer.pullChanges();
        });

        actions.put(Actions.OPEN_URL, new OpenURLAction());

        actions.put(Actions.MERGE_WITH_FETCHED_ENTRY, new MergeWithFetchedEntryAction(this, frame.getDialogService()));

        actions.put(Actions.REPLACE_ALL, () -> (new ReplaceStringAction(this)).execute());

        actions.put(new SpecialFieldValueViewModel(SpecialField.RELEVANCE.getValues().get(0)).getCommand(),
                    new SpecialFieldViewModel(SpecialField.RELEVANCE, undoManager).getSpecialFieldAction(SpecialField.RELEVANCE.getValues().get(0), frame));

        actions.put(new SpecialFieldValueViewModel(SpecialField.QUALITY.getValues().get(0)).getCommand(),
                    new SpecialFieldViewModel(SpecialField.QUALITY, undoManager).getSpecialFieldAction(SpecialField.QUALITY.getValues().get(0), frame));

        actions.put(new SpecialFieldValueViewModel(SpecialField.PRINTED.getValues().get(0)).getCommand(),
                    new SpecialFieldViewModel(SpecialField.PRINTED, undoManager).getSpecialFieldAction(SpecialField.PRINTED.getValues().get(0), frame));

        for (SpecialFieldValue prio : SpecialField.PRIORITY.getValues()) {
            actions.put(new SpecialFieldValueViewModel(prio).getCommand(),
                        new SpecialFieldViewModel(SpecialField.PRIORITY, undoManager).getSpecialFieldAction(prio, this.frame));
        }
        for (SpecialFieldValue rank : SpecialField.RANKING.getValues()) {
            actions.put(new SpecialFieldValueViewModel(rank).getCommand(),
                        new SpecialFieldViewModel(SpecialField.RANKING, undoManager).getSpecialFieldAction(rank, this.frame));
        }
        for (SpecialFieldValue status : SpecialField.READ_STATUS.getValues()) {
            actions.put(new SpecialFieldValueViewModel(status).getCommand(),
                        new SpecialFieldViewModel(SpecialField.READ_STATUS, undoManager).getSpecialFieldAction(status, this.frame));
        }

        actions.put(Actions.TOGGLE_PREVIEW, () -> {
            PreviewPreferences previewPreferences = Globals.prefs.getPreviewPreferences();
            boolean enabled = !previewPreferences.isPreviewPanelEnabled();
            PreviewPreferences newPreviewPreferences = previewPreferences.getBuilder()
                                                                         .withPreviewPanelEnabled(enabled)
                                                                         .build();
            Globals.prefs.storePreviewPreferences(newPreviewPreferences);
            DefaultTaskExecutor.runInJavaFXThread(() -> setPreviewActiveBasePanels(enabled));
        });

        actions.put(Actions.NEXT_PREVIEW_STYLE, this::nextPreviewStyle);
        actions.put(Actions.PREVIOUS_PREVIEW_STYLE, this::previousPreviewStyle);

        actions.put(Actions.SEND_AS_EMAIL, new SendAsEMailAction(frame));

        actions.put(Actions.WRITE_XMP, new WriteXMPAction(this)::execute);

        actions.put(Actions.ABBREVIATE_ISO, new AbbreviateAction(this, true));
        actions.put(Actions.ABBREVIATE_MEDLINE, new AbbreviateAction(this, false));
        actions.put(Actions.UNABBREVIATE, new UnabbreviateAction(this));

        actions.put(Actions.DOWNLOAD_FULL_TEXT, new FindFullTextAction(this)::execute);
    }

    /**
     * Generates and copies citations based on the selected entries to the clipboard
     *
     * @param outputFormat the desired {@link CitationStyleOutputFormat}
     */
    private void copyCitationToClipboard(CitationStyleOutputFormat outputFormat) {
        CitationStyleToClipboardWorker worker = new CitationStyleToClipboardWorker(this, outputFormat, dialogService, Globals.clipboardManager, Globals.prefs.getPreviewPreferences());
        worker.copyCitationStyleToClipboard(Globals.TASK_EXECUTOR);
    }

    /**
     * Removes the selected entries from the database
     *
     * @param cut If false the user will get asked if he really wants to delete the entries, and it will be localized as
     *            "deleted". If true the action will be localized as "cut"
     */
    public void delete(boolean cut) {
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

        NamedCompound compound;
        if (cut) {
            compound = new NamedCompound((entries.size() > 1 ? Localization.lang("cut entries") : Localization.lang("cut entry")));
        } else {
            compound = new NamedCompound((entries.size() > 1 ? Localization.lang("delete entries") : Localization.lang("delete entry")));
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

    public void delete(BibEntry entry) {
        delete(false, Collections.singletonList(entry));
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
            Globals.clipboardManager.setContent(String.join("\n", titles));

            if (titles.size() == selectedBibEntries.size()) {
                // All entries had titles.
                output((selectedBibEntries.size() > 1 ? Localization.lang("Copied titles") : Localization.lang("Copied title")) + '.');
            } else {
                output(Localization.lang("Warning: %0 out of %1 entries have undefined title.", Integer.toString(selectedBibEntries.size() - titles.size()), Integer.toString(selectedBibEntries.size())));
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
            Globals.clipboardManager.setContent(citeCommand + "{" + sb + '}');

            if (keys.size() == bes.size()) {
                // All entries had keys.
                output(bes.size() > 1 ? Localization.lang("Copied keys") : Localization.lang("Copied key") + '.');
            } else {
                output(Localization.lang("Warning: %0 out of %1 entries have undefined BibTeX key.", Integer.toString(bes.size() - keys.size()), Integer.toString(bes.size())));
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

            Globals.clipboardManager.setContent(String.join(",", keys));

            if (keys.size() == bes.size()) {
                // All entries had keys.
                output((bes.size() > 1 ? Localization.lang("Copied keys") : Localization.lang("Copied key")) + '.');
            } else {
                output(Localization.lang("Warning: %0 out of %1 entries have undefined BibTeX key.", Integer.toString(bes.size() - keys.size()), Integer.toString(bes.size())));
            }
        }
    }

    private void copyKeyAndTitle() {
        List<BibEntry> bes = mainTable.getSelectedEntries();
        if (!bes.isEmpty()) {
            // OK: in a future version, this string should be configurable to allow arbitrary exports
            StringReader sr = new StringReader("\\bibtexkey - \\begin{title}\\format[RemoveBrackets]{\\title}\\end{title}\n");
            Layout layout;
            try {
                layout = new LayoutHelper(sr, Globals.prefs.getLayoutFormatterPreferences(Globals.journalAbbreviationLoader))
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

            Globals.clipboardManager.setContent(sb.toString());

            if (copied == bes.size()) {
                // All entries had keys.
                output((bes.size() > 1 ? Localization.lang("Copied keys") : Localization.lang("Copied key")) + '.');
            } else {
                output(Localization.lang("Warning: %0 out of %1 entries have undefined BibTeX key.", Integer.toString(bes.size() - copied), Integer.toString(bes.size())));
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
            ExternalFileMenuItem item = new ExternalFileMenuItem(frame(), "", flEntry.getLink(), flEntry.getType().map(ExternalFileType::getIcon).map(JabRefIcon::getSmallIcon).orElse(null), bibDatabaseContext, flEntry.getType());
            item.doClick();
        });
    }

    /**
     * This method is called from JabRefFrame if a database specific action is requested by the user. Runs the command
     * if it is defined, or prints an error message to the standard error stream.
     *
     * @param command The name of the command to run.
     */
    public void runCommand(final Actions command) {
        if (!actions.containsKey(command)) {
            LOGGER.info("No action defined for '" + command + '\'');
            return;
        }

        BaseAction action = actions.get(command);
        try {
            action.action();
        } catch (Throwable ex) {
            LOGGER.error("runCommand error: " + ex.getMessage(), ex);
        }
    }

    public void registerUndoableChanges(List<FieldChange> changes) {
        NamedCompound ce = new NamedCompound(Localization.lang("Save actions"));
        for (FieldChange change : changes) {
            ce.addEdit(new UndoableFieldChange(change));
        }
        ce.end();
        if (ce.hasEdits()) {
            getUndoManager().addEdit(ce);
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

                // Set owner and timestamp
                UpdateField.setAutomaticFields(bibEntry, true, true, Globals.prefs.getUpdateFieldPreferences());

                // Create an UndoableInsertEntry object.
                getUndoManager().addEdit(new UndoableInsertEntry(bibDatabaseContext.getDatabase(), bibEntry));
                output(Localization.lang("Added new '%0' entry.", bibEntry.getType()));

                markBaseChanged(); // The database just changed.
                if (Globals.prefs.getBoolean(JabRefPreferences.AUTO_OPEN_FORM)) {
                    showAndEdit(bibEntry);
                }
                clearAndSelect(bibEntry);
            } catch (KeyCollisionException ex) {
                LOGGER.info("Collision for bibtex key" + bibEntry.getId(), ex);
            }
        }
    }

    public void editEntryAndFocusField(BibEntry entry, String fieldName) {
        showAndEdit(entry);
        Platform.runLater(() -> {
            // Focus field and entry in main table (async to give entry editor time to load)
            entryEditor.setFocusToField(fieldName);
            clearAndSelect(entry);
        });
    }

    public void updateTableFont() {
        mainTable.updateFont();
    }

    private void createMainTable() {
        bibDatabaseContext.getDatabase().registerListener(SpecialFieldDatabaseChangeListener.INSTANCE);

        mainTable = new MainTable(tableModel, frame, this, bibDatabaseContext, preferences.getTablePreferences(), externalFileTypes, preferences.getKeyBindings());

        mainTable.updateFont();

        // Add the listener that binds selection to state manager (TODO: should be replaced by proper JavaFX binding as soon as table is implemented in JavaFX)
        mainTable.addSelectionListener(listEvent -> Globals.stateManager.setSelectedEntries(mainTable.getSelectedEntries()));

        // Update entry editor and preview according to selected entries
        mainTable.addSelectionListener(event -> mainTable.getSelectedEntries()
                                                         .stream()
                                                         .findFirst()
                                                         .ifPresent(entry -> {
                                                             preview.setEntry(entry);
                                                             entryEditor.setEntry(entry);
                                                         }));

        // TODO: Register these actions globally
        /*
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
        */
    }

    public void setupMainPanel() {
        splitPane = new SplitPane();
        splitPane.setOrientation(Orientation.VERTICAL);
        adjustSplitter(); // restore last splitting state (before mainTable is created as creation affects the stored size of the entryEditors)

        createMainTable();

        ScrollPane pane = mainTable.getPane();
        AnchorPane anchorPane = new AnchorPane(pane);
        AnchorPane.setBottomAnchor(pane, 0.0);
        AnchorPane.setTopAnchor(pane, 0.0);
        AnchorPane.setLeftAnchor(pane, 0.0);
        AnchorPane.setRightAnchor(pane, 0.0);
        splitPane.getItems().add(anchorPane);
        this.getChildren().setAll(splitPane);

        // Set up name autocompleter for search:
        instantiateSearchAutoCompleter();
        this.getDatabase().registerListener(new SearchAutoCompleteListener());

        setupAutoCompletion();

        // Saves the divider position as soon as it changes
        // We need to keep a reference to the subscription, otherwise the binding gets garbage collected
        dividerPositionSubscription = EasyBind.monadic(Bindings.valueAt(splitPane.getDividers(), 0))
                                              .flatMap(SplitPane.Divider::positionProperty)
                                              .subscribe((observable, oldValue, newValue) -> saveDividerLocation(newValue));
    }

    /**
     * Set up auto completion for this database
     */
    private void setupAutoCompletion() {
        AutoCompletePreferences autoCompletePreferences = preferences.getAutoCompletePreferences();
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

    private void adjustSplitter() {
        if (mode == BasePanelMode.SHOWING_PREVIEW) {
            splitPane.setDividerPositions(Globals.prefs.getPreviewPreferences().getPreviewPanelDividerPosition().doubleValue());
        } else if (mode == BasePanelMode.SHOWING_EDITOR) {
            splitPane.setDividerPositions(preferences.getEntryEditorDividerPosition());
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
        DefaultTaskExecutor.runInJavaFXThread(() -> {

            showBottomPane(BasePanelMode.SHOWING_EDITOR);

            if (entry != getShowing()) {
                entryEditor.setEntry(entry);
                showing = entry;
            }
            entryEditor.requestFocus();

        });
    }

    private void showBottomPane(BasePanelMode newMode) {
        Node pane;
        switch (newMode) {
            case SHOWING_PREVIEW:
                pane = preview;
                break;
            case SHOWING_EDITOR:
                pane = entryEditor;
                break;
            default:
                throw new UnsupportedOperationException("new mode not recognized: " + newMode.name());
        }
        if (splitPane.getItems().size() == 2) {
            splitPane.getItems().set(1, pane);
        } else {
            splitPane.getItems().add(1, pane);
        }
        mode = newMode;
        adjustSplitter();
    }

    private void showAndEdit() {
        if (!mainTable.getSelectedEntries().isEmpty()) {
            showAndEdit(mainTable.getSelectedEntries().get(0));
        }
    }

    /**
     * Sets the given preview panel as the bottom component in the split panel. Updates the mode to SHOWING_PREVIEW.
     *
     * @param entry The entry to show in the preview.
     */
    private void showPreview(BibEntry entry) {
        showBottomPane(BasePanelMode.SHOWING_PREVIEW);

        preview.setEntry(entry);
    }

    private void showPreview() {
        if (!mainTable.getSelectedEntries().isEmpty()) {
            showPreview(mainTable.getSelectedEntries().get(0));
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
    public void closeBottomPane() {
        mode = BasePanelMode.SHOWING_NOTHING;
        splitPane.getItems().removeAll(entryEditor, preview);
    }

    /**
     * This method selects the given entry, and scrolls it into view in the table. If an entryEditor is shown, it is
     * given focus afterwards.
     */
    public void clearAndSelect(final BibEntry bibEntry) {
        mainTable.clearAndSelect(bibEntry);
    }

    public void selectPreviousEntry() {
        mainTable.getSelectionModel().clearAndSelect(mainTable.getSelectionModel().getSelectedIndex() - 1);
    }

    public void selectNextEntry() {
        mainTable.getSelectionModel().clearAndSelect(mainTable.getSelectionModel().getSelectedIndex() + 1);
    }

    /**
     * This method is called from an EntryEditor when it should be closed. We relay to the selection listener, which
     * takes care of the rest.
     *
     * @param editor The entry editor to close.
     */
    public void entryEditorClosing(EntryEditor editor) {
        if (Globals.prefs.getPreviewPreferences().isPreviewPanelEnabled()) {
            showPreview(editor.getEntry());
        } else {
            closeBottomPane();
        }
        mainTable.requestFocus();
    }

    /**
     * Closes the entry editor or preview panel if it is showing the given entry.
     */
    public void ensureNotShowingBottomPanel(BibEntry entry) {
        if (((mode == BasePanelMode.SHOWING_EDITOR) && (entryEditor.getEntry() == entry))
            || ((mode == BasePanelMode.SHOWING_PREVIEW) && (preview.getEntry() == entry))) {
            closeBottomPane();
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
        DefaultTaskExecutor.runInJavaFXThread(frame::updateAllTabTitles);
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
            boolean proceed = dialogService.showConfirmationDialogAndWait(Localization.lang("Change entry type"), Localization.lang("Multiple entries selected. Do you want to change the type of all these to '%0'?"));
            if (!proceed) {
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
            String title = Localization.lang("Delete entry");
            String message = Localization.lang("Really delete the selected entry?");
            String okButton = Localization.lang("Delete entry");
            String cancelButton = Localization.lang("Keep entry");
            if (numberOfEntries > 1) {
                title = Localization.lang("Delete multiple entries");
                message = Localization.lang("Really delete the %0 selected entries?", Integer.toString(numberOfEntries));
                okButton = Localization.lang("Delete entries");
                cancelButton = Localization.lang("Keep entries");
            }

            return dialogService.showConfirmationDialogWithOptOutAndWait(title,
                                                                         message,
                                                                         okButton,
                                                                         cancelButton,
                                                                         Localization.lang("Disable this confirmation dialog"),
                                                                         optOut -> Globals.prefs.putBoolean(JabRefPreferences.CONFIRM_DELETE, !optOut));
        } else {
            return true;
        }
    }

    /**
     * Depending on whether a preview or an entry editor is showing, save the current divider location in the correct preference setting.
     */
    private void saveDividerLocation(Number position) {
        if (position == null) {
            return;
        }

        if (mode == BasePanelMode.SHOWING_PREVIEW) {
            PreviewPreferences previewPreferences = Globals.prefs.getPreviewPreferences()
                                                                 .getBuilder()
                                                                 .withPreviewPanelDividerPosition(position)
                                                                 .build();
            Globals.prefs.storePreviewPreferences(previewPreferences);
        } else if (mode == BasePanelMode.SHOWING_EDITOR) {
            preferences.setEntryEditorDividerPosition(position.doubleValue());
        }
    }

    /**
     * Perform necessary cleanup when this BasePanel is closed.
     */
    public void cleanUp() {
        changeMonitor.ifPresent(DatabaseChangeMonitor::unregister);
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

    public String formatOutputMessage(String start, int count) {
        return String.format("%s %d %s.", start, count, (count > 1 ? Localization.lang("entries") : Localization.lang("entry")));
    }

    /**
     * Set the preview active state for all BasePanel instances.
     */
    private void setPreviewActiveBasePanels(boolean enabled) {
        for (int i = 0; i < frame.getTabbedPane().getTabs().size(); i++) {
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
        changeMonitor = Optional.of(new DatabaseChangeMonitor(bibDatabaseContext, Globals.getFileUpdateMonitor(), Globals.TASK_EXECUTOR));
    }

    public void updateTimeStamp() {
        changeMonitor.ifPresent(DatabaseChangeMonitor::markAsSaved);
    }

    public void copy() {
        mainTable.copy();
    }

    public void paste() {
        mainTable.paste();
    }

    public void cut() {
        mainTable.cut();
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
            final List<Path> dirs = basePanel.getBibDatabaseContext().getFileDirectoriesAsPaths(Globals.prefs.getFilePreferences());
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
            ensureNotShowingBottomPanel(entryRemovedEvent.getBibEntry());
        }
    }

    /**
     * Ensures that the search auto completer is up to date when entries are changed AKA Let the auto completer, if any,
     * harvest words from the entry
     * Actual methods for autocomplete indexing  must run in javafx thread
     */
    private class SearchAutoCompleteListener {

        @Subscribe
        public void listen(EntryAddedEvent addedEntryEvent) {
            DefaultTaskExecutor.runInJavaFXThread(() -> searchAutoCompleter.indexEntry(addedEntryEvent.getBibEntry()));
        }

        @Subscribe
        public void listen(EntryChangedEvent entryChangedEvent) {
            DefaultTaskExecutor.runInJavaFXThread(() -> searchAutoCompleter.indexEntry(entryChangedEvent.getBibEntry()));
        }
    }

    /**
     * Ensures that the results of the current search are updated when a new entry is inserted into the database
     * Actual methods for performing search must run in javafx thread
     */
    private class SearchListener {

        @Subscribe
        public void listen(EntryAddedEvent addedEntryEvent) {
            DefaultTaskExecutor.runInJavaFXThread(() -> frame.getGlobalSearchBar().performSearch());
        }

        @Subscribe
        public void listen(EntryChangedEvent entryChangedEvent) {
            DefaultTaskExecutor.runInJavaFXThread(() -> frame.getGlobalSearchBar().performSearch());
        }

        @Subscribe
        public void listen(EntryRemovedEvent removedEntryEvent) {
            // IMO only used to update the status (found X entries)
            DefaultTaskExecutor.runInJavaFXThread(() -> frame.getGlobalSearchBar().performSearch());
        }
    }

    @Subscribe
    public void listen(EntryChangedEvent entryChangedEvent) {
        this.markBaseChanged();
    }

    private class UndoAction implements BaseAction {

        @Override
        public void action() {
            try {
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

                    List<LinkedFile> files = bes.get(0).getFiles();

                    Optional<LinkedFile> linkedFile = files.stream()
                                                           .filter(file -> (FieldName.URL.equalsIgnoreCase(file.getFileType())
                                                                            || FieldName.PS.equalsIgnoreCase(file.getFileType())
                                                                            || FieldName.PDF.equalsIgnoreCase(file.getFileType())))
                                                           .findFirst();

                    if (linkedFile.isPresent()) {

                        try {

                            JabRefDesktop.openExternalFileAnyFormat(bibDatabaseContext,
                                                                    linkedFile.get().getLink(),
                                                                    ExternalFileTypes.getInstance().fromLinkedFile(linkedFile.get(), true));

                            output(Localization.lang("External viewer called") + '.');
                        } catch (IOException e) {
                            output(Localization.lang("Could not open link"));
                            LOGGER.info("Could not open link", e);
                        }
                    } else {
                        output(Localization.lang("No URL defined") + '.');
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
        public void action() {
            showPreview();
            preview.print();
        }
    }
}
