package org.jabref.gui.frame;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.geometry.Orientation;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import org.jabref.gui.ClipBoardManager;
import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.LibraryTabContainer;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionFactory;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.desktop.os.NativeDesktop;
import org.jabref.gui.entryeditor.EntryEditor;
import org.jabref.gui.importer.NewEntryAction;
import org.jabref.gui.importer.actions.OpenDatabaseAction;
import org.jabref.gui.keyboard.KeyBinding;
import org.jabref.gui.libraryproperties.LibraryPropertiesAction;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.push.GuiPushToApplicationCommand;
import org.jabref.gui.search.GlobalSearchBar;
import org.jabref.gui.search.SearchType;
import org.jabref.gui.sidepane.SidePane;
import org.jabref.gui.sidepane.SidePaneType;
import org.jabref.gui.undo.CountingUndoManager;
import org.jabref.gui.undo.RedoAction;
import org.jabref.gui.undo.UndoAction;
import org.jabref.gui.util.BindingsHelper;
import org.jabref.gui.welcome.WelcomeTab;
import org.jabref.logic.UiCommand;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.git.util.GitHandlerRegistry;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.util.BuildInfo;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.util.DirectoryUpdateMonitor;
import org.jabref.model.util.FileUpdateMonitor;

import com.airhacks.afterburner.injection.Injector;
import com.tobiasdiez.easybind.EasyBind;
import com.tobiasdiez.easybind.EasyObservableList;
import com.tobiasdiez.easybind.Subscription;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.jabref.gui.actions.ActionHelper.needsSavedLocalDatabase;

/**
 * Represents the inner frame of the JabRef window
 */
public class JabRefFrame extends BorderPane implements LibraryTabContainer, UiMessageHandler {
    /**
     * Defines the different modes that the tab can operate in
     */
    private enum PanelMode { MAIN_TABLE, MAIN_TABLE_AND_ENTRY_EDITOR }

    public static final String FRAME_TITLE = "JabRef";

    private static final Logger LOGGER = LoggerFactory.getLogger(JabRefFrame.class);

    private final GuiPreferences preferences;
    private final AiService aiService;
    private final GlobalSearchBar globalSearchBar;

    private final FileHistoryMenu fileHistory;

    @SuppressWarnings({"FieldCanBeLocal"}) private EasyObservableList<BibDatabaseContext> openDatabaseList;

    private final Stage mainStage;
    private final StateManager stateManager;
    private final CountingUndoManager undoManager;
    private final DialogService dialogService;
    private final FileUpdateMonitor fileUpdateMonitor;
    private final DirectoryUpdateMonitor directoryUpdateMonitor;
    private final BibEntryTypesManager entryTypesManager;
    private final ClipBoardManager clipBoardManager;
    private final TaskExecutor taskExecutor;
    private final GitHandlerRegistry gitHandlerRegistry;

    private final JabRefFrameViewModel viewModel;
    private final GuiPushToApplicationCommand pushToApplicationCommand;
    private final SplitPane horizontalSplit = new SplitPane();
    private final SidePane sidePane;
    private final SplitPane verticalSplit = new SplitPane();
    private final TabPane tabbedPane = new TabPane();
    private final EntryEditor entryEditor;
    private final ObjectProperty<PanelMode> panelMode = new SimpleObjectProperty<>(PanelMode.MAIN_TABLE);

    // We need to keep a reference to the subscription, otherwise the binding gets garbage collected
    private Subscription horizontalDividerSubscription;
    private Subscription verticalDividerSubscription;

    public JabRefFrame(Stage mainStage,
                       DialogService dialogService,
                       FileUpdateMonitor fileUpdateMonitor,
                       DirectoryUpdateMonitor directoryUpdateMonitor,
                       GuiPreferences preferences,
                       AiService aiService,
                       StateManager stateManager,
                       CountingUndoManager undoManager,
                       BibEntryTypesManager entryTypesManager,
                       ClipBoardManager clipBoardManager,
                       TaskExecutor taskExecutor,
                       GitHandlerRegistry gitHandlerRegistry) {
        this.mainStage = mainStage;
        this.dialogService = dialogService;
        this.fileUpdateMonitor = fileUpdateMonitor;
        this.directoryUpdateMonitor = directoryUpdateMonitor;
        this.preferences = preferences;
        this.aiService = aiService;
        this.stateManager = stateManager;
        this.undoManager = undoManager;
        this.entryTypesManager = entryTypesManager;
        this.clipBoardManager = clipBoardManager;
        this.taskExecutor = taskExecutor;
        this.gitHandlerRegistry = gitHandlerRegistry;

        setId("frame");

        // Create components
        this.viewModel = new JabRefFrameViewModel(
                preferences,
                aiService,
                stateManager,
                dialogService,
                this,
                this::getOpenDatabaseAction,
                entryTypesManager,
                fileUpdateMonitor,
                directoryUpdateMonitor,
                undoManager,
                clipBoardManager,
                taskExecutor);
        Injector.setModelOrService(UiMessageHandler.class, viewModel);

        FrameDndHandler frameDndHandler = new FrameDndHandler(
                tabbedPane,
                mainStage::getScene,
                this::getOpenDatabaseAction,
                stateManager);

        this.globalSearchBar = new GlobalSearchBar(
                this,
                stateManager,
                this.preferences,
                undoManager,
                dialogService,
                SearchType.NORMAL_SEARCH);

        this.entryEditor = new EntryEditor(this::getCurrentLibraryTab,
                // Actions are recreated here since this avoids passing more parameters and the amount of additional memory consumption is neglegtable.
                new UndoAction(this::getCurrentLibraryTab, undoManager, dialogService, stateManager),
                new RedoAction(this::getCurrentLibraryTab, undoManager, dialogService, stateManager));
        Injector.setModelOrService(EntryEditor.class, entryEditor);

        this.sidePane = new SidePane(
                this,
                this.preferences,
                Injector.instantiateModelOrService(JournalAbbreviationRepository.class),
                taskExecutor,
                dialogService,
                aiService,
                stateManager,
                entryEditor,
                fileUpdateMonitor,
                directoryUpdateMonitor,
                entryTypesManager,
                clipBoardManager,
                undoManager);

        this.pushToApplicationCommand = new GuiPushToApplicationCommand(
                stateManager,
                dialogService,
                this.preferences,
                taskExecutor);

        this.fileHistory = new FileHistoryMenu(
                this.preferences.getLastFilesOpenedPreferences().getFileHistory(),
                dialogService,
                getOpenDatabaseAction());

        fileHistory.disableProperty().bind(Bindings.isEmpty(fileHistory.getItems()));

        this.setOnKeyTyped(key -> {
            if (this.fileHistory.isShowing()) {
                if (this.fileHistory.openFileByKey(key)) {
                    this.fileHistory.getParentMenu().hide();
                }
            }
        });

        initLayout();
        initKeyBindings();
        frameDndHandler.initDragAndDrop();
        initBindings();
    }

    private void initLayout() {
        MainToolBar mainToolBar = new MainToolBar(
                this,
                pushToApplicationCommand,
                globalSearchBar,
                dialogService,
                stateManager,
                preferences,
                aiService,
                fileUpdateMonitor,
                directoryUpdateMonitor,
                taskExecutor,
                entryTypesManager,
                clipBoardManager,
                undoManager);

        MainMenu mainMenu = new MainMenu(
                this,
                fileHistory,
                sidePane,
                pushToApplicationCommand,
                preferences,
                stateManager,
                fileUpdateMonitor,
                directoryUpdateMonitor,
                taskExecutor,
                dialogService,
                Injector.instantiateModelOrService(JournalAbbreviationRepository.class),
                entryTypesManager,
                undoManager,
                clipBoardManager,
                this::getOpenDatabaseAction,
                aiService,
                entryEditor,
                gitHandlerRegistry);

        VBox head = new VBox(mainMenu, mainToolBar);
        head.setSpacing(0d);
        setTop(head);

        verticalSplit.getItems().addAll(tabbedPane);
        verticalSplit.setOrientation(Orientation.VERTICAL);
        updateEditorPane();

        horizontalSplit.getItems().addAll(verticalSplit);
        horizontalSplit.setOrientation(Orientation.HORIZONTAL);

        SplitPane.setResizableWithParent(sidePane, false);
        sidePane.widthProperty().addListener(_ -> updateSidePane());
        sidePane.getChildren().addListener((InvalidationListener) _ -> updateSidePane());
        updateSidePane();
        setCenter(horizontalSplit);
    }

    private void updateSidePane() {
        if (sidePane.getChildren().isEmpty()) {
            if (horizontalDividerSubscription != null) {
                horizontalDividerSubscription.unsubscribe();
            }
            horizontalSplit.getItems().remove(sidePane);
        } else {
            if (!horizontalSplit.getItems().contains(sidePane)) {
                horizontalSplit.getItems().addFirst(sidePane);
                updateHorizontalDividerPosition();
            }
        }
    }

    private void updateEditorPane() {
        if (panelMode.get() == PanelMode.MAIN_TABLE) {
            if (verticalDividerSubscription != null) {
                verticalDividerSubscription.unsubscribe();
            }
            verticalSplit.getItems().remove(entryEditor);
        } else {
            if (!verticalSplit.getItems().contains(entryEditor)) {
                verticalSplit.getItems().addLast(entryEditor);
                updateVerticalDividerPosition();
            }
        }
    }

    public void updateHorizontalDividerPosition() {
        if (mainStage.isShowing() && !sidePane.getChildren().isEmpty()) {
            horizontalSplit.setDividerPositions(preferences.getGuiPreferences().getHorizontalDividerPosition() / horizontalSplit.getWidth());
            horizontalDividerSubscription = EasyBind.valueAt(horizontalSplit.getDividers(), 0)
                                                    .mapObservable(SplitPane.Divider::positionProperty)
                                                    .listenToValues((_, newValue) -> preferences.getGuiPreferences().setHorizontalDividerPosition(newValue.doubleValue()));
        }
    }

    public void updateVerticalDividerPosition() {
        if (mainStage.isShowing() && panelMode.get() == PanelMode.MAIN_TABLE_AND_ENTRY_EDITOR) {
            verticalSplit.setDividerPositions(preferences.getGuiPreferences().getVerticalDividerPosition());
            verticalDividerSubscription = EasyBind.valueAt(verticalSplit.getDividers(), 0)
                                                  .mapObservable(SplitPane.Divider::positionProperty)
                                                  .listenToValues((_, newValue) -> preferences.getGuiPreferences().setVerticalDividerPosition(newValue.doubleValue()));
        }
    }

    private void initKeyBindings() {
        addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            Optional<KeyBinding> keyBinding = preferences.getKeyBindingRepository().mapToKeyBinding(event);
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
                        globalSearchBar.requestFocus();
                        break;
                    case OPEN_GLOBAL_SEARCH_DIALOG:
                        globalSearchBar.openGlobalSearchDialog();
                        break;
                    case NEW_ARTICLE:
                        new NewEntryAction(StandardEntryType.Article, this::getCurrentLibraryTab, dialogService, preferences, stateManager).execute();
                        break;
                    case NEW_BOOK:
                        new NewEntryAction(StandardEntryType.Book, this::getCurrentLibraryTab, dialogService, preferences, stateManager).execute();
                        break;
                    case NEW_INBOOK:
                        new NewEntryAction(StandardEntryType.InBook, this::getCurrentLibraryTab, dialogService, preferences, stateManager).execute();
                        break;
                    case NEW_MASTERSTHESIS:
                        new NewEntryAction(StandardEntryType.MastersThesis, this::getCurrentLibraryTab, dialogService, preferences, stateManager).execute();
                        break;
                    case NEW_PHDTHESIS:
                        new NewEntryAction(StandardEntryType.PhdThesis, this::getCurrentLibraryTab, dialogService, preferences, stateManager).execute();
                        break;
                    case NEW_PROCEEDINGS:
                        new NewEntryAction(StandardEntryType.Proceedings, this::getCurrentLibraryTab, dialogService, preferences, stateManager).execute();
                        break;
                    case NEW_TECHREPORT:
                        new NewEntryAction(StandardEntryType.TechReport, this::getCurrentLibraryTab, dialogService, preferences, stateManager).execute();
                        break;
                    case NEW_UNPUBLISHED:
                        new NewEntryAction(StandardEntryType.Unpublished, this::getCurrentLibraryTab, dialogService, preferences, stateManager).execute();
                        break;
                    case NEW_INPROCEEDINGS:
                        new NewEntryAction(StandardEntryType.InProceedings, this::getCurrentLibraryTab, dialogService, preferences, stateManager).execute();
                        break;
                    case BACK:
                        Optional.ofNullable(getCurrentLibraryTab()).ifPresent(LibraryTab::back);
                        event.consume();
                        break;
                    case FORWARD:
                        Optional.ofNullable(getCurrentLibraryTab()).ifPresent(LibraryTab::forward);
                        event.consume();
                        break;
                    case CLOSE_DATABASE:
                        new CloseDatabaseAction(this, stateManager).execute();
                        event.consume();
                        break;
                    default:
                }
            }
        });
    }

    private void initBindings() {
        BindingsHelper.bindContentFiltered(tabbedPane.getTabs(), stateManager.getOpenDatabases(), LibraryTab.class::isInstance);

        // the binding for stateManager.activeDatabaseProperty() is at org.jabref.gui.LibraryTab.onDatabaseLoadingSucceed

        // Subscribe to the search
        EasyBind.subscribe(stateManager.activeSearchQuery(SearchType.NORMAL_SEARCH), query -> {
            if (getCurrentLibraryTab() != null) {
                getCurrentLibraryTab().searchQueryProperty().set(query);
            }
        });

        // Wait for the scene to be created, otherwise focusOwnerProperty is not provided
        Platform.runLater(() -> stateManager.focusOwnerProperty().bind(
                EasyBind.map(mainStage.getScene().focusOwnerProperty(), Optional::ofNullable)));

        EasyBind.subscribe(tabbedPane.getSelectionModel().selectedItemProperty(), selectedTab -> {
            if (selectedTab instanceof LibraryTab libraryTab) {
                stateManager.setActiveDatabase(libraryTab.getBibDatabaseContext());
                stateManager.activeTabProperty().set(Optional.of(libraryTab));
                stateManager.setSelectedEntries(libraryTab.getSelectedEntries());

                // Update active search query when switching between databases
                if (preferences.getSearchPreferences().shouldKeepSearchString()) {
                    libraryTab.searchQueryProperty().set(stateManager.activeSearchQuery(SearchType.NORMAL_SEARCH).get());
                } else {
                    stateManager.activeSearchQuery(SearchType.NORMAL_SEARCH).set(libraryTab.searchQueryProperty().get());
                }
                stateManager.searchResultSize(SearchType.NORMAL_SEARCH).bind(libraryTab.resultSizeProperty());
                globalSearchBar.setAutoCompleter(libraryTab.getAutoCompleter());

                // Listen for auto-completer changes after real context is loaded
                libraryTab.setAutoCompleterChangedListener(() -> globalSearchBar.setAutoCompleter(libraryTab.getAutoCompleter()));

                // [impl->req~maintable.focus~1]
                Platform.runLater(() -> libraryTab.getMainTable().requestFocus());

                // Set window title dynamically
                mainStage.titleProperty().bind(Bindings.createStringBinding(
                        () -> libraryTab.textProperty().getValue() + " – " + FRAME_TITLE, // not a minus, but codepoint 2013
                        libraryTab.textProperty()));
            } else {
                // Check if the previously active database was closed
                if (stateManager.getActiveDatabase().isPresent()) {
                    String activeUID = stateManager.getActiveDatabase().get().getUid();
                    boolean wasClosed = tabbedPane.getTabs().stream()
                                                  .filter(tab -> tab instanceof LibraryTab)
                                                  .noneMatch(ltab -> ((LibraryTab) ltab).getBibDatabaseContext().getUid().equals(activeUID));
                    if (wasClosed) {
                        tabbedPane.getSelectionModel().selectNext();
                    }
                }

                // All databases are closed or an unknown tab is selected
                stateManager.setActiveDatabase(null);
                stateManager.activeTabProperty().set(Optional.empty());
                stateManager.setSelectedEntries(List.of());
                mainStage.titleProperty().unbind();
                mainStage.setTitle(FRAME_TITLE);
            }
        });

        BindingsHelper.bindBidirectional((ObservableValue<Boolean>) stateManager.getEditorShowing(), panelMode,
                mode -> stateManager.getEditorShowing().setValue(mode == PanelMode.MAIN_TABLE_AND_ENTRY_EDITOR),
                showing -> panelMode.setValue(showing ? PanelMode.MAIN_TABLE_AND_ENTRY_EDITOR : PanelMode.MAIN_TABLE));
        EasyBind.subscribe(panelMode, mode -> {
            updateEditorPane();
            if (mode == PanelMode.MAIN_TABLE_AND_ENTRY_EDITOR) {
                entryEditor.requestFocus();
            }
        });

        // Hide tab bar
        stateManager.getOpenDatabases().addListener((ListChangeListener<BibDatabaseContext>) _ -> updateTabBarVisible());
        tabbedPane.getTabs().addListener((ListChangeListener<Tab>) _ -> updateTabBarVisible());

        stateManager.canGoBackProperty().bind(
                stateManager.activeTabProperty().flatMap(
                        optionalTab -> optionalTab
                                .map(LibraryTab::canGoBackProperty)
                                .orElse(new SimpleBooleanProperty(false))
                )
        );

        stateManager.canGoForwardProperty().bind(
                stateManager.activeTabProperty().flatMap(
                        optionalTab -> optionalTab
                                .map(LibraryTab::canGoForwardProperty)
                                .orElse(new SimpleBooleanProperty(false))
                )
        );
    }

    private void updateTabBarVisible() {
        // When WelcomeTab is open, the tabbar should be visible
        if (preferences.getWorkspacePreferences().shouldHideTabBar() && tabbedPane.getTabs().size() <= 1) {
            if (!tabbedPane.getStyleClass().contains("hide-tab-bar")) {
                tabbedPane.getStyleClass().add("hide-tab-bar");
            }
        } else {
            tabbedPane.getStyleClass().remove("hide-tab-bar");
        }
    }

    /* ************************************************************************
     *
     * Public API
     *
     **************************************************************************/

    /**
     * Returns a list of all LibraryTabs in this frame.
     */
    public @NonNull ObservableList<LibraryTab> getLibraryTabs() {
        return EasyBind.map(tabbedPane.getTabs().filtered(LibraryTab.class::isInstance), LibraryTab.class::cast);
    }

    /**
     * Returns the currently viewed LibraryTab.
     */
    public LibraryTab getCurrentLibraryTab() {
        if (tabbedPane.getSelectionModel().getSelectedItem() == null
                || !(tabbedPane.getSelectionModel().getSelectedItem() instanceof LibraryTab)) {
            return null;
        }
        return (LibraryTab) tabbedPane.getSelectionModel().getSelectedItem();
    }

    public void showLibraryTab(@NonNull LibraryTab libraryTab) {
        tabbedPane.getSelectionModel().select(libraryTab);
    }

    public void showWelcomeTab() {
        // The loop iterates through all tabs in tabbedPane to check if a WelcomeTab already exists. If yes, it is selected.
        for (Tab tab : tabbedPane.getTabs()) {
            if (!(tab instanceof LibraryTab)) {
                tabbedPane.getSelectionModel().select(tab);
                return;
            }
        }
        // WelcomeTab not found

        WelcomeTab welcomeTab = new WelcomeTab(
                Injector.instantiateModelOrService(Stage.class),
                this,
                preferences,
                aiService,
                dialogService,
                stateManager,
                fileUpdateMonitor,
                directoryUpdateMonitor,
                entryTypesManager,
                undoManager,
                clipBoardManager,
                taskExecutor,
                fileHistory,
                Injector.instantiateModelOrService(BuildInfo.class),
                preferences.getWorkspacePreferences()
        );
        tabbedPane.getTabs().add(welcomeTab);
        tabbedPane.getSelectionModel().select(welcomeTab);
    }

    /**
     * Opens a new tab with existing data.
     * Asynchronous loading is done at {@link LibraryTab#createLibraryTab}.
     * Similar method: {@link OpenDatabaseAction#openTheFile(Path)}
     */
    public void addTab(@NonNull BibDatabaseContext databaseContext, boolean raisePanel) {
        LibraryTab libraryTab = LibraryTab.createLibraryTab(
                databaseContext,
                this,
                dialogService,
                aiService,
                preferences,
                stateManager,
                fileUpdateMonitor,
                directoryUpdateMonitor,
                entryTypesManager,
                undoManager,
                clipBoardManager,
                taskExecutor);
        addTab(libraryTab, raisePanel);
    }

    public void addTab(@NonNull LibraryTab libraryTab, boolean raisePanel) {
        tabbedPane.getTabs().add(libraryTab);
        if (raisePanel) {
            tabbedPane.getSelectionModel().select(libraryTab);
            tabbedPane.requestFocus();
            libraryTab.getMainTable().requestFocus();
        }

        libraryTab.setContextMenu(createTabContextMenuFor(libraryTab));
    }

    private ContextMenu createTabContextMenuFor(LibraryTab tab) {
        ContextMenu contextMenu = new ContextMenu();
        ActionFactory factory = new ActionFactory();

        contextMenu.getItems().addAll(
                factory.createMenuItem(StandardActions.LIBRARY_PROPERTIES, new LibraryPropertiesAction(tab::getBibDatabaseContext, stateManager)),
                factory.createMenuItem(StandardActions.OPEN_DATABASE_FOLDER, new OpenDatabaseFolder(dialogService, stateManager, preferences, tab::getBibDatabaseContext)),
                factory.createMenuItem(StandardActions.OPEN_CONSOLE, new OpenConsoleAction(() -> {
                    LibraryTab currentTab = getCurrentLibraryTab();
                    return (currentTab == null) ? null : currentTab.getBibDatabaseContext();
                }, stateManager, preferences, dialogService)),
                new SeparatorMenuItem(),
                factory.createMenuItem(StandardActions.CLOSE_LIBRARY, new CloseDatabaseAction(this, tab, stateManager)),
                factory.createMenuItem(StandardActions.CLOSE_OTHER_LIBRARIES, new CloseOthersDatabaseAction(tab)),
                factory.createMenuItem(StandardActions.CLOSE_ALL_LIBRARIES, new CloseAllDatabaseAction()));

        return contextMenu;
    }

    public boolean close() {
        return viewModel.close();
    }

    public boolean closeTab(LibraryTab tab) {
        return closeTabs(List.of(tab));
    }

    public boolean closeTabs(@NonNull List<LibraryTab> tabs) {
        // Only accept library tabs that are shown in the tab container
        List<LibraryTab> toClose = tabs.stream()
                                       .distinct()
                                       .filter(getLibraryTabs()::contains)
                                       .toList();

        if (toClose.isEmpty()) {
            // Nothing to do
            return true;
        }

        // Ask before closing any tab, if any tab has changes
        for (LibraryTab libraryTab : toClose) {
            if (!libraryTab.requestClose()) {
                return false;
            }
        }

        // Close after checking for changes and saving all databases
        for (LibraryTab libraryTab : toClose) {
            tabbedPane.getTabs().remove(libraryTab);
            // Trigger org.jabref.gui.LibraryTab.onClosed
            Event.fireEvent(libraryTab, new Event(this, libraryTab, Tab.CLOSED_EVENT));
        }
        // Force group update in the GroupTreeViewModel when all the libraries are closed
        if (tabbedPane.getTabs().isEmpty()) {
            stateManager.setActiveDatabase(null);
        }
        return true;
    }

    private OpenDatabaseAction getOpenDatabaseAction() {
        return new OpenDatabaseAction(
                this,
                preferences,
                aiService,
                dialogService,
                stateManager,
                fileUpdateMonitor,
                directoryUpdateMonitor,
                entryTypesManager,
                undoManager,
                clipBoardManager,
                taskExecutor);
    }

    /**
     * Refreshes the ui after preferences changes
     */
    public void refresh() {
        // Disabled, because Bindings implement automatic update. Left here as commented out code to guide if something does not work after updating the preferences.
        // getLibraryTabs().forEach(LibraryTab::setupMainPanel);
        getLibraryTabs().forEach(tab -> tab.getMainTable().getTableModel().resetFieldFormatter());
    }

    public void openLastEditedDatabases() {
        List<Path> lastFiles = preferences.getLastFilesOpenedPreferences().getLastFilesOpened();
        if (lastFiles.isEmpty()) {
            return;
        }

        getOpenDatabaseAction().openFiles(lastFiles);
    }

    @Deprecated
    public Stage getMainStage() {
        return mainStage;
    }

    @Override
    public void handleUiCommands(List<UiCommand> uiCommands) {
        if (uiCommands.stream().anyMatch(UiCommand.Focus.class::isInstance)) {
            mainStage.toFront();
            return;
        }
        viewModel.handleUiCommands(uiCommands);
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
            if (frame.viewModel.close()) {
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
                    tabContainer.closeTab(tabContainer.getCurrentLibraryTab());
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
            this.executable.bind(ActionHelper.needsMultipleDatabases(stateManager));
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
            tabbedPane.getTabs().removeIf(t -> t instanceof WelcomeTab);
            for (Tab tab : tabbedPane.getTabs()) {
                Platform.runLater(() -> closeTab((LibraryTab) tab));
            }
        }
    }

    public static class OpenDatabaseFolder extends SimpleCommand {

        private final Supplier<BibDatabaseContext> databaseContext;
        private final DialogService dialogService;
        private final GuiPreferences preferences;

        public OpenDatabaseFolder(DialogService dialogService, StateManager stateManager, GuiPreferences preferences, Supplier<BibDatabaseContext> databaseContext) {
            this.dialogService = dialogService;
            this.preferences = preferences;
            this.databaseContext = databaseContext;
            this.executable.bind(needsSavedLocalDatabase(stateManager));
        }

        @Override
        public void execute() {
            Optional.of(databaseContext.get()).flatMap(BibDatabaseContext::getDatabasePath).ifPresent(path -> {
                try {
                    NativeDesktop.openFolderAndSelectFile(path, preferences.getExternalApplicationsPreferences(), dialogService);
                } catch (IOException e) {
                    LOGGER.info("Could not open folder", e);
                }
            });
        }
    }
}
