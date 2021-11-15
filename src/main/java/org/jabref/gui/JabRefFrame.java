package org.jabref.gui;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TimerTask;
import java.util.stream.Collectors;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.geometry.Orientation;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Separator;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.control.skin.TabPaneSkin;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import org.jabref.gui.actions.ActionFactory;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.auximport.NewSubLibraryAction;
import org.jabref.gui.bibtexextractor.ExtractBibtexAction;
import org.jabref.gui.citationkeypattern.CitationKeyPatternAction;
import org.jabref.gui.citationkeypattern.GenerateCitationKeyAction;
import org.jabref.gui.cleanup.CleanupAction;
import org.jabref.gui.contentselector.ManageContentSelectorAction;
import org.jabref.gui.copyfiles.CopyFilesAction;
import org.jabref.gui.customentrytypes.CustomizeEntryAction;
import org.jabref.gui.desktop.JabRefDesktop;
import org.jabref.gui.dialogs.AutosaveUiManager;
import org.jabref.gui.documentviewer.ShowDocumentViewerAction;
import org.jabref.gui.duplicationFinder.DuplicateSearch;
import org.jabref.gui.edit.CopyMoreAction;
import org.jabref.gui.edit.EditAction;
import org.jabref.gui.edit.ManageKeywordsAction;
import org.jabref.gui.edit.MassSetFieldsAction;
import org.jabref.gui.edit.OpenBrowserAction;
import org.jabref.gui.edit.ReplaceStringAction;
import org.jabref.gui.entryeditor.OpenEntryEditorAction;
import org.jabref.gui.entryeditor.PreviewSwitchAction;
import org.jabref.gui.exporter.ExportCommand;
import org.jabref.gui.exporter.ExportToClipboardAction;
import org.jabref.gui.exporter.SaveAction;
import org.jabref.gui.exporter.SaveAllAction;
import org.jabref.gui.exporter.SaveDatabaseAction;
import org.jabref.gui.exporter.WriteMetadataToPdfAction;
import org.jabref.gui.externalfiles.AutoLinkFilesAction;
import org.jabref.gui.externalfiles.DownloadFullTextAction;
import org.jabref.gui.externalfiles.FindUnlinkedFilesAction;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.help.AboutAction;
import org.jabref.gui.help.ErrorConsoleAction;
import org.jabref.gui.help.HelpAction;
import org.jabref.gui.help.SearchForUpdateAction;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.importer.GenerateEntryFromIdDialog;
import org.jabref.gui.importer.ImportCommand;
import org.jabref.gui.importer.ImportEntriesDialog;
import org.jabref.gui.importer.NewDatabaseAction;
import org.jabref.gui.importer.NewEntryAction;
import org.jabref.gui.importer.actions.OpenDatabaseAction;
import org.jabref.gui.importer.fetcher.LookupIdentifierAction;
import org.jabref.gui.integrity.IntegrityCheckAction;
import org.jabref.gui.journals.AbbreviateAction;
import org.jabref.gui.keyboard.KeyBinding;
import org.jabref.gui.keyboard.KeyBindingRepository;
import org.jabref.gui.libraryproperties.LibraryPropertiesAction;
import org.jabref.gui.menus.FileHistoryMenu;
import org.jabref.gui.mergeentries.MergeEntriesAction;
import org.jabref.gui.metadata.BibtexStringEditorAction;
import org.jabref.gui.metadata.PreambleEditor;
import org.jabref.gui.preferences.ShowPreferencesAction;
import org.jabref.gui.preview.CopyCitationAction;
import org.jabref.gui.push.PushToApplicationAction;
import org.jabref.gui.push.PushToApplicationsManager;
import org.jabref.gui.search.GlobalSearchBar;
import org.jabref.gui.search.RebuildFulltextSearchIndexAction;
import org.jabref.gui.shared.ConnectToSharedDatabaseCommand;
import org.jabref.gui.shared.PullChangesFromSharedAction;
import org.jabref.gui.sidepane.SidePane;
import org.jabref.gui.sidepane.SidePaneComponent;
import org.jabref.gui.sidepane.SidePaneType;
import org.jabref.gui.slr.ExistingStudySearchAction;
import org.jabref.gui.slr.StartNewStudyAction;
import org.jabref.gui.specialfields.SpecialFieldMenuItemFactory;
import org.jabref.gui.texparser.ParseLatexAction;
import org.jabref.gui.undo.CountingUndoManager;
import org.jabref.gui.undo.UndoRedoAction;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.DefaultTaskExecutor;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.autosaveandbackup.AutosaveManager;
import org.jabref.logic.autosaveandbackup.BackupManager;
import org.jabref.logic.citationstyle.CitationStyleOutputFormat;
import org.jabref.logic.importer.IdFetcher;
import org.jabref.logic.importer.ImportCleanup;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.WebFetchers;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.shared.DatabaseLocation;
import org.jabref.logic.undo.AddUndoableActionEvent;
import org.jabref.logic.undo.UndoChangeEvent;
import org.jabref.logic.undo.UndoRedoEvent;
import org.jabref.logic.util.OS;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.SpecialField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.preferences.PreferencesService;
import org.jabref.preferences.TelemetryPreferences;

import com.google.common.eventbus.Subscribe;
import com.tobiasdiez.easybind.EasyBind;
import com.tobiasdiez.easybind.EasyObservableList;
import org.controlsfx.control.PopOver;
import org.controlsfx.control.TaskProgressView;
import org.fxmisc.richtext.CodeArea;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The main window of the application.
 */
public class JabRefFrame extends BorderPane {

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
    private final PushToApplicationsManager pushToApplicationsManager;
    private final DialogService dialogService;
    private SidePane sidePane;
    private TabPane tabbedPane;
    private PopOver progressViewPopOver;
    private PopOver entryFromIdPopOver;

    private final TaskExecutor taskExecutor;

    public JabRefFrame(Stage mainStage) {
        this.mainStage = mainStage;
        this.dialogService = new JabRefDialogService(mainStage, this, prefs);
        this.stateManager = Globals.stateManager;
        this.pushToApplicationsManager = new PushToApplicationsManager(dialogService, stateManager, prefs);
        this.undoManager = Globals.undoManager;
        this.globalSearchBar = new GlobalSearchBar(this, stateManager, prefs, undoManager);
        this.fileHistory = new FileHistoryMenu(prefs, dialogService, getOpenDatabaseAction());
        this.taskExecutor = Globals.TASK_EXECUTOR;
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
            });

            this.getScene().setOnDragExited(event -> tabbedPane.getTabs().remove(dndIndicator));

            this.getScene().setOnDragDropped(event -> {
                tabbedPane.getTabs().remove(dndIndicator);
                List<Path> bibFiles = DragAndDropHelper.getBibFiles(event.getDragboard());
                OpenDatabaseAction openDatabaseAction = this.getOpenDatabaseAction();
                openDatabaseAction.openFiles(bibFiles, true);
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
                            }
                            break;
                        }
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
                    Localization.lang("To improve the user experience, we would like to collect anonymous statistics on the features you use. We will only record what features you access and how often you do it. We will neither collect any personal data nor the content of bibliographic items. If you choose to allow data collection, you can later disable it via Options -> Preferences -> General."),
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
        // all the logic is done in openIt. Even raising an existing panel
        getOpenDatabaseAction().openFile(file, true);
    }

    /**
     * The MacAdapter calls this method when "About" is selected from the application menu.
     */
    public void about() {
        HelpAction.getMainHelpPageCommand().execute();
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
        if (prefs.getGuiPreferences().shouldOpenLastEdited()) {
            // Here we store the names of all current files. If there is no current file, we remove any
            // previously stored filename.
            if (filenames.isEmpty()) {
                prefs.clearEditedFiles();
            } else {
                Path focusedDatabase = getCurrentLibraryTab().getBibDatabaseContext()
                                                             .getDatabasePath()
                                                             .orElse(null);
                prefs.getGuiPreferences().setLastFilesOpened(filenames);
                prefs.getGuiPreferences().setLastFocusedFile(focusedDatabase);
            }
        }

        fileHistory.storeHistory();
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
        if (stateManager.getAnyTaskRunning().getValue()) {
            Optional<ButtonType> shouldClose = dialogService.showBackgroundProgressDialogAndWait(
                    Localization.lang("Please wait..."),
                    Localization.lang("Waiting for background tasks to finish. Quit anyway?"),
                    stateManager
            );
            if (!(shouldClose.isPresent() && (shouldClose.get() == ButtonType.YES))) {
                return false;
            }
        }

        // Then ask if the user really wants to close, if the library has not been saved since last save.
        List<String> filenames = new ArrayList<>();
        for (int i = 0; i < tabbedPane.getTabs().size(); i++) {
            LibraryTab libraryTab = getLibraryTabAt(i);
            final BibDatabaseContext context = libraryTab.getBibDatabaseContext();

            if (context.hasEmptyEntries()) {
                if (!confirmEmptyEntry(libraryTab, context)) {
                    return false;
                }
            }

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
            BackupManager.shutdown(context);
            context.getDatabasePath().map(Path::toAbsolutePath).map(Path::toString).ifPresent(filenames::add);
        }

        WaitForSaveFinishedDialog waitForSaveFinishedDialog = new WaitForSaveFinishedDialog(dialogService);
        waitForSaveFinishedDialog.showAndWait(getLibraryTabs());

        // Good bye!
        tearDownJabRef(filenames);
        Platform.exit();
        return true;
    }

    private void initLayout() {
        setId("frame");

        VBox head = new VBox(createMenu(), createToolbar());
        head.setSpacing(0d);
        setTop(head);

        splitPane.getItems().addAll(sidePane, tabbedPane);
        SplitPane.setResizableWithParent(sidePane, false);

        // We need to wait with setting the divider since it gets reset a few times during the initial set-up
        mainStage.showingProperty().addListener(new ChangeListener<>() {

            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean showing) {
                if (showing) {
                    setDividerPosition();

                    EasyBind.subscribe(sidePane.visibleProperty(), visible -> {
                        if (visible) {
                            if (!splitPane.getItems().contains(sidePane)) {
                                splitPane.getItems().add(0, sidePane);
                                setDividerPosition();
                            }
                        } else {
                            splitPane.getItems().remove(sidePane);
                        }
                    });

                    mainStage.showingProperty().removeListener(this);
                    observable.removeListener(this);
                }
            }
        });

        setCenter(splitPane);
    }

    private void setDividerPosition() {
        splitPane.setDividerPositions(prefs.getGuiPreferences().getSidePaneWidth());
        if (!splitPane.getDividers().isEmpty()) {
            EasyBind.subscribe(splitPane.getDividers().get(0).positionProperty(),
                    position -> prefs.getGuiPreferences().setSidePaneWidth(position.doubleValue()));
        }
    }

    private Node createToolbar() {
        final ActionFactory factory = new ActionFactory(Globals.getKeyPrefs());

        final Region leftSpacer = new Region();
        final Region rightSpacer = new Region();

        final PushToApplicationAction pushToApplicationAction = getPushToApplicationsManager().getPushToApplicationAction();
        final Button pushToApplicationButton = factory.createIconButton(pushToApplicationAction.getActionInformation(), pushToApplicationAction);
        pushToApplicationsManager.registerReconfigurable(pushToApplicationButton);

        // Setup Toolbar

        ToolBar toolBar = new ToolBar(

                new HBox(
                        factory.createIconButton(StandardActions.NEW_LIBRARY, new NewDatabaseAction(this, prefs)),
                        factory.createIconButton(StandardActions.OPEN_LIBRARY, new OpenDatabaseAction(this, prefs, dialogService, stateManager)),
                        factory.createIconButton(StandardActions.SAVE_LIBRARY, new SaveAction(SaveAction.SaveMethod.SAVE, this, prefs, stateManager))),

                leftSpacer,

                globalSearchBar,

                rightSpacer,

                new HBox(
                        factory.createIconButton(StandardActions.NEW_ARTICLE, new NewEntryAction(this, StandardEntryType.Article, dialogService, prefs, stateManager)),
                        factory.createIconButton(StandardActions.NEW_ENTRY, new NewEntryAction(this, dialogService, prefs, stateManager)),
                        createNewEntryFromIdButton(),
                        factory.createIconButton(StandardActions.NEW_ENTRY_FROM_PLAIN_TEXT, new ExtractBibtexAction(dialogService, prefs, stateManager)),
                        factory.createIconButton(StandardActions.DELETE_ENTRY, new EditAction(StandardActions.DELETE_ENTRY, this, stateManager))
                ),

                new Separator(Orientation.VERTICAL),

                new HBox(
                        factory.createIconButton(StandardActions.UNDO, new UndoRedoAction(StandardActions.UNDO, this, dialogService, stateManager)),
                        factory.createIconButton(StandardActions.REDO, new UndoRedoAction(StandardActions.REDO, this, dialogService, stateManager)),
                        factory.createIconButton(StandardActions.CUT, new EditAction(StandardActions.CUT, this, stateManager)),
                        factory.createIconButton(StandardActions.COPY, new EditAction(StandardActions.COPY, this, stateManager)),
                        factory.createIconButton(StandardActions.PASTE, new EditAction(StandardActions.PASTE, this, stateManager))
                ),

                new Separator(Orientation.VERTICAL),

                new HBox(
                        pushToApplicationButton,
                        factory.createIconButton(StandardActions.GENERATE_CITE_KEYS, new GenerateCitationKeyAction(this, dialogService, stateManager, taskExecutor, prefs)),
                        factory.createIconButton(StandardActions.CLEANUP_ENTRIES, new CleanupAction(this, prefs, dialogService, stateManager))
                ),

                new Separator(Orientation.VERTICAL),

                new HBox(
                        factory.createIconButton(StandardActions.OPEN_GITHUB, new OpenBrowserAction("https://github.com/JabRef/jabref")),
                        factory.createIconButton(StandardActions.OPEN_FACEBOOK, new OpenBrowserAction("https://www.facebook.com/JabRef/")),
                        factory.createIconButton(StandardActions.OPEN_TWITTER, new OpenBrowserAction("https://twitter.com/jabref_org"))
                ),

                new Separator(Orientation.VERTICAL),

                new HBox(
                        createTaskIndicator()
                )
        );

        leftSpacer.setPrefWidth(50);
        leftSpacer.setMinWidth(Region.USE_PREF_SIZE);
        leftSpacer.setMaxWidth(Region.USE_PREF_SIZE);
        HBox.setHgrow(globalSearchBar, Priority.ALWAYS);
        HBox.setHgrow(rightSpacer, Priority.SOMETIMES);

        toolBar.getStyleClass().add("mainToolbar");

        return toolBar;
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
                         .map(tab -> (LibraryTab) tab)
                         .collect(Collectors.toList());
    }

    public void showLibraryTabAt(int i) {
        tabbedPane.getSelectionModel().select(i);
    }

    public void showLibraryTab(LibraryTab libraryTab) {
        tabbedPane.getSelectionModel().select(libraryTab);
    }

    public void init() {
        sidePane = new SidePane(prefs, taskExecutor, dialogService, stateManager, undoManager);

        tabbedPane = new TabPane();
        tabbedPane.setTabDragPolicy(TabPane.TabDragPolicy.REORDER);

        initLayout();
        initKeyBindings();
        initDragAndDrop();

        // Bind global state
        FilteredList<Tab> filteredTabs = new FilteredList<>(tabbedPane.getTabs());
        filteredTabs.setPredicate(tab -> tab instanceof LibraryTab);

        // This variable cannot be inlined, since otherwise the list created by EasyBind is being garbage collected
        openDatabaseList = EasyBind.map(filteredTabs, tab -> ((LibraryTab) tab).getBibDatabaseContext());
        EasyBind.bindContent(stateManager.getOpenDatabases(), openDatabaseList);

        stateManager.activeDatabaseProperty().bind(
                EasyBind.map(tabbedPane.getSelectionModel().selectedItemProperty(),
                        selectedTab -> Optional.ofNullable(selectedTab)
                                               .filter(tab -> tab instanceof LibraryTab)
                                               .map(tab -> (LibraryTab) tab)
                                               .map(LibraryTab::getBibDatabaseContext)));

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
        /*
         * The following state listener makes sure focus is registered with the
         * correct database when the user switches tabs. Without this,
         * cut/paste/copy operations would some times occur in the wrong tab.
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
            stateManager.activeSearchQueryProperty().set(libraryTab.getCurrentSearchQuery());

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

    private MenuBar createMenu() {
        ActionFactory factory = new ActionFactory(Globals.getKeyPrefs());
        Menu file = new Menu(Localization.lang("File"));
        Menu edit = new Menu(Localization.lang("Edit"));
        Menu library = new Menu(Localization.lang("Library"));
        Menu quality = new Menu(Localization.lang("Quality"));
        Menu lookup = new Menu(Localization.lang("Lookup"));
        Menu view = new Menu(Localization.lang("View"));
        Menu tools = new Menu(Localization.lang("Tools"));
        Menu options = new Menu(Localization.lang("Options"));
        Menu help = new Menu(Localization.lang("Help"));

        file.getItems().addAll(
                factory.createMenuItem(StandardActions.NEW_LIBRARY, new NewDatabaseAction(this, prefs)),
                factory.createMenuItem(StandardActions.OPEN_LIBRARY, getOpenDatabaseAction()),
                fileHistory,
                factory.createMenuItem(StandardActions.SAVE_LIBRARY, new SaveAction(SaveAction.SaveMethod.SAVE, this, prefs, stateManager)),
                factory.createMenuItem(StandardActions.SAVE_LIBRARY_AS, new SaveAction(SaveAction.SaveMethod.SAVE_AS, this, prefs, stateManager)),
                factory.createMenuItem(StandardActions.SAVE_ALL, new SaveAllAction(this, prefs)),

                new SeparatorMenuItem(),

                factory.createSubMenu(StandardActions.IMPORT,
                        factory.createMenuItem(StandardActions.IMPORT_INTO_CURRENT_LIBRARY, new ImportCommand(this, false, prefs, stateManager)),
                        factory.createMenuItem(StandardActions.IMPORT_INTO_NEW_LIBRARY, new ImportCommand(this, true, prefs, stateManager))),

                factory.createSubMenu(StandardActions.EXPORT,
                        factory.createMenuItem(StandardActions.EXPORT_ALL, new ExportCommand(this, false, prefs)),
                        factory.createMenuItem(StandardActions.EXPORT_SELECTED, new ExportCommand(this, true, prefs)),
                        factory.createMenuItem(StandardActions.SAVE_SELECTED_AS_PLAIN_BIBTEX, new SaveAction(SaveAction.SaveMethod.SAVE_SELECTED, this, prefs, stateManager))),

                new SeparatorMenuItem(),

                factory.createSubMenu(StandardActions.REMOTE_DB,
                        factory.createMenuItem(StandardActions.CONNECT_TO_SHARED_DB, new ConnectToSharedDatabaseCommand(this)),
                        factory.createMenuItem(StandardActions.PULL_CHANGES_FROM_SHARED_DB, new PullChangesFromSharedAction(stateManager))
                ),

                new SeparatorMenuItem(),

                factory.createMenuItem(StandardActions.CLOSE_LIBRARY, new CloseDatabaseAction()),
                factory.createMenuItem(StandardActions.QUIT, new CloseAction())
        );

        edit.getItems().addAll(
                factory.createMenuItem(StandardActions.UNDO, new UndoRedoAction(StandardActions.UNDO, this, dialogService, stateManager)),
                factory.createMenuItem(StandardActions.REDO, new UndoRedoAction(StandardActions.REDO, this, dialogService, stateManager)),

                new SeparatorMenuItem(),

                factory.createMenuItem(StandardActions.CUT, new EditAction(StandardActions.CUT, this, stateManager)),

                factory.createMenuItem(StandardActions.COPY, new EditAction(StandardActions.COPY, this, stateManager)),
                factory.createSubMenu(StandardActions.COPY_MORE,
                        factory.createMenuItem(StandardActions.COPY_TITLE, new CopyMoreAction(StandardActions.COPY_TITLE, dialogService, stateManager, Globals.getClipboardManager(), prefs)),
                        factory.createMenuItem(StandardActions.COPY_KEY, new CopyMoreAction(StandardActions.COPY_KEY, dialogService, stateManager, Globals.getClipboardManager(), prefs)),
                        factory.createMenuItem(StandardActions.COPY_CITE_KEY, new CopyMoreAction(StandardActions.COPY_CITE_KEY, dialogService, stateManager, Globals.getClipboardManager(), prefs)),
                        factory.createMenuItem(StandardActions.COPY_KEY_AND_TITLE, new CopyMoreAction(StandardActions.COPY_KEY_AND_TITLE, dialogService, stateManager, Globals.getClipboardManager(), prefs)),
                        factory.createMenuItem(StandardActions.COPY_KEY_AND_LINK, new CopyMoreAction(StandardActions.COPY_KEY_AND_LINK, dialogService, stateManager, Globals.getClipboardManager(), prefs)),
                        factory.createMenuItem(StandardActions.COPY_CITATION_PREVIEW, new CopyCitationAction(CitationStyleOutputFormat.HTML, dialogService, stateManager, Globals.getClipboardManager(), prefs.getPreviewPreferences())),
                        factory.createMenuItem(StandardActions.EXPORT_SELECTED_TO_CLIPBOARD, new ExportToClipboardAction(this, dialogService, Globals.exportFactory, Globals.getClipboardManager(), Globals.TASK_EXECUTOR, prefs))),

                factory.createMenuItem(StandardActions.PASTE, new EditAction(StandardActions.PASTE, this, stateManager)),

                new SeparatorMenuItem(),

                factory.createMenuItem(StandardActions.REPLACE_ALL, new ReplaceStringAction(this, stateManager)),
                factory.createMenuItem(StandardActions.GENERATE_CITE_KEYS, new GenerateCitationKeyAction(this, dialogService, stateManager, taskExecutor, prefs)),

                new SeparatorMenuItem(),

                factory.createMenuItem(StandardActions.MANAGE_KEYWORDS, new ManageKeywordsAction(stateManager)),
                factory.createMenuItem(StandardActions.MASS_SET_FIELDS, new MassSetFieldsAction(stateManager, dialogService, undoManager))
        );

        if (prefs.getSpecialFieldsPreferences().isSpecialFieldsEnabled()) {
            edit.getItems().addAll(
                    new SeparatorMenuItem(),
                    // ToDo: SpecialField needs the active BasePanel to mark it as changed.
                    //  Refactor BasePanel, should mark the BibDatabaseContext or the UndoManager as dirty instead!
                    SpecialFieldMenuItemFactory.createSpecialFieldMenu(SpecialField.RANKING, factory, this, dialogService, prefs, undoManager, stateManager),
                    SpecialFieldMenuItemFactory.getSpecialFieldSingleItem(SpecialField.RELEVANCE, factory, this, dialogService, prefs, undoManager, stateManager),
                    SpecialFieldMenuItemFactory.getSpecialFieldSingleItem(SpecialField.QUALITY, factory, this, dialogService, prefs, undoManager, stateManager),
                    SpecialFieldMenuItemFactory.getSpecialFieldSingleItem(SpecialField.PRINTED, factory, this, dialogService, prefs, undoManager, stateManager),
                    SpecialFieldMenuItemFactory.createSpecialFieldMenu(SpecialField.PRIORITY, factory, this, dialogService, prefs, undoManager, stateManager),
                    SpecialFieldMenuItemFactory.createSpecialFieldMenu(SpecialField.READ_STATUS, factory, this, dialogService, prefs, undoManager, stateManager)
            );
        }

        // @formatter:off
        library.getItems().addAll(
                factory.createMenuItem(StandardActions.NEW_ENTRY, new NewEntryAction(this, dialogService, prefs, stateManager)),
                factory.createMenuItem(StandardActions.NEW_ENTRY_FROM_PLAIN_TEXT, new ExtractBibtexAction(dialogService, prefs, stateManager)),
                factory.createMenuItem(StandardActions.DELETE_ENTRY, new EditAction(StandardActions.DELETE_ENTRY, this, stateManager)),

                new SeparatorMenuItem(),

                factory.createMenuItem(StandardActions.LIBRARY_PROPERTIES, new LibraryPropertiesAction(this, stateManager)),
                factory.createMenuItem(StandardActions.EDIT_PREAMBLE, new PreambleEditor(stateManager, undoManager, this.getDialogService())),
                factory.createMenuItem(StandardActions.EDIT_STRINGS, new BibtexStringEditorAction(stateManager)),
                factory.createMenuItem(StandardActions.MANAGE_CITE_KEY_PATTERNS, new CitationKeyPatternAction(this, stateManager))
        );

        quality.getItems().addAll(
                factory.createMenuItem(StandardActions.FIND_DUPLICATES, new DuplicateSearch(this, dialogService, stateManager)),
                factory.createMenuItem(StandardActions.MERGE_ENTRIES, new MergeEntriesAction(this, dialogService, stateManager)),
                factory.createMenuItem(StandardActions.CHECK_INTEGRITY, new IntegrityCheckAction(this, stateManager, Globals.TASK_EXECUTOR)),
                factory.createMenuItem(StandardActions.CLEANUP_ENTRIES, new CleanupAction(this, this.prefs, dialogService, stateManager)),

                new SeparatorMenuItem(),

                factory.createMenuItem(StandardActions.SET_FILE_LINKS, new AutoLinkFilesAction(dialogService, prefs, stateManager, undoManager, Globals.TASK_EXECUTOR)),

                new SeparatorMenuItem(),

                factory.createSubMenu(StandardActions.ABBREVIATE,
                        factory.createMenuItem(StandardActions.ABBREVIATE_DEFAULT, new AbbreviateAction(StandardActions.ABBREVIATE_DEFAULT, this, dialogService, stateManager)),
                        factory.createMenuItem(StandardActions.ABBREVIATE_MEDLINE, new AbbreviateAction(StandardActions.ABBREVIATE_MEDLINE, this, dialogService, stateManager)),
                        factory.createMenuItem(StandardActions.ABBREVIATE_SHORTEST_UNIQUE, new AbbreviateAction(StandardActions.ABBREVIATE_SHORTEST_UNIQUE, this, dialogService, stateManager))),

                factory.createMenuItem(StandardActions.UNABBREVIATE, new AbbreviateAction(StandardActions.UNABBREVIATE, this, dialogService, stateManager))
        );

        Menu lookupIdentifiers = factory.createSubMenu(StandardActions.LOOKUP_DOC_IDENTIFIER);
        for (IdFetcher<?> fetcher : WebFetchers.getIdFetchers(prefs.getImportFormatPreferences())) {
            LookupIdentifierAction<?> identifierAction = new LookupIdentifierAction<>(this, fetcher, stateManager, undoManager);
            lookupIdentifiers.getItems().add(factory.createMenuItem(identifierAction.getAction(), identifierAction));
        }

        lookup.getItems().addAll(
                lookupIdentifiers,
                factory.createMenuItem(StandardActions.DOWNLOAD_FULL_TEXT, new DownloadFullTextAction(dialogService, stateManager, prefs)),

                new SeparatorMenuItem(),

                factory.createMenuItem(StandardActions.FIND_UNLINKED_FILES, new FindUnlinkedFilesAction(dialogService, stateManager))
        );

        // PushToApplication
        final PushToApplicationAction pushToApplicationAction = pushToApplicationsManager.getPushToApplicationAction();
        final MenuItem pushToApplicationMenuItem = factory.createMenuItem(pushToApplicationAction.getActionInformation(), pushToApplicationAction);
        pushToApplicationsManager.registerReconfigurable(pushToApplicationMenuItem);

        tools.getItems().addAll(
                factory.createMenuItem(StandardActions.PARSE_LATEX, new ParseLatexAction(stateManager)),
                factory.createMenuItem(StandardActions.NEW_SUB_LIBRARY_FROM_AUX, new NewSubLibraryAction(this, stateManager)),

                new SeparatorMenuItem(),

                factory.createMenuItem(StandardActions.WRITE_METADATA_TO_PDF, new WriteMetadataToPdfAction(stateManager, prefs.getGeneralPreferences().getDefaultBibDatabaseMode(), Globals.entryTypesManager, prefs.getFieldWriterPreferences(), dialogService, taskExecutor, prefs.getFilePreferences(), prefs.getXmpPreferences(), prefs.getGeneralPreferences().getDefaultEncoding())),
                factory.createMenuItem(StandardActions.COPY_LINKED_FILES, new CopyFilesAction(dialogService, prefs, stateManager)),

                new SeparatorMenuItem(),

                factory.createMenuItem(StandardActions.SEND_AS_EMAIL, new SendAsEMailAction(dialogService, this.prefs, stateManager)),
                pushToApplicationMenuItem,
                new SeparatorMenuItem(),
                factory.createMenuItem(StandardActions.START_NEW_STUDY, new StartNewStudyAction(this, Globals.getFileUpdateMonitor(), Globals.TASK_EXECUTOR, prefs, stateManager)),
                factory.createMenuItem(StandardActions.SEARCH_FOR_EXISTING_STUDY, new ExistingStudySearchAction(this, Globals.getFileUpdateMonitor(), Globals.TASK_EXECUTOR, prefs, stateManager)),

                new SeparatorMenuItem(),

                factory.createMenuItem(StandardActions.REBUILD_FULLTEXT_SEARCH_INDEX, new RebuildFulltextSearchIndexAction(stateManager, this::getCurrentLibraryTab, dialogService, prefs.getFilePreferences()))
        );

        SidePaneComponent webSearch = sidePane.getComponent(SidePaneType.WEB_SEARCH);
        SidePaneComponent groups = sidePane.getComponent(SidePaneType.GROUPS);
        SidePaneComponent openOffice = sidePane.getComponent(SidePaneType.OPEN_OFFICE);

        view.getItems().addAll(
                factory.createCheckMenuItem(webSearch.getToggleAction(), webSearch.getToggleCommand(), sidePane.isComponentVisible(SidePaneType.WEB_SEARCH)),
                factory.createCheckMenuItem(groups.getToggleAction(), groups.getToggleCommand(), sidePane.isComponentVisible(SidePaneType.GROUPS)),
                factory.createCheckMenuItem(openOffice.getToggleAction(), openOffice.getToggleCommand(), sidePane.isComponentVisible(SidePaneType.OPEN_OFFICE)),

                new SeparatorMenuItem(),

                factory.createMenuItem(StandardActions.NEXT_PREVIEW_STYLE, new PreviewSwitchAction(PreviewSwitchAction.Direction.NEXT, this, stateManager)),
                factory.createMenuItem(StandardActions.PREVIOUS_PREVIEW_STYLE, new PreviewSwitchAction(PreviewSwitchAction.Direction.PREVIOUS, this, stateManager)),

                new SeparatorMenuItem(),

                factory.createMenuItem(StandardActions.SHOW_PDF_VIEWER, new ShowDocumentViewerAction(stateManager, prefs)),
                factory.createMenuItem(StandardActions.EDIT_ENTRY, new OpenEntryEditorAction(this, stateManager)),
                factory.createMenuItem(StandardActions.OPEN_CONSOLE, new OpenConsoleAction(stateManager, prefs))
        );

        options.getItems().addAll(
                factory.createMenuItem(StandardActions.SHOW_PREFS, new ShowPreferencesAction(this, Globals.TASK_EXECUTOR)),

                new SeparatorMenuItem(),

                factory.createMenuItem(StandardActions.MANAGE_CONTENT_SELECTORS, new ManageContentSelectorAction(this, stateManager)),
                factory.createMenuItem(StandardActions.CUSTOMIZE_ENTRY_TYPES, new CustomizeEntryAction(stateManager, Globals.entryTypesManager))
        );

        help.getItems().addAll(
                factory.createMenuItem(StandardActions.HELP, HelpAction.getMainHelpPageCommand()),
                factory.createMenuItem(StandardActions.OPEN_FORUM, new OpenBrowserAction("http://discourse.jabref.org/")),

                new SeparatorMenuItem(),

                factory.createMenuItem(StandardActions.ERROR_CONSOLE, new ErrorConsoleAction()),

                new SeparatorMenuItem(),

                factory.createMenuItem(StandardActions.DONATE, new OpenBrowserAction("https://donations.jabref.org")),
                factory.createMenuItem(StandardActions.SEARCH_FOR_UPDATES, new SearchForUpdateAction(Globals.BUILD_INFO, prefs.getVersionPreferences(), dialogService, Globals.TASK_EXECUTOR)),
                factory.createSubMenu(StandardActions.WEB_MENU,
                        factory.createMenuItem(StandardActions.OPEN_WEBPAGE, new OpenBrowserAction("https://jabref.org/")),
                        factory.createMenuItem(StandardActions.OPEN_BLOG, new OpenBrowserAction("https://blog.jabref.org/")),
                        factory.createMenuItem(StandardActions.OPEN_FACEBOOK, new OpenBrowserAction("https://www.facebook.com/JabRef/")),
                        factory.createMenuItem(StandardActions.OPEN_TWITTER, new OpenBrowserAction("https://twitter.com/jabref_org")),
                        factory.createMenuItem(StandardActions.OPEN_GITHUB, new OpenBrowserAction("https://github.com/JabRef/jabref")),

                        new SeparatorMenuItem(),

                        factory.createMenuItem(StandardActions.OPEN_DEV_VERSION_LINK, new OpenBrowserAction("https://builds.jabref.org/master/")),
                        factory.createMenuItem(StandardActions.OPEN_CHANGELOG, new OpenBrowserAction("https://github.com/JabRef/jabref/blob/master/CHANGELOG.md"))
                ),
                factory.createMenuItem(StandardActions.ABOUT, new AboutAction())
        );

        // @formatter:on
        MenuBar menu = new MenuBar();
        menu.getStyleClass().add("mainMenu");
        menu.getMenus().addAll(
                file,
                edit,
                library,
                quality,
                lookup,
                tools,
                view,
                options,
                help);
        menu.setUseSystemMenuBar(true);
        return menu;
    }

    private Button createNewEntryFromIdButton() {
        Button newEntryFromIdButton = new Button();

        newEntryFromIdButton.setGraphic(IconTheme.JabRefIcons.IMPORT.getGraphicNode());
        newEntryFromIdButton.getStyleClass().setAll("icon-button");
        newEntryFromIdButton.setFocusTraversable(false);
        newEntryFromIdButton.disableProperty().bind(ActionHelper.needsDatabase(stateManager).not());
        newEntryFromIdButton.setOnMouseClicked(event -> {
            GenerateEntryFromIdDialog entryFromId = new GenerateEntryFromIdDialog(getCurrentLibraryTab(), dialogService, prefs, taskExecutor, stateManager);

            if (entryFromIdPopOver == null) {
                entryFromIdPopOver = new PopOver(entryFromId.getDialogPane());
                entryFromIdPopOver.setTitle(Localization.lang("Import by ID"));
                entryFromIdPopOver.setArrowLocation(PopOver.ArrowLocation.TOP_CENTER);
                entryFromIdPopOver.setContentNode(entryFromId.getDialogPane());
                entryFromIdPopOver.show(newEntryFromIdButton);
                entryFromId.setEntryFromIdPopOver(entryFromIdPopOver);
            } else if (entryFromIdPopOver.isShowing()) {
                entryFromIdPopOver.hide();
            } else {
                entryFromIdPopOver.setContentNode(entryFromId.getDialogPane());
                entryFromIdPopOver.show(newEntryFromIdButton);
                entryFromId.setEntryFromIdPopOver(entryFromIdPopOver);
            }
        });
        newEntryFromIdButton.setTooltip(new Tooltip(Localization.lang("Import by ID")));

        return newEntryFromIdButton;
    }

    private Group createTaskIndicator() {
        ProgressIndicator indicator = new ProgressIndicator();
        indicator.getStyleClass().add("progress-indicatorToolbar");
        indicator.progressProperty().bind(stateManager.getTasksProgress());

        Tooltip someTasksRunning = new Tooltip(Localization.lang("Background Tasks are running"));
        Tooltip noTasksRunning = new Tooltip(Localization.lang("Background Tasks are done"));
        indicator.setTooltip(noTasksRunning);
        stateManager.getAnyTaskRunning().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                indicator.setTooltip(someTasksRunning);
            } else {
                indicator.setTooltip(noTasksRunning);
            }
        });

        /*
        The label of the indicator cannot be removed with styling. Therefore,
        hide it and clip it to a square of (width x width) each time width is updated.
         */
        indicator.widthProperty().addListener((observable, oldValue, newValue) -> {
            /*
            The indeterminate spinner is wider than the determinate spinner.
            We must make sure they are the same width for the clipping to result in a square of the same size always.
             */
            if (!indicator.isIndeterminate()) {
                indicator.setPrefWidth(newValue.doubleValue());
            }
            if (newValue.doubleValue() > 0) {
                Rectangle clip = new Rectangle(newValue.doubleValue(), newValue.doubleValue());
                indicator.setClip(clip);
            }
        });

        indicator.setOnMouseClicked(event -> {
            TaskProgressView<Task<?>> taskProgressView = new TaskProgressView<>();
            EasyBind.bindContent(taskProgressView.getTasks(), stateManager.getBackgroundTasks());
            taskProgressView.setRetainTasks(true);
            taskProgressView.setGraphicFactory(BackgroundTask::getIcon);

            if (progressViewPopOver == null) {
                progressViewPopOver = new PopOver(taskProgressView);
                progressViewPopOver.setTitle(Localization.lang("Background Tasks"));
                progressViewPopOver.setArrowLocation(PopOver.ArrowLocation.RIGHT_TOP);
                progressViewPopOver.setContentNode(taskProgressView);
                progressViewPopOver.show(indicator);
            } else if (progressViewPopOver.isShowing()) {
                progressViewPopOver.hide();
            } else {
                progressViewPopOver.setContentNode(taskProgressView);
                progressViewPopOver.show(indicator);
            }
        });

        return new Group(indicator);
    }

    public void addParserResult(ParserResult parserResult, boolean focusPanel) {
        if (parserResult.toOpenTab()) {
            // Add the entries to the open tab.
            LibraryTab libraryTab = getCurrentLibraryTab();
            if (libraryTab == null) {
                // There is no open tab to add to, so we create a new tab:
                addTab(parserResult.getDatabaseContext(), focusPanel);
            } else {
                addImportedEntries(libraryTab, parserResult);
            }
        } else {
            // only add tab if DB is not already open
            Optional<LibraryTab> libraryTab = getLibraryTabs().stream()
                                                              .filter(p -> p.getBibDatabaseContext()
                                                                            .getDatabasePath()
                                                                            .equals(parserResult.getPath()))
                                                              .findFirst();

            if (libraryTab.isPresent()) {
                tabbedPane.getSelectionModel().select(libraryTab.get());
            } else {
                addTab(parserResult.getDatabaseContext(), focusPanel);
            }
        }
    }

    /**
     * This method causes all open LibraryTabs to set up their tables anew. When called from PreferencesDialogViewModel,
     * this updates to the new settings.
     * We need to notify all tabs about the changes to avoid problems when changing the column set.
     */
    public void setupAllTables() {
        tabbedPane.getTabs().forEach(tab -> {
            LibraryTab libraryTab = (LibraryTab) tab;
            if (libraryTab.getDatabase() != null) {
                DefaultTaskExecutor.runInJavaFXThread(libraryTab::setupMainPanel);
            }
        });
    }

    private ContextMenu createTabContextMenuFor(LibraryTab tab, KeyBindingRepository keyBindingRepository) {
        ContextMenu contextMenu = new ContextMenu();
        ActionFactory factory = new ActionFactory(keyBindingRepository);

        contextMenu.getItems().addAll(
                factory.createMenuItem(StandardActions.OPEN_DATABASE_FOLDER, new OpenDatabaseFolder(tab.getBibDatabaseContext())),
                factory.createMenuItem(StandardActions.OPEN_CONSOLE, new OpenConsoleAction(tab.getBibDatabaseContext(), stateManager, prefs)),
                new SeparatorMenuItem(),
                factory.createMenuItem(StandardActions.CLOSE_LIBRARY, new CloseDatabaseAction(tab)),
                factory.createMenuItem(StandardActions.CLOSE_OTHER_LIBRARIES, new CloseOthersDatabaseAction(tab)),
                factory.createMenuItem(StandardActions.CLOSE_ALL_LIBRARIES, new CloseAllDatabaseAction())
        );

        return contextMenu;
    }

    public void addTab(LibraryTab libraryTab, boolean raisePanel) {
        tabbedPane.getTabs().add(libraryTab);

        libraryTab.setOnCloseRequest(event -> {
            closeTab(libraryTab);
            libraryTab.getDataLoadingTask().cancel();
            event.consume();
        });

        libraryTab.setContextMenu(createTabContextMenuFor(libraryTab, Globals.getKeyPrefs()));

        if (raisePanel) {
            tabbedPane.getSelectionModel().select(libraryTab);
        }

        libraryTab.getUndoManager().registerListener(new UndoRedoEventManager());

        BibDatabaseContext context = libraryTab.getBibDatabaseContext();

        if (readyForAutosave(context)) {
            AutosaveManager autosaver = AutosaveManager.start(context);
            autosaver.registerListener(new AutosaveUiManager(libraryTab));
        }

        BackupManager.start(context, Globals.entryTypesManager, prefs);

        trackOpenNewDatabase(libraryTab);
    }

    private void trackOpenNewDatabase(LibraryTab libraryTab) {
        Globals.getTelemetryClient().ifPresent(client -> client.trackEvent(
                "OpenNewDatabase",
                Map.of(),
                Map.of("NumberOfEntries", (double) libraryTab.getBibDatabaseContext().getDatabase().getEntryCount())));
    }

    public LibraryTab addTab(BibDatabaseContext databaseContext, boolean raisePanel) {
        Objects.requireNonNull(databaseContext);

        LibraryTab libraryTab = new LibraryTab(this, prefs, stateManager, databaseContext, ExternalFileTypes.getInstance());
        addTab(libraryTab, raisePanel);
        return libraryTab;
    }

    private boolean readyForAutosave(BibDatabaseContext context) {
        return ((context.getLocation() == DatabaseLocation.SHARED) ||
                ((context.getLocation() == DatabaseLocation.LOCAL) && prefs.shouldAutosave()))
                &&
                context.getDatabasePath().isPresent();
    }

    /**
     * Opens the import inspection dialog to let the user decide which of the given entries to import.
     *
     * @param panel        The BasePanel to add to.
     * @param parserResult The entries to add.
     */
    private void addImportedEntries(final LibraryTab panel, final ParserResult parserResult) {
        BackgroundTask<ParserResult> task = BackgroundTask.wrap(() -> parserResult);
        ImportCleanup cleanup = new ImportCleanup(panel.getBibDatabaseContext().getMode());
        cleanup.doPostCleanup(parserResult.getDatabase().getEntries());
        ImportEntriesDialog dialog = new ImportEntriesDialog(panel.getBibDatabaseContext(), task);
        dialog.setTitle(Localization.lang("Import"));
        dialogService.showCustomDialogAndWait(dialog);
    }

    public FileHistoryMenu getFileHistory() {
        return fileHistory;
    }

    /**
     * Ask if the user really wants to close the given database
     *
     * @return true if the user choose to close the database
     */
    private boolean confirmClose(LibraryTab libraryTab) {
        String filename = libraryTab.getBibDatabaseContext()
                                    .getDatabasePath()
                                    .map(Path::toAbsolutePath)
                                    .map(Path::toString)
                                    .orElse(Localization.lang("untitled"));

        ButtonType saveChanges = new ButtonType(Localization.lang("Save changes"), ButtonBar.ButtonData.YES);
        ButtonType discardChanges = new ButtonType(Localization.lang("Discard changes"), ButtonBar.ButtonData.NO);
        ButtonType cancel = new ButtonType(Localization.lang("Return to JabRef"), ButtonBar.ButtonData.CANCEL_CLOSE);

        Optional<ButtonType> response = dialogService.showCustomButtonDialogAndWait(Alert.AlertType.CONFIRMATION,
                Localization.lang("Save before closing"),
                Localization.lang("Library '%0' has changed.", filename),
                saveChanges, discardChanges, cancel);

        if (response.isPresent() && response.get().equals(saveChanges)) {
            // The user wants to save.
            try {
                SaveDatabaseAction saveAction = new SaveDatabaseAction(libraryTab, prefs, Globals.entryTypesManager);
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
        return response.isEmpty() || !response.get().equals(cancel);
    }

    /**
     * Ask if the user really wants to remove any empty entries
     */
    private Boolean confirmEmptyEntry(LibraryTab libraryTab, BibDatabaseContext context) {
        String filename = libraryTab.getBibDatabaseContext()
                                    .getDatabasePath()
                                    .map(Path::toAbsolutePath)
                                    .map(Path::toString)
                                    .orElse(Localization.lang("untitled"));

        ButtonType deleteEmptyEntries = new ButtonType(Localization.lang("Delete empty entries"), ButtonBar.ButtonData.YES);
        ButtonType keepEmptyEntries = new ButtonType(Localization.lang("Keep empty entries"), ButtonBar.ButtonData.NO);
        ButtonType cancel = new ButtonType(Localization.lang("Return to JabRef"), ButtonBar.ButtonData.CANCEL_CLOSE);

        Optional<ButtonType> response = dialogService.showCustomButtonDialogAndWait(Alert.AlertType.CONFIRMATION,
                Localization.lang("Empty entries"),
                Localization.lang("Library '%0' has empty entries. Do you want to delete them?", filename),
                deleteEmptyEntries, keepEmptyEntries, cancel);
        if (response.isPresent() && response.get().equals(deleteEmptyEntries)) {
            // The user wants to delete.
            try {
                for (BibEntry currentEntry : new ArrayList<BibEntry>(context.getEntries())) {
                    if (currentEntry.getFields().isEmpty()) {
                        context.getDatabase().removeEntries(Collections.singletonList(currentEntry));
                    }
                }
                SaveDatabaseAction saveAction = new SaveDatabaseAction(libraryTab, prefs, Globals.entryTypesManager);
                if (saveAction.save()) {
                    return true;
                }
                // The action was either canceled or unsuccessful.
                dialogService.notify(Localization.lang("Unable to save library"));
            } catch (Throwable ex) {
                LOGGER.error("A problem occurred when trying to delete the empty entries", ex);
                dialogService.showErrorDialogAndWait(Localization.lang("Delete empty entries"), Localization.lang("Could not delete empty entries."), ex);
            }
            // Save was cancelled or an error occurred.
            return false;
        }
        return !response.get().equals(cancel);
    }

    private void closeTab(LibraryTab libraryTab) {
        // empty tab without database
        if (libraryTab == null) {
            return;
        }

        final BibDatabaseContext context = libraryTab.getBibDatabaseContext();
        if (context.hasEmptyEntries()) {
            if (!confirmEmptyEntry(libraryTab, context)) {
                return;
            }
        }

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
        BackupManager.shutdown(context);
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
        return new OpenDatabaseAction(this, prefs, dialogService, stateManager);
    }

    public PushToApplicationsManager getPushToApplicationsManager() {
        return pushToApplicationsManager;
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

    /**
     * The action concerned with closing the window.
     */
    private class CloseAction extends SimpleCommand {

        @Override
        public void execute() {
            quit();
        }
    }

    private class CloseDatabaseAction extends SimpleCommand {
        private final LibraryTab libraryTab;

        public CloseDatabaseAction(LibraryTab libraryTab) {
            this.libraryTab = libraryTab;
        }

        /**
         * Using this constructor will result in executing the command on the currently open library tab
         * */
        public CloseDatabaseAction() {
            this(null);
        }

        @Override
        public void execute() {
            closeTab(Optional.ofNullable(libraryTab).orElse(getCurrentLibraryTab()));
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
        private final BibDatabaseContext databaseContext;

        public OpenDatabaseFolder(BibDatabaseContext databaseContext) {
            this.databaseContext = databaseContext;
        }

        @Override
        public void execute() {
            Optional.of(databaseContext).flatMap(BibDatabaseContext::getDatabasePath).ifPresent(path -> {
                try {
                    JabRefDesktop.openFolderAndSelectFile(path, prefs);
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
