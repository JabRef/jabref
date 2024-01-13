package org.jabref.gui;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.TimerTask;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.collections.transformation.FilteredList;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.skin.TabPaneSkin;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.TransferMode;
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
import org.jabref.gui.sidepane.SidePane;
import org.jabref.gui.sidepane.SidePaneType;
import org.jabref.gui.undo.CountingUndoManager;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.DefaultTaskExecutor;
import org.jabref.gui.util.TaskExecutor;
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
import org.jabref.model.groups.GroupTreeNode;
import org.jabref.model.util.FileUpdateMonitor;
import org.jabref.preferences.PreferencesService;
import org.jabref.preferences.TelemetryPreferences;

import com.google.common.eventbus.Subscribe;
import com.tobiasdiez.easybind.EasyBind;
import com.tobiasdiez.easybind.EasyObservableList;
import com.tobiasdiez.easybind.Subscription;
import org.fxmisc.richtext.CodeArea;
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

    @SuppressWarnings({"FieldCanBeLocal"}) private EasyObservableList<BibDatabaseContext> openDatabaseList;

    private final Stage mainStage;
    private final StateManager stateManager;
    private final CountingUndoManager undoManager;
    private final DialogService dialogService;
    private final FileUpdateMonitor fileUpdateMonitor;
    private final BibEntryTypesManager entryTypesManager;
    private final PushToApplicationCommand pushToApplicationCommand;
    private SidePane sidePane;
    private TabPane tabbedPane;

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

        this.globalSearchBar = new GlobalSearchBar(this, stateManager, prefs, undoManager, dialogService);
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

    private void initDragAndDrop() {
        Tab dndIndicator = new Tab(Localization.lang("Open files..."), null);
        dndIndicator.getStyleClass().add("drop");

        EasyBind.subscribe(tabbedPane.skinProperty(), skin -> {
            if (!(skin instanceof TabPaneSkin)) {
                return;
            }
            // Add drag and drop listeners to JabRefFrame
            this.getScene().setOnDragOver(event -> onSceneDragOver(event, dndIndicator));
            this.getScene().setOnDragEntered(event -> {
                // It is necessary to setOnDragOver for newly opened tabs
                // drag'n'drop on tabs covered dnd on tabbedPane, so dnd on tabs should contain all dnds on tabbedPane
                for (Node destinationTabNode : tabbedPane.lookupAll(".tab")) {
                    destinationTabNode.setOnDragOver(tabDragEvent -> onTabDragOver(event, tabDragEvent, dndIndicator));
                    destinationTabNode.setOnDragExited(event1 -> tabbedPane.getTabs().remove(dndIndicator));
                    destinationTabNode.setOnDragDropped(tabDragEvent -> onTabDragDropped(destinationTabNode, tabDragEvent, dndIndicator));
                }
                event.consume();
            });
            this.getScene().setOnDragExited(event -> tabbedPane.getTabs().remove(dndIndicator));
            this.getScene().setOnDragDropped(event -> onSceneDragDropped(event, dndIndicator));
        });
    }

    private void onTabDragDropped(Node destinationTabNode, DragEvent tabDragEvent, Tab dndIndicator) {
        Dragboard dragboard = tabDragEvent.getDragboard();

        if (DragAndDropHelper.hasBibFiles(dragboard)) {
            tabbedPane.getTabs().remove(dndIndicator);
            List<Path> bibFiles = DragAndDropHelper.getBibFiles(dragboard);
            OpenDatabaseAction openDatabaseAction = this.getOpenDatabaseAction();
            openDatabaseAction.openFiles(bibFiles);
            tabDragEvent.setDropCompleted(true);
            tabDragEvent.consume();
        } else {
            for (Tab libraryTab : tabbedPane.getTabs()) {
                if (libraryTab.getId().equals(destinationTabNode.getId()) &&
                        !tabbedPane.getSelectionModel().getSelectedItem().equals(libraryTab)) {
                    LibraryTab destinationLibraryTab = (LibraryTab) libraryTab;
                    if (DragAndDropHelper.hasGroups(dragboard)) {
                        List<String> groupPathToSources = DragAndDropHelper.getGroups(dragboard);

                        copyRootNode(destinationLibraryTab);

                        GroupTreeNode destinationLibraryGroupRoot = destinationLibraryTab
                                .getBibDatabaseContext()
                                .getMetaData()
                                .getGroups().get();

                        for (String pathToSource : groupPathToSources) {
                            GroupTreeNode groupTreeNodeToCopy = getCurrentLibraryTab()
                                    .getBibDatabaseContext()
                                    .getMetaData()
                                    .getGroups()
                                    .get()
                                    .getChildByPath(pathToSource)
                                    .get();
                            copyGroupTreeNode((LibraryTab) libraryTab, destinationLibraryGroupRoot, groupTreeNodeToCopy);
                        }
                        return;
                    }
                    destinationLibraryTab.dropEntry(stateManager.getLocalDragboard().getBibEntries());
                }
            }
            tabDragEvent.consume();
        }
    }

    private void onTabDragOver(DragEvent event, DragEvent tabDragEvent, Tab dndIndicator) {
        if (DragAndDropHelper.hasBibFiles(tabDragEvent.getDragboard()) || DragAndDropHelper.hasGroups(tabDragEvent.getDragboard())) {
            tabDragEvent.acceptTransferModes(TransferMode.ANY);
            if (!tabbedPane.getTabs().contains(dndIndicator)) {
                tabbedPane.getTabs().add(dndIndicator);
            }
            event.consume();
        } else {
            tabbedPane.getTabs().remove(dndIndicator);
        }

        if (tabDragEvent.getDragboard().hasContent(DragAndDropDataFormats.ENTRIES)) {
            tabDragEvent.acceptTransferModes(TransferMode.COPY);
            tabDragEvent.consume();
        }
    }

    private void onSceneDragOver(DragEvent event, Tab dndIndicator) {
        if (DragAndDropHelper.hasBibFiles(event.getDragboard())) {
            event.acceptTransferModes(TransferMode.ANY);
            if (!tabbedPane.getTabs().contains(dndIndicator)) {
                tabbedPane.getTabs().add(dndIndicator);
            }
            event.consume();
        } else {
            tabbedPane.getTabs().remove(dndIndicator);
        }
        // Accept drag entries from MainTable
        if (event.getDragboard().hasContent(DragAndDropDataFormats.ENTRIES)) {
            event.acceptTransferModes(TransferMode.COPY);
            event.consume();
        }
    }

    private void onSceneDragDropped(DragEvent event, Tab dndIndicator) {
        tabbedPane.getTabs().remove(dndIndicator);
        List<Path> bibFiles = DragAndDropHelper.getBibFiles(event.getDragboard());
        OpenDatabaseAction openDatabaseAction = this.getOpenDatabaseAction();
        openDatabaseAction.openFiles(bibFiles);
        event.setDropCompleted(true);
        event.consume();
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

    private void initShowTrackingNotification() {
        if (prefs.getTelemetryPreferences().shouldAskToCollectTelemetry()) {
            JabRefExecutorService.INSTANCE.submit(new TimerTask() {
                @Override
                public void run() {
                    DefaultTaskExecutor.runInJavaFXThread(JabRefFrame.this::showTrackingNotification);
                }
            }, 60000); // run in one minute
        }
    }

    private void showTrackingNotification() {
        TelemetryPreferences telemetryPreferences = prefs.getTelemetryPreferences();
        boolean shouldCollect = telemetryPreferences.shouldCollectTelemetry();

        if (!telemetryPreferences.shouldCollectTelemetry()) {
            shouldCollect = dialogService.showConfirmationDialogAndWait(
                    Localization.lang("Telemetry: Help make JabRef better"),
                    Localization.lang("To improve the user experience, we would like to collect anonymous statistics on the features you use. We will only record what features you access and how often you do it. We will neither collect any personal data nor the content of bibliographic items. If you choose to allow data collection, you can later disable it via File -> Preferences -> General."),
                    Localization.lang("Share anonymous statistics"),
                    Localization.lang("Don't share"));
        }

        telemetryPreferences.setCollectTelemetry(shouldCollect);
        telemetryPreferences.setAskToCollectTelemetry(false);
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

        WaitForSaveFinishedDialog waitForSaveFinishedDialog = new WaitForSaveFinishedDialog(dialogService);
        waitForSaveFinishedDialog.showAndWait(getLibraryTabs());

        return true;
    }

    /**
     * outprints the Data from the Screen (only in debug mode)
     *
     * @param mainStage JabRefs stage
     */
    private void debugLogWindowState(Stage mainStage) {
        if (LOGGER.isDebugEnabled()) {
            String debugLogString = "SCREEN DATA:" +
                    "mainStage.WINDOW_MAXIMISED: " + mainStage.isMaximized() + "\n" +
                    "mainStage.POS_X: " + mainStage.getX() + "\n" +
                    "mainStage.POS_Y: " + mainStage.getY() + "\n" +
                    "mainStage.SIZE_X: " + mainStage.getWidth() + "\n" +
                    "mainStages.SIZE_Y: " + mainStage.getHeight() + "\n";
            LOGGER.debug(debugLogString);
        }
    }

    private void initLayout() {
        setId("frame");

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
                Globals.predatoryJournalRepository,
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
    public List<LibraryTab> getLibraryTabs() {
        return tabbedPane.getTabs().stream()
                         .filter(LibraryTab.class::isInstance)
                         .map(LibraryTab.class::cast)
                         .collect(Collectors.toList());
    }

    @Deprecated
    public void showLibraryTabAt(int i) {
        tabbedPane.getSelectionModel().select(i);
    }

    public void showLibraryTab(LibraryTab libraryTab) {
        tabbedPane.getSelectionModel().select(libraryTab);
    }

    public void init() {
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
        tabbedPane = new TabPane();
        tabbedPane.setTabDragPolicy(TabPane.TabDragPolicy.REORDER);

        initLayout();
        initKeyBindings();
        initDragAndDrop();

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
            } else if (selectedTab == null) {
                // All databases are closed
                stateManager.setActiveDatabase(null);
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
                    () -> libraryTab.textProperty().getValue() + " \u2013 " + FRAME_TITLE,
                    libraryTab.textProperty());
            mainStage.titleProperty().bind(windowTitle);
        });
        initShowTrackingNotification();
    }

    /**
     * Returns the currently viewed BasePanel.
     */
    public LibraryTab getCurrentLibraryTab() {
        if ((tabbedPane == null) || (tabbedPane.getSelectionModel().getSelectedItem() == null)) {
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
                factory.createMenuItem(StandardActions.CLOSE_LIBRARY, new CloseDatabaseAction(this, tab)),
                factory.createMenuItem(StandardActions.CLOSE_OTHER_LIBRARIES, new CloseOthersDatabaseAction(tab)),
                factory.createMenuItem(StandardActions.CLOSE_ALL_LIBRARIES, new CloseAllDatabaseAction()));

        return contextMenu;
    }

    public void addTab(LibraryTab libraryTab, boolean raisePanel) {
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
     */
    public void addTab(BibDatabaseContext databaseContext, boolean raisePanel) {
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
     * Might be called when a user asks JabRef at the command line
     * i) to import a file or
     * ii) to open a .bib file
     */
    public void addTab(ParserResult parserResult, boolean raisePanel) {
        if (parserResult.toOpenTab()) {
            // Add the entries to the open tab.
            LibraryTab libraryTab = getCurrentLibraryTab();
            if (libraryTab == null) {
                // There is no open tab to add to, so we create a new tab:
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

    public boolean closeTab(LibraryTab libraryTab) {
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

    private void copyGroupTreeNode(LibraryTab destinationLibraryTab, GroupTreeNode parent, GroupTreeNode groupTreeNodeToCopy) {
        List<BibEntry> allEntries = getCurrentLibraryTab()
                .getBibDatabaseContext()
                .getEntries();
        // add groupTreeNodeToCopy to the parent-- in the first run that will the source/main GroupTreeNode
        GroupTreeNode copiedNode = parent.addSubgroup(groupTreeNodeToCopy.copyNode().getGroup());
        // add all entries of a groupTreeNode to the new library.
        destinationLibraryTab.dropEntry(groupTreeNodeToCopy.getEntriesInGroup(allEntries));
        // List of all children of groupTreeNodeToCopy
        List<GroupTreeNode> children = groupTreeNodeToCopy.getChildren();

        if (!children.isEmpty()) {
            // use recursion to add all subgroups of the original groupTreeNodeToCopy
            for (GroupTreeNode child : children) {
                copyGroupTreeNode(destinationLibraryTab, copiedNode, child);
            }
        }
    }

    private void copyRootNode(LibraryTab destinationLibraryTab) {
        if (destinationLibraryTab.getBibDatabaseContext().getMetaData().getGroups().isPresent()) {
            return;
        }
        // a root (all entries) GroupTreeNode
        GroupTreeNode currentLibraryGroupRoot = getCurrentLibraryTab().getBibDatabaseContext()
                                                                      .getMetaData()
                                                                      .getGroups()
                                                                      .get()
                                                                      .copyNode();

        // add currentLibraryGroupRoot to the Library if it does not have a root.
        destinationLibraryTab.getBibDatabaseContext()
                             .getMetaData()
                             .setGroups(currentLibraryGroupRoot);
    }

    /**
     * Refreshes the ui after preferences changes
     */
    public void refresh() {
        globalSearchBar.updateHintVisibility();
        getLibraryTabs().forEach(LibraryTab::setupMainPanel);
        getLibraryTabs().forEach(tab -> tab.getMainTable().getTableModel().resetFieldFormatter());
    }

    void openDatabases(List<ParserResult> parserResults, boolean isBlank) {
        final List<ParserResult> failed = new ArrayList<>();
        final List<ParserResult> toOpenTab = new ArrayList<>();

        // If the option is enabled, open the last edited libraries, if any.
        if (!isBlank && prefs.getWorkspacePreferences().shouldOpenLastEdited()) {
            openLastEditedDatabases();
        }

        // From here on, the libraries provided by command line arguments are treated

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
                            Globals.stateManager,
                            Globals.entryTypesManager,
                            fileUpdateMonitor,
                            undoManager,
                            Globals.TASK_EXECUTOR);
                } catch (
                        SQLException |
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

        for (ParserResult pr : failed) {
            String message = Localization.lang("Error opening file '%0'",
                    pr.getPath().map(Path::toString).orElse("(File name unknown)")) + "\n" +
                    pr.getErrorMessage();

            dialogService.showErrorDialogAndWait(Localization.lang("Error opening file"), message);
        }

        // Display warnings, if any
        for (int tabNumber = 0; tabNumber < parserResults.size(); tabNumber++) {
            // ToDo: Method needs to be rewritten, because the index of the parser result and of the libraryTab may not
            //  be identical, if there are also other tabs opened, that are not libraryTabs. Currently there are none,
            //  therefore for now this ok.
            ParserResult pr = parserResults.get(tabNumber);
            if (pr.hasWarnings()) {
                ParserResultWarningDialog.showParserResultWarningDialog(pr, dialogService);
                showLibraryTabAt(tabNumber);
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

    private void openLastEditedDatabases() {
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

        public CloseDatabaseAction(LibraryTabContainer tabContainer, LibraryTab libraryTab) {
            this.tabContainer = tabContainer;
            this.libraryTab = libraryTab;
        }

        /**
         * Using this constructor will result in executing the command on the currently open library tab
         */
        public CloseDatabaseAction(LibraryTabContainer tabContainer) {
            this(tabContainer, null);
        }

        @Override
        public void execute() {
            Platform.runLater(() -> {
                if (libraryTab == null) {
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
