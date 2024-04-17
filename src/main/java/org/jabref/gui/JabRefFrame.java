package org.jabref.gui;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableBooleanValue;
import javafx.collections.transformation.FilteredList;
import javafx.event.Event;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import org.jabref.gui.actions.ActionFactory;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.desktop.JabRefDesktop;
import org.jabref.gui.importer.ImportEntriesDialog;
import org.jabref.gui.importer.NewEntryAction;
import org.jabref.gui.importer.ParserResultWarningDialog;
import org.jabref.gui.importer.actions.OpenDatabaseAction;
import org.jabref.gui.keyboard.KeyBinding;
import org.jabref.gui.keyboard.KeyBindingRepository;
import org.jabref.gui.libraryproperties.LibraryPropertiesAction;
import org.jabref.gui.menus.FileHistoryMenu;
import org.jabref.gui.push.PushToApplicationCommand;
import org.jabref.gui.search.GlobalSearchBar;
import org.jabref.gui.search.SearchType;
import org.jabref.gui.sidepane.SidePane;
import org.jabref.gui.sidepane.SidePaneType;
import org.jabref.gui.undo.CountingUndoManager;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.UiCommand;
import org.jabref.logic.importer.ImportCleanup;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.shared.DatabaseNotSupportedException;
import org.jabref.logic.shared.exception.InvalidDBMSConnectionPropertiesException;
import org.jabref.logic.shared.exception.NotASharedDatabaseException;
import org.jabref.logic.undo.AddUndoableActionEvent;
import org.jabref.logic.undo.UndoChangeEvent;
import org.jabref.logic.undo.UndoRedoEvent;
import org.jabref.logic.util.OS;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.util.FileUpdateMonitor;
import org.jabref.preferences.PreferencesService;

import com.google.common.eventbus.Subscribe;
import com.tobiasdiez.easybind.EasyBind;
import com.tobiasdiez.easybind.EasyObservableList;
import com.tobiasdiez.easybind.Subscription;
import org.fxmisc.richtext.CodeArea;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents the inner frame of the JabRef window
 */
public class JabRefFrame extends BorderPane implements LibraryTabContainer {

    public static final String FRAME_TITLE = "JabRef";

    private static final Logger LOGGER = LoggerFactory.getLogger(JabRefFrame.class);

    private final SplitPane splitPane = new SplitPane();
    private final PreferencesService prefs;
    private final GlobalSearchBar globalSearchBar;

    private final FileHistoryMenu fileHistory;
    private final FrameDndHandler frameDndHandler;

    @SuppressWarnings({"FieldCanBeLocal"}) private EasyObservableList<BibDatabaseContext> openDatabaseList;

    private final Stage mainStage;
    private final StateManager stateManager;
    private final CountingUndoManager undoManager;
    private final DialogService dialogService;
    private final FileUpdateMonitor fileUpdateMonitor;
    private final BibEntryTypesManager entryTypesManager;
    private final PushToApplicationCommand pushToApplicationCommand;
    private SidePane sidePane;
    private final TabPane tabbedPane = new TabPane();

    private Subscription dividerSubscription;

    private final TaskExecutor taskExecutor;

    public JabRefFrame(Stage mainStage,
                       DialogService dialogService,
                       FileUpdateMonitor fileUpdateMonitor,
                       PreferencesService preferencesService) {
        this.mainStage = mainStage;
        this.dialogService = dialogService;
        this.fileUpdateMonitor = fileUpdateMonitor;
        this.prefs = preferencesService;

        this.stateManager = Globals.stateManager;
        this.undoManager = Globals.undoManager;
        this.entryTypesManager = Globals.entryTypesManager;
        this.taskExecutor = Globals.TASK_EXECUTOR;

        this.frameDndHandler = new FrameDndHandler(tabbedPane, mainStage::getScene, this::getOpenDatabaseAction, stateManager);

        this.globalSearchBar = new GlobalSearchBar(this, stateManager, prefs, undoManager, dialogService, SearchType.NORMAL_SEARCH);
        this.pushToApplicationCommand = new PushToApplicationCommand(stateManager, dialogService, prefs, taskExecutor);
        this.fileHistory = new FileHistoryMenu(prefs.getGuiPreferences().getFileHistory(), dialogService, getOpenDatabaseAction());

        this.setOnKeyTyped(key -> {
            if (this.fileHistory.isShowing()) {
                if (this.fileHistory.openFileByKey(key)) {
                    this.fileHistory.getParentMenu().hide();
                }
            }
        });

        init();
    }

    private void initKeyBindings() {
        addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            Optional<KeyBinding> keyBinding = Globals.getKeyPrefs().mapToKeyBinding(event);
            if (keyBinding.isPresent()) {
                switch (keyBinding.get()) {
                    case FOCUS_ENTRY_TABLE:
                        getCurrentLibraryTab().getMainTable().requestFocus();
                        event.consume();
                        break;
                    case FOCUS_GROUP_LIST:
                        sidePane.getSidePaneComponent(SidePaneType.GROUPS).requestFocus();
                        event.consume();
                    break;
                    case NEXT_LIBRARY:
                        tabbedPane.getSelectionModel().selectNext();
                        event.consume();
                        break;
                    case PREVIOUS_LIBRARY:
                        tabbedPane.getSelectionModel().selectPrevious();
                        event.consume();
                        break;
                    case SEARCH:
                        globalSearchBar.focus();
                        break;
                    case NEW_ARTICLE:
                        new NewEntryAction(this::getCurrentLibraryTab, StandardEntryType.Article, dialogService, prefs, stateManager).execute();
                        break;
                    case NEW_BOOK:
                        new NewEntryAction(this::getCurrentLibraryTab, StandardEntryType.Book, dialogService, prefs, stateManager).execute();
                        break;
                    case NEW_INBOOK:
                        new NewEntryAction(this::getCurrentLibraryTab, StandardEntryType.InBook, dialogService, prefs, stateManager).execute();
                        break;
                    case NEW_MASTERSTHESIS:
                        new NewEntryAction(this::getCurrentLibraryTab, StandardEntryType.MastersThesis, dialogService, prefs, stateManager).execute();
                        break;
                    case NEW_PHDTHESIS:
                        new NewEntryAction(this::getCurrentLibraryTab, StandardEntryType.PhdThesis, dialogService, prefs, stateManager).execute();
                        break;
                    case NEW_PROCEEDINGS:
                        new NewEntryAction(this::getCurrentLibraryTab, StandardEntryType.Proceedings, dialogService, prefs, stateManager).execute();
                        break;
                    case NEW_TECHREPORT:
                        new NewEntryAction(this::getCurrentLibraryTab, StandardEntryType.TechReport, dialogService, prefs, stateManager).execute();
                        break;
                    case NEW_UNPUBLISHED:
                        new NewEntryAction(this::getCurrentLibraryTab, StandardEntryType.Unpublished, dialogService, prefs, stateManager).execute();
                        break;
                    case NEW_INPROCEEDINGS:
                        new NewEntryAction(this::getCurrentLibraryTab, StandardEntryType.InProceedings, dialogService, prefs, stateManager).execute();
                        break;
                    case PASTE:
                        if (OS.OS_X) { // Workaround for a jdk issue that executes paste twice when using cmd+v in a TextField
                            // Extra workaround for CodeArea, which does not inherit from TextInputControl
                            if (!(stateManager.getFocusOwner().isPresent() && (stateManager.getFocusOwner().get() instanceof CodeArea))) {
                                event.consume();
                                break;
                            }
                            break;
                        }
                        break;
                    default:
                }
            }
        });
    }

    private void storeLastOpenedFiles(List<Path> filenames, Path focusedDatabase) {
        if (prefs.getWorkspacePreferences().shouldOpenLastEdited()) {
            // Here we store the names of all current files. If there is no current file, we remove any
            // previously stored filename.
            if (filenames.isEmpty()) {
                prefs.getGuiPreferences().getLastFilesOpened().clear();
            } else {
                prefs.getGuiPreferences().setLastFilesOpened(filenames);
                prefs.getGuiPreferences().setLastFocusedFile(focusedDatabase);
            }
        }
    }

    /**
     * Quit JabRef
     *
     * @return true if the user chose to quit; false otherwise
     */
    public boolean close() {
        // Ask if the user really wants to close, if there are still background tasks running
        // The background tasks may make changes themselves that need saving.
        if (stateManager.getAnyTasksThatWillNotBeRecoveredRunning().getValue()) {
            Optional<ButtonType> shouldClose = dialogService.showBackgroundProgressDialogAndWait(
                    Localization.lang("Please wait..."),
                    Localization.lang("Waiting for background tasks to finish. Quit anyway?"),
                    stateManager);
            if (!(shouldClose.isPresent() && (shouldClose.get() == ButtonType.YES))) {
                return false;
            }
        }

        // Read the opened and focused databases before closing them
        List<Path> openedLibraries = getLibraryTabs().stream()
                                                     .map(LibraryTab::getBibDatabaseContext)
                                                     .map(BibDatabaseContext::getDatabasePath)
                                                     .flatMap(Optional::stream)
                                                     .toList();
        Path focusedLibraries = Optional.ofNullable(getCurrentLibraryTab())
                                        .map(LibraryTab::getBibDatabaseContext)
                                        .flatMap(BibDatabaseContext::getDatabasePath)
                                        .orElse(null);

        // Then ask if the user really wants to close, if the library has not been saved since last save.
        if (!closeTabs()) {
            return false;
        }

        storeLastOpenedFiles(openedLibraries, focusedLibraries); // store only if successfully having closed the libraries

        ProcessingLibraryDialog processingLibraryDialog = new ProcessingLibraryDialog(dialogService);
        processingLibraryDialog.showAndWait(getLibraryTabs());

        return true;
    }

    private void initLayout() {
        setId("frame");

        sidePane = new SidePane(
                this,
                prefs,
                Globals.journalAbbreviationRepository,
                taskExecutor,
                dialogService,
                stateManager,
                fileUpdateMonitor,
                entryTypesManager,
                undoManager);

        MainToolBar mainToolBar = new MainToolBar(
                this,
                pushToApplicationCommand,
                globalSearchBar,
                dialogService,
                stateManager,
                prefs,
                fileUpdateMonitor,
                taskExecutor,
                entryTypesManager,
                undoManager);

        MainMenu mainMenu = new MainMenu(
                this,
                sidePane,
                pushToApplicationCommand,
                prefs,
                stateManager,
                fileUpdateMonitor,
                taskExecutor,
                dialogService,
                Globals.journalAbbreviationRepository,
                entryTypesManager,
                undoManager,
                Globals.getClipboardManager());

        VBox head = new VBox(mainMenu, mainToolBar);
        head.setSpacing(0d);
        setTop(head);

        splitPane.getItems().addAll(tabbedPane);
        SplitPane.setResizableWithParent(sidePane, false);

        sidePane.getChildren().addListener((InvalidationListener) c -> updateSidePane());
        updateSidePane();

        // We need to wait with setting the divider since it gets reset a few times during the initial set-up
        mainStage.showingProperty().addListener(new InvalidationListener() {
            @Override
            public void invalidated(Observable observable) {
                if (mainStage.isShowing()) {
                    Platform.runLater(() -> {
                        setDividerPosition();
                        observable.removeListener(this);
                    });
                }
            }
        });

        setCenter(splitPane);
    }

    private void updateSidePane() {
        if (sidePane.getChildren().isEmpty()) {
            if (dividerSubscription != null) {
                dividerSubscription.unsubscribe();
            }
            splitPane.getItems().remove(sidePane);
        } else {
            if (!splitPane.getItems().contains(sidePane)) {
                splitPane.getItems().addFirst(sidePane);
                setDividerPosition();
            }
        }
    }

    private void setDividerPosition() {
        if (mainStage.isShowing() && !sidePane.getChildren().isEmpty()) {
            splitPane.setDividerPositions(prefs.getGuiPreferences().getSidePaneWidth() / splitPane.getWidth());
            dividerSubscription = EasyBind.subscribe(sidePane.widthProperty(), width -> prefs.getGuiPreferences().setSidePaneWidth(width.doubleValue()));
        }
    }

    /**
     * Returns a list of all LibraryTabs in this frame.
     */
    public @NonNull List<LibraryTab> getLibraryTabs() {
        return tabbedPane.getTabs().stream()
                         .filter(LibraryTab.class::isInstance)
                         .map(LibraryTab.class::cast)
                         .toList();
    }

    public void showLibraryTab(@NonNull LibraryTab libraryTab) {
        tabbedPane.getSelectionModel().select(libraryTab);
    }

    public void init() {
        initLayout();
        initKeyBindings();
        frameDndHandler.initDragAndDrop();

        // Bind global state
        FilteredList<Tab> filteredTabs = new FilteredList<>(tabbedPane.getTabs());
        filteredTabs.setPredicate(LibraryTab.class::isInstance);

        // This variable cannot be inlined, since otherwise the list created by EasyBind is being garbage collected
        openDatabaseList = EasyBind.map(filteredTabs, tab -> ((LibraryTab) tab).getBibDatabaseContext());
        EasyBind.bindContent(stateManager.getOpenDatabases(), openDatabaseList);

        // the binding for stateManager.activeDatabaseProperty() is at org.jabref.gui.LibraryTab.onDatabaseLoadingSucceed

        // Subscribe to the search
        EasyBind.subscribe(stateManager.activeSearchQueryProperty(),
                query -> {
                    if (prefs.getSearchPreferences().shouldKeepSearchString()) {
                        for (LibraryTab tab : getLibraryTabs()) {
                            tab.setCurrentSearchQuery(query);
                        }
                    } else {
                        if (getCurrentLibraryTab() != null) {
                            getCurrentLibraryTab().setCurrentSearchQuery(query);
                        }
                    }
                });

        // Wait for the scene to be created, otherwise focusOwnerProperty is not provided
        Platform.runLater(() -> stateManager.focusOwnerProperty().bind(
                EasyBind.map(mainStage.getScene().focusOwnerProperty(), Optional::ofNullable)));

        EasyBind.subscribe(tabbedPane.getSelectionModel().selectedItemProperty(), selectedTab -> {
            if (selectedTab instanceof LibraryTab libraryTab) {
                stateManager.setActiveDatabase(libraryTab.getBibDatabaseContext());
                stateManager.activeTabProperty().set(Optional.of(libraryTab));
            } else if (selectedTab == null) {
                // All databases are closed
                stateManager.setActiveDatabase(null);
                stateManager.activeTabProperty().set(Optional.empty());
            }
        });

        /*
         * The following state listener makes sure focus is registered with the
         * correct database when the user switches tabs. Without this,
         * cut/paste/copy operations would sometimes occur in the wrong tab.
         */
        EasyBind.subscribe(tabbedPane.getSelectionModel().selectedItemProperty(), tab -> {
            if (!(tab instanceof LibraryTab libraryTab)) {
                stateManager.setSelectedEntries(Collections.emptyList());
                mainStage.titleProperty().unbind();
                mainStage.setTitle(FRAME_TITLE);
                return;
            }

            // Poor-mans binding to global state
            stateManager.setSelectedEntries(libraryTab.getSelectedEntries());

            // Update active search query when switching between databases
            if (prefs.getSearchPreferences().shouldKeepSearchString() && libraryTab.getCurrentSearchQuery().isEmpty() && stateManager.activeSearchQueryProperty().get().isPresent()) {
                // apply search query also when opening a new library and keep search string is activated
                libraryTab.setCurrentSearchQuery(stateManager.activeSearchQueryProperty().get());
            } else {
                stateManager.activeSearchQueryProperty().set(libraryTab.getCurrentSearchQuery());
            }

            // Update search autocompleter with information for the correct database:
            globalSearchBar.setAutoCompleter(libraryTab.getAutoCompleter());

            libraryTab.getUndoManager().postUndoRedoEvent();
            libraryTab.getMainTable().requestFocus();

            // Set window title - copy tab title
            StringBinding windowTitle = Bindings.createStringBinding(
                    () -> libraryTab.textProperty().getValue() + " – " + FRAME_TITLE, // not a minus, but codepoint 2013
                    libraryTab.textProperty());
            mainStage.titleProperty().bind(windowTitle);
        });

        Telemetry.initTrackingNotification(dialogService, prefs.getTelemetryPreferences());
    }

    /**
     * Returns the currently viewed BasePanel.
     */
    public LibraryTab getCurrentLibraryTab() {
        if (tabbedPane.getSelectionModel().getSelectedItem() == null) {
            return null;
        }
        return (LibraryTab) tabbedPane.getSelectionModel().getSelectedItem();
    }

    private ContextMenu createTabContextMenuFor(LibraryTab tab, KeyBindingRepository keyBindingRepository) {
        ContextMenu contextMenu = new ContextMenu();
        ActionFactory factory = new ActionFactory(keyBindingRepository);

        contextMenu.getItems().addAll(
                factory.createMenuItem(StandardActions.LIBRARY_PROPERTIES, new LibraryPropertiesAction(tab::getBibDatabaseContext, stateManager)),
                factory.createMenuItem(StandardActions.OPEN_DATABASE_FOLDER, new OpenDatabaseFolder(tab::getBibDatabaseContext)),
                factory.createMenuItem(StandardActions.OPEN_CONSOLE, new OpenConsoleAction(tab::getBibDatabaseContext, stateManager, prefs, dialogService)),
                new SeparatorMenuItem(),
                factory.createMenuItem(StandardActions.CLOSE_LIBRARY, new CloseDatabaseAction(this, tab, stateManager)),
                factory.createMenuItem(StandardActions.CLOSE_OTHER_LIBRARIES, new CloseOthersDatabaseAction(tab)),
                factory.createMenuItem(StandardActions.CLOSE_ALL_LIBRARIES, new CloseAllDatabaseAction()));

        return contextMenu;
    }

    public void addTab(@NonNull LibraryTab libraryTab, boolean raisePanel) {
        tabbedPane.getTabs().add(libraryTab);
        if (raisePanel) {
            tabbedPane.getSelectionModel().select(libraryTab);
            tabbedPane.requestFocus();
        }

        libraryTab.setContextMenu(createTabContextMenuFor(libraryTab, Globals.getKeyPrefs()));

        libraryTab.getUndoManager().registerListener(new UndoRedoEventManager());
    }

    /**
     * Opens a new tab with existing data.
     * Asynchronous loading is done at {@link LibraryTab#createLibraryTab}.
     * Similar method: {@link OpenDatabaseAction#openTheFile(Path)}
     */
    public void addTab(@NonNull BibDatabaseContext databaseContext, boolean raisePanel) {
        Objects.requireNonNull(databaseContext);
        LibraryTab libraryTab = LibraryTab.createLibraryTab(
                databaseContext,
                this,
                dialogService,
                prefs,
                stateManager,
                fileUpdateMonitor,
                entryTypesManager,
                undoManager,
                taskExecutor);
        addTab(libraryTab, raisePanel);
    }

    /**
     * Should be called when a user asks JabRef at the command line
     * i) to import a file or
     * ii) to open a .bib file
     */
    public void addTab(ParserResult parserResult, boolean raisePanel) {
        if (parserResult.toOpenTab()) {
            LOGGER.trace("Adding the entries to the open tab.");
            LibraryTab libraryTab = getCurrentLibraryTab();
            if (libraryTab == null) {
                LOGGER.debug("No open tab found to add entries to. Creating a new tab.");
                addTab(parserResult.getDatabaseContext(), raisePanel);
            } else {
                addImportedEntries(libraryTab, parserResult);
            }
        } else {
            // only add tab if library is not already open
            Optional<LibraryTab> libraryTab = getLibraryTabs().stream()
                                                              .filter(p -> p.getBibDatabaseContext()
                                                                            .getDatabasePath()
                                                                            .equals(parserResult.getPath()))
                                                              .findFirst();

            if (libraryTab.isPresent()) {
                tabbedPane.getSelectionModel().select(libraryTab.get());
            } else {
                // On this place, a tab is added after loading using the command line
                // This takes a different execution path than loading a library using the GUI
                addTab(parserResult.getDatabaseContext(), raisePanel);
            }
        }
    }

    /**
     * Opens the import inspection dialog to let the user decide which of the given entries to import.
     *
     * @param panel        The BasePanel to add to.
     * @param parserResult The entries to add.
     */
    private void addImportedEntries(final LibraryTab panel, final ParserResult parserResult) {
        BackgroundTask<ParserResult> task = BackgroundTask.wrap(() -> parserResult);
        ImportCleanup cleanup = ImportCleanup.targeting(panel.getBibDatabaseContext().getMode());
        cleanup.doPostCleanup(parserResult.getDatabase().getEntries());
        ImportEntriesDialog dialog = new ImportEntriesDialog(panel.getBibDatabaseContext(), task);
        dialog.setTitle(Localization.lang("Import"));
        dialogService.showCustomDialogAndWait(dialog);
    }

    public boolean closeTab(@NonNull LibraryTab libraryTab) {
        if (libraryTab.requestClose()) {
            tabbedPane.getTabs().remove(libraryTab);
            Event.fireEvent(libraryTab, new Event(this, libraryTab, Tab.CLOSED_EVENT));
            return true;
        }
        return false;
    }

    private boolean closeTabs() {
        // Ask before closing any tab, if any tab has changes
        for (LibraryTab libraryTab : getLibraryTabs()) {
            if (!libraryTab.requestClose()) {
                return false;
            }
        }

        // Close after checking for changes and saving all databases
        for (LibraryTab libraryTab : getLibraryTabs()) {
            tabbedPane.getTabs().remove(libraryTab);
            Event.fireEvent(libraryTab, new Event(this, libraryTab, Tab.CLOSED_EVENT));
        }
        return true;
    }

    public boolean closeCurrentTab() {
        return closeTab(getCurrentLibraryTab());
    }

    public OpenDatabaseAction getOpenDatabaseAction() {
        return new OpenDatabaseAction(
                this,
                prefs,
                dialogService,
                stateManager,
                fileUpdateMonitor,
                entryTypesManager,
                undoManager,
                taskExecutor);
    }

    /**
     * Refreshes the ui after preferences changes
     */
    public void refresh() {
        globalSearchBar.updateHintVisibility();
        getLibraryTabs().forEach(LibraryTab::setupMainPanel);
        getLibraryTabs().forEach(tab -> tab.getMainTable().getTableModel().resetFieldFormatter());
    }

    void openDatabases(List<ParserResult> parserResults) {
        final List<ParserResult> failed = new ArrayList<>();
        final List<ParserResult> toOpenTab = new ArrayList<>();

        // Remove invalid databases
        List<ParserResult> invalidDatabases = parserResults.stream()
                                                           .filter(ParserResult::isInvalid)
                                                           .toList();
        failed.addAll(invalidDatabases);
        parserResults.removeAll(invalidDatabases);

        // passed file (we take the first one) should be focused
        Path focusedFile = parserResults.stream()
                                        .findFirst()
                                        .flatMap(ParserResult::getPath)
                                        .orElse(prefs.getGuiPreferences()
                                                     .getLastFocusedFile())
                                        .toAbsolutePath();

        // Add all bibDatabases databases to the frame:
        boolean first = false;
        for (ParserResult parserResult : parserResults) {
            // Define focused tab
            if (parserResult.getPath().filter(path -> path.toAbsolutePath().equals(focusedFile)).isPresent()) {
                first = true;
            }

            if (parserResult.getDatabase().isShared()) {
                try {
                    OpenDatabaseAction.openSharedDatabase(
                            parserResult,
                            this,
                            dialogService,
                            prefs,
                            stateManager,
                            entryTypesManager,
                            fileUpdateMonitor,
                            undoManager,
                            taskExecutor);
                } catch (SQLException |
                         DatabaseNotSupportedException |
                         InvalidDBMSConnectionPropertiesException |
                         NotASharedDatabaseException e) {
                    LOGGER.error("Connection error", e);
                    dialogService.showErrorDialogAndWait(
                            Localization.lang("Connection error"),
                            Localization.lang("A local copy will be opened."),
                            e);
                    toOpenTab.add(parserResult);
                }
            } else if (parserResult.toOpenTab()) {
                // things to be appended to an opened tab should be done after opening all tabs
                // add them to the list
                toOpenTab.add(parserResult);
            } else {
                addTab(parserResult, first);
                first = false;
            }
        }

        // finally add things to the currently opened tab
        for (ParserResult parserResult : toOpenTab) {
            addTab(parserResult, first);
            first = false;
        }

        for (ParserResult parserResult : failed) {
            String message = Localization.lang("Error opening file '%0'",
                    parserResult.getPath().map(Path::toString).orElse("(File name unknown)")) + "\n" +
                    parserResult.getErrorMessage();
            dialogService.showErrorDialogAndWait(Localization.lang("Error opening file"), message);
        }

        // Display warnings, if any
        for (ParserResult parserResult : parserResults) {
            if (parserResult.hasWarnings()) {
                ParserResultWarningDialog.showParserResultWarningDialog(parserResult, dialogService);
                getLibraryTabs().stream()
                                .filter(tab -> parserResult.getDatabase().equals(tab.getDatabase()))
                                .findAny()
                                .ifPresent(this::showLibraryTab);
            }
        }

        // After adding the databases, go through each and see if
        // any post open actions need to be done. For instance, checking
        // if we found new entry types that can be imported, or checking
        // if the database contents should be modified due to new features
        // in this version of JabRef.
        parserResults.forEach(pr -> OpenDatabaseAction.performPostOpenActions(pr, dialogService));

        LOGGER.debug("Finished adding panels");
    }

    public void openLastEditedDatabases() {
        List<Path> lastFiles = prefs.getGuiPreferences().getLastFilesOpened();
        if (lastFiles.isEmpty()) {
            return;
        }

        getOpenDatabaseAction().openFiles(lastFiles);
    }

    public FileHistoryMenu getFileHistory() {
        return fileHistory;
    }

    public Stage getMainStage() {
        return mainStage;
    }

    /**
     * Handles commands submitted by the command line or by the remote host to be executed in the ui
     * Needs to run in a certain order. E.g. databases have to be loaded before selecting an entry.
     *
     * @param uiCommands to be handled
     */
    public void handleUiCommands(List<UiCommand> uiCommands) {
        LOGGER.debug("Handling UI commands {}", uiCommands);
        if (uiCommands.isEmpty()) {
            return;
        }

        // Handle blank workspace
        boolean blank = uiCommands.stream().anyMatch(UiCommand.BlankWorkspace.class::isInstance);

        // Handle OpenDatabases
        if (!blank) {
            uiCommands.stream()
                    .filter(UiCommand.OpenDatabases.class::isInstance)
                    .map(UiCommand.OpenDatabases.class::cast)
                    .forEach(command -> openDatabases(command.parserResults()));
        }

        // Handle jumpToEntry
        uiCommands.stream()
                  .filter(UiCommand.JumpToEntryKey.class::isInstance)
                  .map(UiCommand.JumpToEntryKey.class::cast)
                  .map(UiCommand.JumpToEntryKey::citationKey)
                  .filter(Objects::nonNull)
                  .findAny().ifPresent(entryKey -> {
                      LOGGER.debug("Jump to entry {} requested", entryKey);
                      // tabs must be present and contents async loaded for an entry to be selected
                      waitForLoadingFinished(() -> jumpToEntry(entryKey));
                  });
    }

    private void jumpToEntry(String entryKey) {
        // check current library tab first
        LibraryTab currentLibraryTab = getCurrentLibraryTab();
        List<LibraryTab> sortedTabs = getLibraryTabs().stream()
                                                .sorted(Comparator.comparing(tab -> tab != currentLibraryTab))
                                                .toList();
        for (LibraryTab libraryTab : sortedTabs) {
            Optional<BibEntry> bibEntry = libraryTab.getDatabase()
                                                    .getEntries().stream()
                                                    .filter(entry -> entry.getCitationKey().orElse("")
                                                                          .equals(entryKey))
                                                    .findAny();
            if (bibEntry.isPresent()) {
                LOGGER.debug("Found entry {} in library tab {}", entryKey, libraryTab);
                libraryTab.clearAndSelect(bibEntry.get());
                showLibraryTab(libraryTab);
                break;
            }
        }

        LOGGER.trace("End of loop");

        if (stateManager.getSelectedEntries().isEmpty()) {
            dialogService.notify(Localization.lang("Citation key '%0' to select not found in open libraries.", entryKey));
        }
    }

    private void waitForLoadingFinished(Runnable runnable) {
        LOGGER.trace("Waiting for all tabs being loaded");

        CompletableFuture<Void> future = new CompletableFuture<>();

        List<ObservableBooleanValue> loadings = getLibraryTabs().stream()
                                                                .map(LibraryTab::getLoading)
                                                                .collect(Collectors.toList());

        // Create a listener for each observable
        ChangeListener<Boolean> listener = (observable, oldValue, newValue) -> {
            if (observable != null) {
                loadings.remove(observable);
            }
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Count of loading tabs: {}", loadings.size());
                LOGGER.trace("Count of loading tabs really true: {}", loadings.stream().filter(ObservableBooleanValue::get).count());
            }
            for (ObservableBooleanValue obs : loadings) {
                if (obs.get()) {
                    // Exit the listener if any of the observables is still true
                    return;
                }
            }
            // All observables are false, complete the future
            LOGGER.trace("Future completed");
            future.complete(null);
        };

        for (ObservableBooleanValue obs : loadings) {
            obs.addListener(listener);
        }

        LOGGER.trace("Fire once");
        // Due to concurrency, it might be that the observables are already false, so we trigger one evaluation
        listener.changed(null, null, false);
        LOGGER.trace("Waiting for state changes...");

        future.thenRun(() -> {
            LOGGER.debug("All tabs loaded. Jumping to entry.");
            for (ObservableBooleanValue obs : loadings) {
                obs.removeListener(listener);
            }
            runnable.run();
        });
    }

    /**
     * The action concerned with closing the window.
     */
    static protected class CloseAction extends SimpleCommand {

        private final JabRefFrame frame;

        public CloseAction(JabRefFrame frame) {
            this.frame = frame;
        }

        @Override
        public void execute() {
            if (frame.close()) {
                frame.mainStage.close();
            }
        }
    }

    static protected class CloseDatabaseAction extends SimpleCommand {

        private final LibraryTabContainer tabContainer;
        private final LibraryTab libraryTab;

        public CloseDatabaseAction(LibraryTabContainer tabContainer, LibraryTab libraryTab, StateManager stateManager) {
            this.tabContainer = tabContainer;
            this.libraryTab = libraryTab;
            this.executable.bind(ActionHelper.needsDatabase(stateManager));
        }

        /**
         * Using this constructor will result in executing the command on the currently open library tab
         */
        public CloseDatabaseAction(LibraryTabContainer tabContainer, StateManager stateManager) {
            this(tabContainer, null, stateManager);
        }

        @Override
        public void execute() {
            Platform.runLater(() -> {
                if (libraryTab == null) {
                    if (tabContainer.getCurrentLibraryTab() == null) {
                        LOGGER.error("No library tab to close");
                        return;
                    }
                    tabContainer.closeCurrentTab();
                } else {
                    tabContainer.closeTab(libraryTab);
                }
            });
        }
    }

    private class CloseOthersDatabaseAction extends SimpleCommand {

        private final LibraryTab libraryTab;

        public CloseOthersDatabaseAction(LibraryTab libraryTab) {
            this.libraryTab = libraryTab;
            this.executable.bind(ActionHelper.isOpenMultiDatabase(tabbedPane));
        }

        @Override
        public void execute() {
            LibraryTab toKeepLibraryTab = Optional.of(libraryTab).get();
            for (Tab tab : tabbedPane.getTabs()) {
                LibraryTab libraryTab = (LibraryTab) tab;
                if (libraryTab != toKeepLibraryTab) {
                    Platform.runLater(() -> closeTab(libraryTab));
                }
            }
        }
    }

    private class CloseAllDatabaseAction extends SimpleCommand {

        @Override
        public void execute() {
            for (Tab tab : tabbedPane.getTabs()) {
                Platform.runLater(() -> closeTab((LibraryTab) tab));
            }
        }
    }

    private class OpenDatabaseFolder extends SimpleCommand {

        private final Supplier<BibDatabaseContext> databaseContext;

        public OpenDatabaseFolder(Supplier<BibDatabaseContext> databaseContext) {
            this.databaseContext = databaseContext;
        }

        @Override
        public void execute() {
            Optional.of(databaseContext.get()).flatMap(BibDatabaseContext::getDatabasePath).ifPresent(path -> {
                try {
                    JabRefDesktop.openFolderAndSelectFile(path, prefs.getExternalApplicationsPreferences(), dialogService);
                } catch (IOException e) {
                    LOGGER.info("Could not open folder", e);
                }
            });
        }
    }

    private class UndoRedoEventManager {

        @Subscribe
        public void listen(UndoRedoEvent event) {
            updateTexts(event);
            JabRefFrame.this.getCurrentLibraryTab().updateEntryEditorIfShowing();
        }

        @Subscribe
        public void listen(AddUndoableActionEvent event) {
            updateTexts(event);
        }

        private void updateTexts(UndoChangeEvent event) {
            /* TODO
            SwingUtilities.invokeLater(() -> {
                undo.putValue(Action.SHORT_DESCRIPTION, event.getUndoDescription());
                undo.setEnabled(event.isCanUndo());
                redo.putValue(Action.SHORT_DESCRIPTION, event.getRedoDescription());
                redo.setEnabled(event.isCanRedo());
            });
            */
        }
    }
}
