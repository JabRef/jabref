package org.jabref.gui;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.function.Supplier;
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
import javafx.event.Event;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.SplitPane;
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
import org.jabref.gui.collab.DatabaseChangeMonitor;
import org.jabref.gui.dialogs.AutosaveUiManager;
import org.jabref.gui.entryeditor.EntryEditor;
import org.jabref.gui.exporter.SaveDatabaseAction;
import org.jabref.gui.externalfiles.ImportHandler;
import org.jabref.gui.fieldeditors.LinkedFileViewModel;
import org.jabref.gui.importer.actions.OpenDatabaseAction;
import org.jabref.gui.linkedfile.DeleteFileAction;
import org.jabref.gui.maintable.BibEntryTableViewModel;
import org.jabref.gui.maintable.MainTable;
import org.jabref.gui.maintable.MainTableDataModel;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.undo.CountingUndoManager;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.undo.RedoAction;
import org.jabref.gui.undo.UndoAction;
import org.jabref.gui.undo.UndoableFieldChange;
import org.jabref.gui.undo.UndoableInsertEntries;
import org.jabref.gui.undo.UndoableRemoveEntries;
import org.jabref.gui.util.OptionalObjectProperty;
import org.jabref.gui.util.UiTaskExecutor;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.citationstyle.CitationStyleCache;
import org.jabref.logic.importer.FetcherClientException;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.FetcherServerException;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.pdf.FileAnnotationCache;
import org.jabref.logic.search.IndexManager;
import org.jabref.logic.shared.DatabaseLocation;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.TaskExecutor;
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
import org.jabref.model.entry.BibtexString;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.event.EntriesEventSource;
import org.jabref.model.entry.event.FieldChangedEvent;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.groups.GroupTreeNode;
import org.jabref.model.search.query.SearchQuery;
import org.jabref.model.util.DirectoryMonitor;
import org.jabref.model.util.DirectoryMonitorManager;
import org.jabref.model.util.FileUpdateMonitor;

import com.airhacks.afterburner.injection.Injector;
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
    private final GuiPreferences preferences;
    private final FileUpdateMonitor fileUpdateMonitor;
    private final StateManager stateManager;
    private final BibEntryTypesManager entryTypesManager;
    private final BooleanProperty changedProperty = new SimpleBooleanProperty(false);
    private final BooleanProperty nonUndoableChangeProperty = new SimpleBooleanProperty(false);

    private BibDatabaseContext bibDatabaseContext;
    private MainTableDataModel tableModel;
    private FileAnnotationCache annotationCache;
    private EntryEditor entryEditor;
    private MainTable mainTable;
    private PanelMode mode = PanelMode.MAIN_TABLE;
    private SplitPane splitPane;
    private DatabaseNotification databaseNotificationPane;

    // Indicates whether the tab is loading data using a dataloading task
    // The constructors take care to the right true/false assignment during start.
    private final SimpleBooleanProperty loading = new SimpleBooleanProperty(false);

    // initially, the dialog is loading, not saving
    private boolean saving = false;

    private PersonNameSuggestionProvider searchAutoCompleter;

    // Used to track whether the base has changed since last save.
    private BibEntry showing;

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
    private final DirectoryMonitorManager directoryMonitorManager;

    private ImportHandler importHandler;
    private IndexManager indexManager;

    private final AiService aiService;

    /**
     * @param isDummyContext Indicates whether the database context is a dummy. A dummy context is used to display a progress indicator while parsing the database.
     *                       If the context is a dummy, the Lucene index should not be created, as both the dummy context and the actual context share the same index path {@link BibDatabaseContext#getFulltextIndexPath()}.
     *                       If the index is created for the dummy context, the actual context will not be able to open the index until it is closed by the dummy context.
     *                       Closing the index takes time and will slow down opening the library.
     */
    private LibraryTab(BibDatabaseContext bibDatabaseContext,
                       LibraryTabContainer tabContainer,
                       DialogService dialogService,
                       AiService aiService,
                       GuiPreferences preferences,
                       StateManager stateManager,
                       FileUpdateMonitor fileUpdateMonitor,
                       BibEntryTypesManager entryTypesManager,
                       CountingUndoManager undoManager,
                       ClipBoardManager clipBoardManager,
                       TaskExecutor taskExecutor,
                       boolean isDummyContext) {
        this.tabContainer = Objects.requireNonNull(tabContainer);
        this.bibDatabaseContext = Objects.requireNonNull(bibDatabaseContext);
        this.undoManager = undoManager;
        this.dialogService = dialogService;
        this.preferences = Objects.requireNonNull(preferences);
        this.stateManager = Objects.requireNonNull(stateManager);
        this.fileUpdateMonitor = fileUpdateMonitor;
        this.entryTypesManager = entryTypesManager;
        this.clipBoardManager = clipBoardManager;
        this.taskExecutor = taskExecutor;
        this.directoryMonitorManager = new DirectoryMonitorManager(Injector.instantiateModelOrService(DirectoryMonitor.class));
        this.aiService = aiService;

        initializeComponentsAndListeners(isDummyContext);

        // set LibraryTab ID for drag'n'drop
        // ID content doesn't matter, we only need different tabs to have different ID
        this.setId(Long.valueOf(new Random().nextLong()).toString());

        setOnCloseRequest(this::onCloseRequest);
        setOnClosed(this::onClosed);
    }

    private void initializeComponentsAndListeners(boolean isDummyContext) {
        if (!isDummyContext) {
            createIndexManager();
        }

        if (tableModel != null) {
            tableModel.unbind();
        }

        bibDatabaseContext.getDatabase().registerListener(this);
        bibDatabaseContext.getMetaData().registerListener(this);

        this.selectedGroupsProperty = new SimpleListProperty<>(stateManager.getSelectedGroups(bibDatabaseContext));
        this.tableModel = new MainTableDataModel(getBibDatabaseContext(), preferences, taskExecutor, getIndexManager(), selectedGroupsProperty(), searchQueryProperty(), resultSizeProperty());

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

        this.getDatabase().registerListener(new IndexUpdateListener());
        this.getDatabase().registerListener(new EntriesRemovedListener());

        // ensure that at each addition of a new entry, the entry is added to the groups interface
        this.bibDatabaseContext.getDatabase().registerListener(new GroupTreeListener());
        // ensure that all entry changes mark the panel as changed
        this.bibDatabaseContext.getDatabase().registerListener(this);

        this.getDatabase().registerListener(new UpdateTimestampListener(preferences));

        this.entryEditor = createEntryEditor();

        aiService.setupDatabase(bibDatabaseContext);

        Platform.runLater(() -> {
            EasyBind.subscribe(changedProperty, this::updateTabTitle);
            stateManager.getOpenDatabases().addListener((ListChangeListener<BibDatabaseContext>) c ->
                    updateTabTitle(changedProperty.getValue()));
        });
    }

    private EntryEditor createEntryEditor() {
        Supplier<LibraryTab> tabSupplier = () -> this;
        return new EntryEditor(this,
                // Actions are recreated here since this avoids passing more parameters and the amount of additional memory consumption is neglegtable.
                new UndoAction(tabSupplier, undoManager, dialogService, stateManager),
                new RedoAction(tabSupplier, undoManager, dialogService, stateManager));
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

        LOGGER.trace("loading.set(false);");
        loading.set(false);
        dataLoadingTask = null;
    }

    public void createIndexManager() {
        indexManager = new IndexManager(bibDatabaseContext, taskExecutor, preferences);
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

    private void setDatabaseContext(BibDatabaseContext bibDatabaseContext) {
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

        this.bibDatabaseContext = Objects.requireNonNull(bibDatabaseContext);

        stateManager.getOpenDatabases().add(bibDatabaseContext);

        initializeComponentsAndListeners(false);
        installAutosaveManagerAndBackupManager();
    }

    public void installAutosaveManagerAndBackupManager() {
        if (isDatabaseReadyForAutoSave(bibDatabaseContext)) {
            AutosaveManager autosaveManager = AutosaveManager.start(bibDatabaseContext);
            autosaveManager.registerListener(new AutosaveUiManager(this, dialogService, preferences, entryTypesManager));
        }
        if (isDatabaseReadyForBackup(bibDatabaseContext) && preferences.getFilePreferences().shouldCreateBackup()) {
            BackupManager.start(this, bibDatabaseContext, Injector.instantiateModelOrService(BibEntryTypesManager.class), preferences);
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
                tabTitle.append('*');
                tabTitle.append(Localization.lang("untitled"));
            } else {
                addSharedDbInformation(tabTitle, bibDatabaseContext);
                addSharedDbInformation(toolTipText, bibDatabaseContext);
            }
            addModeInfo(toolTipText, bibDatabaseContext);
            if ((databaseLocation == DatabaseLocation.LOCAL) && bibDatabaseContext.getDatabase().hasEntries()) {
                addChangedInformation(toolTipText, Localization.lang("untitled"));
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
        NamedCompound ce = new NamedCompound(Localization.lang("Save actions"));
        for (FieldChange change : changes) {
            ce.addEdit(new UndoableFieldChange(change));
        }
        ce.end();
        if (ce.hasEdits()) {
            getUndoManager().addEdit(ce);
        }
    }

    public void editEntryAndFocusField(BibEntry entry, Field field) {
        showAndEdit(entry);
        Platform.runLater(() -> {
            // Focus field and entry in main table (async to give entry editor time to load)
            entryEditor.setFocusToField(field);
        });
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
            if (!entries.isEmpty()) {
                // Update entry editor and preview according to selected entries
                entryEditor.setCurrentlyEditedEntry(entries.getFirst());
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

    public EntryEditor getEntryEditor() {
        return entryEditor;
    }

    /**
     * Sets the entry editor as the bottom component in the split pane. If an entry editor already was shown, makes sure that the divider doesn't move. Updates the mode to {@link PanelMode#MAIN_TABLE_AND_ENTRY_EDITOR}.
     * Then shows the given entry.
     *
     * Additionally, selects the entry in the main table - so that the selected entry in the main table always corresponds to the edited entry.
     *
     * @param entry The entry to edit.
     */
    public void showAndEdit(BibEntry entry) {
        this.clearAndSelect(entry);
        if (!splitPane.getItems().contains(entryEditor)) {
            splitPane.getItems().addLast(entryEditor);
            mode = PanelMode.MAIN_TABLE_AND_ENTRY_EDITOR;
            splitPane.setDividerPositions(preferences.getEntryEditorPreferences().getDividerPosition());
        }

        // We use != instead of equals because of performance reasons
        if (entry != showing) {
            entryEditor.setCurrentlyEditedEntry(entry);
            showing = entry;
        }
        entryEditor.requestFocus();
    }

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

        if ((mode == PanelMode.MAIN_TABLE_AND_ENTRY_EDITOR) && (entriesToCheck.contains(entryEditor.getCurrentlyEditedEntry()))) {
            closeBottomPane();
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

    /**
     * Depending on whether a preview or an entry editor is showing, save the current divider location in the correct preference setting.
     */
    private void saveDividerLocation(Number position) {
        if (mode == PanelMode.MAIN_TABLE_AND_ENTRY_EDITOR) {
            preferences.getEntryEditorPreferences().setDividerPosition(position.doubleValue());
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
     * @return <code>true</code> if the user choose to close the database
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
                SaveDatabaseAction saveAction = new SaveDatabaseAction(this, dialogService, preferences, Injector.instantiateModelOrService(BibEntryTypesManager.class));
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
            directoryMonitorManager.unregister();
        } catch (RuntimeException e) {
            LOGGER.error("Problem when closing directory monitor", e);
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

    public BibDatabaseContext getBibDatabaseContext() {
        return this.bibDatabaseContext;
    }

    public DirectoryMonitorManager getDirectoryMonitorManager() {
        return directoryMonitorManager;
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

        importHandler.importCleanedEntries(entries);
        getUndoManager().addEdit(new UndoableInsertEntries(bibDatabaseContext.getDatabase(), entries));
        markBaseChanged();
        if (preferences.getEntryEditorPreferences().shouldOpenOnNewEntry()) {
            showAndEdit(entries.getFirst());
        } else {
            clearAndSelect(entries.getFirst());
        }
    }

    public void copyEntry() {
        int entriesCopied = doCopyEntry(getSelectedEntries());
        if (entriesCopied >= 0) {
            dialogService.notify(Localization.lang("Copied %0 entry(s)", entriesCopied));
        } else {
            dialogService.notify(Localization.lang("Copy failed", entriesCopied));
        }
    }

    private int doCopyEntry(List<BibEntry> selectedEntries) {
        if (selectedEntries.isEmpty()) {
            return 0;
        }

        List<BibtexString> stringConstants = bibDatabaseContext.getDatabase().getUsedStrings(selectedEntries);
        try {
            if (stringConstants.isEmpty()) {
                clipBoardManager.setContent(selectedEntries, entryTypesManager);
            } else {
                clipBoardManager.setContent(selectedEntries, entryTypesManager, stringConstants);
            }
            return selectedEntries.size();
        } catch (IOException e) {
            LOGGER.error("Error while copying selected entries to clipboard.", e);
            return -1;
        }
    }

    public void pasteEntry() {
        List<BibEntry> entriesToAdd;
        String content = ClipBoardManager.getContents();
        entriesToAdd = importHandler.handleBibTeXData(content);
        if (entriesToAdd.isEmpty()) {
            entriesToAdd = handleNonBibTeXStringData(content);
        }
        if (entriesToAdd.isEmpty()) {
            return;
        }

        importHandler.importEntriesWithDuplicateCheck(bibDatabaseContext, entriesToAdd);
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

    public void dropEntry(List<BibEntry> entriesToAdd) {
        importHandler.importEntriesWithDuplicateCheck(bibDatabaseContext, entriesToAdd);
    }

    public void cutEntry() {
        int entriesCopied = doCopyEntry(getSelectedEntries());
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
        dialogService.notify(Localization.lang("Deleted %0 entry(s)", entriesDeleted));
    }

    public void deleteEntry(BibEntry entry) {
        doDeleteEntry(StandardActions.DELETE_ENTRY, Collections.singletonList(entry));
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

        ensureNotShowingBottomPanel(entries);
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

    /**
     * Creates a new library tab. Contents are loaded by the {@code dataLoadingTask}. Most of the other parameters are required by {@code resetChangeMonitor()}.
     *
     * @param dataLoadingTask The task to execute to load the data asynchronously.
     * @param file the path to the file (loaded by the dataLoadingTask)
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

    public static LibraryTab createLibraryTab(BibDatabaseContext databaseContext,
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
        Objects.requireNonNull(databaseContext);

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

    private class EntriesRemovedListener {

        @Subscribe
        public void listen(EntriesRemovedEvent entriesRemovedEvent) {
            ensureNotShowingBottomPanel(entriesRemovedEvent.getBibEntries());
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
                delay.setOnFinished(e -> this.hide());
                delay.play();
            }
        }
    }

    public DatabaseNotification getNotificationPane() {
        return databaseNotificationPane;
    }

    @Override
    public String toString() {
        return "LibraryTab{" +
                "bibDatabaseContext=" + bibDatabaseContext +
                ", showing=" + showing +
                '}';
    }

    public LibraryTabContainer getLibraryTabContainer() {
        return tabContainer;
    }
}
