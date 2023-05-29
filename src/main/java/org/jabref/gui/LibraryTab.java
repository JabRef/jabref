package org.jabref.gui;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ListChangeListener;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.util.Duration;

import org.jabref.gui.autocompleter.AutoCompletePreferences;
import org.jabref.gui.autocompleter.PersonNameSuggestionProvider;
import org.jabref.gui.autocompleter.SuggestionProviders;
import org.jabref.gui.collab.DatabaseChangeMonitor;
import org.jabref.gui.dialogs.AutosaveUiManager;
import org.jabref.gui.entryeditor.EntryEditor;
import org.jabref.gui.importer.actions.OpenDatabaseAction;
import org.jabref.gui.maintable.MainTable;
import org.jabref.gui.maintable.MainTableDataModel;
import org.jabref.gui.undo.CountingUndoManager;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.undo.UndoableFieldChange;
import org.jabref.gui.undo.UndoableInsertEntries;
import org.jabref.gui.undo.UndoableRemoveEntries;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.DefaultTaskExecutor;
import org.jabref.logic.autosaveandbackup.AutosaveManager;
import org.jabref.logic.autosaveandbackup.BackupManager;
import org.jabref.logic.citationstyle.CitationStyleCache;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.util.FileFieldParser;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.pdf.FileAnnotationCache;
import org.jabref.logic.pdf.search.indexing.IndexingTaskManager;
import org.jabref.logic.pdf.search.indexing.PdfIndexer;
import org.jabref.logic.search.SearchQuery;
import org.jabref.logic.shared.DatabaseLocation;
import org.jabref.logic.util.UpdateField;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.FieldChange;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.event.BibDatabaseContextChangedEvent;
import org.jabref.model.database.event.EntriesAddedEvent;
import org.jabref.model.database.event.EntriesRemovedEvent;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.event.EntriesEventSource;
import org.jabref.model.entry.event.EntryChangedEvent;
import org.jabref.model.entry.event.FieldChangedEvent;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.util.FileUpdateMonitor;
import org.jabref.preferences.PreferencesService;

import com.google.common.eventbus.Subscribe;
import com.tobiasdiez.easybind.EasyBind;
import com.tobiasdiez.easybind.Subscription;
import org.controlsfx.control.NotificationPane;
import org.controlsfx.control.action.Action;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LibraryTab extends Tab {

    private static final Logger LOGGER = LoggerFactory.getLogger(LibraryTab.class);
    private final JabRefFrame frame;
    private final CountingUndoManager undoManager;
    private final DialogService dialogService;
    private final PreferencesService preferencesService;
    private final FileUpdateMonitor fileUpdateMonitor;
    private final StateManager stateManager;
    private final BooleanProperty changedProperty = new SimpleBooleanProperty(false);
    private final BooleanProperty nonUndoableChangeProperty = new SimpleBooleanProperty(false);

    private BibDatabaseContext bibDatabaseContext;
    private MainTableDataModel tableModel;
    private CitationStyleCache citationStyleCache;
    private FileAnnotationCache annotationCache;
    private EntryEditor entryEditor;
    private MainTable mainTable;
    private BasePanelMode mode = BasePanelMode.SHOWING_NOTHING;
    private SplitPane splitPane;
    private DatabaseNotification databaseNotificationPane;
    private boolean saving;
    private PersonNameSuggestionProvider searchAutoCompleter;

    // Used to track whether the base has changed since last save.
    private BibEntry showing;

    private SuggestionProviders suggestionProviders;

    @SuppressWarnings({"FieldCanBeLocal"})
    private Subscription dividerPositionSubscription;

    // the query the user searches when this BasePanel is active
    private Optional<SearchQuery> currentSearchQuery = Optional.empty();

    private Optional<DatabaseChangeMonitor> changeMonitor = Optional.empty();

    private BackgroundTask<ParserResult> dataLoadingTask;

    private final IndexingTaskManager indexingTaskManager = new IndexingTaskManager(Globals.TASK_EXECUTOR);

    public LibraryTab(BibDatabaseContext bibDatabaseContext,
                      JabRefFrame frame,
                      PreferencesService preferencesService,
                      StateManager stateManager,
                      FileUpdateMonitor fileUpdateMonitor) {
        this.frame = Objects.requireNonNull(frame);
        this.bibDatabaseContext = Objects.requireNonNull(bibDatabaseContext);
        this.undoManager = frame.getUndoManager();
        this.dialogService = frame.getDialogService();
        this.preferencesService = Objects.requireNonNull(preferencesService);
        this.stateManager = Objects.requireNonNull(stateManager);
        this.fileUpdateMonitor = fileUpdateMonitor;

        bibDatabaseContext.getDatabase().registerListener(this);
        bibDatabaseContext.getMetaData().registerListener(this);

        this.tableModel = new MainTableDataModel(getBibDatabaseContext(), preferencesService, stateManager);

        citationStyleCache = new CitationStyleCache(bibDatabaseContext);
        annotationCache = new FileAnnotationCache(bibDatabaseContext, preferencesService.getFilePreferences());

        setupMainPanel();
        setupAutoCompletion();

        this.getDatabase().registerListener(new SearchListener());
        this.getDatabase().registerListener(new IndexUpdateListener());
        this.getDatabase().registerListener(new EntriesRemovedListener());

        // ensure that at each addition of a new entry, the entry is added to the groups interface
        this.bibDatabaseContext.getDatabase().registerListener(new GroupTreeListener());
        // ensure that all entry changes mark the panel as changed
        this.bibDatabaseContext.getDatabase().registerListener(this);

        this.getDatabase().registerListener(new UpdateTimestampListener(preferencesService));

        this.entryEditor = new EntryEditor(this);

        // set LibraryTab ID for drag'n'drop
        // ID content doesn't matter, we only need different tabs to have different ID
        this.setId(Long.valueOf(new Random().nextLong()).toString());

        Platform.runLater(() -> {
            EasyBind.subscribe(changedProperty, this::updateTabTitle);
            stateManager.getOpenDatabases().addListener((ListChangeListener<BibDatabaseContext>) c ->
                    updateTabTitle(changedProperty.getValue()));
        });
    }

    private static void addChangedInformation(StringBuilder text, String fileName) {
        text.append("\n");
        text.append(Localization.lang("Library '%0' has changed.", fileName));
    }

    private static void addModeInfo(StringBuilder text, BibDatabaseContext bibDatabaseContext) {
        String mode = bibDatabaseContext.getMode().getFormattedName();
        String modeInfo = String.format("\n%s", Localization.lang("%0 mode", mode));
        text.append(modeInfo);
    }

    private static void addSharedDbInformation(StringBuilder text, BibDatabaseContext bibDatabaseContext) {
        text.append(bibDatabaseContext.getDBMSSynchronizer().getDBName());
        text.append(" [");
        text.append(Localization.lang("shared"));
        text.append("]");
    }

    public void setDataLoadingTask(BackgroundTask<ParserResult> dataLoadingTask) {
        this.dataLoadingTask = dataLoadingTask;
    }

    public void cancelLoading() {
        if (dataLoadingTask != null) {
            dataLoadingTask.cancel();
        }
    }

    /**
     * The layout to display in the tab when it's loading
     */
    public Node createLoadingAnimationLayout() {
        ProgressIndicator progressIndicator = new ProgressIndicator(ProgressIndicator.INDETERMINATE_PROGRESS);
        BorderPane pane = new BorderPane();
        pane.setCenter(progressIndicator);

        return pane;
    }

    public void onDatabaseLoadingStarted() {
        Node loadingLayout = createLoadingAnimationLayout();
        getMainTable().placeholderProperty().setValue(loadingLayout);
        frame.addTab(this, true);
    }

    public void onDatabaseLoadingSucceed(ParserResult result) {
        BibDatabaseContext context = result.getDatabaseContext();
        OpenDatabaseAction.performPostOpenActions(this, result);

        feedData(context);

        if (preferencesService.getFilePreferences().shouldFulltextIndexLinkedFiles()) {
            try {
                indexingTaskManager.updateIndex(PdfIndexer.of(bibDatabaseContext, preferencesService.getFilePreferences()), bibDatabaseContext);
            } catch (IOException e) {
                LOGGER.error("Cannot access lucene index", e);
            }
        }
    }

    public void onDatabaseLoadingFailed(Exception ex) {
        String title = Localization.lang("Connection error");
        String content = String.format("%s\n\n%s", ex.getMessage(), Localization.lang("A local copy will be opened."));

        dialogService.showErrorDialogAndWait(title, content, ex);
    }

    public void feedData(BibDatabaseContext bibDatabaseContextFromParserResult) {
        cleanUp();

        if (this.getTabPane().getSelectionModel().selectedItemProperty().get().equals(this)) {
            // If you open an existing library, a library tab with a loading animation is added immediately.
            // At that point, the library tab is given a temporary bibDatabaseContext with no entries.
            // This line is necessary because, while there is already a binding that updates the active database when a new tab is added,
            // it doesn't handle the case when a library is loaded asynchronously.
            // See org.jabref.gui.LibraryTab.createLibraryTab for the asynchronous loading.
            stateManager.setActiveDatabase(bibDatabaseContextFromParserResult);
        }

        // Remove existing dummy BibDatabaseContext and add correct BibDatabaseContext from ParserResult to trigger changes in the openDatabases list in the stateManager
        Optional<BibDatabaseContext> foundExistingBibDatabase = stateManager.getOpenDatabases().stream().filter(databaseContext -> databaseContext.equals(this.bibDatabaseContext)).findFirst();
        foundExistingBibDatabase.ifPresent(databaseContext -> stateManager.getOpenDatabases().remove(databaseContext));

        this.bibDatabaseContext = Objects.requireNonNull(bibDatabaseContextFromParserResult);

        stateManager.getOpenDatabases().add(bibDatabaseContextFromParserResult);

        bibDatabaseContextFromParserResult.getDatabase().registerListener(this);
        bibDatabaseContextFromParserResult.getMetaData().registerListener(this);

        this.tableModel = new MainTableDataModel(getBibDatabaseContext(), preferencesService, stateManager);
        citationStyleCache = new CitationStyleCache(bibDatabaseContextFromParserResult);
        annotationCache = new FileAnnotationCache(bibDatabaseContextFromParserResult, preferencesService.getFilePreferences());

        setupMainPanel();
        setupAutoCompletion();

        this.getDatabase().registerListener(new SearchListener());
        this.getDatabase().registerListener(new EntriesRemovedListener());

        // ensure that at each addition of a new entry, the entry is added to the groups interface
        this.bibDatabaseContext.getDatabase().registerListener(new GroupTreeListener());
        // ensure that all entry changes mark the panel as changed
        this.bibDatabaseContext.getDatabase().registerListener(this);

        this.getDatabase().registerListener(new UpdateTimestampListener(preferencesService));

        this.entryEditor = new EntryEditor(this);

        Platform.runLater(() -> {
            EasyBind.subscribe(changedProperty, this::updateTabTitle);
            stateManager.getOpenDatabases().addListener((ListChangeListener<BibDatabaseContext>) c ->
                    updateTabTitle(changedProperty.getValue()));
        });

        installAutosaveManagerAndBackupManager();
    }

    public void installAutosaveManagerAndBackupManager() {
        if (isDatabaseReadyForAutoSave(bibDatabaseContext)) {
            AutosaveManager autosaveManager = AutosaveManager.start(bibDatabaseContext);
            autosaveManager.registerListener(new AutosaveUiManager(this));
        }
        if (isDatabaseReadyForBackup(bibDatabaseContext) && preferencesService.getFilePreferences().shouldCreateBackup()) {
            BackupManager.start(bibDatabaseContext, Globals.entryTypesManager, preferencesService);
        }
    }

    private boolean isDatabaseReadyForAutoSave(BibDatabaseContext context) {
        return ((context.getLocation() == DatabaseLocation.SHARED)
                || ((context.getLocation() == DatabaseLocation.LOCAL)
                && preferencesService.getLibraryPreferences().shouldAutoSave()))
                && context.getDatabasePath().isPresent();
    }

    private boolean isDatabaseReadyForBackup(BibDatabaseContext context) {
        return (context.getLocation() == DatabaseLocation.LOCAL) && context.getDatabasePath().isPresent();
    }

    /**
     * Sets the title of the tab modification-asterisk filename – path-fragment
     * <p>
     * The modification-asterisk (*) is shown if the file was modified since last save (path-fragment is only shown if filename is not (globally) unique)
     * <p>
     * Example: *jabref-authors.bib – testbib
     */
    public void updateTabTitle(boolean isChanged) {
        boolean isAutosaveEnabled = preferencesService.getLibraryPreferences().shouldAutoSave();

        DatabaseLocation databaseLocation = bibDatabaseContext.getLocation();
        Optional<Path> file = bibDatabaseContext.getDatabasePath();

        StringBuilder tabTitle = new StringBuilder();
        StringBuilder toolTipText = new StringBuilder();

        if (file.isPresent()) {
            // Modification asterisk
            if (isChanged && !isAutosaveEnabled) {
                tabTitle.append('*');
            }

            // Filename
            Path databasePath = file.get();
            String fileName = databasePath.getFileName().toString();
            tabTitle.append(fileName);
            toolTipText.append(databasePath.toAbsolutePath().toString());

            if (databaseLocation == DatabaseLocation.SHARED) {
                tabTitle.append(" \u2013 ");
                addSharedDbInformation(tabTitle, bibDatabaseContext);
                toolTipText.append(' ');
                addSharedDbInformation(toolTipText, bibDatabaseContext);
            }

            // Database mode
            addModeInfo(toolTipText, bibDatabaseContext);

            // Changed information (tooltip)
            if (isChanged && !isAutosaveEnabled) {
                addChangedInformation(toolTipText, fileName);
            }

            // Unique path fragment
            Optional<String> uniquePathPart = FileUtil.getUniquePathDirectory(stateManager.collectAllDatabasePaths(), databasePath);
            uniquePathPart.ifPresent(part -> tabTitle.append(" \u2013 ").append(part));
        } else {
            if (databaseLocation == DatabaseLocation.LOCAL) {
                tabTitle.append(Localization.lang("untitled"));
                if (bibDatabaseContext.getDatabase().hasEntries()) {
                    // if the database is not empty and no file is assigned,
                    // the database came from an import and has to be treated somehow
                    // -> mark as changed
                    tabTitle.append('*');
                }
            } else {
                addSharedDbInformation(tabTitle, bibDatabaseContext);
                addSharedDbInformation(toolTipText, bibDatabaseContext);
            }
            addModeInfo(toolTipText, bibDatabaseContext);
            if ((databaseLocation == DatabaseLocation.LOCAL) && bibDatabaseContext.getDatabase().hasEntries()) {
                addChangedInformation(toolTipText, Localization.lang("untitled"));
            }
        }

        DefaultTaskExecutor.runInJavaFXThread(() -> {
            textProperty().setValue(tabTitle.toString());
            setTooltip(new Tooltip(toolTipText.toString()));
        });

        if (preferencesService.getFilePreferences().shouldFulltextIndexLinkedFiles()) {
            indexingTaskManager.updateDatabaseName(tabTitle.toString());
        }
    }

    @Subscribe
    public void listen(BibDatabaseContextChangedEvent event) {
        this.changedProperty.setValue(true);
    }

    /**
     * Returns a collection of suggestion providers, which are populated from the current library.
     */
    public SuggestionProviders getSuggestionProviders() {
        return suggestionProviders;
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

    /**
     * Removes the selected entries from the database
     *
     * @param cut If false the user will get asked if he really wants to delete the entries, and it will be localized as "deleted". If true the action will be localized as "cut"
     */
    public void delete(boolean cut) {
        delete(cut, mainTable.getSelectedEntries());
    }

    /**
     * Removes the selected entries from the database
     *
     * @param cut If false the user will get asked if he really wants to delete the entries, and it will be localized as "deleted". If true the action will be localized as "cut"
     */
    private void delete(boolean cut, List<BibEntry> entries) {
        if (entries.isEmpty()) {
            return;
        }
        if (!cut && !showDeleteConfirmationDialog(entries.size())) {
            return;
        }

        getUndoManager().addEdit(new UndoableRemoveEntries(bibDatabaseContext.getDatabase(), entries, cut));
        bibDatabaseContext.getDatabase().removeEntries(entries);
        ensureNotShowingBottomPanel(entries);

        this.changedProperty.setValue(true);
        dialogService.notify(formatOutputMessage(cut ? Localization.lang("Cut") : Localization.lang("Deleted"), entries.size()));

        // prevent the main table from loosing focus
        mainTable.requestFocus();
    }

    public void delete(BibEntry entry) {
        delete(false, Collections.singletonList(entry));
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

    public void insertEntry(final BibEntry bibEntry) {
        if (bibEntry != null) {
            insertEntries(Collections.singletonList(bibEntry));
        }
    }

    /**
     * This method is called from JabRefFrame when the user wants to create a new entry or entries. It is necessary when the user would expect the added entry or one of the added entries to be selected in the entry editor
     *
     * @param entries The new entries.
     */

    public void insertEntries(final List<BibEntry> entries) {
        if (!entries.isEmpty()) {
            bibDatabaseContext.getDatabase().insertEntries(entries);

            // Set owner and timestamp
            UpdateField.setAutomaticFields(entries,
                    preferencesService.getOwnerPreferences(),
                    preferencesService.getTimestampPreferences());
            // Create an UndoableInsertEntries object.
            getUndoManager().addEdit(new UndoableInsertEntries(bibDatabaseContext.getDatabase(), entries));

            this.changedProperty.setValue(true); // The database just changed.
            if (preferencesService.getEntryEditorPreferences().shouldOpenOnNewEntry()) {
                showAndEdit(entries.get(0));
            }
            clearAndSelect(entries.get(0));
        }
    }

    public void editEntryAndFocusField(BibEntry entry, Field field) {
        showAndEdit(entry);
        Platform.runLater(() -> {
            // Focus field and entry in main table (async to give entry editor time to load)
            entryEditor.setFocusToField(field);
            clearAndSelect(entry);
        });
    }

    private void createMainTable() {
        mainTable = new MainTable(tableModel,
                this,
                bibDatabaseContext,
                preferencesService,
                dialogService,
                stateManager,
                Globals.getKeyPrefs(),
                Globals.getClipboardManager(),
                Globals.TASK_EXECUTOR,
                fileUpdateMonitor);

        // Add the listener that binds selection to state manager (TODO: should be replaced by proper JavaFX binding as soon as table is implemented in JavaFX)
        mainTable.addSelectionListener(listEvent -> stateManager.setSelectedEntries(mainTable.getSelectedEntries()));

        // Update entry editor and preview according to selected entries
        mainTable.addSelectionListener(event -> mainTable.getSelectedEntries()
                                                         .stream()
                                                         .findFirst()
                                                         .ifPresent(entryEditor::setEntry));
    }

    public void setupMainPanel() {
        splitPane = new SplitPane();
        splitPane.setOrientation(Orientation.VERTICAL);

        createMainTable();

        splitPane.getItems().add(mainTable);
        databaseNotificationPane = new DatabaseNotification(splitPane);
        setContent(databaseNotificationPane);

        // Saves the divider position as soon as it changes
        // We need to keep a reference to the subscription, otherwise the binding gets garbage collected
        dividerPositionSubscription = EasyBind.valueAt(splitPane.getDividers(), 0)
                                              .mapObservable(SplitPane.Divider::positionProperty)
                                              .subscribeToValues(this::saveDividerLocation);

        // Add changePane in case a file is present - otherwise just add the splitPane to the panel
        Optional<Path> file = bibDatabaseContext.getDatabasePath();
        if (file.isPresent()) {
            resetChangeMonitor();
        } else {
            if (bibDatabaseContext.getDatabase().hasEntries()) {
                // if the database is not empty and no file is assigned,
                // the database came from an import and has to be treated somehow
                // -> mark as changed
                this.changedProperty.setValue(true);
            }
        }
    }

    /**
     * Set up auto completion for this database
     */
    private void setupAutoCompletion() {
        AutoCompletePreferences autoCompletePreferences = preferencesService.getAutoCompletePreferences();
        if (autoCompletePreferences.shouldAutoComplete()) {
            suggestionProviders = new SuggestionProviders(getDatabase(), Globals.journalAbbreviationRepository, autoCompletePreferences);
        } else {
            // Create empty suggestion providers if auto completion is deactivated
            suggestionProviders = new SuggestionProviders();
        }
        searchAutoCompleter = new PersonNameSuggestionProvider(FieldFactory.getPersonNameFields(), getDatabase());
    }

    public void updateSearchManager() {
        frame.getGlobalSearchBar().setAutoCompleter(searchAutoCompleter);
    }

    public EntryEditor getEntryEditor() {
        return entryEditor;
    }

    /**
     * Sets the entry editor as the bottom component in the split pane. If an entry editor already was shown, makes sure that the divider doesn't move. Updates the mode to SHOWING_EDITOR. Then shows the given entry.
     *
     * @param entry The entry to edit.
     */
    public void showAndEdit(BibEntry entry) {
        showBottomPane(BasePanelMode.SHOWING_EDITOR);

        // We use != instead of equals because of performance reasons
        if (entry != getShowing()) {
            entryEditor.setEntry(entry);
            showing = entry;
        }
        entryEditor.requestFocus();
    }

    private void showBottomPane(BasePanelMode newMode) {
        if (newMode != BasePanelMode.SHOWING_EDITOR) {
            throw new UnsupportedOperationException("new mode not recognized: " + newMode.name());
        }
        Node pane = entryEditor;

        if (splitPane.getItems().size() == 2) {
            splitPane.getItems().set(1, pane);
        } else {
            splitPane.getItems().add(1, pane);
        }
        mode = newMode;

        splitPane.setDividerPositions(preferencesService.getEntryEditorPreferences().getDividerPosition());
    }

    /**
     * Removes the bottom component.
     */
    public void closeBottomPane() {
        mode = BasePanelMode.SHOWING_NOTHING;
        splitPane.getItems().remove(entryEditor);
    }

    /**
     * This method selects the given entry, and scrolls it into view in the table. If an entryEditor is shown, it is given focus afterwards.
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
     * This method is called from an EntryEditor when it should be closed. We relay to the selection listener, which takes care of the rest.
     */
    public void entryEditorClosing() {
        closeBottomPane();
        mainTable.requestFocus();
    }

    /**
     * Closes the entry editor if it is showing any of the given entries.
     */
    private void ensureNotShowingBottomPanel(List<BibEntry> entriesToCheck) {
        // This method is not able to close the bottom pane currently

        if ((mode == BasePanelMode.SHOWING_EDITOR) && (entriesToCheck.contains(entryEditor.getEntry()))) {
            closeBottomPane();
        }
    }

    public void updateEntryEditorIfShowing() {
        if (mode == BasePanelMode.SHOWING_EDITOR) {
            BibEntry currentEntry = entryEditor.getEntry();
            showAndEdit(currentEntry);
        }
    }

    /**
     * Put an asterisk behind the filename to indicate the database has changed.
     */

    public synchronized void markChangedOrUnChanged() {
        if (getUndoManager().hasChanged()) {
            this.changedProperty.setValue(true);
        } else if (changedProperty.getValue() && !nonUndoableChangeProperty.getValue()) {
            this.changedProperty.setValue(false);
        }
    }

    public BibDatabase getDatabase() {
        return bibDatabaseContext.getDatabase();
    }

    private boolean showDeleteConfirmationDialog(int numberOfEntries) {
        if (preferencesService.getWorkspacePreferences().shouldConfirmDelete()) {
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
                    Localization.lang("Do not ask again"),
                    optOut -> preferencesService.getWorkspacePreferences().setConfirmDelete(!optOut));
        } else {
            return true;
        }
    }

    /**
     * Depending on whether a preview or an entry editor is showing, save the current divider location in the correct preference setting.
     */
    private void saveDividerLocation(Number position) {
        if (mode == BasePanelMode.SHOWING_EDITOR) {
            preferencesService.getEntryEditorPreferences().setDividerPosition(position.doubleValue());
        }
    }

    /**
     * Perform necessary cleanup when this BasePanel is closed.
     */
    public void cleanUp() {
        changeMonitor.ifPresent(DatabaseChangeMonitor::unregister);
        AutosaveManager.shutdown(bibDatabaseContext);
        BackupManager.shutdown(bibDatabaseContext, preferencesService.getFilePreferences().getBackupDirectory(), preferencesService.getFilePreferences().shouldCreateBackup());
    }

    /**
     * Get an array containing the currently selected entries. The array is stable and not changed if the selection changes
     *
     * @return A list containing the selected entries. Is never null.
     */
    public List<BibEntry> getSelectedEntries() {
        return mainTable.getSelectedEntries();
    }

    public BibDatabaseContext getBibDatabaseContext() {
        return this.bibDatabaseContext;
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
     */
    public void setCurrentSearchQuery(Optional<SearchQuery> currentSearchQuery) {
        this.currentSearchQuery = currentSearchQuery;
    }

    public CitationStyleCache getCitationStyleCache() {
        return citationStyleCache;
    }

    public FileAnnotationCache getAnnotationCache() {
        return annotationCache;
    }

    public void resetChangeMonitor() {
        changeMonitor.ifPresent(DatabaseChangeMonitor::unregister);
        changeMonitor = Optional.of(new DatabaseChangeMonitor(bibDatabaseContext,
                fileUpdateMonitor,
                Globals.TASK_EXECUTOR,
                dialogService,
                preferencesService,
                databaseNotificationPane));
    }

    public void copy() {
        mainTable.copy();
    }

    public void paste() {
        mainTable.paste();
    }

    public void dropEntry(List<BibEntry> entriesToAdd) {
        mainTable.dropEntry(entriesToAdd);
    }

    public void cut() {
        mainTable.cut();
    }

    public BooleanProperty changedProperty() {
        return changedProperty;
    }

    public boolean isModified() {
        return changedProperty.getValue();
    }

    public void markBaseChanged() {
        this.changedProperty.setValue(true);
    }

    public BooleanProperty nonUndoableChangeProperty() {
        return nonUndoableChangeProperty;
    }

    public void markNonUndoableBaseChanged() {
        this.nonUndoableChangeProperty.setValue(true);
        this.changedProperty.setValue(true);
    }

    public void resetChangedProperties() {
        this.nonUndoableChangeProperty.setValue(false);
        this.changedProperty.setValue(false);
    }

    /**
     * Creates a new library tab. Contents are loaded by the {@code dataLoadingTask}. Most of the other parameters are required by {@code resetChangeMonitor()}.
     *
     * @param dataLoadingTask The task to execute to load the data. It is executed using {@link org.jabref.gui.Globals.TASK_EXECUTOR}.
     * @param file the path to the file (loaded by the dataLoadingTask)
     */
    public static LibraryTab createLibraryTab(BackgroundTask<ParserResult> dataLoadingTask,
                                              Path file,
                                              PreferencesService preferencesService,
                                              StateManager stateManager,
                                              JabRefFrame frame,
                                              FileUpdateMonitor fileUpdateMonitor) {
        BibDatabaseContext context = new BibDatabaseContext();
        context.setDatabasePath(file);

        LibraryTab newTab = new LibraryTab(context, frame, preferencesService, stateManager, fileUpdateMonitor);

        newTab.setDataLoadingTask(dataLoadingTask);
        dataLoadingTask.onRunning(newTab::onDatabaseLoadingStarted)
                       .onSuccess(newTab::onDatabaseLoadingSucceed)
                       .onFailure(newTab::onDatabaseLoadingFailed)
                       .executeWith(Globals.TASK_EXECUTOR);

        return newTab;
    }

    private class GroupTreeListener {

        @Subscribe
        public void listen(EntriesAddedEvent addedEntriesEvent) {
            // if the event is an undo, don't add it to the current group
            if (addedEntriesEvent.getEntriesEventSource() == EntriesEventSource.UNDO) {
                return;
            }

            // Automatically add new entries to the selected group (or set of groups)
            if (preferencesService.getGroupsPreferences().shouldAutoAssignGroup()) {
                stateManager.getSelectedGroup(bibDatabaseContext).forEach(
                        selectedGroup -> selectedGroup.addEntriesToGroup(addedEntriesEvent.getBibEntries()));
            }
        }
    }

    private class EntriesRemovedListener {

        @Subscribe
        public void listen(EntriesRemovedEvent entriesRemovedEvent) {
            ensureNotShowingBottomPanel(entriesRemovedEvent.getBibEntries());
        }
    }

    /**
     * Ensures that the results of the current search are updated when a new entry is inserted into the database Actual methods for performing search must run in javafx thread
     */
    private class SearchListener {

        @Subscribe
        public void listen(EntriesAddedEvent addedEntryEvent) {
            DefaultTaskExecutor.runInJavaFXThread(() -> frame.getGlobalSearchBar().performSearch());
        }

        @Subscribe
        public void listen(EntryChangedEvent entryChangedEvent) {
            DefaultTaskExecutor.runInJavaFXThread(() -> frame.getGlobalSearchBar().performSearch());
        }

        @Subscribe
        public void listen(EntriesRemovedEvent removedEntriesEvent) {
            // IMO only used to update the status (found X entries)
            DefaultTaskExecutor.runInJavaFXThread(() -> frame.getGlobalSearchBar().performSearch());
        }
    }

    private class IndexUpdateListener {

        @Subscribe
        public void listen(EntriesAddedEvent addedEntryEvent) {
            if (preferencesService.getFilePreferences().shouldFulltextIndexLinkedFiles()) {
                try {
                    PdfIndexer pdfIndexer = PdfIndexer.of(bibDatabaseContext, preferencesService.getFilePreferences());
                    for (BibEntry addedEntry : addedEntryEvent.getBibEntries()) {
                        indexingTaskManager.addToIndex(pdfIndexer, addedEntry, bibDatabaseContext);
                    }
                } catch (IOException e) {
                    LOGGER.error("Cannot access lucene index", e);
                }
            }
        }

        @Subscribe
        public void listen(EntriesRemovedEvent removedEntriesEvent) {
            if (preferencesService.getFilePreferences().shouldFulltextIndexLinkedFiles()) {
                try {
                    PdfIndexer pdfIndexer = PdfIndexer.of(bibDatabaseContext, preferencesService.getFilePreferences());
                    for (BibEntry removedEntry : removedEntriesEvent.getBibEntries()) {
                        indexingTaskManager.removeFromIndex(pdfIndexer, removedEntry);
                    }
                } catch (IOException e) {
                    LOGGER.error("Cannot access lucene index", e);
                }
            }
        }

        @Subscribe
        public void listen(FieldChangedEvent fieldChangedEvent) {
            if (preferencesService.getFilePreferences().shouldFulltextIndexLinkedFiles()) {
                if (fieldChangedEvent.getField().equals(StandardField.FILE)) {
                    List<LinkedFile> oldFileList = FileFieldParser.parse(fieldChangedEvent.getOldValue());
                    List<LinkedFile> newFileList = FileFieldParser.parse(fieldChangedEvent.getNewValue());

                    List<LinkedFile> addedFiles = new ArrayList<>(newFileList);
                    addedFiles.remove(oldFileList);
                    List<LinkedFile> removedFiles = new ArrayList<>(oldFileList);
                    removedFiles.remove(newFileList);

                    try {
                        indexingTaskManager.addToIndex(PdfIndexer.of(bibDatabaseContext, preferencesService.getFilePreferences()), fieldChangedEvent.getBibEntry(), addedFiles, bibDatabaseContext);
                        indexingTaskManager.removeFromIndex(PdfIndexer.of(bibDatabaseContext, preferencesService.getFilePreferences()), fieldChangedEvent.getBibEntry(), removedFiles);
                    } catch (IOException e) {
                        LOGGER.warn("I/O error when writing lucene index", e);
                    }
                }
            }
        }
    }

    public IndexingTaskManager getIndexingTaskManager() {
        return indexingTaskManager;
    }

    public static class DatabaseNotification extends NotificationPane {
        public DatabaseNotification(Node content) {
            super(content);
        }

        public void notify(Node graphic, String text, List<Action> actions, Duration duration) {
            this.setGraphic(graphic);
            this.setText(text);
            this.getActions().setAll(actions);
            this.show();
            if ((duration != null) && !duration.equals(Duration.ZERO)) {
                PauseTransition delay = new PauseTransition(duration);
                delay.setOnFinished(e -> this.hide());
                delay.play();
            }
        }
    }

    public DatabaseNotification getNotificationPane() {
        return databaseNotificationPane;
    }
}
