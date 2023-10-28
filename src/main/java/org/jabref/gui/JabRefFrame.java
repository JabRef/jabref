package org.jabref.gui;

import java.io.IOException;
import java.nio.file.Path;
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
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.skin.TabPaneSkin;
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
import org.jabref.gui.autosaveandbackup.AutosaveManager;
import org.jabref.gui.autosaveandbackup.BackupManager;
import org.jabref.gui.desktop.JabRefDesktop;
import org.jabref.gui.exporter.SaveDatabaseAction;
import org.jabref.gui.help.HelpAction;
import org.jabref.gui.importer.ImportEntriesDialog;
import org.jabref.gui.importer.NewEntryAction;
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
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.importer.ImportCleanup;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.shared.DatabaseLocation;
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
import org.jabref.preferences.GuiPreferences;
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
 * The main window of the application.
 */
public class JabRefFrame extends BorderPane implements LibraryTabContainer {

    public static final String FRAME_TITLE = "JabRef";

    private static final Logger LOGGER = LoggerFactory.getLogger(JabRefFrame.class);

    private final SplitPane splitPane = new SplitPane();
    private final PreferencesService prefs = Globals.prefs;
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

    public JabRefFrame(Stage mainStage) {
        this.mainStage = mainStage;
        this.stateManager = Globals.stateManager;
        this.dialogService = new JabRefDialogService(mainStage);
        this.undoManager = Globals.undoManager;
        this.fileUpdateMonitor = Globals.getFileUpdateMonitor();
        this.entryTypesManager = Globals.entryTypesManager;
        this.globalSearchBar = new GlobalSearchBar(this, stateManager, prefs, undoManager, dialogService);
        this.taskExecutor = Globals.TASK_EXECUTOR;
        this.pushToApplicationCommand = new PushToApplicationCommand(stateManager, dialogService, prefs, taskExecutor);
        this.fileHistory = new FileHistoryMenu(prefs.getGuiPreferences().getFileHistory(), dialogService, getOpenDatabaseAction());
        this.setOnKeyTyped(key -> {
            if (this.fileHistory.isShowing()) {
                if (this.fileHistory.openFileByKey(key)) {
                    this.fileHistory.getParentMenu().hide();
                }
            }
        });
    }

    private void initDragAndDrop() {
        Tab dndIndicator = new Tab(Localization.lang("Open files..."), null);
        dndIndicator.getStyleClass().add("drop");

        EasyBind.subscribe(tabbedPane.skinProperty(), skin -> {
            if (!(skin instanceof TabPaneSkin)) {
                return;
            }
            // Add drag and drop listeners to JabRefFrame
            this.getScene().setOnDragOver(event -> {
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
            });

            this.getScene().setOnDragEntered(event -> {
                // It is necessary to setOnDragOver for newly opened tabs
                // drag'n'drop on tabs covered dnd on tabbedPane, so dnd on tabs should contain all dnds on tabbedPane
                tabbedPane.lookupAll(".tab").forEach(destinationTabNode -> {
                    destinationTabNode.setOnDragOver(tabDragEvent -> {
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
                    });
                    destinationTabNode.setOnDragExited(event1 -> tabbedPane.getTabs().remove(dndIndicator));
                    destinationTabNode.setOnDragDropped(tabDragEvent -> {

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
                    });
                });
                event.consume();
            });

            this.getScene().setOnDragExited(event -> tabbedPane.getTabs().remove(dndIndicator));
            this.getScene().setOnDragDropped(event -> {
                tabbedPane.getTabs().remove(dndIndicator);
                List<Path> bibFiles = DragAndDropHelper.getBibFiles(event.getDragboard());
                OpenDatabaseAction openDatabaseAction = this.getOpenDatabaseAction();
                openDatabaseAction.openFiles(bibFiles);
                event.setDropCompleted(true);
                event.consume();
            });
        });
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
                        getGlobalSearchBar().focus();
                        break;
                    case NEW_ARTICLE:
                        new NewEntryAction(this, StandardEntryType.Article, dialogService, prefs, stateManager).execute();
                        break;
                    case NEW_BOOK:
                        new NewEntryAction(this, StandardEntryType.Book, dialogService, prefs, stateManager).execute();
                        break;
                    case NEW_INBOOK:
                        new NewEntryAction(this, StandardEntryType.InBook, dialogService, prefs, stateManager).execute();
                        break;
                    case NEW_MASTERSTHESIS:
                        new NewEntryAction(this, StandardEntryType.MastersThesis, dialogService, prefs, stateManager).execute();
                        break;
                    case NEW_PHDTHESIS:
                        new NewEntryAction(this, StandardEntryType.PhdThesis, dialogService, prefs, stateManager).execute();
                        break;
                    case NEW_PROCEEDINGS:
                        new NewEntryAction(this, StandardEntryType.Proceedings, dialogService, prefs, stateManager).execute();
                        break;
                    case NEW_TECHREPORT:
                        new NewEntryAction(this, StandardEntryType.TechReport, dialogService, prefs, stateManager).execute();
                        break;
                    case NEW_UNPUBLISHED:
                        new NewEntryAction(this, StandardEntryType.Unpublished, dialogService, prefs, stateManager).execute();
                        break;
                    case NEW_INPROCEEDINGS:
                        new NewEntryAction(this, StandardEntryType.InProceedings, dialogService, prefs, stateManager).execute();
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

    /**
     * The MacAdapter calls this method when a "BIB" file has been double-clicked from the Finder.
     */
    public void openAction(String filePath) {
        Path file = Path.of(filePath);
        getOpenDatabaseAction().openFile(file);
    }

    /**
     * The MacAdapter calls this method when "About" is selected from the application menu.
     */
    public void about() {
        new HelpAction(HelpFile.CONTENTS, dialogService, prefs.getFilePreferences()).execute();
    }

    /**
     * Tears down all things started by JabRef
     * <p>
     * FIXME: Currently some threads remain and therefore hinder JabRef to be closed properly
     *
     * @param filenames the filenames of all currently opened files - used for storing them if prefs openLastEdited is
     *                  set to true
     */
    private void tearDownJabRef(List<String> filenames) {
        if (prefs.getWorkspacePreferences().shouldOpenLastEdited()) {
            // Here we store the names of all current files. If there is no current file, we remove any
            // previously stored filename.
            if (filenames.isEmpty()) {
                prefs.getGuiPreferences().getLastFilesOpened().clear();
            } else {
                Path focusedDatabase = getCurrentLibraryTab().getBibDatabaseContext()
                                                             .getDatabasePath()
                                                             .orElse(null);
                prefs.getGuiPreferences().setLastFilesOpened(filenames);
                prefs.getGuiPreferences().setLastFocusedFile(focusedDatabase);
            }
        }

        prefs.flush();
    }

    /**
     * General info dialog.  The MacAdapter calls this method when "Quit" is selected from the application menu, Cmd-Q
     * is pressed, or "Quit" is selected from the Dock. The function returns a boolean indicating if quitting is ok or
     * not.
     * <p>
     * Non-OSX JabRef calls this when choosing "Quit" from the menu
     * <p>
     * SIDE EFFECT: tears down JabRef
     *
     * @return true if the user chose to quit; false otherwise
     */
    public boolean quit() {
        // First ask if the user really wants to close, if there are still background tasks running
        /*
        It is important to wait for unfinished background tasks before checking if a save-operation is needed, because
        the background tasks may make changes themselves that need saving.
         */
        if (stateManager.getAnyTasksThatWillNotBeRecoveredRunning().getValue()) {
            Optional<ButtonType> shouldClose = dialogService.showBackgroundProgressDialogAndWait(
                    Localization.lang("Please wait..."),
                    Localization.lang("Waiting for background tasks to finish. Quit anyway?"),
                    stateManager);
            if (!(shouldClose.isPresent() && (shouldClose.get() == ButtonType.YES))) {
                return false;
            }
        }

        // Then ask if the user really wants to close, if the library has not been saved since last save.
        List<String> filenames = new ArrayList<>();
        for (int i = 0; i < tabbedPane.getTabs().size(); i++) {
            LibraryTab libraryTab = getLibraryTabAt(i);
            final BibDatabaseContext context = libraryTab.getBibDatabaseContext();
            if (libraryTab.isModified() && (context.getLocation() == DatabaseLocation.LOCAL)) {
                tabbedPane.getSelectionModel().select(i);
                if (!confirmClose(libraryTab)) {
                    return false;
                }
            } else if (context.getLocation() == DatabaseLocation.SHARED) {
                context.convertToLocalDatabase();
                context.getDBMSSynchronizer().closeSharedDatabase();
                context.clearDBMSSynchronizer();
            }
            AutosaveManager.shutdown(context);
            BackupManager.shutdown(context, prefs.getFilePreferences().getBackupDirectory(), prefs.getFilePreferences().shouldCreateBackup());
            context.getDatabasePath().map(Path::toAbsolutePath).map(Path::toString).ifPresent(filenames::add);
        }

        WaitForSaveFinishedDialog waitForSaveFinishedDialog = new WaitForSaveFinishedDialog(dialogService);
        waitForSaveFinishedDialog.showAndWait(getLibraryTabs());

        // We call saveWindow state here again because under Mac the windowClose listener on the stage isn't triggered when using cmd + q
        saveWindowState();
        // Good bye!
        tearDownJabRef(filenames);
        Platform.exit();
        return true;
    }

    public void saveWindowState() {
        GuiPreferences preferences = prefs.getGuiPreferences();
        preferences.setPositionX(mainStage.getX());
        preferences.setPositionY(mainStage.getY());
        preferences.setSizeX(mainStage.getWidth());
        preferences.setSizeY(mainStage.getHeight());
        preferences.setWindowMaximised(mainStage.isMaximized());
        preferences.setWindowFullScreen(mainStage.isFullScreen());
        debugLogWindowState(mainStage);
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
                splitPane.getItems().add(0, sidePane);
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
     * Returns the indexed LibraryTab.
     *
     * @param i Index of base
     */
    public LibraryTab getLibraryTabAt(int i) {
        return (LibraryTab) tabbedPane.getTabs().get(i);
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
            libraryTab.updateSearchManager();

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

    /**
     * @return the BasePanel count.
     */
    public int getBasePanelCount() {
        return tabbedPane.getTabs().size();
    }

    /**
     * @deprecated do not operate on tabs but on BibDatabaseContexts
     */
    @Deprecated
    public TabPane getTabbedPane() {
        return tabbedPane;
    }

    /**
     * This method causes all open LibraryTabs to set up their tables anew. When called from PreferencesDialogViewModel,
     * this updates to the new settings. We need to notify all tabs about the changes to avoid problems when changing
     * the column set.
     */
    public void setupAllTables() {
        tabbedPane.getTabs().forEach(tab -> {
            if (tab instanceof LibraryTab libraryTab && (libraryTab.getDatabase() != null)) {
                DefaultTaskExecutor.runInJavaFXThread(libraryTab::setupMainPanel);
            }
        });
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

        libraryTab.setOnCloseRequest(event -> {
            libraryTab.cancelLoading();
            closeTab(libraryTab);
            event.consume();
        });

        libraryTab.setContextMenu(createTabContextMenuFor(libraryTab, Globals.getKeyPrefs()));

        libraryTab.getUndoManager().registerListener(new UndoRedoEventManager());
    }

    /**
     * Opens a new tab with existing data.
     * Asynchronous loading is done at  {@link org.jabref.gui.LibraryTab#createLibraryTab(BackgroundTask, Path, DialogService, PreferencesService, StateManager, JabRefFrame, FileUpdateMonitor, BibEntryTypesManager, CountingUndoManager)}.
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

    public FileHistoryMenu getFileHistory() {
        return fileHistory;
    }

    /**
     * Ask if the user really wants to close the given database.
     * Offers to save or discard the changes -- or return to the library
     *
     * @return <code>true</code> if the user choose to close the database
     */
    private boolean confirmClose(LibraryTab libraryTab) {
        String filename = libraryTab.getBibDatabaseContext()
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
                SaveDatabaseAction saveAction = new SaveDatabaseAction(libraryTab, dialogService, prefs, Globals.entryTypesManager);
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
            BackupManager.discardBackup(libraryTab.getBibDatabaseContext(), prefs.getFilePreferences().getBackupDirectory());
            return true;
        }

        return false;
    }

    public void closeTab(LibraryTab libraryTab) {
        // empty tab without database
        if (libraryTab == null) {
            libraryTab = getCurrentLibraryTab();
        }

        final BibDatabaseContext context = libraryTab.getBibDatabaseContext();

        if (libraryTab.isModified() && (context.getLocation() == DatabaseLocation.LOCAL)) {
            if (confirmClose(libraryTab)) {
                removeTab(libraryTab);
            } else {
                return;
            }
        } else if (context.getLocation() == DatabaseLocation.SHARED) {
            context.convertToLocalDatabase();
            context.getDBMSSynchronizer().closeSharedDatabase();
            context.clearDBMSSynchronizer();
            removeTab(libraryTab);
        } else {
            removeTab(libraryTab);
        }
        AutosaveManager.shutdown(context);
        BackupManager.shutdown(context, prefs.getFilePreferences().getBackupDirectory(), prefs.getFilePreferences().shouldCreateBackup());
    }

    private void removeTab(LibraryTab libraryTab) {
        DefaultTaskExecutor.runInJavaFXThread(() -> {
            libraryTab.cleanUp();
            tabbedPane.getTabs().remove(libraryTab);
        });
    }

    public void closeCurrentTab() {
        removeTab(getCurrentLibraryTab());
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

    public GlobalSearchBar getGlobalSearchBar() {
        return globalSearchBar;
    }

    public CountingUndoManager getUndoManager() {
        return undoManager;
    }

    public DialogService getDialogService() {
        return dialogService;
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
        if (!destinationLibraryTab.getBibDatabaseContext().getMetaData().getGroups().isEmpty()) {
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
            frame.quit();
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
            tabContainer.closeTab(libraryTab);
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
                    closeTab(libraryTab);
                }
            }
        }
    }

    private class CloseAllDatabaseAction extends SimpleCommand {

        @Override
        public void execute() {
            for (Tab tab : tabbedPane.getTabs()) {
                closeTab((LibraryTab) tab);
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
