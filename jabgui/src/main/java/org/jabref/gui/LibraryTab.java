package org.jabref.gui;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

import javax.swing.undo.UndoManager;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
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
import org.jabref.gui.clipboard.ClipBoardManager;
import org.jabref.gui.collab.DatabaseChangeMonitor;
import org.jabref.gui.dialogs.AutosaveUiManager;
import org.jabref.gui.exporter.SaveDatabaseAction;
import org.jabref.gui.externalfiles.AutoRenameFileOnEntryChange;
import org.jabref.gui.externalfiles.ImportHandler;
import org.jabref.gui.fieldeditors.LinkedFileViewModel;
import org.jabref.gui.importer.actions.OpenDatabaseAction;
import org.jabref.gui.linkedfile.DeleteFileAction;
import org.jabref.gui.maintable.BibEntryTableViewModel;
import org.jabref.gui.maintable.MainTable;
import org.jabref.gui.maintable.MainTableDataModel;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.undo.CountingUndoManager;
import org.jabref.gui.undo.NamedCompoundEdit;
import org.jabref.gui.undo.UndoableFieldChange;
import org.jabref.gui.undo.UndoableInsertEntries;
import org.jabref.gui.undo.UndoableRemoveEntries;
import org.jabref.gui.util.UiTaskExecutor;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.citationstyle.CitationStyleCache;
import org.jabref.logic.command.CommandSelectionTab;
import org.jabref.logic.importer.FetcherClientException;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.FetcherServerException;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.pdf.FileAnnotationCache;
import org.jabref.logic.search.IndexManager;
import org.jabref.logic.search.PostgreServer;
import org.jabref.logic.shared.DatabaseLocation;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.CoarseChangeFilter;
import org.jabref.logic.util.OptionalObjectProperty;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.FieldChange;
import org.jabref.model.TransferInformation;
import org.jabref.model.TransferMode;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.event.BibDatabaseContextChangedEvent;
import org.jabref.model.database.event.EntriesAddedEvent;
import org.jabref.model.database.event.EntriesRemovedEvent;
import org.jabref.model.entry.Author;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.BibtexString;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.event.EntriesEventSource;
import org.jabref.model.entry.event.FieldChangedEvent;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.groups.GroupTreeNode;
import org.jabref.model.search.query.SearchQuery;
import org.jabref.model.util.FileUpdateMonitor;

import com.airhacks.afterburner.injection.Injector;
import com.google.common.eventbus.Subscribe;
import com.tobiasdiez.easybind.EasyBind;
import com.tobiasdiez.easybind.Subscription;
import org.controlsfx.control.NotificationPane;
import org.controlsfx.control.action.Action;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.jabref.gui.util.InsertUtil.addEntriesWithFeedback;

/**
 * Represents the ui area where the notifier pane, the library table and the entry editor are shown.
 */
public class LibraryTab extends Tab implements CommandSelectionTab {
    private static final Logger LOGGER = LoggerFactory.getLogger(LibraryTab.class);
    private final LibraryTabContainer tabContainer;
    private final CountingUndoManager undoManager;
    private final DialogService dialogService;
    private final GuiPreferences preferences;
    private final FileUpdateMonitor fileUpdateMonitor;
    private final StateManager stateManager;
    private final BibEntryTypesManager entryTypesManager;
    private final BooleanProperty changedProperty = new SimpleBooleanProperty(false);
    private final BooleanProperty nonUndoableChangeProperty = new SimpleBooleanProperty(false);
    private final NavigationHistory navigationHistory = new NavigationHistory();
    private final BooleanProperty canGoBackProperty = new SimpleBooleanProperty(false);
    private final BooleanProperty canGoForwardProperty = new SimpleBooleanProperty(false);
    private boolean backOrForwardNavigationActionTriggered = false;

    private BibDatabaseContext bibDatabaseContext;

    // All subscribers needing "coarse" change events should use this filter
    // See https://devdocs.jabref.org/code-howtos/eventbus.html for details
    private CoarseChangeFilter coarseChangeFilter;

    private MainTableDataModel tableModel;
    private FileAnnotationCache annotationCache;
    private MainTable mainTable;
    private DatabaseNotification databaseNotificationPane;
    private AutoRenameFileOnEntryChange autoRenameFileOnEntryChange;

    // Indicates whether the tab is loading data using a dataloading task
    // The constructors take care to the right true/false assignment during start.
    private final SimpleBooleanProperty loading = new SimpleBooleanProperty(false);

    // initially, the dialog is loading, not saving
    private boolean saving = false;

    private PersonNameSuggestionProvider searchAutoCompleter;

    private SuggestionProviders suggestionProviders;

    @SuppressWarnings({"FieldCanBeLocal"})
    private Subscription dividerPositionSubscription;

    private ListProperty<GroupTreeNode> selectedGroupsProperty;
    private final OptionalObjectProperty<SearchQuery> searchQueryProperty = OptionalObjectProperty.empty();
    private final IntegerProperty resultSize = new SimpleIntegerProperty(0);

    private Optional<DatabaseChangeMonitor> changeMonitor = Optional.empty();

    private BackgroundTask<ParserResult> dataLoadingTask;

    private final ClipBoardManager clipBoardManager;
    private final TaskExecutor taskExecutor;

    private ImportHandler importHandler;
    private IndexManager indexManager;

    private final AiService aiService;

    private Runnable autoCompleterChangedListener;

    /**
     * @param isDummyContext Indicates whether the database context is a dummy. A dummy context is used to display a progress indicator while parsing the database.
     *                       If the context is a dummy, the Lucene index should not be created, as both the dummy context and the actual context share the same index path {@link BibDatabaseContext#getFulltextIndexPath()}.
     *                       If the index is created for the dummy context, the actual context will not be able to open the index until it is closed by the dummy context.
     *                       Closing the index takes time and will slow down opening the library.
     */
    private LibraryTab(@NonNull BibDatabaseContext bibDatabaseContext,
                       @NonNull LibraryTabContainer tabContainer,
                       @NonNull DialogService dialogService,
                       AiService aiService,
                       @NonNull GuiPreferences preferences,
                       @NonNull StateManager stateManager,
                       FileUpdateMonitor fileUpdateMonitor,
                       BibEntryTypesManager entryTypesManager,
                       CountingUndoManager undoManager,
                       ClipBoardManager clipBoardManager,
                       TaskExecutor taskExecutor,
                       boolean isDummyContext) {
        this.bibDatabaseContext = bibDatabaseContext;
        this.tabContainer = tabContainer;
        this.undoManager = undoManager;
        this.dialogService = dialogService;
        this.preferences = preferences;
        this.stateManager = stateManager;
        assert bibDatabaseContext.getDatabasePath().isEmpty() || fileUpdateMonitor != null;
        this.fileUpdateMonitor = fileUpdateMonitor;
        this.entryTypesManager = entryTypesManager;
        this.clipBoardManager = clipBoardManager;
        this.taskExecutor = taskExecutor;
        this.aiService = aiService;

        initializeComponentsAndListeners(isDummyContext);

        // set LibraryTab ID for drag'n'drop
        // ID content doesn't matter, we only need different tabs to have different ID
        this.setId(Long.valueOf(new Random().nextLong()).toString());

        setOnCloseRequest(this::onCloseRequest);
        setOnClosed(this::onClosed);

        stateManager.activeDatabaseProperty().addListener((_, _, _) -> {
            if (preferences.getSearchPreferences().isFulltext()) {
                mainTable.getTableModel().refreshSearchMatches();
            }
        });
    }

    private void initializeComponentsAndListeners(boolean isDummyContext) {
        if (!isDummyContext) {
            createIndexManager();
        }

        if (tableModel != null) {
            tableModel.unbind();
        }

        this.selectedGroupsProperty = new SimpleListProperty<>(stateManager.getSelectedGroups(bibDatabaseContext));
        this.tableModel = new MainTableDataModel(getBibDatabaseContext(), preferences, taskExecutor, getIndexManager(), selectedGroupsProperty(), searchQueryProperty, resultSizeProperty());

        new CitationStyleCache(bibDatabaseContext);
        annotationCache = new FileAnnotationCache(bibDatabaseContext, preferences.getFilePreferences());
        importHandler = new ImportHandler(
                bibDatabaseContext,
                preferences,
                fileUpdateMonitor,
                undoManager,
                stateManager,
                dialogService,
                taskExecutor);

        setupMainPanel();
        setupAutoCompletion();

        this.coarseChangeFilter = new CoarseChangeFilter(bibDatabaseContext);

        this.getDatabase().registerListener(new IndexUpdateListener());

        // ensure that at each addition of a new entry, the entry is added to the groups interface
        this.bibDatabaseContext.getDatabase().registerListener(new GroupTreeListener());
        // ensure that all entry changes mark the panel as changed
        this.bibDatabaseContext.getDatabase().registerListener(this);
        this.bibDatabaseContext.getMetaData().registerListener(this);

        this.getDatabase().registerListener(new UpdateTimestampListener(preferences));

        autoRenameFileOnEntryChange = new AutoRenameFileOnEntryChange(bibDatabaseContext, preferences.getFilePreferences());
        coarseChangeFilter.registerListener(autoRenameFileOnEntryChange);

        aiService.setupDatabase(bibDatabaseContext);

        Platform.runLater(() -> {
            EasyBind.subscribe(changedProperty, this::updateTabTitle);
            stateManager.getOpenDatabases().addListener((ListChangeListener<BibDatabaseContext>) _ ->
                    updateTabTitle(changedProperty.getValue()));
        });
    }

    public void setAutoCompleterChangedListener(@NonNull Runnable listener) {
        this.autoCompleterChangedListener = listener;
    }

    private static void addChangedInformation(StringBuilder text) {
        text.append("\n");
        text.append(Localization.lang("The library has been modified."));
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
        this.loading.set(true);
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
    }

    private void onDatabaseLoadingSucceed(ParserResult result) {
        OpenDatabaseAction.performPostOpenActions(result, dialogService, preferences);
        if (result.getChangedOnMigration()) {
            this.markBaseChanged();
        }

        setDatabaseContext(result.getDatabaseContext());
        // Notify listeners that the auto-completer may have changed
        if (autoCompleterChangedListener != null) {
            autoCompleterChangedListener.run();
        }
        LOGGER.trace("loading.set(false);");
        loading.set(false);
        dataLoadingTask = null;
    }

    public void createIndexManager() {
        indexManager = new IndexManager(bibDatabaseContext, taskExecutor, preferences, Injector.instantiateModelOrService(PostgreServer.class));
        stateManager.setIndexManager(bibDatabaseContext, indexManager);
    }

    public IndexManager getIndexManager() {
        return indexManager;
    }

    public void closeIndexManger() {
        indexManager.close();
    }

    private void onDatabaseLoadingFailed(Exception ex) {
        loading.set(false);

        String title = Localization.lang("Connection error");
        String content = "%s\n\n%s".formatted(ex.getMessage(), Localization.lang("A local copy will be opened."));

        dialogService.showErrorDialogAndWait(title, content, ex);
    }

    private void setDatabaseContext(@NonNull BibDatabaseContext bibDatabaseContext) {
        TabPane tabPane = this.getTabPane();

        if (tabPane == null) {
            LOGGER.debug("User interrupted loading. Not showing any library.");
            return;
        }
        if (tabPane.getSelectionModel().selectedItemProperty().get().equals(this)) {
            LOGGER.debug("This case should not happen.");
            stateManager.setActiveDatabase(bibDatabaseContext);
            stateManager.activeTabProperty().set(Optional.of(this));
        }

        // Remove existing dummy BibDatabaseContext and add correct BibDatabaseContext from ParserResult to trigger changes in the openDatabases list in the stateManager
        Optional<BibDatabaseContext> foundExistingBibDatabase = stateManager.getOpenDatabases().stream().filter(databaseContext -> databaseContext.equals(this.bibDatabaseContext)).findFirst();
        foundExistingBibDatabase.ifPresent(databaseContext -> stateManager.getOpenDatabases().remove(databaseContext));

        this.bibDatabaseContext = bibDatabaseContext;

        stateManager.getOpenDatabases().add(bibDatabaseContext);

        initializeComponentsAndListeners(false);
        installAutosaveManagerAndBackupManager();
    }

    public void installAutosaveManagerAndBackupManager() {
        if (isDatabaseReadyForAutoSave(bibDatabaseContext)) {
            AutosaveManager autosaveManager = AutosaveManager.start(bibDatabaseContext, coarseChangeFilter);
            autosaveManager.registerListener(new AutosaveUiManager(this, dialogService, preferences, entryTypesManager, stateManager));
        }
        if (isDatabaseReadyForBackup(bibDatabaseContext) && preferences.getFilePreferences().shouldCreateBackup()) {
            BackupManager.start(this, bibDatabaseContext, coarseChangeFilter, Injector.instantiateModelOrService(BibEntryTypesManager.class), preferences);
        }
    }

    private boolean isDatabaseReadyForAutoSave(BibDatabaseContext context) {
        return ((context.getLocation() == DatabaseLocation.SHARED)
                || ((context.getLocation() == DatabaseLocation.LOCAL)
                && preferences.getLibraryPreferences().shouldAutoSave()))
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
        boolean isAutosaveEnabled = preferences.getLibraryPreferences().shouldAutoSave();

        DatabaseLocation databaseLocation = bibDatabaseContext.getLocation();
        Optional<Path> file = bibDatabaseContext.getDatabasePath();

        StringBuilder tabTitle = new StringBuilder();
        StringBuilder toolTipText = new StringBuilder();

        if (file.isPresent()) {
            // Modification asterisk
            if (isChanged && !isAutosaveEnabled) {
                tabTitle.append('*');
            }

            Path databasePath = file.get();
            tabTitle.append(databasePath.getFileName().toString());
            Optional<String> uniquePathPart = FileUtil.getUniquePathDirectory(stateManager.getAllDatabasePaths(), databasePath);
            uniquePathPart.ifPresent(part -> tabTitle.append(" \u2013 ").append(part));
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
                addChangedInformation(toolTipText);
            }
        } else {
            if (databaseLocation == DatabaseLocation.LOCAL) {
                tabTitle.append('*');
                tabTitle.append(Localization.lang("untitled"));
            } else {
                addSharedDbInformation(tabTitle, bibDatabaseContext);
                addSharedDbInformation(toolTipText, bibDatabaseContext);
            }
            addModeInfo(toolTipText, bibDatabaseContext);
            if ((databaseLocation == DatabaseLocation.LOCAL) && bibDatabaseContext.getDatabase().hasEntries()) {
                addChangedInformation(toolTipText);
            }
        }

        UiTaskExecutor.runInJavaFXThread(() -> {
            textProperty().setValue(tabTitle.toString());
            setTooltip(new Tooltip(toolTipText.toString()));
        });
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

    public void registerUndoableChanges(List<FieldChange> changes) {
        NamedCompoundEdit compoundEdit = new NamedCompoundEdit(Localization.lang("Save actions"));
        for (FieldChange change : changes) {
            compoundEdit.addEdit(new UndoableFieldChange(change));
        }
        compoundEdit.end();
        if (compoundEdit.hasEdits()) {
            getUndoManager().addEdit(compoundEdit);
        }
    }

    private void createMainTable() {
        mainTable = new MainTable(tableModel,
                this,
                tabContainer,
                bibDatabaseContext,
                preferences,
                dialogService,
                stateManager,
                preferences.getKeyBindingRepository(),
                clipBoardManager,
                entryTypesManager,
                taskExecutor,
                importHandler);

        // Add the listener that binds selection to state manager (TODO: should be replaced by proper JavaFX binding as soon as table is implemented in JavaFX)
        // content binding between StateManager#getselectedEntries and mainTable#getSelectedEntries does not work here as it does not trigger the ActionHelper#needsEntriesSelected checker for the menubar
        mainTable.addSelectionListener(event -> {
            List<BibEntry> entries = event.getList().stream().map(BibEntryTableViewModel::getEntry).toList();
            stateManager.setSelectedEntries(entries);

            // track navigation history for single selections
            if (entries.size() == 1) {
                newEntryShowing(entries.getFirst());
            } else if (entries.isEmpty()) {
                // an empty selection isn't a navigational step, so we don't alter the history list
                // this avoids adding a "null" entry to the back/forward stack
                // we just refresh the UI button states to ensure they are consistent with the latest history.
                updateNavigationState();
            }
        });
    }

    public void setupMainPanel() {
        createMainTable();

        databaseNotificationPane = new DatabaseNotification(mainTable);
        setContent(databaseNotificationPane);

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
        AutoCompletePreferences autoCompletePreferences = preferences.getAutoCompletePreferences();
        if (autoCompletePreferences.shouldAutoComplete()) {
            suggestionProviders = new SuggestionProviders(
                    getDatabase(),
                    Injector.instantiateModelOrService(JournalAbbreviationRepository.class),
                    autoCompletePreferences);
        } else {
            // Create empty suggestion providers if auto-completion is deactivated
            suggestionProviders = new SuggestionProviders();
        }
        searchAutoCompleter = new PersonNameSuggestionProvider(FieldFactory.getPersonNameFields(), getDatabase());
    }

    public SuggestionProvider<Author> getAutoCompleter() {
        return searchAutoCompleter;
    }

    public void showAndEdit(BibEntry entry) {
        this.clearAndSelect(entry);
        stateManager.getEditorShowing().setValue(true);
    }

    /**
     * This method selects the given entry, and scrolls it into view in the table. If an entryEditor is shown, it is given focus afterwards.
     */
    public void clearAndSelect(final BibEntry bibEntry) {
        mainTable.clearAndSelect(bibEntry);
    }

    @Override
    public void clearAndSelect(final List<BibEntry> bibEntries) {
        mainTable.clearAndSelect(bibEntries);
    }

    public void selectPreviousEntry() {
        mainTable.getSelectionModel().clearAndSelect(mainTable.getSelectionModel().getSelectedIndex() - 1);
    }

    public void selectNextEntry() {
        mainTable.getSelectionModel().clearAndSelect(mainTable.getSelectionModel().getSelectedIndex() + 1);
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

    /**
     * Initializes a pop-up dialog box to confirm whether the user wants to delete the selected entry
     * Keep track of user preference:
     * if the user prefers not to ask before deleting, delete the selected entry without displaying the dialog box
     *
     * @param numberOfEntries number of entries user is selecting
     * @return true if user confirm to delete entry
     */
    private boolean showDeleteConfirmationDialog(int numberOfEntries) {
        if (preferences.getWorkspacePreferences().shouldConfirmDelete()) {
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

            return dialogService.showConfirmationDialogWithOptOutAndWait(
                    title,
                    message,
                    okButton,
                    cancelButton,
                    Localization.lang("Do not ask again"),
                    optOut -> preferences.getWorkspacePreferences().setConfirmDelete(!optOut));
        } else {
            return true;
        }
    }

    public boolean requestClose() {
        if (bibDatabaseContext.getLocation() == DatabaseLocation.LOCAL) {
            if (isModified()) {
                return confirmClose();
            }
        }
        return true;
    }

    /**
     * Ask if the user really wants to close the given database.
     * Offers to save or discard the changes -- or return to the library
     *
     * @return <code>true</code> if the user chooses to close the database
     */
    private boolean confirmClose() {
        // Database could not have been changed, since it is still loading
        if (dataLoadingTask != null) {
            dataLoadingTask.cancel();
            loading.setValue(false);
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
                Localization.lang("Library '%0' has been modified.", filename),
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
                SaveDatabaseAction saveAction = new SaveDatabaseAction(this, dialogService, preferences, Injector.instantiateModelOrService(BibEntryTypesManager.class), stateManager);
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
            BackupManager.discardBackup(bibDatabaseContext, preferences.getFilePreferences().getBackupDirectory());
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
        if (dataLoadingTask != null) {
            dataLoadingTask.cancel();
        }
        if (bibDatabaseContext.getLocation() == DatabaseLocation.SHARED) {
            bibDatabaseContext.convertToLocalDatabase();
            bibDatabaseContext.getDBMSSynchronizer().closeSharedDatabase();
            bibDatabaseContext.clearDBMSSynchronizer();
        }
        try {
            changeMonitor.ifPresent(DatabaseChangeMonitor::unregister);
        } catch (RuntimeException e) {
            LOGGER.error("Problem when closing change monitor", e);
        }
        try {
            if (indexManager != null) {
                indexManager.close();
            }
        } catch (RuntimeException e) {
            LOGGER.error("Problem when closing index manager", e);
        }

        try {
            AutosaveManager.shutdown(bibDatabaseContext);
        } catch (RuntimeException e) {
            LOGGER.error("Problem when shutting down autosave manager", e);
        }
        try {
            BackupManager.shutdown(bibDatabaseContext,
                    preferences.getFilePreferences().getBackupDirectory(),
                    preferences.getFilePreferences().shouldCreateBackup());
        } catch (RuntimeException e) {
            LOGGER.error("Problem when shutting down backup manager", e);
        }

        if (tableModel != null) {
            tableModel.unbind();
        }

        if (autoRenameFileOnEntryChange != null) {
            coarseChangeFilter.unregisterListener(autoRenameFileOnEntryChange);
        }

        // clean up the groups map
        stateManager.clearSelectedGroups(bibDatabaseContext);
    }

    /**
     * Get an array containing the currently selected entries. The array is stable and not changed if the selection changes
     *
     * @return A list containing the selected entries. Is never null.
     */
    public List<BibEntry> getSelectedEntries() {
        return mainTable.getSelectedEntries();
    }

    @Override
    public BibDatabaseContext getBibDatabaseContext() {
        return this.bibDatabaseContext;
    }

    public boolean isSaving() {
        return saving;
    }

    public void setSaving(boolean saving) {
        this.saving = saving;
    }

    public ObservableBooleanValue getLoading() {
        return loading;
    }

    public CountingUndoManager getUndoManager() {
        return undoManager;
    }

    public MainTable getMainTable() {
        return mainTable;
    }

    public ListProperty<GroupTreeNode> selectedGroupsProperty() {
        return selectedGroupsProperty;
    }

    public OptionalObjectProperty<SearchQuery> searchQueryProperty() {
        return searchQueryProperty;
    }

    public IntegerProperty resultSizeProperty() {
        return resultSize;
    }

    public FileAnnotationCache getAnnotationCache() {
        return annotationCache;
    }

    public void resetChangeMonitor() {
        changeMonitor.ifPresent(DatabaseChangeMonitor::unregister);
        assert bibDatabaseContext.getDatabasePath().isEmpty() || fileUpdateMonitor != null;
        changeMonitor = Optional.of(new DatabaseChangeMonitor(bibDatabaseContext,
                fileUpdateMonitor,
                taskExecutor,
                dialogService,
                preferences,
                databaseNotificationPane,
                undoManager,
                stateManager));
    }

    public void insertEntry(final BibEntry bibEntry) {
        insertEntries(List.of(bibEntry));
    }

    public void insertEntries(final List<BibEntry> entries) {
        if (entries.isEmpty()) {
            return;
        }

        importHandler.importCleanedEntries(null, entries);
        getUndoManager().addEdit(new UndoableInsertEntries(bibDatabaseContext.getDatabase(), entries));
        markBaseChanged();
        stateManager.setSelectedEntries(entries);
        if (preferences.getEntryEditorPreferences().shouldOpenOnNewEntry()) {
            showAndEdit(entries.getFirst());
        } else {
            clearAndSelect(entries.getFirst());
        }
    }

    public void copyEntry() {
        int entriesCopied = doCopyEntry(TransferMode.COPY, getSelectedEntries());
        if (entriesCopied >= 0) {
            dialogService.notify(Localization.lang("Copied %0 entry(s)", entriesCopied));
        } else {
            dialogService.notify(Localization.lang("Copy failed", entriesCopied));
        }
    }

    private int doCopyEntry(TransferMode transferMode, List<BibEntry> selectedEntries) {
        if (selectedEntries.isEmpty()) {
            return 0;
        }

        List<BibtexString> stringConstants = bibDatabaseContext.getDatabase().getUsedStrings(selectedEntries);
        try {
            clipBoardManager.setContent(transferMode, bibDatabaseContext, selectedEntries, entryTypesManager, stringConstants);
            return selectedEntries.size();
        } catch (IOException e) {
            LOGGER.error("Error while copying selected entries to clipboard.", e);
            return -1;
        }
    }

    public void pasteEntry() {
        String content = ClipBoardManager.getContents();
        List<BibEntry> entriesToAdd = importHandler.handleBibTeXData(content);
        if (entriesToAdd.isEmpty()) {
            entriesToAdd = handleNonBibTeXStringData(content);
        }
        if (entriesToAdd.isEmpty()) {
            return;
        }
        // Now, the BibEntries to add are known
        // The definitive insertion needs to happen now.
        addEntriesWithFeedback(
                clipBoardManager.getJabRefClipboardTransferData(),
                entriesToAdd,
                bibDatabaseContext,
                Localization.lang("Pasted %0 entry(s) to %1"),
                Localization.lang("Pasted %0 entry(s) to %1. %2 were skipped"),
                dialogService,
                importHandler,
                stateManager
        );
    }

    private List<BibEntry> handleNonBibTeXStringData(String data) {
        try {
            return this.importHandler.handleStringData(data);
        } catch (FetcherException exception) {
            if (exception instanceof FetcherClientException) {
                dialogService.showInformationDialogAndWait(Localization.lang("Look up identifier"), Localization.lang("No data was found for the identifier"));
            } else if (exception instanceof FetcherServerException) {
                dialogService.showInformationDialogAndWait(Localization.lang("Look up identifier"), Localization.lang("Server not available"));
            } else {
                dialogService.showErrorDialogAndWait(exception);
            }
            return List.of();
        }
    }

    public void dropEntry(BibDatabaseContext sourceBibDatabaseContext, List<BibEntry> entriesToAdd) {
        addEntriesWithFeedback(
                new TransferInformation(sourceBibDatabaseContext, TransferMode.NONE), // "NONE", because we don't know the modifiers here and thus cannot say whether the attached file (and entry(s)) should be copied or moved
                entriesToAdd,
                bibDatabaseContext,
                Localization.lang("Moved %0 entry(s) to %1"),
                Localization.lang("Moved %0 entry(s) to %1. %2 were skipped"),
                dialogService,
                importHandler,
                stateManager
        );
    }

    public void cutEntry() {
        int entriesCopied = doCopyEntry(TransferMode.MOVE, getSelectedEntries());
        int entriesDeleted = doDeleteEntry(StandardActions.CUT, mainTable.getSelectedEntries());

        if (entriesCopied == entriesDeleted) {
            dialogService.notify(Localization.lang("Cut %0 entry(s)", entriesCopied));
        } else {
            dialogService.notify(Localization.lang("Cut failed", entriesCopied));
            undoManager.undo();
            clipBoardManager.setContent("");
        }
    }

    /**
     * Removes the selected entries and files linked to selected entries from the database
     */
    public void deleteEntry() {
        int entriesDeleted = doDeleteEntry(StandardActions.DELETE_ENTRY, mainTable.getSelectedEntries());
        if (entriesDeleted > 0) {
            dialogService.notify(Localization.lang("Deleted %0 entry(s)", entriesDeleted));
        }
    }

    public void deleteEntry(BibEntry entry) {
        doDeleteEntry(StandardActions.DELETE_ENTRY, List.of(entry));
    }

    /**
     * Removes the selected entries and files linked to selected entries from the database
     *
     * @param mode If DELETE_ENTRY the user will get asked if he really wants to delete the entries, and it will be localized as "deleted". If true the action will be localized as "cut"
     */
    private int doDeleteEntry(StandardActions mode, List<BibEntry> entries) {
        if (entries.isEmpty()) {
            return 0;
        }
        if (mode == StandardActions.DELETE_ENTRY && !showDeleteConfirmationDialog(entries.size())) {
            return -1;
        }

        // Delete selected entries
        getUndoManager().addEdit(new UndoableRemoveEntries(bibDatabaseContext.getDatabase(), entries, mode == StandardActions.CUT));
        bibDatabaseContext.getDatabase().removeEntries(entries);

        if (mode != StandardActions.CUT) {
            List<LinkedFile> linkedFileList = entries.stream()
                                                     .flatMap(entry -> entry.getFiles().stream())
                                                     .distinct()
                                                     .toList();

            if (!linkedFileList.isEmpty()) {
                List<LinkedFileViewModel> viewModels = linkedFileList.stream()
                                                                     .map(linkedFile -> LinkedFileViewModel.fromLinkedFile(linkedFile, null, bibDatabaseContext, null, null, preferences))
                                                                     .collect(Collectors.toList());

                new DeleteFileAction(dialogService, preferences.getFilePreferences(), bibDatabaseContext, viewModels).execute();
            }
        }

        markBaseChanged();

        // prevent the main table from loosing focus
        mainTable.requestFocus();

        return entries.size();
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

    public void back() {
        navigationHistory.back().ifPresent(this::navigateToEntry);
    }

    public void forward() {
        navigationHistory.forward().ifPresent(this::navigateToEntry);
    }

    private void navigateToEntry(BibEntry entry) {
        backOrForwardNavigationActionTriggered = true;
        clearAndSelect(entry);
        updateNavigationState();
    }

    public boolean canGoBack() {
        return navigationHistory.canGoBack();
    }

    public boolean canGoForward() {
        return navigationHistory.canGoForward();
    }

    private void newEntryShowing(BibEntry entry) {
        // skip history updates if this is from a back/forward operation
        if (backOrForwardNavigationActionTriggered) {
            backOrForwardNavigationActionTriggered = false;
            return;
        }

        navigationHistory.add(entry);
        updateNavigationState();
    }

    /**
     * Updates the StateManager with current navigation state
     * Only update if this is the active tab
     */
    public void updateNavigationState() {
        canGoBackProperty.set(canGoBack());
        canGoForwardProperty.set(canGoForward());
    }

    /**
     * Creates a new library tab. Contents are loaded by the {@code dataLoadingTask}. Most of the other parameters are required by {@code resetChangeMonitor()}.
     *
     * @param dataLoadingTask The task to execute to load the data asynchronously.
     * @param file            the path to the file (loaded by the dataLoadingTask)
     */
    public static LibraryTab createLibraryTab(BackgroundTask<ParserResult> dataLoadingTask,
                                              Path file,
                                              DialogService dialogService,
                                              AiService aiService,
                                              GuiPreferences preferences,
                                              StateManager stateManager,
                                              LibraryTabContainer tabContainer,
                                              FileUpdateMonitor fileUpdateMonitor,
                                              BibEntryTypesManager entryTypesManager,
                                              CountingUndoManager undoManager,
                                              ClipBoardManager clipBoardManager,
                                              TaskExecutor taskExecutor) {
        BibDatabaseContext context = new BibDatabaseContext();
        context.setDatabasePath(file);

        LibraryTab newTab = new LibraryTab(
                context,
                tabContainer,
                dialogService,
                aiService,
                preferences,
                stateManager,
                fileUpdateMonitor,
                entryTypesManager,
                undoManager,
                clipBoardManager,
                taskExecutor,
                true);

        newTab.setDataLoadingTask(dataLoadingTask);
        dataLoadingTask.onRunning(newTab::onDatabaseLoadingStarted)
                       .onSuccess(newTab::onDatabaseLoadingSucceed)
                       .onFailure(newTab::onDatabaseLoadingFailed)
                       .executeWith(taskExecutor);

        return newTab;
    }

    public static LibraryTab createLibraryTab(@NonNull BibDatabaseContext databaseContext,
                                              LibraryTabContainer tabContainer,
                                              DialogService dialogService,
                                              AiService aiService,
                                              GuiPreferences preferences,
                                              StateManager stateManager,
                                              FileUpdateMonitor fileUpdateMonitor,
                                              BibEntryTypesManager entryTypesManager,
                                              UndoManager undoManager,
                                              ClipBoardManager clipBoardManager,
                                              TaskExecutor taskExecutor) {
        return new LibraryTab(
                databaseContext,
                tabContainer,
                dialogService,
                aiService,
                preferences,
                stateManager,
                fileUpdateMonitor,
                entryTypesManager,
                (CountingUndoManager) undoManager,
                clipBoardManager,
                taskExecutor,
                false);
    }

    public BooleanProperty canGoBackProperty() {
        return canGoBackProperty;
    }

    public BooleanProperty canGoForwardProperty() {
        return canGoForwardProperty;
    }

    private class GroupTreeListener {

        @Subscribe
        public void listen(EntriesAddedEvent addedEntriesEvent) {
            // if the event is an undo, don't add it to the current group
            if (addedEntriesEvent.getEntriesEventSource() == EntriesEventSource.UNDO) {
                return;
            }

            // Automatically add new entries to the selected group (or set of groups)
            if (preferences.getGroupsPreferences().shouldAutoAssignGroup()) {
                stateManager.getSelectedGroups(bibDatabaseContext).forEach(
                        selectedGroup -> selectedGroup.addEntriesToGroup(addedEntriesEvent.getBibEntries()));
            }
        }
    }

    private class IndexUpdateListener {

        @Subscribe
        public void listen(EntriesAddedEvent addedEntryEvent) {
            indexManager.addToIndex(addedEntryEvent.getBibEntries());
        }

        @Subscribe
        public void listen(EntriesRemovedEvent removedEntriesEvent) {
            indexManager.removeFromIndex(removedEntriesEvent.getBibEntries());
        }

        @Subscribe
        public void listen(FieldChangedEvent fieldChangedEvent) {
            indexManager.updateEntry(fieldChangedEvent);
        }
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
                delay.setOnFinished(this::handle);
                delay.play();
            }
        }

        private void handle(ActionEvent e) {
            this.hide();
        }
    }

    public DatabaseNotification getNotificationPane() {
        return databaseNotificationPane;
    }

    @Override
    public String toString() {
        return "LibraryTab{" +
                "bibDatabaseContext=" + bibDatabaseContext +
                '}';
    }
}
