package org.jabref.gui;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;

import javax.swing.undo.UndoManager;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ListChangeListener;
import javafx.event.Event;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.util.Duration;

import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.autocompleter.AutoCompletePreferences;
import org.jabref.gui.autocompleter.PersonNameSuggestionProvider;
import org.jabref.gui.autocompleter.SuggestionProvider;
import org.jabref.gui.autocompleter.SuggestionProviders;
import org.jabref.gui.autosaveandbackup.AutosaveManager;
import org.jabref.gui.autosaveandbackup.BackupManager;
import org.jabref.gui.collab.DatabaseChangeMonitor;
import org.jabref.gui.dialogs.AutosaveUiManager;
import org.jabref.gui.entryeditor.EntryEditor;
import org.jabref.gui.exporter.SaveDatabaseAction;
import org.jabref.gui.importer.actions.OpenDatabaseAction;
import org.jabref.gui.maintable.BibEntryTableViewModel;
import org.jabref.gui.maintable.MainTable;
import org.jabref.gui.maintable.MainTableDataModel;
import org.jabref.gui.undo.CountingUndoManager;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.undo.UndoableFieldChange;
import org.jabref.gui.undo.UndoableInsertEntries;
import org.jabref.gui.undo.UndoableRemoveEntries;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.DefaultTaskExecutor;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.citationstyle.CitationStyleCache;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.util.FileFieldParser;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.pdf.FileAnnotationCache;
import org.jabref.logic.pdf.search.IndexingTaskManager;
import org.jabref.logic.pdf.search.PdfIndexer;
import org.jabref.logic.pdf.search.PdfIndexerManager;
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
import org.jabref.model.entry.Author;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.event.EntriesEventSource;
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

/**
 * Represents the ui area where the notifier pane, the library table and the entry editor are shown.
 */
public class LibraryTab extends Tab {

    /**
     * Defines the different modes that the tab can operate in
     */
    private enum PanelMode { MAIN_TABLE, MAIN_TABLE_AND_ENTRY_EDITOR }

    private static final Logger LOGGER = LoggerFactory.getLogger(LibraryTab.class);
    private final LibraryTabContainer tabContainer;
    private final CountingUndoManager undoManager;
    private final DialogService dialogService;
    private final PreferencesService preferencesService;
    private final FileUpdateMonitor fileUpdateMonitor;
    private final StateManager stateManager;
    private final BibEntryTypesManager entryTypesManager;
    private final BooleanProperty changedProperty = new SimpleBooleanProperty(false);
    private final BooleanProperty nonUndoableChangeProperty = new SimpleBooleanProperty(false);

    private BibDatabaseContext bibDatabaseContext;
    private MainTableDataModel tableModel;
    private CitationStyleCache citationStyleCache;
    private FileAnnotationCache annotationCache;
    private EntryEditor entryEditor;
    private MainTable mainTable;
    private PanelMode mode = PanelMode.MAIN_TABLE;
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

    private final IndexingTaskManager indexingTaskManager;
    private final TaskExecutor taskExecutor;

    private LibraryTab(BibDatabaseContext bibDatabaseContext,
                      LibraryTabContainer tabContainer,
                      DialogService dialogService,
                      PreferencesService preferencesService,
                      StateManager stateManager,
                      FileUpdateMonitor fileUpdateMonitor,
                      BibEntryTypesManager entryTypesManager,
                      CountingUndoManager undoManager,
                      TaskExecutor taskExecutor) {
        this.tabContainer = Objects.requireNonNull(tabContainer);
        this.bibDatabaseContext = Objects.requireNonNull(bibDatabaseContext);
        this.undoManager = undoManager;
        this.dialogService = dialogService;
        this.preferencesService = Objects.requireNonNull(preferencesService);
        this.stateManager = Objects.requireNonNull(stateManager);
        this.fileUpdateMonitor = fileUpdateMonitor;
        this.entryTypesManager = entryTypesManager;
        this.indexingTaskManager = new IndexingTaskManager(taskExecutor);
        this.taskExecutor = taskExecutor;

        bibDatabaseContext.getDatabase().registerListener(this);
        bibDatabaseContext.getMetaData().registerListener(this);

        this.tableModel = new MainTableDataModel(getBibDatabaseContext(), preferencesService, stateManager);

        citationStyleCache = new CitationStyleCache(bibDatabaseContext);
        annotationCache = new FileAnnotationCache(bibDatabaseContext, preferencesService.getFilePreferences());

        setupMainPanel();
        setupAutoCompletion();

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

        setOnCloseRequest(this::onCloseRequest);
        setOnClosed(this::onClosed);
    }

    private static void addChangedInformation(StringBuilder text, String fileName) {
        text.append("\n");
        text.append(Localization.lang("Library '%0' has changed.", fileName));
    }

    private static void addModeInfo(StringBuilder text, BibDatabaseContext bibDatabaseContext) {
        String mode = bibDatabaseContext.getMode().getFormattedName();
        String modeInfo = "\n%s".formatted(Localization.lang("%0 mode", mode));
        text.append(modeInfo);
    }

    private static void addSharedDbInformation(StringBuilder text, BibDatabaseContext bibDatabaseContext) {
        text.append(bibDatabaseContext.getDBMSSynchronizer().getDBName());
        text.append(" [");
        text.append(Localization.lang("shared"));
        text.append("]");
    }

    private void setDataLoadingTask(BackgroundTask<ParserResult> dataLoadingTask) {
        this.dataLoadingTask = dataLoadingTask;
    }

    /**
     * The layout to display in the tab when it is loading
     */
    private Node createLoadingAnimationLayout() {
        ProgressIndicator progressIndicator = new ProgressIndicator(ProgressIndicator.INDETERMINATE_PROGRESS);
        BorderPane pane = new BorderPane();
        pane.setCenter(progressIndicator);
        return pane;
    }

    private void onDatabaseLoadingStarted() {
        Node loadingLayout = createLoadingAnimationLayout();
        getMainTable().placeholderProperty().setValue(loadingLayout);
        tabContainer.addTab(this, true);
    }

    private void onDatabaseLoadingSucceed(ParserResult result) {
        BibDatabaseContext context = result.getDatabaseContext();
        OpenDatabaseAction.performPostOpenActions(result, dialogService);

        setDatabaseContext(context);

        if (preferencesService.getFilePreferences().shouldFulltextIndexLinkedFiles()) {
            try {
                indexingTaskManager.updateIndex(PdfIndexerManager.getIndexer(bibDatabaseContext, preferencesService.getFilePreferences()), bibDatabaseContext);
            } catch (IOException e) {
                LOGGER.error("Cannot access lucene index", e);
            }
        }

        dataLoadingTask = null;
    }

    private void onDatabaseLoadingFailed(Exception ex) {
        String title = Localization.lang("Connection error");
        String content = "%s\n\n%s".formatted(ex.getMessage(), Localization.lang("A local copy will be opened."));

        dialogService.showErrorDialogAndWait(title, content, ex);
    }

    private void setDatabaseContext(BibDatabaseContext bibDatabaseContext) {
        if (this.getTabPane().getSelectionModel().selectedItemProperty().get().equals(this)) {
            LOGGER.debug("This case should not happen.");
            stateManager.setActiveDatabase(bibDatabaseContext);
        }

        // Remove existing dummy BibDatabaseContext and add correct BibDatabaseContext from ParserResult to trigger changes in the openDatabases list in the stateManager
        Optional<BibDatabaseContext> foundExistingBibDatabase = stateManager.getOpenDatabases().stream().filter(databaseContext -> databaseContext.equals(this.bibDatabaseContext)).findFirst();
        foundExistingBibDatabase.ifPresent(databaseContext -> stateManager.getOpenDatabases().remove(databaseContext));

        this.bibDatabaseContext = Objects.requireNonNull(bibDatabaseContext);

        stateManager.getOpenDatabases().add(bibDatabaseContext);

        bibDatabaseContext.getDatabase().registerListener(this);
        bibDatabaseContext.getMetaData().registerListener(this);

        this.tableModel = new MainTableDataModel(getBibDatabaseContext(), preferencesService, stateManager);
        citationStyleCache = new CitationStyleCache(bibDatabaseContext);
        annotationCache = new FileAnnotationCache(bibDatabaseContext, preferencesService.getFilePreferences());

        setupMainPanel();
        setupAutoCompletion();

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
            autosaveManager.registerListener(new AutosaveUiManager(this, dialogService, preferencesService, entryTypesManager));
        }
        if (isDatabaseReadyForBackup(bibDatabaseContext) && preferencesService.getFilePreferences().shouldCreateBackup()) {
            BackupManager.start(this, bibDatabaseContext, Globals.entryTypesManager, preferencesService);
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
            toolTipText.append(databasePath.toAbsolutePath());

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

    /**
     * Removes the selected entries from the database
     *
     * @param mode If DELETE_ENTRY the user will get asked if he really wants to delete the entries, and it will be localized as "deleted". If true the action will be localized as "cut"
     */
    public void delete(StandardActions mode) {
        delete(mode, mainTable.getSelectedEntries());
    }

    /**
     * Removes the selected entries from the database
     *
     * @param mode If DELETE_ENTRY the user will get asked if he really wants to delete the entries, and it will be localized as "deleted". If true the action will be localized as "cut"
     */
    private void delete(StandardActions mode, List<BibEntry> entries) {
        if (entries.isEmpty()) {
            return;
        }
        if (mode == StandardActions.DELETE_ENTRY && !showDeleteConfirmationDialog(entries.size())) {
            return;
        }

        getUndoManager().addEdit(new UndoableRemoveEntries(bibDatabaseContext.getDatabase(), entries, mode == StandardActions.CUT));
        bibDatabaseContext.getDatabase().removeEntries(entries);
        ensureNotShowingBottomPanel(entries);

        this.changedProperty.setValue(true);
        switch (mode) {
            case StandardActions.CUT ->
                    dialogService.notify(Localization.lang("Cut %0 entry(ies)", entries.size()));
            case StandardActions.DELETE_ENTRY ->
                    dialogService.notify(Localization.lang("Deleted %0 entry(ies)", entries.size()));
        }

        // prevent the main table from loosing focus
        mainTable.requestFocus();
    }

    public void delete(BibEntry entry) {
        delete(StandardActions.DELETE_ENTRY, Collections.singletonList(entry));
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
                showAndEdit(entries.getFirst());
            }
            clearAndSelect(entries.getFirst());
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
                tabContainer,
                bibDatabaseContext,
                preferencesService,
                dialogService,
                stateManager,
                Globals.getKeyPrefs(),
                Globals.getClipboardManager(),
                entryTypesManager,
                taskExecutor,
                fileUpdateMonitor);
        // Add the listener that binds selection to state manager (TODO: should be replaced by proper JavaFX binding as soon as table is implemented in JavaFX)
        // content binding between StateManager#getselectedEntries and mainTable#getSelectedEntries does not work here as it does not trigger the ActionHelper#needsEntriesSelected checker for the menubar
        mainTable.addSelectionListener(event -> {
            List<BibEntry> entries = event.getList().stream().map(BibEntryTableViewModel::getEntry).toList();
            stateManager.setSelectedEntries(entries);
            if (!entries.isEmpty()) {
                // Update entry editor and preview according to selected entries
                entryEditor.setEntry(entries.getFirst());
            }
        });
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
     * Set up autocompletion for this database
     */
    private void setupAutoCompletion() {
        AutoCompletePreferences autoCompletePreferences = preferencesService.getAutoCompletePreferences();
        if (autoCompletePreferences.shouldAutoComplete()) {
            suggestionProviders = new SuggestionProviders(getDatabase(), Globals.journalAbbreviationRepository, autoCompletePreferences);
        } else {
            // Create empty suggestion providers if auto-completion is deactivated
            suggestionProviders = new SuggestionProviders();
        }
        searchAutoCompleter = new PersonNameSuggestionProvider(FieldFactory.getPersonNameFields(), getDatabase());
    }

    public SuggestionProvider<Author> getAutoCompleter() {
        return searchAutoCompleter;
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
        if (!splitPane.getItems().contains(entryEditor)) {
            splitPane.getItems().addLast(entryEditor);
            mode = PanelMode.MAIN_TABLE_AND_ENTRY_EDITOR;
            splitPane.setDividerPositions(preferencesService.getEntryEditorPreferences().getDividerPosition());
        }

        // We use != instead of equals because of performance reasons
        if (entry != showing) {
            entryEditor.setEntry(entry);
            showing = entry;
        }
        entryEditor.requestFocus();
    }

    /**
     * Removes the bottom component.
     */
    public void closeBottomPane() {
        mode = PanelMode.MAIN_TABLE;
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

        if ((mode == PanelMode.MAIN_TABLE_AND_ENTRY_EDITOR) && (entriesToCheck.contains(entryEditor.getEntry()))) {
            closeBottomPane();
        }
    }

    public void updateEntryEditorIfShowing() {
        if (mode == PanelMode.MAIN_TABLE_AND_ENTRY_EDITOR) {
            BibEntry currentEntry = entryEditor.getEntry();
            showAndEdit(currentEntry);
        }
    }

    /**
     * Put an asterisk behind the filename to indicate the database has changed.
     */

    public synchronized void markChangedOrUnChanged() {
        if (undoManager.hasChanged()) {
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
        if (mode == PanelMode.MAIN_TABLE_AND_ENTRY_EDITOR) {
            preferencesService.getEntryEditorPreferences().setDividerPosition(position.doubleValue());
        }
    }

    public boolean requestClose() {
        if (isModified() && (bibDatabaseContext.getLocation() == DatabaseLocation.LOCAL)) {
            return confirmClose();
        } else if (bibDatabaseContext.getLocation() == DatabaseLocation.SHARED) {
            bibDatabaseContext.convertToLocalDatabase();
            bibDatabaseContext.getDBMSSynchronizer().closeSharedDatabase();
            bibDatabaseContext.clearDBMSSynchronizer();
        }

        return true;
    }

    /**
     * Ask if the user really wants to close the given database.
     * Offers to save or discard the changes -- or return to the library
     *
     * @return <code>true</code> if the user choose to close the database
     */
    private boolean confirmClose() {
        // Database could not have been changed, since it is still loading
        if (dataLoadingTask != null) {
            dataLoadingTask.cancel();
            return true;
        }

        String filename = getBibDatabaseContext()
                .getDatabasePath()
                .map(Path::toAbsolutePath)
                .map(Path::toString)
                .orElse(Localization.lang("untitled"));

        ButtonType saveChanges = new ButtonType(Localization.lang("Save changes"), ButtonBar.ButtonData.YES);
        ButtonType discardChanges = new ButtonType(Localization.lang("Discard changes"), ButtonBar.ButtonData.NO);
        ButtonType returnToLibrary = new ButtonType(Localization.lang("Return to library"), ButtonBar.ButtonData.CANCEL_CLOSE);

        Optional<ButtonType> response = dialogService.showCustomButtonDialogAndWait(Alert.AlertType.CONFIRMATION,
                Localization.lang("Save before closing"),
                Localization.lang("Library '%0' has changed.", filename),
                saveChanges, discardChanges, returnToLibrary);

        if (response.isEmpty()) {
            return true;
        }

        ButtonType buttonType = response.get();

        if (buttonType.equals(returnToLibrary)) {
            return false;
        }

        if (buttonType.equals(saveChanges)) {
            try {
                SaveDatabaseAction saveAction = new SaveDatabaseAction(this, dialogService, preferencesService, Globals.entryTypesManager);
                if (saveAction.save()) {
                    return true;
                }
                // The action was either canceled or unsuccessful.
                dialogService.notify(Localization.lang("Unable to save library"));
            } catch (Throwable ex) {
                LOGGER.error("A problem occurred when trying to save the file", ex);
                dialogService.showErrorDialogAndWait(Localization.lang("Save library"), Localization.lang("Could not save file."), ex);
            }
            // Save was cancelled or an error occurred.
            return false;
        }

        if (buttonType.equals(discardChanges)) {
            BackupManager.discardBackup(bibDatabaseContext, preferencesService.getFilePreferences().getBackupDirectory());
            return true;
        }

        return false;
    }

    private void onCloseRequest(Event event) {
        if (!requestClose()) {
            event.consume();
        }
    }

    /**
     * Perform necessary cleanup when this Library is closed.
     */
    private void onClosed(Event event) {
        changeMonitor.ifPresent(DatabaseChangeMonitor::unregister);
        PdfIndexerManager.shutdownIndexer(bibDatabaseContext);
        AutosaveManager.shutdown(bibDatabaseContext);
        BackupManager.shutdown(bibDatabaseContext,
                preferencesService.getFilePreferences().getBackupDirectory(),
                preferencesService.getFilePreferences().shouldCreateBackup());
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
                taskExecutor,
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

    public boolean isModified() {
        return changedProperty.getValue();
    }

    public void markBaseChanged() {
        this.changedProperty.setValue(true);
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
     * @param dataLoadingTask The task to execute to load the data asynchronously.
     * @param file the path to the file (loaded by the dataLoadingTask)
     */
    public static LibraryTab createLibraryTab(BackgroundTask<ParserResult> dataLoadingTask,
                                              Path file,
                                              DialogService dialogService,
                                              PreferencesService preferencesService,
                                              StateManager stateManager,
                                              LibraryTabContainer tabContainer,
                                              FileUpdateMonitor fileUpdateMonitor,
                                              BibEntryTypesManager entryTypesManager,
                                              CountingUndoManager undoManager,
                                              TaskExecutor taskExecutor) {
        BibDatabaseContext context = new BibDatabaseContext();
        context.setDatabasePath(file);

        LibraryTab newTab = new LibraryTab(
                context,
                tabContainer,
                dialogService,
                preferencesService,
                stateManager,
                fileUpdateMonitor,
                entryTypesManager,
                undoManager,
                taskExecutor);

        newTab.setDataLoadingTask(dataLoadingTask);
        dataLoadingTask.onRunning(newTab::onDatabaseLoadingStarted)
                       .onSuccess(newTab::onDatabaseLoadingSucceed)
                       .onFailure(newTab::onDatabaseLoadingFailed)
                       .executeWith(taskExecutor);

        return newTab;
    }

    public static LibraryTab createLibraryTab(BibDatabaseContext databaseContext,
                                              LibraryTabContainer tabContainer,
                                              DialogService dialogService,
                                              PreferencesService preferencesService,
                                              StateManager stateManager,
                                              FileUpdateMonitor fileUpdateMonitor,
                                              BibEntryTypesManager entryTypesManager,
                                              UndoManager undoManager,
                                              TaskExecutor taskExecutor) {
        Objects.requireNonNull(databaseContext);

        return new LibraryTab(
                databaseContext,
                tabContainer,
                dialogService,
                preferencesService,
                stateManager,
                fileUpdateMonitor,
                entryTypesManager,
                (CountingUndoManager) undoManager,
                taskExecutor);
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

    private class IndexUpdateListener {

        @Subscribe
        public void listen(EntriesAddedEvent addedEntryEvent) {
            if (preferencesService.getFilePreferences().shouldFulltextIndexLinkedFiles()) {
                try {
                    PdfIndexer pdfIndexer = PdfIndexerManager.getIndexer(bibDatabaseContext, preferencesService.getFilePreferences());
                    indexingTaskManager.addToIndex(pdfIndexer, addedEntryEvent.getBibEntries());
                } catch (IOException e) {
                    LOGGER.error("Cannot access lucene index", e);
                }
            }
        }

        @Subscribe
        public void listen(EntriesRemovedEvent removedEntriesEvent) {
            if (preferencesService.getFilePreferences().shouldFulltextIndexLinkedFiles()) {
                try {
                    PdfIndexer pdfIndexer = PdfIndexerManager.getIndexer(bibDatabaseContext, preferencesService.getFilePreferences());
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
                        PdfIndexer indexer = PdfIndexerManager.getIndexer(bibDatabaseContext, preferencesService.getFilePreferences());
                        indexingTaskManager.addToIndex(indexer, fieldChangedEvent.getBibEntry(), addedFiles);
                        indexingTaskManager.removeFromIndex(indexer, removedFiles);
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
