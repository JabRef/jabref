package org.jabref.gui;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TimerTask;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
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
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import org.jabref.Globals;
import org.jabref.JabRefExecutorService;
import org.jabref.gui.actions.ActionFactory;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.auximport.NewSubLibraryAction;
import org.jabref.gui.bibtexextractor.ExtractBibtexAction;
import org.jabref.gui.bibtexkeypattern.BibtexKeyPatternAction;
import org.jabref.gui.bibtexkeypattern.GenerateBibtexKeyAction;
import org.jabref.gui.cleanup.CleanupAction;
import org.jabref.gui.contentselector.ManageContentSelectorAction;
import org.jabref.gui.copyfiles.CopyFilesAction;
import org.jabref.gui.customentrytypes.CustomizeEntryAction;
import org.jabref.gui.customizefields.SetupGeneralFieldsAction;
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
import org.jabref.gui.exporter.ManageCustomExportsAction;
import org.jabref.gui.exporter.SaveAction;
import org.jabref.gui.exporter.SaveAllAction;
import org.jabref.gui.exporter.SaveDatabaseAction;
import org.jabref.gui.exporter.WriteXMPAction;
import org.jabref.gui.externalfiles.AutoLinkFilesAction;
import org.jabref.gui.externalfiles.DownloadFullTextAction;
import org.jabref.gui.externalfiles.FindUnlinkedFilesAction;
import org.jabref.gui.externalfiletype.EditExternalFileTypesAction;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.help.AboutAction;
import org.jabref.gui.help.ErrorConsoleAction;
import org.jabref.gui.help.HelpAction;
import org.jabref.gui.help.SearchForUpdateAction;
import org.jabref.gui.importer.ImportCommand;
import org.jabref.gui.importer.ImportEntriesDialog;
import org.jabref.gui.importer.ManageCustomImportsAction;
import org.jabref.gui.importer.NewDatabaseAction;
import org.jabref.gui.importer.NewEntryAction;
import org.jabref.gui.importer.actions.OpenDatabaseAction;
import org.jabref.gui.importer.fetcher.LookupIdentifierAction;
import org.jabref.gui.integrity.IntegrityCheckAction;
import org.jabref.gui.journals.AbbreviateAction;
import org.jabref.gui.journals.ManageJournalsAction;
import org.jabref.gui.keyboard.CustomizeKeyBindingAction;
import org.jabref.gui.keyboard.KeyBinding;
import org.jabref.gui.libraryproperties.LibraryPropertiesAction;
import org.jabref.gui.menus.FileHistoryMenu;
import org.jabref.gui.mergeentries.MergeEntriesAction;
import org.jabref.gui.metadata.BibtexStringEditorAction;
import org.jabref.gui.metadata.PreambleEditor;
import org.jabref.gui.preferences.ShowPreferencesAction;
import org.jabref.gui.preview.CopyCitationAction;
import org.jabref.gui.protectedterms.ManageProtectedTermsAction;
import org.jabref.gui.push.PushToApplicationAction;
import org.jabref.gui.push.PushToApplicationsManager;
import org.jabref.gui.search.GlobalSearchBar;
import org.jabref.gui.shared.ConnectToSharedDatabaseCommand;
import org.jabref.gui.shared.PullChangesFromSharedAction;
import org.jabref.gui.specialfields.SpecialFieldMenuItemFactory;
import org.jabref.gui.texparser.ParseLatexAction;
import org.jabref.gui.undo.CountingUndoManager;
import org.jabref.gui.undo.UndoRedoAction;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.DefaultTaskExecutor;
import org.jabref.gui.util.ThemeLoader;
import org.jabref.logic.autosaveandbackup.AutosaveManager;
import org.jabref.logic.autosaveandbackup.BackupManager;
import org.jabref.logic.citationstyle.CitationStyleOutputFormat;
import org.jabref.logic.importer.IdFetcher;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.WebFetchers;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.undo.AddUndoableActionEvent;
import org.jabref.logic.undo.UndoChangeEvent;
import org.jabref.logic.undo.UndoRedoEvent;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.database.shared.DatabaseLocation;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.SpecialField;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.preferences.JabRefPreferences;
import org.jabref.preferences.LastFocusedTabPreferences;

import com.google.common.eventbus.Subscribe;
import org.fxmisc.easybind.EasyBind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The main window of the application.
 */
public class JabRefFrame extends BorderPane {

    // Frame titles.
    public static final String FRAME_TITLE = "JabRef";

    private static final Logger LOGGER = LoggerFactory.getLogger(JabRefFrame.class);

    private final SplitPane splitPane = new SplitPane();
    private final JabRefPreferences prefs = Globals.prefs;
    private final ThemeLoader themeLoader = Globals.getThemeLoader();
    private final GlobalSearchBar globalSearchBar = new GlobalSearchBar(this, Globals.stateManager);

    private final ProgressBar progressBar = new ProgressBar();
    private final FileHistoryMenu fileHistory;

    private final Stage mainStage;
    private final StateManager stateManager;
    private final CountingUndoManager undoManager;
    private final PushToApplicationsManager pushToApplicationsManager;
    private final DialogService dialogService;
    private final JabRefExecutorService executorService;
    private SidePaneManager sidePaneManager;
    private TabPane tabbedPane;
    private SidePane sidePane;

    public JabRefFrame(Stage mainStage) {
        this.mainStage = mainStage;
        this.dialogService = new JabRefDialogService(mainStage, this, prefs, themeLoader);
        this.stateManager = Globals.stateManager;
        this.pushToApplicationsManager = new PushToApplicationsManager(dialogService, stateManager);
        this.undoManager = Globals.undoManager;
        this.fileHistory = new FileHistoryMenu(prefs, dialogService, getOpenDatabaseAction());
        this.executorService = JabRefExecutorService.INSTANCE;
    }

    private static BasePanel getBasePanel(Tab tab) {
        return (BasePanel) tab.getContent();
    }

    private void initDragAndDrop() {
        Tab dndIndicator = new Tab(Localization.lang("Open files..."), null);
        dndIndicator.getStyleClass().add("drop");

        EasyBind.subscribe(tabbedPane.skinProperty(), skin -> {
            if (!(skin instanceof TabPaneSkin)) {
                return;
            }

            // We need to get the tab header, the following is a ugly workaround
            Node tabHeaderArea = ((TabPaneSkin) this.tabbedPane.getSkin())
                    .getChildren()
                    .stream()
                    .filter(node -> node.getStyleClass().contains("tab-header-area"))
                    .findFirst()
                    .orElseThrow();

            tabHeaderArea.setOnDragOver(event -> {
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

            tabHeaderArea.setOnDragExited(event -> tabbedPane.getTabs().remove(dndIndicator));

            tabHeaderArea.setOnDragDropped(event -> {
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
                        getCurrentBasePanel().getMainTable().requestFocus();
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
                    default:
                }
            }
        });
    }

    private void initShowTrackingNotification() {
        if (!Globals.prefs.shouldAskToCollectTelemetry()) {
            JabRefExecutorService.INSTANCE.submit(new TimerTask() {

                @Override
                public void run() {
                    DefaultTaskExecutor.runInJavaFXThread(JabRefFrame.this::showTrackingNotification);
                }
            }, 60000); // run in one minute
        }
    }

    private Void showTrackingNotification() {
        if (!Globals.prefs.shouldCollectTelemetry()) {
            boolean shouldCollect = dialogService.showConfirmationDialogAndWait(
                    Localization.lang("Telemetry: Help make JabRef better"),
                    Localization.lang("To improve the user experience, we would like to collect anonymous statistics on the features you use. We will only record what features you access and how often you do it. We will neither collect any personal data nor the content of bibliographic items. If you choose to allow data collection, you can later disable it via Options -> Preferences -> General."),
                    Localization.lang("Share anonymous statistics"),
                    Localization.lang("Don't share"));
            Globals.prefs.setShouldCollectTelemetry(shouldCollect);
        }

        Globals.prefs.askedToCollectTelemetry();

        return null;
    }

    public void refreshTitleAndTabs() {
        DefaultTaskExecutor.runInJavaFXThread(() -> {

            setWindowTitle();
            updateAllTabTitles();
        });
    }

    /**
     * Sets the title of the main window.
     */
    public void setWindowTitle() {
        BasePanel panel = getCurrentBasePanel();

        // no database open
        if (panel == null) {
            // setTitle(FRAME_TITLE);
            return;
        }

        String mode = panel.getBibDatabaseContext().getMode().getFormattedName();
        String modeInfo = String.format(" (%s)", Localization.lang("%0 mode", mode));
        boolean isAutosaveEnabled = Globals.prefs.getBoolean(JabRefPreferences.LOCAL_AUTO_SAVE);

        if (panel.getBibDatabaseContext().getLocation() == DatabaseLocation.LOCAL) {
            String changeFlag = panel.isModified() && !isAutosaveEnabled ? "*" : "";
            String databaseFile = panel.getBibDatabaseContext()
                                       .getDatabasePath()
                                       .map(Path::toString)
                                       .orElse(Localization.lang("untitled"));
            // setTitle(FRAME_TITLE + " - " + databaseFile + changeFlag + modeInfo);
        } else if (panel.getBibDatabaseContext().getLocation() == DatabaseLocation.SHARED) {
            // setTitle(FRAME_TITLE + " - " + panel.getBibDatabaseContext().getDBMSSynchronizer().getDBName() + " ["
            //        + Localization.lang("shared") + "]" + modeInfo);
        }
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

    public JabRefPreferences prefs() {
        return prefs;
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
        // prefs.putBoolean(JabRefPreferences.WINDOW_MAXIMISED, getExtendedState() == Frame.MAXIMIZED_BOTH);

        if (prefs.getBoolean(JabRefPreferences.OPEN_LAST_EDITED)) {
            // Here we store the names of all current files. If
            // there is no current file, we remove any
            // previously stored filename.
            if (filenames.isEmpty()) {
                prefs.remove(JabRefPreferences.LAST_EDITED);
            } else {
                prefs.putStringList(JabRefPreferences.LAST_EDITED, filenames);
                Path focusedDatabase = getCurrentBasePanel().getBibDatabaseContext().getDatabasePath().orElse(null);
                new LastFocusedTabPreferences(prefs).setLastFocusedTab(focusedDatabase);
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
        // First ask if the user really wants to close, if the library has not been saved since last save.
        List<String> filenames = new ArrayList<>();
        for (int i = 0; i < tabbedPane.getTabs().size(); i++) {
            BasePanel panel = getBasePanelAt(i);
            final BibDatabaseContext context = panel.getBibDatabaseContext();

            if (panel.isModified() && (context.getLocation() == DatabaseLocation.LOCAL)) {
                tabbedPane.getSelectionModel().select(i);
                if (!confirmClose(panel)) {
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
        waitForSaveFinishedDialog.showAndWait(getBasePanelList());

        // Good bye!
        tearDownJabRef(filenames);
        Platform.exit();
        return true;
    }

    private void initLayout() {
        setProgressBarVisible(false);

        setId("frame");

        VBox head = new VBox(createMenu(), createToolbar());
        head.setSpacing(0d);
        setTop(head);

        splitPane.getItems().addAll(sidePane, tabbedPane);

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
        splitPane.setDividerPositions(prefs.getDouble(JabRefPreferences.SIDE_PANE_WIDTH));
        if (!splitPane.getDividers().isEmpty()) {
            EasyBind.subscribe(splitPane.getDividers().get(0).positionProperty(),
                    position -> prefs.putDouble(JabRefPreferences.SIDE_PANE_WIDTH, position.doubleValue()));
        }
    }

    private Node createToolbar() {
        Pane leftSpacer = new Pane();
        leftSpacer.setMinWidth(50);
        HBox.setHgrow(leftSpacer, Priority.SOMETIMES);
        Pane rightSpacer = new Pane();
        HBox.setHgrow(rightSpacer, Priority.SOMETIMES);

        ActionFactory factory = new ActionFactory(Globals.getKeyPrefs());

        Button newLibrary;
        if (Globals.prefs.getBoolean(JabRefPreferences.BIBLATEX_DEFAULT_MODE)) {
            newLibrary = factory.createIconButton(StandardActions.NEW_LIBRARY_BIBLATEX, new NewDatabaseAction(this, BibDatabaseMode.BIBLATEX));
        } else {
            newLibrary = factory.createIconButton(StandardActions.NEW_LIBRARY_BIBTEX, new NewDatabaseAction(this, BibDatabaseMode.BIBTEX));
        }

        HBox leftSide = new HBox(
                newLibrary,
                factory.createIconButton(StandardActions.OPEN_LIBRARY, new OpenDatabaseAction(this)),
                factory.createIconButton(StandardActions.SAVE_LIBRARY, new SaveAction(SaveAction.SaveMethod.SAVE, this, stateManager)),
                leftSpacer
        );

        final PushToApplicationAction pushToApplicationAction = getPushToApplicationsManager().getPushToApplicationAction();
        final Button pushToApplicationButton = factory.createIconButton(pushToApplicationAction.getActionInformation(), pushToApplicationAction);
        pushToApplicationsManager.setToolBarButton(pushToApplicationButton);

        HBox rightSide = new HBox(
                factory.createIconButton(StandardActions.NEW_ARTICLE, new NewEntryAction(this, StandardEntryType.Article, dialogService, Globals.prefs, stateManager)),
                factory.createIconButton(StandardActions.NEW_ENTRY, new NewEntryAction(this, dialogService, Globals.prefs, stateManager)),
                factory.createIconButton(StandardActions.NEW_ENTRY_FROM_PLAIN_TEXT, new ExtractBibtexAction(stateManager)),
                factory.createIconButton(StandardActions.DELETE_ENTRY, new EditAction(StandardActions.DELETE_ENTRY, this, stateManager)),
                new Separator(Orientation.VERTICAL),
                factory.createIconButton(StandardActions.UNDO, new UndoRedoAction(StandardActions.UNDO, this, dialogService, stateManager)),
                factory.createIconButton(StandardActions.REDO, new UndoRedoAction(StandardActions.REDO, this, dialogService, stateManager)),
                factory.createIconButton(StandardActions.CUT, new EditAction(StandardActions.CUT, this, stateManager)),
                factory.createIconButton(StandardActions.COPY, new EditAction(StandardActions.COPY, this, stateManager)),
                factory.createIconButton(StandardActions.PASTE, new EditAction(StandardActions.PASTE, this, stateManager)),
                new Separator(Orientation.VERTICAL),
                pushToApplicationButton,
                factory.createIconButton(StandardActions.GENERATE_CITE_KEYS, new GenerateBibtexKeyAction(this, dialogService, stateManager)),
                factory.createIconButton(StandardActions.CLEANUP_ENTRIES, new CleanupAction(this, prefs, dialogService, stateManager)),
                new Separator(Orientation.VERTICAL),
                factory.createIconButton(StandardActions.OPEN_GITHUB, new OpenBrowserAction("https://github.com/JabRef/jabref")),
                factory.createIconButton(StandardActions.OPEN_FACEBOOK, new OpenBrowserAction("https://www.facebook.com/JabRef/")),
                factory.createIconButton(StandardActions.OPEN_TWITTER, new OpenBrowserAction("https://twitter.com/jabref_org"))
        );

        HBox.setHgrow(globalSearchBar, Priority.ALWAYS);

        ToolBar toolBar = new ToolBar(
                leftSide,

                globalSearchBar,

                rightSpacer,
                rightSide);
        toolBar.getStyleClass().add("mainToolbar");

        return toolBar;
    }

    /**
     * Returns the indexed BasePanel.
     *
     * @param i Index of base
     */
    public BasePanel getBasePanelAt(int i) {
        return (BasePanel) tabbedPane.getTabs().get(i).getContent();
    }

    /**
     * Returns a list of BasePanel.
     */
    public List<BasePanel> getBasePanelList() {
        List<BasePanel> returnList = new ArrayList<>();
        for (int i = 0; i < getBasePanelCount(); i++) {
            returnList.add(getBasePanelAt(i));
        }
        return returnList;
    }

    public void showBasePanelAt(int i) {
        tabbedPane.getSelectionModel().select(i);
    }

    public void showBasePanel(BasePanel bp) {
        tabbedPane.getSelectionModel().select(getTab(bp));
    }

    public void init() {
        sidePaneManager = new SidePaneManager(Globals.prefs, this);
        sidePane = sidePaneManager.getPane();

        tabbedPane = new TabPane();
        tabbedPane.setTabDragPolicy(TabPane.TabDragPolicy.REORDER);

        initLayout();

        initKeyBindings();

        initDragAndDrop();

        // setBounds(GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds());
        // WindowLocation pw = new WindowLocation(this, JabRefPreferences.POS_X, JabRefPreferences.POS_Y, JabRefPreferences.SIZE_X,
        //        JabRefPreferences.SIZE_Y);
        // pw.displayWindowAtStoredLocation();

        // Bind global state
        stateManager.activeDatabaseProperty().bind(
                EasyBind.map(tabbedPane.getSelectionModel().selectedItemProperty(),
                        tab -> Optional.ofNullable(tab).map(JabRefFrame::getBasePanel).map(BasePanel::getBibDatabaseContext)));

        // Subscribe to the search
        EasyBind.subscribe(stateManager.activeSearchQueryProperty(),
                query -> {
                    if (getCurrentBasePanel() != null) {
                        getCurrentBasePanel().setCurrentSearchQuery(query);
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
            if (tab == null) {
                stateManager.setSelectedEntries(Collections.emptyList());
                return;
            }

            BasePanel newBasePanel = getBasePanel(tab);

            // Poor-mans binding to global state
            stateManager.setSelectedEntries(newBasePanel.getSelectedEntries());

            // Update active search query when switching between databases
            stateManager.activeSearchQueryProperty().set(newBasePanel.getCurrentSearchQuery());

            // groupSidePane.getToggleCommand().setSelected(sidePaneManager.isComponentVisible(GroupSidePane.class));
            // previewToggle.setSelected(Globals.prefs.getPreviewPreferences().isPreviewPanelEnabled());
            // generalFetcher.getToggleCommand().setSelected(sidePaneManager.isComponentVisible(WebSearchPane.class));
            // openOfficePanel.getToggleCommand().setSelected(sidePaneManager.isComponentVisible(OpenOfficeSidePanel.class));

            setWindowTitle();
            // Update search autocompleter with information for the correct database:
            newBasePanel.updateSearchManager();

            newBasePanel.getUndoManager().postUndoRedoEvent();
            newBasePanel.getMainTable().requestFocus();
        });
        initShowTrackingNotification();
    }

    /**
     * Returns the currently viewed BasePanel.
     */
    public BasePanel getCurrentBasePanel() {
        if ((tabbedPane == null) || (tabbedPane.getSelectionModel().getSelectedItem() == null)) {
            return null;
        }
        return getBasePanel(tabbedPane.getSelectionModel().getSelectedItem());
    }

    /**
     * @return the BasePanel count.
     */
    public int getBasePanelCount() {
        return tabbedPane.getTabs().size();
    }

    private Tab getTab(BasePanel comp) {
        for (Tab tab : tabbedPane.getTabs()) {
            if (tab.getContent() == comp) {
                return tab;
            }
        }
        return null;
    }

    /**
     * @deprecated do not operate on tabs but on BibDatabaseContexts
     */
    @Deprecated
    public TabPane getTabbedPane() {
        return tabbedPane;
    }

    public void setTabTitle(BasePanel comp, String title, String toolTip) {
        DefaultTaskExecutor.runInJavaFXThread(() -> {
            Tab tab = getTab(comp);
            tab.setText(title);
            tab.setTooltip(new Tooltip(toolTip));
        });
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
                factory.createSubMenu(StandardActions.NEW_LIBRARY,
                        factory.createMenuItem(StandardActions.NEW_LIBRARY_BIBTEX, new NewDatabaseAction(this, BibDatabaseMode.BIBTEX)),
                        factory.createMenuItem(StandardActions.NEW_LIBRARY_BIBLATEX, new NewDatabaseAction(this, BibDatabaseMode.BIBLATEX))),

                factory.createMenuItem(StandardActions.OPEN_LIBRARY, getOpenDatabaseAction()),
                fileHistory,
                factory.createMenuItem(StandardActions.SAVE_LIBRARY, new SaveAction(SaveAction.SaveMethod.SAVE, this, stateManager)),
                factory.createMenuItem(StandardActions.SAVE_LIBRARY_AS, new SaveAction(SaveAction.SaveMethod.SAVE_AS, this, stateManager)),
                factory.createMenuItem(StandardActions.SAVE_ALL, new SaveAllAction(this)),

                new SeparatorMenuItem(),

                factory.createSubMenu(StandardActions.IMPORT,
                        factory.createMenuItem(StandardActions.IMPORT_INTO_CURRENT_LIBRARY, new ImportCommand(this, false, stateManager)),
                        factory.createMenuItem(StandardActions.IMPORT_INTO_NEW_LIBRARY, new ImportCommand(this, true, stateManager))),

                factory.createSubMenu(StandardActions.EXPORT,
                        factory.createMenuItem(StandardActions.EXPORT_ALL, new ExportCommand(this, false, Globals.prefs)),
                        factory.createMenuItem(StandardActions.EXPORT_SELECTED, new ExportCommand(this, true, Globals.prefs)),
                        factory.createMenuItem(StandardActions.SAVE_SELECTED_AS_PLAIN_BIBTEX, new SaveAction(SaveAction.SaveMethod.SAVE_SELECTED, this, stateManager))),

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
                        factory.createMenuItem(StandardActions.COPY_TITLE, new CopyMoreAction(StandardActions.COPY_TITLE, dialogService, stateManager, Globals.clipboardManager, prefs)),
                        factory.createMenuItem(StandardActions.COPY_KEY, new CopyMoreAction(StandardActions.COPY_KEY, dialogService, stateManager, Globals.clipboardManager, prefs)),
                        factory.createMenuItem(StandardActions.COPY_CITE_KEY, new CopyMoreAction(StandardActions.COPY_CITE_KEY, dialogService, stateManager, Globals.clipboardManager, prefs)),
                        factory.createMenuItem(StandardActions.COPY_KEY_AND_TITLE, new CopyMoreAction(StandardActions.COPY_KEY_AND_TITLE, dialogService, stateManager, Globals.clipboardManager, prefs)),
                        factory.createMenuItem(StandardActions.COPY_KEY_AND_LINK, new CopyMoreAction(StandardActions.COPY_KEY_AND_LINK, dialogService, stateManager, Globals.clipboardManager, prefs)),
                        factory.createMenuItem(StandardActions.COPY_CITATION_PREVIEW, new CopyCitationAction(CitationStyleOutputFormat.HTML, dialogService, stateManager, Globals.clipboardManager, prefs.getPreviewPreferences())),
                        factory.createMenuItem(StandardActions.EXPORT_SELECTED_TO_CLIPBOARD, new ExportToClipboardAction(this, dialogService, Globals.exportFactory, Globals.clipboardManager, Globals.TASK_EXECUTOR))),

                factory.createMenuItem(StandardActions.PASTE, new EditAction(StandardActions.PASTE, this, stateManager)),

                new SeparatorMenuItem(),

                factory.createMenuItem(StandardActions.REPLACE_ALL, new ReplaceStringAction(this, stateManager)),
                factory.createMenuItem(StandardActions.GENERATE_CITE_KEYS, new GenerateBibtexKeyAction(this, dialogService, stateManager)),

                new SeparatorMenuItem(),

                factory.createMenuItem(StandardActions.MANAGE_KEYWORDS, new ManageKeywordsAction(stateManager)),
                factory.createMenuItem(StandardActions.MASS_SET_FIELDS, new MassSetFieldsAction(stateManager, dialogService, undoManager))
        );

        if (Globals.prefs.getBoolean(JabRefPreferences.SPECIALFIELDSENABLED)) {
            edit.getItems().addAll(
                    new SeparatorMenuItem(),
                    // ToDo: SpecialField needs the active BasePanel to mark it as changed.
                    //  Refactor BasePanel, should mark the BibDatabaseContext or the UndoManager as dirty instead!
                    SpecialFieldMenuItemFactory.createSpecialFieldMenu(SpecialField.RANKING, factory, this, dialogService, stateManager),
                    SpecialFieldMenuItemFactory.getSpecialFieldSingleItem(SpecialField.RELEVANCE, factory, this, dialogService, stateManager),
                    SpecialFieldMenuItemFactory.getSpecialFieldSingleItem(SpecialField.QUALITY, factory, this, dialogService, stateManager),
                    SpecialFieldMenuItemFactory.getSpecialFieldSingleItem(SpecialField.PRINTED, factory, this, dialogService, stateManager),
                    SpecialFieldMenuItemFactory.createSpecialFieldMenu(SpecialField.PRIORITY, factory, this, dialogService, stateManager),
                    SpecialFieldMenuItemFactory.createSpecialFieldMenu(SpecialField.READ_STATUS, factory, this, dialogService, stateManager)
            );
        }

        // @formatter:off
        library.getItems().addAll(
                factory.createMenuItem(StandardActions.NEW_ENTRY, new NewEntryAction(this, dialogService, Globals.prefs, stateManager)),
                factory.createMenuItem(StandardActions.NEW_ENTRY_FROM_PLAIN_TEXT, new ExtractBibtexAction(stateManager)),
                factory.createMenuItem(StandardActions.DELETE_ENTRY, new EditAction(StandardActions.DELETE_ENTRY, this, stateManager)),

                new SeparatorMenuItem(),

                factory.createMenuItem(StandardActions.LIBRARY_PROPERTIES, new LibraryPropertiesAction(this, stateManager)),
                factory.createMenuItem(StandardActions.EDIT_PREAMBLE, new PreambleEditor(stateManager, undoManager, this.getDialogService())),
                factory.createMenuItem(StandardActions.EDIT_STRINGS, new BibtexStringEditorAction(stateManager)),
                factory.createMenuItem(StandardActions.MANAGE_CITE_KEY_PATTERNS, new BibtexKeyPatternAction(this, stateManager))
        );

        quality.getItems().addAll(
                factory.createMenuItem(StandardActions.FIND_DUPLICATES, new DuplicateSearch(this, dialogService, stateManager)),
                factory.createMenuItem(StandardActions.MERGE_ENTRIES, new MergeEntriesAction(this, dialogService, stateManager)),
                factory.createMenuItem(StandardActions.CHECK_INTEGRITY, new IntegrityCheckAction(this, stateManager, Globals.TASK_EXECUTOR)),
                factory.createMenuItem(StandardActions.CLEANUP_ENTRIES, new CleanupAction(this, this.prefs, dialogService, stateManager)),

                new SeparatorMenuItem(),

                factory.createMenuItem(StandardActions.SET_FILE_LINKS, new AutoLinkFilesAction(this, prefs, stateManager, undoManager, Globals.TASK_EXECUTOR)),

                new SeparatorMenuItem(),

                factory.createSubMenu(StandardActions.ABBREVIATE,
                        factory.createMenuItem(StandardActions.ABBREVIATE_DEFAULT, new AbbreviateAction(StandardActions.ABBREVIATE_DEFAULT, this, dialogService, stateManager)),
                        factory.createMenuItem(StandardActions.ABBREVIATE_MEDLINE, new AbbreviateAction(StandardActions.ABBREVIATE_MEDLINE, this, dialogService, stateManager)),
                        factory.createMenuItem(StandardActions.ABBREVIATE_SHORTEST_UNIQUE, new AbbreviateAction(StandardActions.ABBREVIATE_SHORTEST_UNIQUE, this, dialogService, stateManager))),

                factory.createMenuItem(StandardActions.UNABBREVIATE, new AbbreviateAction(StandardActions.UNABBREVIATE, this, dialogService, stateManager))
        );

        Menu lookupIdentifiers = factory.createSubMenu(StandardActions.LOOKUP_DOC_IDENTIFIER);
        for (IdFetcher<?> fetcher : WebFetchers.getIdFetchers(Globals.prefs.getImportFormatPreferences())) {
            LookupIdentifierAction<?> identifierAction = new LookupIdentifierAction<>(this, fetcher, stateManager, undoManager);
            lookupIdentifiers.getItems().add(factory.createMenuItem(identifierAction.getAction(), identifierAction));
        }

        lookup.getItems().addAll(
                lookupIdentifiers,
                factory.createMenuItem(StandardActions.DOWNLOAD_FULL_TEXT, new DownloadFullTextAction(dialogService, stateManager, prefs)),

                new SeparatorMenuItem(),

                factory.createMenuItem(StandardActions.FIND_UNLINKED_FILES, new FindUnlinkedFilesAction(this, stateManager))
        );

        // PushToApplication
        final PushToApplicationAction pushToApplicationAction = pushToApplicationsManager.getPushToApplicationAction();
        final MenuItem pushToApplicationMenuItem = factory.createMenuItem(pushToApplicationAction.getActionInformation(), pushToApplicationAction);
        pushToApplicationsManager.setMenuItem(pushToApplicationMenuItem);

        tools.getItems().addAll(
                factory.createMenuItem(StandardActions.PARSE_LATEX, new ParseLatexAction(stateManager)),
                factory.createMenuItem(StandardActions.NEW_SUB_LIBRARY_FROM_AUX, new NewSubLibraryAction(this, stateManager)),

                new SeparatorMenuItem(),

                factory.createMenuItem(StandardActions.WRITE_XMP, new WriteXMPAction(stateManager, dialogService)),
                factory.createMenuItem(StandardActions.COPY_LINKED_FILES, new CopyFilesAction(stateManager, this.getDialogService())),

                new SeparatorMenuItem(),

                factory.createMenuItem(StandardActions.SEND_AS_EMAIL, new SendAsEMailAction(dialogService, stateManager)),
                pushToApplicationMenuItem
        );

        SidePaneComponent webSearch = sidePaneManager.getComponent(SidePaneType.WEB_SEARCH);
        SidePaneComponent groups = sidePaneManager.getComponent(SidePaneType.GROUPS);
        SidePaneComponent openOffice = sidePaneManager.getComponent(SidePaneType.OPEN_OFFICE);

        view.getItems().add(new SeparatorMenuItem());
        view.setOnShowing(event -> {
            view.getItems().clear();
            view.getItems().addAll(
                    factory.createCheckMenuItem(webSearch.getToggleAction(), webSearch.getToggleCommand(), sidePaneManager.isComponentVisible(SidePaneType.WEB_SEARCH)),
                    factory.createCheckMenuItem(groups.getToggleAction(), groups.getToggleCommand(), sidePaneManager.isComponentVisible(SidePaneType.GROUPS)),
                    factory.createCheckMenuItem(openOffice.getToggleAction(), openOffice.getToggleCommand(), sidePaneManager.isComponentVisible(SidePaneType.OPEN_OFFICE)),

                    new SeparatorMenuItem(),

                    factory.createMenuItem(StandardActions.NEXT_PREVIEW_STYLE, new PreviewSwitchAction(PreviewSwitchAction.Direction.NEXT, this, stateManager)),
                    factory.createMenuItem(StandardActions.PREVIOUS_PREVIEW_STYLE, new PreviewSwitchAction(PreviewSwitchAction.Direction.PREVIOUS, this, stateManager)),

                    new SeparatorMenuItem(),

                    factory.createMenuItem(StandardActions.SHOW_PDF_VIEWER, new ShowDocumentViewerAction(stateManager, prefs)),
                    factory.createMenuItem(StandardActions.EDIT_ENTRY, new OpenEntryEditorAction(this, stateManager)),
                    factory.createMenuItem(StandardActions.OPEN_CONSOLE, new OpenConsoleAction(stateManager))
            );
        });

        options.getItems().addAll(
                factory.createMenuItem(StandardActions.SHOW_PREFS, new ShowPreferencesAction(this, Globals.TASK_EXECUTOR)),

                new SeparatorMenuItem(),

                factory.createMenuItem(StandardActions.SETUP_GENERAL_FIELDS, new SetupGeneralFieldsAction()),
                factory.createMenuItem(StandardActions.MANAGE_CUSTOM_IMPORTS, new ManageCustomImportsAction()),
                factory.createMenuItem(StandardActions.MANAGE_CUSTOM_EXPORTS, new ManageCustomExportsAction()),
                factory.createMenuItem(StandardActions.MANAGE_EXTERNAL_FILETYPES, new EditExternalFileTypesAction()),
                factory.createMenuItem(StandardActions.MANAGE_JOURNALS, new ManageJournalsAction()),
                factory.createMenuItem(StandardActions.CUSTOMIZE_KEYBINDING, new CustomizeKeyBindingAction()),
                factory.createMenuItem(StandardActions.MANAGE_PROTECTED_TERMS, new ManageProtectedTermsAction()),

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

    public void addParserResult(ParserResult parserResult, boolean focusPanel) {
        if (parserResult.toOpenTab()) {
            // Add the entries to the open tab.
            BasePanel panel = getCurrentBasePanel();
            if (panel == null) {
                // There is no open tab to add to, so we create a new tab:
                addTab(parserResult.getDatabaseContext(), focusPanel);
            } else {
                addImportedEntries(panel, parserResult);
            }
        } else {
            // only add tab if DB is not already open
            Optional<BasePanel> panel = getBasePanelList().stream()
                                                          .filter(p -> p.getBibDatabaseContext().getDatabasePath().equals(parserResult.getFile()))
                                                          .findFirst();

            if (panel.isPresent()) {
                tabbedPane.getSelectionModel().select(getTab(panel.get()));
            } else {
                addTab(parserResult.getDatabaseContext(), focusPanel);
            }
        }
    }

    /**
     * This method causes all open BasePanels to set up their tables anew. When called from PrefsDialog3, this updates
     * to the new settings.
     */
    public void setupAllTables() {
        // This action can be invoked without an open database, so
        // we have to check if we have one before trying to invoke
        // methods to execute changes in the preferences.

        // We want to notify all tabs about the changes to
        // avoid problems when changing the column set.
        for (int i = 0; i < tabbedPane.getTabs().size(); i++) {
            BasePanel bf = getBasePanelAt(i);

            // Update tables:
            if (bf.getDatabase() != null) {
                DefaultTaskExecutor.runInJavaFXThread(bf::setupMainPanel);
            }
        }
    }

    private List<String> collectDatabaseFilePaths() {
        List<String> dbPaths = new ArrayList<>(getBasePanelCount());

        for (BasePanel basePanel : getBasePanelList()) {
            // db file exists
            if (basePanel.getBibDatabaseContext().getDatabasePath().isPresent()) {
                dbPaths.add(basePanel.getBibDatabaseContext().getDatabasePath().get().toAbsolutePath().toString());
            } else {
                dbPaths.add("");
            }
        }
        return dbPaths;
    }

    private List<String> getUniquePathParts() {
        List<String> dbPaths = collectDatabaseFilePaths();

        return FileUtil.uniquePathSubstrings(dbPaths);
    }

    public void updateAllTabTitles() {
        List<String> paths = getUniquePathParts();
        for (int i = 0; i < getBasePanelCount(); i++) {
            String uniqPath = paths.get(i);
            Optional<Path> file = getBasePanelAt(i).getBibDatabaseContext().getDatabasePath();

            if (file.isPresent()) {
                if (!uniqPath.equals(file.get().getFileName()) && uniqPath.contains(File.separator)) {
                    // remove filename
                    uniqPath = uniqPath.substring(0, uniqPath.lastIndexOf(File.separator));
                    tabbedPane.getTabs().get(i).setText(getBasePanelAt(i).getTabTitle() + " \u2014 " + uniqPath);
                } else {
                    // set original filename (again)
                    tabbedPane.getTabs().get(i).setText(getBasePanelAt(i).getTabTitle());
                }
            } else {
                tabbedPane.getTabs().get(i).setText(getBasePanelAt(i).getTabTitle());
            }
            tabbedPane.getTabs().get(i).setTooltip(new Tooltip(file.map(Path::toAbsolutePath).map(Path::toString).orElse(null)));
        }
    }

    public void addTab(BasePanel basePanel, boolean raisePanel) {
        // add tab
        Tab newTab = new Tab(basePanel.getTabTitle(), basePanel);
        tabbedPane.getTabs().add(newTab);
        newTab.setOnCloseRequest(event -> {
            closeTab((BasePanel) newTab.getContent());
            event.consume();
        });

        // update all tab titles
        updateAllTabTitles();

        if (raisePanel) {
            tabbedPane.getSelectionModel().select(newTab);
        }

        // Register undo/redo listener
        basePanel.getUndoManager().registerListener(new UndoRedoEventManager());

        BibDatabaseContext context = basePanel.getBibDatabaseContext();

        if (readyForAutosave(context)) {
            AutosaveManager autosaver = AutosaveManager.start(context);
            autosaver.registerListener(new AutosaveUiManager(basePanel));
        }

        BackupManager.start(context, Globals.entryTypesManager, prefs);

        // Track opening
        trackOpenNewDatabase(basePanel);
    }

    private void trackOpenNewDatabase(BasePanel basePanel) {
        Map<String, String> properties = new HashMap<>();
        Map<String, Double> measurements = new HashMap<>();
        measurements.put("NumberOfEntries", (double) basePanel.getBibDatabaseContext().getDatabase().getEntryCount());

        Globals.getTelemetryClient().ifPresent(client -> client.trackEvent("OpenNewDatabase", properties, measurements));
    }

    public BasePanel addTab(BibDatabaseContext databaseContext, boolean raisePanel) {
        Objects.requireNonNull(databaseContext);

        BasePanel bp = new BasePanel(this, BasePanelPreferences.from(Globals.prefs), databaseContext, ExternalFileTypes.getInstance());
        addTab(bp, raisePanel);
        return bp;
    }

    private boolean readyForAutosave(BibDatabaseContext context) {
        return ((context.getLocation() == DatabaseLocation.SHARED) ||
                ((context.getLocation() == DatabaseLocation.LOCAL) && Globals.prefs.getBoolean(JabRefPreferences.LOCAL_AUTO_SAVE)))
                &&
                context.getDatabasePath().isPresent();
    }

    /**
     * Opens the import inspection dialog to let the user decide which of the given entries to import.
     *
     * @param panel        The BasePanel to add to.
     * @param parserResult The entries to add.
     */
    private void addImportedEntries(final BasePanel panel, final ParserResult parserResult) {
        BackgroundTask<ParserResult> task = BackgroundTask.wrap(() -> parserResult);
        ImportEntriesDialog dialog = new ImportEntriesDialog(panel.getBibDatabaseContext(), task);
        dialog.setTitle(Localization.lang("Import"));
        dialog.showAndWait();
    }

    public FileHistoryMenu getFileHistory() {
        return fileHistory;
    }

    /**
     * Set the visibility of the progress bar in the right end of the status line at the bottom of the frame.
     */
    public void setProgressBarVisible(final boolean visible) {
        progressBar.setVisible(visible);
    }

    /**
     * Sets the indeterminate status of the progress bar.
     * <p>
     */
    public void setProgressBarIndeterminate(final boolean value) {
        progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
    }

    /**
     * Return a boolean, if the selected entry have file
     *
     * @param selectEntryList A selected entries list of the current base pane
     * @return true, if the selected entry contains file. false, if multiple entries are selected or the selected entry
     * doesn't contains file
     */
    private boolean isExistFile(List<BibEntry> selectEntryList) {
        if (selectEntryList.size() == 1) {
            BibEntry selectedEntry = selectEntryList.get(0);
            return selectedEntry.getField(StandardField.FILE).isPresent();
        }
        return false;
    }

    /**
     * Return a boolean, if the selected entry have url or doi
     *
     * @param selectEntryList A selected entries list of the current base pane
     * @return true, if the selected entry contains url or doi. false, if multiple entries are selected or the selected
     * entry doesn't contains url or doi
     */
    private boolean isExistURLorDOI(List<BibEntry> selectEntryList) {
        if (selectEntryList.size() == 1) {
            BibEntry selectedEntry = selectEntryList.get(0);
            return (selectedEntry.getField(StandardField.URL).isPresent() || selectedEntry.getField(StandardField.DOI).isPresent());
        }
        return false;
    }

    /**
     * Ask if the user really wants to close the given database
     *
     * @return true if the user choose to close the database
     */
    private boolean confirmClose(BasePanel panel) {
        String filename = panel.getBibDatabaseContext()
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
                SaveDatabaseAction saveAction = new SaveDatabaseAction(panel, Globals.prefs, Globals.entryTypesManager);
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

    private void closeTab(BasePanel panel) {
        // empty tab without database
        if (panel == null) {
            return;
        }

        final BibDatabaseContext context = panel.getBibDatabaseContext();

        if (panel.isModified() && (context.getLocation() == DatabaseLocation.LOCAL)) {
            if (confirmClose(panel)) {
                removeTab(panel);
            } else {
                return;
            }
        } else if (context.getLocation() == DatabaseLocation.SHARED) {
            context.convertToLocalDatabase();
            context.getDBMSSynchronizer().closeSharedDatabase();
            context.clearDBMSSynchronizer();
            removeTab(panel);
        } else {
            removeTab(panel);
        }
        AutosaveManager.shutdown(context);
        BackupManager.shutdown(context);
    }

    private void removeTab(BasePanel panel) {
        DefaultTaskExecutor.runInJavaFXThread(() -> {
            panel.cleanUp();
            tabbedPane.getTabs().remove(getTab(panel));
            setWindowTitle();
            // update tab titles
            updateAllTabTitles();
        });
    }

    public void closeCurrentTab() {
        removeTab(getCurrentBasePanel());
    }

    public OpenDatabaseAction getOpenDatabaseAction() {
        return new OpenDatabaseAction(this);
    }

    public SidePaneManager getSidePaneManager() {
        return sidePaneManager;
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

        @Override
        public void execute() {
            closeTab(getCurrentBasePanel());
        }
    }

    private class UndoRedoEventManager {

        @Subscribe
        public void listen(UndoRedoEvent event) {
            updateTexts(event);
            JabRefFrame.this.getCurrentBasePanel().updateEntryEditorIfShowing();
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
