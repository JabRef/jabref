package org.jabref.gui;

import java.awt.Component;
import java.awt.Window;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TimerTask;
import java.util.stream.Collectors;

import javax.swing.Action;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.input.DataFormat;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;
import javafx.util.Duration;

import org.jabref.Globals;
import org.jabref.JabRefExecutorService;
import org.jabref.gui.actions.ActionFactory;
import org.jabref.gui.actions.Actions;
import org.jabref.gui.actions.AutoLinkFilesAction;
import org.jabref.gui.actions.BibtexKeyPatternAction;
import org.jabref.gui.actions.ConnectToSharedDatabaseCommand;
import org.jabref.gui.actions.CopyFilesAction;
import org.jabref.gui.actions.CustomizeEntryAction;
import org.jabref.gui.actions.CustomizeKeyBindingAction;
import org.jabref.gui.actions.DatabasePropertiesAction;
import org.jabref.gui.actions.EditExternalFileTypesAction;
import org.jabref.gui.actions.ErrorConsoleAction;
import org.jabref.gui.actions.LookupIdentifierAction;
import org.jabref.gui.actions.ManageCustomExportsAction;
import org.jabref.gui.actions.ManageCustomImportsAction;
import org.jabref.gui.actions.ManageJournalsAction;
import org.jabref.gui.actions.ManageKeywordsAction;
import org.jabref.gui.actions.ManageProtectedTermsAction;
import org.jabref.gui.actions.NewDatabaseAction;
import org.jabref.gui.actions.NewEntryAction;
import org.jabref.gui.actions.NewEntryFromPlainTextAction;
import org.jabref.gui.actions.NewSubLibraryAction;
import org.jabref.gui.actions.OldDatabaseCommandWrapper;
import org.jabref.gui.actions.OpenBrowserAction;
import org.jabref.gui.actions.SearchForUpdateAction;
import org.jabref.gui.actions.SetupGeneralFieldsAction;
import org.jabref.gui.actions.ShowDocumentViewerAction;
import org.jabref.gui.actions.ShowPreferencesAction;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.dialogs.AutosaveUIManager;
import org.jabref.gui.edit.MassSetFieldsAction;
import org.jabref.gui.exporter.ExportCommand;
import org.jabref.gui.exporter.ExportToClipboardAction;
import org.jabref.gui.exporter.SaveAllAction;
import org.jabref.gui.exporter.SaveDatabaseAction;
import org.jabref.gui.externalfiles.FindUnlinkedFilesAction;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.help.AboutAction;
import org.jabref.gui.help.HelpAction;
import org.jabref.gui.importer.ImportCommand;
import org.jabref.gui.importer.ImportInspectionDialog;
import org.jabref.gui.importer.actions.OpenDatabaseAction;
import org.jabref.gui.integrity.IntegrityCheckAction;
import org.jabref.gui.keyboard.KeyBinding;
import org.jabref.gui.menus.FileHistoryMenu;
import org.jabref.gui.mergeentries.MergeEntriesAction;
import org.jabref.gui.push.PushToApplicationButton;
import org.jabref.gui.push.PushToApplications;
import org.jabref.gui.search.GlobalSearchBar;
import org.jabref.gui.specialfields.SpecialFieldMenuItemFactory;
import org.jabref.gui.undo.CountingUndoManager;
import org.jabref.gui.util.DefaultTaskExecutor;
import org.jabref.logic.autosaveandbackup.AutosaveManager;
import org.jabref.logic.autosaveandbackup.BackupManager;
import org.jabref.logic.importer.IdFetcher;
import org.jabref.logic.importer.OpenDatabase;
import org.jabref.logic.importer.OutputPrinter;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.WebFetchers;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.search.SearchQuery;
import org.jabref.logic.undo.AddUndoableActionEvent;
import org.jabref.logic.undo.UndoChangeEvent;
import org.jabref.logic.undo.UndoRedoEvent;
import org.jabref.logic.util.OS;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.database.shared.DatabaseLocation;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BiblatexEntryTypes;
import org.jabref.model.entry.BibtexEntryTypes;
import org.jabref.model.entry.FieldName;
import org.jabref.model.entry.specialfields.SpecialField;
import org.jabref.preferences.JabRefPreferences;
import org.jabref.preferences.LastFocusedTabPreferences;
import org.jabref.preferences.SearchPreferences;

import com.google.common.eventbus.Subscribe;
import com.jfoenix.controls.JFXSnackbar;
import com.jfoenix.controls.JFXSnackbar.SnackbarEvent;
import com.jfoenix.controls.JFXSnackbarLayout;
import org.eclipse.fx.ui.controls.tabpane.DndTabPane;
import org.eclipse.fx.ui.controls.tabpane.DndTabPaneFactory;
import org.fxmisc.easybind.EasyBind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import osx.macadapter.MacAdapter;

/**
 * The main window of the application.
 */
public class JabRefFrame extends BorderPane implements OutputPrinter {

    // Frame titles.
    public static final String FRAME_TITLE = "JabRef";

    private static final Logger LOGGER = LoggerFactory.getLogger(JabRefFrame.class);
    private static final Duration TOAST_MESSAGE_DISPLAY_TIME = Duration.millis(3000);

    private final SplitPane splitPane = new SplitPane();
    private final JabRefPreferences prefs = Globals.prefs;
    private final GlobalSearchBar globalSearchBar = new GlobalSearchBar(this);
    private final JFXSnackbar statusLine = new JFXSnackbar(this);
    private final ProgressBar progressBar = new ProgressBar();
    private final FileHistoryMenu fileHistory = new FileHistoryMenu(prefs, this);

    // Lists containing different subsets of actions for different purposes
    private final List<Object> specialFieldButtons = new LinkedList<>();
    private final List<Object> openDatabaseOnlyActions = new LinkedList<>();
    private final List<Object> severalDatabasesOnlyActions = new LinkedList<>();
    private final List<Object> openAndSavedDatabasesOnlyActions = new LinkedList<>();
    private final List<Object> sharedDatabaseOnlyActions = new LinkedList<>();
    private final List<Object> noSharedDatabaseActions = new LinkedList<>();
    private final List<Object> oneEntryOnlyActions = new LinkedList<>();
    private final List<Object> oneEntryWithFileOnlyActions = new LinkedList<>();
    private final List<Object> oneEntryWithURLorDOIOnlyActions = new LinkedList<>();
    private final List<Object> twoEntriesOnlyActions = new LinkedList<>();
    private final List<Object> atLeastOneEntryActions = new LinkedList<>();
    private final Stage mainStage;
    // The sidepane manager takes care of populating the sidepane.
    private SidePaneManager sidePaneManager;
    private TabPane tabbedPane;
    private PushToApplications pushApplications;
    private final CountingUndoManager undoManager = new CountingUndoManager();
    private final DialogService dialogService;
    private SidePane sidePane;

    public JabRefFrame(Stage mainStage) {
        this.mainStage = mainStage;
        this.dialogService = new FXDialogService(mainStage);
        init();
    }

    /**
     * Takes a list of Object and calls the method setEnabled on them, depending on whether it is an Action or a
     * Component.
     *
     * @param list List that should contain Actions and Components.
     */
    private static void setEnabled(List<Object> list, boolean enabled) {
        for (Object actionOrComponent : list) {
            if (actionOrComponent instanceof Action) {
                ((Action) actionOrComponent).setEnabled(enabled);
            }
            if (actionOrComponent instanceof Component) {
                ((Component) actionOrComponent).setEnabled(enabled);
                if (actionOrComponent instanceof JPanel) {
                    JPanel root = (JPanel) actionOrComponent;
                    for (int index = 0; index < root.getComponentCount(); index++) {
                        root.getComponent(index).setEnabled(enabled);
                    }
                }
            }
        }
    }

    private void init() {
        sidePaneManager = new SidePaneManager(Globals.prefs, this);
        sidePane = sidePaneManager.getPane();

        Pane containerPane = DndTabPaneFactory.createDefaultDnDPane(DndTabPaneFactory.FeedbackType.MARKER, null);
        tabbedPane = (DndTabPane) containerPane.getChildren().get(0);

        initLayout();

        initActions();

        initKeyBindings();

        tabbedPane.setOnDragOver(event -> {
            if (event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY, TransferMode.MOVE, TransferMode.LINK);
            }
        });

        tabbedPane.setOnDragDropped(event -> {
            boolean success = false;

            if (event.getDragboard().hasContent(DataFormat.FILES)) {
                List<Path> files = event.getDragboard().getFiles().stream().map(File::toPath).filter(FileUtil::isBibFile).collect(Collectors.toList());
                success = true;

                for (Path file : files) {
                    ParserResult pr = OpenDatabase.loadDatabase(file.toString(), Globals.prefs.getImportFormatPreferences(), Globals.getFileUpdateMonitor());
                    addParserResult(pr, true);
                }
            }

            event.setDropCompleted(success);
            event.consume();
        });

        //setBounds(GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds());
        //WindowLocation pw = new WindowLocation(this, JabRefPreferences.POS_X, JabRefPreferences.POS_Y, JabRefPreferences.SIZE_X,
        //        JabRefPreferences.SIZE_Y);
        //pw.displayWindowAtStoredLocation();

        /*
         * The following state listener makes sure focus is registered with the
         * correct database when the user switches tabs. Without this,
         * cut/paste/copy operations would some times occur in the wrong tab.
         */
        EasyBind.subscribe(tabbedPane.getSelectionModel().selectedItemProperty(), e -> {
            if (e == null) {
                Globals.stateManager.activeDatabaseProperty().setValue(Optional.empty());
                return;
            }

            BasePanel currentBasePanel = getCurrentBasePanel();
            if (currentBasePanel == null) {
                return;
            }

            // Poor-mans binding to global state
            // We need to invoke this in the JavaFX thread as all the listeners sit there
            Platform.runLater(() -> Globals.stateManager.activeDatabaseProperty().setValue(Optional.of(currentBasePanel.getBibDatabaseContext())));
            if (new SearchPreferences(Globals.prefs).isGlobalSearch()) {
                globalSearchBar.performSearch();
            } else {
                String content = "";
                Optional<SearchQuery> currentSearchQuery = currentBasePanel.getCurrentSearchQuery();
                if (currentSearchQuery.isPresent()) {
                    content = currentSearchQuery.get().getQuery();
                }
                globalSearchBar.setSearchTerm(content);
            }

            currentBasePanel.getPreviewPanel().updateLayout(Globals.prefs.getPreviewPreferences());

            // groupSidePane.getToggleCommand().setSelected(sidePaneManager.isComponentVisible(GroupSidePane.class));
            //previewToggle.setSelected(Globals.prefs.getPreviewPreferences().isPreviewPanelEnabled());
            //generalFetcher.getToggleCommand().setSelected(sidePaneManager.isComponentVisible(WebSearchPane.class));
            //openOfficePanel.getToggleCommand().setSelected(sidePaneManager.isComponentVisible(OpenOfficeSidePanel.class));

            setWindowTitle();
            // Update search autocompleter with information for the correct database:
            currentBasePanel.updateSearchManager();

            currentBasePanel.getUndoManager().postUndoRedoEvent();
            currentBasePanel.getMainTable().requestFocus();
        });

        //Note: The registration of Apple event is at the end of initialization, because
        //if the events happen too early (ie when the window is not initialized yet), the
        //opened (double-clicked) documents are not displayed.
        if (OS.OS_X) {
            try {
                new MacAdapter().registerMacEvents(this);
            } catch (Exception e) {
                LOGGER.error("Could not interface with Mac OS X methods.", e);
            }
        }

        initShowTrackingNotification();
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
                    case INCREASE_TABLE_FONT_SIZE:
                        increaseTableFontSize();
                        event.consume();
                        break;
                    case DECREASE_TABLE_FONT_SIZE:
                        decreaseTableFontSize();
                        event.consume();
                        break;
                    case DEFAULT_TABLE_FONT_SIZE:
                        setDefaultTableFontSize();
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
                    SwingUtilities.invokeLater(() -> {
                        DefaultTaskExecutor.runInJavaFXThread(JabRefFrame.this::showTrackingNotification);
                    });
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
            //setTitle(FRAME_TITLE);
            return;
        }

        String mode = panel.getBibDatabaseContext().getMode().getFormattedName();
        String modeInfo = String.format(" (%s)", Localization.lang("%0 mode", mode));
        boolean isAutosaveEnabled = Globals.prefs.getBoolean(JabRefPreferences.LOCAL_AUTO_SAVE);

        if (panel.getBibDatabaseContext().getLocation() == DatabaseLocation.LOCAL) {
            String changeFlag = panel.isModified() && !isAutosaveEnabled ? "*" : "";
            String databaseFile = panel.getBibDatabaseContext()
                                       .getDatabaseFile()
                                       .map(File::getPath)
                                       .orElse(GUIGlobals.UNTITLED_TITLE);
            //setTitle(FRAME_TITLE + " - " + databaseFile + changeFlag + modeInfo);
        } else if (panel.getBibDatabaseContext().getLocation() == DatabaseLocation.SHARED) {
            //setTitle(FRAME_TITLE + " - " + panel.getBibDatabaseContext().getDBMSSynchronizer().getDBName() + " ["
            //        + Localization.lang("shared") + "]" + modeInfo);
        }
    }

    /**
     * The MacAdapter calls this method when a "BIB" file has been double-clicked from the Finder.
     */
    public void openAction(String filePath) {
        Path file = Paths.get(filePath);
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
        //prefs.putBoolean(JabRefPreferences.WINDOW_MAXIMISED, getExtendedState() == Frame.MAXIMIZED_BOTH);

        if (prefs.getBoolean(JabRefPreferences.OPEN_LAST_EDITED)) {
            // Here we store the names of all current files. If
            // there is no current file, we remove any
            // previously stored filename.
            if (filenames.isEmpty()) {
                prefs.remove(JabRefPreferences.LAST_EDITED);
            } else {
                prefs.putStringList(JabRefPreferences.LAST_EDITED, filenames);
                File focusedDatabase = getCurrentBasePanel().getBibDatabaseContext().getDatabaseFile().orElse(null);
                new LastFocusedTabPreferences(prefs).setLastFocusedTab(focusedDatabase);
            }
        }

        fileHistory.storeHistory();
        prefs.flush();

        // dispose all windows, even if they are not displayed anymore
        for (Window window : Window.getWindows()) {
            window.dispose();
        }
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
            BibDatabaseContext context = panel.getBibDatabaseContext();

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
            context.getDatabaseFile().map(File::getAbsolutePath).ifPresent(filenames::add);
        }

        // Wait for save operations to finish
        for (int i = 0; i < tabbedPane.getTabs().size(); i++) {
            if (getBasePanelAt(i).isSaving()) {
                // There is a database still being saved, so we need to wait.
                WaitForSaveOperation w = new WaitForSaveOperation(this);
                w.show(); // This method won't return until canceled or the save operation is done.
                if (w.canceled()) {
                    return false; // The user clicked cancel.
                }
            }
        }

        // Good bye!
        tearDownJabRef(filenames);
        Platform.exit();
        return true;
    }

    private void initLayout() {
        setProgressBarVisible(false);

        pushApplications = new PushToApplications(this.getDialogService());

        BorderPane head = new BorderPane();
        head.setTop(createMenu());
        head.setCenter(createToolbar());
        setTop(head);

        SplitPane.setResizableWithParent(sidePane, Boolean.FALSE);
        splitPane.getItems().addAll(sidePane, tabbedPane);

        // We need to wait with setting the divider since it gets reset a few times during the initial set-up
        mainStage.showingProperty().addListener(new ChangeListener<Boolean>() {

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

        /*
        GridBagLayout gbl = new GridBagLayout();
        GridBagConstraints con = new GridBagConstraints();
        con.fill = GridBagConstraints.BOTH;
        con.anchor = GridBagConstraints.WEST;
        JPanel status = new JPanel();
        status.setLayout(gbl);
        con.weighty = 0;
        con.weightx = 0;
        con.gridwidth = 1;
        con.insets = new Insets(0, 2, 0, 0);
        gbl.setConstraints(statusLabel, con);
        status.add(statusLabel);
        con.weightx = 1;
        con.insets = new Insets(0, 4, 0, 0);
        con.gridwidth = 1;
        gbl.setConstraints(statusLine, con);
        status.add(statusLine);
        con.weightx = 0;
        con.gridwidth = GridBagConstraints.REMAINDER;
        con.insets = new Insets(2, 4, 2, 2);
        gbl.setConstraints(progressBar, con);
        status.add(progressBar);
        statusLabel.setForeground(GUIGlobals.ENTRY_EDITOR_LABEL_COLOR.darker());
        */
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
                factory.createIconButton(StandardActions.SAVE_LIBRARY, new OldDatabaseCommandWrapper(Actions.SAVE, this, Globals.stateManager)),
                leftSpacer
        );
        leftSide.setMinWidth(100);
        leftSide.prefWidthProperty().bind(sidePane.widthProperty());
        leftSide.maxWidthProperty().bind(sidePane.widthProperty());

        PushToApplicationButton pushToExternal = new PushToApplicationButton(this, pushApplications.getApplications());
        HBox rightSide = new HBox(
                factory.createIconButton(StandardActions.NEW_ARTICLE, new NewEntryAction(this, BiblatexEntryTypes.ARTICLE, dialogService, Globals.prefs)),
                factory.createIconButton(StandardActions.DELETE_ENTRY, new OldDatabaseCommandWrapper(Actions.DELETE, this, Globals.stateManager)),

                factory.createIconButton(StandardActions.UNDO, new OldDatabaseCommandWrapper(Actions.UNDO, this, Globals.stateManager)),
                factory.createIconButton(StandardActions.REDO, new OldDatabaseCommandWrapper(Actions.REDO, this, Globals.stateManager)),
                factory.createIconButton(StandardActions.CUT, new OldDatabaseCommandWrapper(Actions.CUT, this, Globals.stateManager)),
                factory.createIconButton(StandardActions.COPY, new OldDatabaseCommandWrapper(Actions.COPY, this, Globals.stateManager)),
                factory.createIconButton(StandardActions.PASTE, new OldDatabaseCommandWrapper(Actions.PASTE, this, Globals.stateManager)),

                factory.createIconButton(StandardActions.CLEANUP_ENTRIES, new OldDatabaseCommandWrapper(Actions.CLEANUP, this, Globals.stateManager)),
                factory.createIconButton(pushToExternal.getMenuAction(), pushToExternal),

                factory.createIconButton(StandardActions.FORK_ME, new OpenBrowserAction("https://github.com/JabRef/jabref")),
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

    /**
     * Returns the currently viewed BasePanel.
     */
    public BasePanel getCurrentBasePanel() {
        if ((tabbedPane == null) || (tabbedPane.getSelectionModel().getSelectedItem() == null)) {
            return null;
        }
        return (BasePanel) tabbedPane.getSelectionModel().getSelectedItem().getContent();
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
        Menu view = new Menu(Localization.lang("View"));
        Menu tools = new Menu(Localization.lang("Tools"));
        Menu options = new Menu(Localization.lang("Options"));
        Menu help = new Menu(Localization.lang("Help"));

        file.getItems().addAll(
                factory.createMenuItem(StandardActions.NEW_LIBRARY_BIBTEX, new NewDatabaseAction(this, BibDatabaseMode.BIBTEX)),
                factory.createMenuItem(StandardActions.NEW_LIBRARY_BIBLATEX, new NewDatabaseAction(this, BibDatabaseMode.BIBLATEX)),
                factory.createMenuItem(StandardActions.OPEN_LIBRARY, getOpenDatabaseAction()),
                factory.createMenuItem(StandardActions.SAVE_LIBRARY, new OldDatabaseCommandWrapper(Actions.SAVE, this, Globals.stateManager)),
                factory.createMenuItem(StandardActions.SAVE_LIBRARY_AS, new OldDatabaseCommandWrapper(Actions.SAVE_AS, this, Globals.stateManager)),
                factory.createMenuItem(StandardActions.SAVE_ALL, new SaveAllAction(this)),

                factory.createSubMenu(StandardActions.IMPORT_EXPORT,
                        factory.createMenuItem(StandardActions.MERGE_DATABASE, new OldDatabaseCommandWrapper(Actions.MERGE_DATABASE, this, Globals.stateManager)), // TODO: merge with import
                        factory.createMenuItem(StandardActions.IMPORT_INTO_CURRENT_LIBRARY, new ImportCommand(this, false)),
                        factory.createMenuItem(StandardActions.IMPORT_INTO_NEW_LIBRARY, new ImportCommand(this, true)),
                        factory.createMenuItem(StandardActions.EXPORT_ALL, new ExportCommand(this, false, Globals.prefs)),
                        factory.createMenuItem(StandardActions.EXPORT_SELECTED, new ExportCommand(this, true, Globals.prefs)),
                        factory.createMenuItem(StandardActions.SAVE_SELECTED_AS_PLAIN_BIBTEX, new OldDatabaseCommandWrapper(Actions.SAVE_SELECTED_AS_PLAIN, this, Globals.stateManager))),

                new SeparatorMenuItem(),

                factory.createMenuItem(StandardActions.CONNECT_TO_SHARED_DB, new ConnectToSharedDatabaseCommand(this)),
                factory.createMenuItem(StandardActions.PULL_CHANGES_FROM_SHARED_DB, new OldDatabaseCommandWrapper(Actions.PULL_CHANGES_FROM_SHARED_DATABASE, this, Globals.stateManager)),

                new SeparatorMenuItem(),

                fileHistory,

                new SeparatorMenuItem(),

                factory.createMenuItem(StandardActions.CLOSE_LIBRARY, new CloseDatabaseAction()),
                factory.createMenuItem(StandardActions.QUIT, new CloseAction())

        );

        edit.getItems().addAll(
                factory.createMenuItem(StandardActions.UNDO, new OldDatabaseCommandWrapper(Actions.UNDO, this, Globals.stateManager)),
                factory.createMenuItem(StandardActions.REDO, new OldDatabaseCommandWrapper(Actions.REDO, this, Globals.stateManager)),

                new SeparatorMenuItem(),

                factory.createMenuItem(StandardActions.CUT, new EditAction(Actions.CUT)),

                factory.createMenuItem(StandardActions.COPY, new EditAction(Actions.COPY)),
                factory.createSubMenu(StandardActions.COPY_MORE,
                        factory.createMenuItem(StandardActions.COPY_TITLE, new OldDatabaseCommandWrapper(Actions.COPY_TITLE, this, Globals.stateManager)),
                        factory.createMenuItem(StandardActions.COPY_KEY, new OldDatabaseCommandWrapper(Actions.COPY_KEY, this, Globals.stateManager)),
                        factory.createMenuItem(StandardActions.COPY_CITE_KEY, new OldDatabaseCommandWrapper(Actions.COPY_CITE_KEY, this, Globals.stateManager)),
                        factory.createMenuItem(StandardActions.COPY_KEY_AND_TITLE, new OldDatabaseCommandWrapper(Actions.COPY_KEY_AND_TITLE, this, Globals.stateManager)),
                        factory.createMenuItem(StandardActions.COPY_KEY_AND_LINK, new OldDatabaseCommandWrapper(Actions.COPY_KEY_AND_LINK, this, Globals.stateManager)),
                        factory.createMenuItem(StandardActions.COPY_CITATION_PREVIEW, new OldDatabaseCommandWrapper(Actions.COPY_CITATION_HTML, this, Globals.stateManager)),
                        factory.createMenuItem(StandardActions.EXPORT_SELECTED_TO_CLIPBOARD, new ExportToClipboardAction(this, dialogService))),

                factory.createMenuItem(StandardActions.PASTE, new EditAction(Actions.PASTE)),

                new SeparatorMenuItem(),

                factory.createMenuItem(StandardActions.SEND_AS_EMAIL, new OldDatabaseCommandWrapper(Actions.SEND_AS_EMAIL, this, Globals.stateManager)),

                new SeparatorMenuItem()

        );

        if (Globals.prefs.getBoolean(JabRefPreferences.SPECIALFIELDSENABLED)) {
            boolean menuItemAdded = false;
            if (Globals.prefs.getBoolean(JabRefPreferences.SHOWCOLUMN_RANKING)) {
                edit.getItems().add(SpecialFieldMenuItemFactory.createSpecialFieldMenuForActiveDatabase(SpecialField.RANKING, factory, undoManager));
                menuItemAdded = true;
            }

            if (Globals.prefs.getBoolean(JabRefPreferences.SHOWCOLUMN_RELEVANCE)) {
                edit.getItems().add(SpecialFieldMenuItemFactory.getSpecialFieldSingleItemForActiveDatabase(SpecialField.RELEVANCE, factory));
                menuItemAdded = true;
            }

            if (Globals.prefs.getBoolean(JabRefPreferences.SHOWCOLUMN_QUALITY)) {
                edit.getItems().add(SpecialFieldMenuItemFactory.getSpecialFieldSingleItemForActiveDatabase(SpecialField.QUALITY, factory));
                menuItemAdded = true;
            }

            if (Globals.prefs.getBoolean(JabRefPreferences.SHOWCOLUMN_PRINTED)) {
                edit.getItems().add(SpecialFieldMenuItemFactory.getSpecialFieldSingleItemForActiveDatabase(SpecialField.PRINTED, factory));
                menuItemAdded = true;
            }

            if (Globals.prefs.getBoolean(JabRefPreferences.SHOWCOLUMN_PRIORITY)) {
                edit.getItems().add(SpecialFieldMenuItemFactory.createSpecialFieldMenuForActiveDatabase(SpecialField.PRIORITY, factory, undoManager));
                menuItemAdded = true;
            }

            if (Globals.prefs.getBoolean(JabRefPreferences.SHOWCOLUMN_READ)) {
                edit.getItems().add(SpecialFieldMenuItemFactory.createSpecialFieldMenuForActiveDatabase(SpecialField.READ_STATUS, factory, undoManager));
                menuItemAdded = true;
            }

            if (menuItemAdded) {
                edit.getItems().add(new SeparatorMenuItem());
            }
        }

        edit.getItems().addAll(
                factory.createMenuItem(StandardActions.MANAGE_KEYWORDS, new ManageKeywordsAction(this)),
                factory.createMenuItem(StandardActions.REPLACE_ALL, new OldDatabaseCommandWrapper(Actions.REPLACE_ALL, this, Globals.stateManager)),
                factory.createMenuItem(StandardActions.MASS_SET_FIELDS, new MassSetFieldsAction(this))

        );

        library.getItems().addAll(
                factory.createMenuItem(StandardActions.NEW_ARTICLE, new NewEntryAction(this, BibtexEntryTypes.ARTICLE, dialogService, Globals.prefs)),
                factory.createMenuItem(StandardActions.NEW_ENTRY, new NewEntryAction(this, dialogService, Globals.prefs)),
                factory.createMenuItem(StandardActions.NEW_ENTRY_FROM_PLAINTEX, new NewEntryFromPlainTextAction(this, Globals.prefs.getUpdateFieldPreferences(), dialogService, Globals.prefs)),

                new SeparatorMenuItem(),

                factory.createMenuItem(StandardActions.DELETE_ENTRY, new OldDatabaseCommandWrapper(Actions.DELETE, this, Globals.stateManager)),

                new SeparatorMenuItem(),

                factory.createMenuItem(StandardActions.LIBRARY_PROPERTIES, new DatabasePropertiesAction(this)),
                factory.createMenuItem(StandardActions.EDIT_PREAMBLE, new PreambleEditor(this)),
                factory.createMenuItem(StandardActions.EDIT_STRINGS, new OldDatabaseCommandWrapper(Actions.EDIT_STRINGS, this, Globals.stateManager))
        );

        Menu lookupIdentifiers = factory.createSubMenu(StandardActions.LOOKUP_DOC_IDENTIFIER);
        for (IdFetcher<?> fetcher : WebFetchers.getIdFetchers(Globals.prefs.getImportFormatPreferences())) {
            LookupIdentifierAction<?> identifierAction = new LookupIdentifierAction<>(this, fetcher);
            lookupIdentifiers.getItems().add(factory.createMenuItem(identifierAction.getAction(), identifierAction));
        }

        quality.getItems().addAll(
                factory.createMenuItem(StandardActions.FIND_DUPLICATES, new DuplicateSearch(this, dialogService)),
                factory.createMenuItem(StandardActions.MERGE_ENTRIES, new MergeEntriesAction(this)),

                new SeparatorMenuItem(),

                factory.createMenuItem(StandardActions.RESOLVE_DUPLICATE_KEYS, new OldDatabaseCommandWrapper(Actions.RESOLVE_DUPLICATE_KEYS, this, Globals.stateManager)),
                factory.createMenuItem(StandardActions.CHECK_INTEGRITY, new IntegrityCheckAction(this)),
                factory.createMenuItem(StandardActions.CLEANUP_ENTRIES, new OldDatabaseCommandWrapper(Actions.CLEANUP, this, Globals.stateManager)),
                factory.createMenuItem(StandardActions.GENERATE_CITE_KEYS, new OldDatabaseCommandWrapper(Actions.MAKE_KEY, this, Globals.stateManager)),

                new SeparatorMenuItem(),

                factory.createMenuItem(StandardActions.SET_FILE_LINKS, new AutoLinkFilesAction()),
                factory.createMenuItem(StandardActions.FIND_UNLINKED_FILES, new FindUnlinkedFilesAction(this)),
                lookupIdentifiers,
                factory.createMenuItem(StandardActions.DOWNLOAD_FULL_TEXT, new OldDatabaseCommandWrapper(Actions.DOWNLOAD_FULL_TEXT, this, Globals.stateManager))
        );

        SidePaneComponent webSearch = sidePaneManager.getComponent(SidePaneType.WEB_SEARCH);
        SidePaneComponent groups = sidePaneManager.getComponent(SidePaneType.GROUPS);
        SidePaneComponent openOffice = sidePaneManager.getComponent(SidePaneType.OPEN_OFFICE);

        view.getItems().addAll(
                factory.createMenuItem(webSearch.getToggleAction(), webSearch.getToggleCommand()),
                factory.createMenuItem(groups.getToggleAction(), groups.getToggleCommand()),
                factory.createMenuItem(StandardActions.TOGGLE_PREVIEW, new OldDatabaseCommandWrapper(Actions.TOGGLE_PREVIEW, this, Globals.stateManager)),
                factory.createMenuItem(StandardActions.EDIT_ENTRY, new OldDatabaseCommandWrapper(Actions.EDIT, this, Globals.stateManager)),
                factory.createMenuItem(StandardActions.SHOW_PDV_VIEWER, new ShowDocumentViewerAction()),

                new SeparatorMenuItem(),

                factory.createMenuItem(StandardActions.SELECT_ALL, new OldDatabaseCommandWrapper(Actions.SELECT_ALL, this, Globals.stateManager)),

                new SeparatorMenuItem(),

                factory.createMenuItem(StandardActions.NEXT_PREVIEW_STYLE, new OldDatabaseCommandWrapper(Actions.NEXT_PREVIEW_STYLE, this, Globals.stateManager)),
                factory.createMenuItem(StandardActions.PREVIOUS_PREVIEW_STYLE, new OldDatabaseCommandWrapper(Actions.PREVIOUS_PREVIEW_STYLE, this, Globals.stateManager))
        );

        PushToApplicationButton pushToExternal = new PushToApplicationButton(this, pushApplications.getApplications());
        tools.getItems().addAll(
                factory.createMenuItem(StandardActions.NEW_SUB_LIBRARY_FROM_AUX, new NewSubLibraryAction(this)),
                factory.createMenuItem(StandardActions.WRITE_XMP, new OldDatabaseCommandWrapper(Actions.WRITE_XMP, this, Globals.stateManager)),

                new SeparatorMenuItem(),

                factory.createMenuItem(openOffice.getToggleAction(), openOffice.getToggleCommand()),
                factory.createMenuItem(pushToExternal.getMenuAction(), pushToExternal),

                new SeparatorMenuItem(),

                factory.createMenuItem(StandardActions.OPEN_FOLDER, new OldDatabaseCommandWrapper(Actions.OPEN_FOLDER, this, Globals.stateManager)),
                factory.createMenuItem(StandardActions.OPEN_FILE, new OldDatabaseCommandWrapper(Actions.OPEN_EXTERNAL_FILE, this, Globals.stateManager)),
                factory.createMenuItem(StandardActions.OPEN_URL, new OldDatabaseCommandWrapper(Actions.OPEN_URL, this, Globals.stateManager)),
                factory.createMenuItem(StandardActions.OPEN_CONSOLE, new OldDatabaseCommandWrapper(Actions.OPEN_CONSOLE, this, Globals.stateManager)),
                factory.createMenuItem(StandardActions.COPY_LINKED_FILES, new CopyFilesAction(this)),

                new SeparatorMenuItem(),

                factory.createMenuItem(StandardActions.ABBREVIATE_ISO, new OldDatabaseCommandWrapper(Actions.ABBREVIATE_ISO, this, Globals.stateManager)),
                factory.createMenuItem(StandardActions.ABBREVIATE_MEDLINE, new OldDatabaseCommandWrapper(Actions.ABBREVIATE_MEDLINE, this, Globals.stateManager)),
                factory.createMenuItem(StandardActions.UNABBREVIATE, new OldDatabaseCommandWrapper(Actions.UNABBREVIATE, this, Globals.stateManager))
        );

        options.getItems().addAll(
                factory.createMenuItem(StandardActions.SHOW_PREFS, new ShowPreferencesAction(this, Globals.TASK_EXECUTOR)),

                new SeparatorMenuItem(),

                factory.createMenuItem(StandardActions.SETUP_GENERAL_FIELDS, new SetupGeneralFieldsAction()),
                factory.createMenuItem(StandardActions.MANAGE_CUSTOM_IMPORTS, new ManageCustomImportsAction()),
                factory.createMenuItem(StandardActions.MANAGE_CUSTOM_EXPORTS, new ManageCustomExportsAction()),
                factory.createMenuItem(StandardActions.MANAGE_EXTERNAL_FILETYPES, new EditExternalFileTypesAction()),
                factory.createMenuItem(StandardActions.MANAGE_JOURNALS, new ManageJournalsAction()),
                factory.createMenuItem(StandardActions.CUSTOMIZE_KEYBINDING, new CustomizeKeyBindingAction()),
                factory.createMenuItem(StandardActions.MANAGE_PROTECTED_TERMS, new ManageProtectedTermsAction(this, Globals.protectedTermsLoader)),

                new SeparatorMenuItem(),

                factory.createMenuItem(StandardActions.MANAGE_CONTENT_SELECTORS, new OldDatabaseCommandWrapper(Actions.MANAGE_SELECTORS, this, Globals.stateManager)),
                factory.createMenuItem(StandardActions.CUSTOMIZE_ENTRY_TYPES, new CustomizeEntryAction(this)),
                factory.createMenuItem(StandardActions.MANAGE_CITE_KEY_PATTERNS, new BibtexKeyPatternAction(this)));

        help.getItems().addAll(
                factory.createMenuItem(StandardActions.HELP, HelpAction.getMainHelpPageCommand()),
                factory.createMenuItem(StandardActions.OPEN_FORUM, new OpenBrowserAction("http://discourse.jabref.org/")),

                new SeparatorMenuItem(),

                factory.createMenuItem(StandardActions.ERROR_CONSOLE, new ErrorConsoleAction()),

                new SeparatorMenuItem(),

                factory.createMenuItem(StandardActions.SEARCH_FOR_UPDATES, new SearchForUpdateAction(Globals.BUILD_INFO, prefs.getVersionPreferences(), dialogService, Globals.TASK_EXECUTOR)),
                factory.createSubMenu(StandardActions.WEB_MENU,
                        factory.createMenuItem(StandardActions.OPEN_WEBPAGE, new OpenBrowserAction("https://jabref.org/")),
                        factory.createMenuItem(StandardActions.OPEN_BLOG, new OpenBrowserAction("https://blog.jabref.org/")),
                        factory.createMenuItem(StandardActions.OPEN_FACEBOOK, new OpenBrowserAction("https://www.facebook.com/JabRef/")),
                        factory.createMenuItem(StandardActions.OPEN_TWITTER, new OpenBrowserAction("https://twitter.com/jabref_org")),

                        new SeparatorMenuItem(),

                        factory.createMenuItem(StandardActions.FORK_ME, new OpenBrowserAction("https://github.com/JabRef/jabref")),
                        factory.createMenuItem(StandardActions.OPEN_DEV_VERSION_LINK, new OpenBrowserAction("https://builds.jabref.org/master/")),
                        factory.createMenuItem(StandardActions.OPEN_CHANGELOG, new OpenBrowserAction("https://github.com/JabRef/jabref/blob/master/CHANGELOG.md")),

                        new SeparatorMenuItem(),

                        factory.createMenuItem(StandardActions.DONATE, new OpenBrowserAction("https://donations.jabref.org"))

                ),
                factory.createMenuItem(StandardActions.ABOUT, new AboutAction())
        );

        MenuBar menu = new MenuBar();
        menu.getStyleClass().add("mainMenu");
        menu.getMenus().addAll(
                file,
                edit,
                library,
                quality,
                tools,
                view,
                options,
                help);
        menu.setUseSystemMenuBar(true);
        return menu;
    }

    public void addParserResult(ParserResult pr, boolean focusPanel) {
        if (pr.toOpenTab()) {
            // Add the entries to the open tab.
            BasePanel panel = getCurrentBasePanel();
            if (panel == null) {
                // There is no open tab to add to, so we create a new tab:
                addTab(pr.getDatabaseContext(), focusPanel);
            } else {
                List<BibEntry> entries = new ArrayList<>(pr.getDatabase().getEntries());
                addImportedEntries(panel, entries);
            }
        } else {
            // only add tab if DB is not already open
            Optional<BasePanel> panel = getBasePanelList().stream()
                                                          .filter(p -> p.getBibDatabaseContext().getDatabasePath().equals(pr.getFile()))
                                                          .findFirst();

            if (panel.isPresent()) {
                tabbedPane.getSelectionModel().select(getTab(panel.get()));
            } else {
                addTab(pr.getDatabaseContext(), focusPanel);
            }
        }
    }

    /**
     * Displays the given message at the bottom of the main frame
     *
     * @deprecated use {@link DialogService#notify(String)} instead. However, do not remove this method, it's called from the dialogService
     */
    @Deprecated
    public void output(final String message) {
        DefaultTaskExecutor.runInJavaFXThread(() -> statusLine.fireEvent(new SnackbarEvent(new JFXSnackbarLayout(message), TOAST_MESSAGE_DISPLAY_TIME, null)));
    }

    private void initActions() {
        /*
        openDatabaseOnlyActions.clear();
        openDatabaseOnlyActions.addAll(Arrays.asList(manageSelectors, mergeDatabaseAction, newSubDatabaseAction, save, copyPreview,
                saveAs, saveSelectedAs, saveSelectedAsPlain, undo, redo, cut, deleteEntry, copy, paste, mark, markSpecific, unmark,
                unmarkAll, rankSubMenu, editEntry, selectAll, copyKey, copyCiteKey, copyKeyAndTitle, copyKeyAndLink, editPreamble, editStrings,
                groupSidePane.getToggleCommand(), makeKeyAction, normalSearch, generalFetcher.getToggleCommand(), mergeEntries, cleanupEntries, exportToClipboard, replaceAll,
                sendAsEmail, downloadFullText, lookupIdentifiers, writeXmpAction, openOfficePanel.getToggleCommand(), findUnlinkedFiles, addToGroup, removeFromGroup,
                moveToGroup, autoLinkFile, resolveDuplicateKeys, openUrl, openFolder, openFile, togglePreview,
                dupliCheck, autoSetFile, newEntryAction, newSpec, customizeAction, plainTextImport, getMassSetField(), getManageKeywords(),
                pushExternalButton.getMenuAction(), closeDatabaseAction, getNextPreviewStyleAction(), getPreviousPreviewStyleAction(), checkIntegrity,
                databaseProperties, abbreviateIso, abbreviateMedline,
                unabbreviate, exportAll, exportSelected, importCurrent, saveAll, focusTable, increaseFontSize, decreseFontSize, defaultFontSize,
                toggleRelevance, toggleQualityAssured, togglePrinted, pushExternalButton.getComponent()));
        openDatabaseOnlyActions.addAll(newSpecificEntryAction);
        openDatabaseOnlyActions.addAll(specialFieldButtons);
        severalDatabasesOnlyActions.clear();
        severalDatabasesOnlyActions.addAll(Arrays
                .asList(nextTab, prevTab, sortTabs));
        openAndSavedDatabasesOnlyActions.addAll(Collections.singletonList(openConsole));
        sharedDatabaseOnlyActions.addAll(Collections.singletonList(pullChangesFromSharedDatabase));
        noSharedDatabaseActions.addAll(Arrays.asList(save, saveAll));
        oneEntryOnlyActions.clear();
        oneEntryOnlyActions.addAll(Arrays.asList(editEntry));
        oneEntryWithFileOnlyActions.clear();
        oneEntryWithFileOnlyActions.addAll(Arrays.asList(openFolder, openFile));
        oneEntryWithURLorDOIOnlyActions.clear();
        oneEntryWithURLorDOIOnlyActions.addAll(Arrays.asList(openUrl));
        twoEntriesOnlyActions.clear();
        twoEntriesOnlyActions.addAll(Arrays.asList(mergeEntries));
        atLeastOneEntryActions.clear();
        atLeastOneEntryActions.addAll(Arrays.asList(downloadFullText, lookupIdentifiers, exportLinkedFiles));
        tabbedPane.getTabs().addListener(this::updateEnabledState);
        */
    }

    /**
     * Enable or Disable all actions based on the number of open tabs.
     * <p>
     * The action that are affected are set in initActions.
     */
    public void updateEnabledState(ListChangeListener.Change<? extends Tab> change) {
        int tabCount = tabbedPane.getTabs().size();
        if (!change.next()) {
            return;
        }
        if (change.wasAdded() || change.wasRemoved()) {
            setEnabled(openDatabaseOnlyActions, tabCount > 0);
            setEnabled(severalDatabasesOnlyActions, tabCount > 1);
        }
        if (tabCount == 0) {
            setEnabled(openAndSavedDatabasesOnlyActions, false);
            setEnabled(sharedDatabaseOnlyActions, false);
            setEnabled(oneEntryOnlyActions, false);
        }

        if (tabCount > 0) {
            BasePanel current = getCurrentBasePanel();
            boolean saved = current.getBibDatabaseContext().getDatabasePath().isPresent();
            setEnabled(openAndSavedDatabasesOnlyActions, saved);

            boolean isShared = current.getBibDatabaseContext().getLocation() == DatabaseLocation.SHARED;
            setEnabled(sharedDatabaseOnlyActions, isShared);
            setEnabled(noSharedDatabaseActions, !isShared);

            boolean oneEntrySelected = current.getSelectedEntries().size() == 1;
            setEnabled(oneEntryOnlyActions, oneEntrySelected);
            setEnabled(oneEntryWithFileOnlyActions, isExistFile(current.getSelectedEntries()));
            setEnabled(oneEntryWithURLorDOIOnlyActions, isExistURLorDOI(current.getSelectedEntries()));

            boolean twoEntriesSelected = current.getSelectedEntries().size() == 2;
            setEnabled(twoEntriesOnlyActions, twoEntriesSelected);

            boolean atLeastOneEntrySelected = !current.getSelectedEntries().isEmpty();
            setEnabled(atLeastOneEntryActions, atLeastOneEntrySelected);
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
            try {
                // db file exists
                if (basePanel.getBibDatabaseContext().getDatabaseFile().isPresent()) {
                    dbPaths.add(basePanel.getBibDatabaseContext().getDatabaseFile().get().getCanonicalPath());
                } else {
                    dbPaths.add("");
                }
            } catch (IOException ex) {
                LOGGER.error("Invalid database file path: " + ex.getMessage());
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
            Optional<File> file = getBasePanelAt(i).getBibDatabaseContext().getDatabaseFile();

            if (file.isPresent()) {
                if (!uniqPath.equals(file.get().getName()) && uniqPath.contains(File.separator)) {
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
            tabbedPane.getTabs().get(i).setTooltip(new Tooltip(file.map(File::getAbsolutePath).orElse(null)));
        }
    }

    public void addTab(BasePanel basePanel, boolean raisePanel) {
        DefaultTaskExecutor.runInJavaFXThread(() -> {
            // add tab
            Tab newTab = new Tab(basePanel.getTabTitle(), basePanel);
            tabbedPane.getTabs().add(newTab);

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
                autosaver.registerListener(new AutosaveUIManager(basePanel));
            }

            BackupManager.start(context);

            // Track opening
            trackOpenNewDatabase(basePanel);
        });
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
                context.getDatabaseFile().isPresent();
    }

    /**
     * This method does the job of adding imported entries into the active database, or into a new one. It shows the
     * ImportInspectionDialog if preferences indicate it should be used. Otherwise it imports directly.
     *
     * @param panel   The BasePanel to add to.
     * @param entries The entries to add.
     */
    private void addImportedEntries(final BasePanel panel, final List<BibEntry> entries) {
        SwingUtilities.invokeLater(() -> {
            ImportInspectionDialog diag = new ImportInspectionDialog(JabRefFrame.this, panel,
                    Localization.lang("Import"), false);
            diag.addEntries(entries);
            diag.entryListComplete();
            diag.setVisible(true);
            diag.toFront();
        });
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
     * If not called on the event dispatch thread, this method uses SwingUtilities.invokeLater() to do the actual
     * operation on the EDT.
     */
    public void setProgressBarIndeterminate(final boolean value) {
        // TODO: Reimplement
        /*
        if (SwingUtilities.isEventDispatchThread()) {
            progressBar.setIndeterminate(value);
        } else {
            SwingUtilities.invokeLater(() -> progressBar.setIndeterminate(value));
        }
        */
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
            return selectedEntry.getField(FieldName.FILE).isPresent();
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
            return (selectedEntry.getField(FieldName.URL).isPresent() || selectedEntry.getField(FieldName.DOI).isPresent());
        }
        return false;
    }

    @Override
    public void showMessage(String message, String title, int msgType) {
        JOptionPane.showMessageDialog(null, message, title, msgType);
    }

    @Override
    public void setStatus(String s) {
        output(s);
    }

    @Override
    public void showMessage(String message) {
        JOptionPane.showMessageDialog(null, message);
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
                               .orElse(GUIGlobals.UNTITLED_TITLE);

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
                SaveDatabaseAction saveAction = new SaveDatabaseAction(panel, Globals.prefs);
                if (!saveAction.save()) {
                    // The action was either canceled or unsuccessful.
                    output(Localization.lang("Unable to save library"));
                    return false;
                }
            } catch (Throwable ex) {
                return false;
            }
        } else {
            return !response.isPresent() || !response.get().equals(cancel);
        }
        return false;
    }

    private void closeTab(BasePanel panel) {
        // empty tab without database
        if (panel == null) {
            return;
        }

        BibDatabaseContext context = panel.getBibDatabaseContext();

        if (panel.isModified() && (context.getLocation() == DatabaseLocation.LOCAL)) {
            if (confirmClose(panel)) {
                removeTab(panel);
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
            output(Localization.lang("Closed library") + '.');
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

    public PushToApplications getPushApplications() {
        return pushApplications;
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

    /**
     * Class for handling general actions; cut, copy and paste. The focused component is kept track of by
     * Globals.focusListener, and we call the action stored under the relevant name in its action map.
     */
    private class EditAction extends SimpleCommand {

        private final Actions command;

        public EditAction(Actions command) {
            this.command = command;
        }

        @Override
        public void execute() {
            Node focusOwner = mainStage.getScene().getFocusOwner();
            if (focusOwner != null) {
                if (focusOwner instanceof TextInputControl) {
                    // Focus is on text field -> copy/paste/cut selected text
                    TextInputControl textInput = (TextInputControl) focusOwner;
                    switch (command) {
                        case COPY:
                            textInput.copy();
                            break;
                        case CUT:
                            textInput.cut();
                            break;
                        case PASTE:
                            textInput.paste();
                            break;
                        default:
                            throw new IllegalStateException("Only cut/copy/paste supported but got " + command);
                    }
                } else {
                    // Not sure what is selected -> copy/paste/cut selected entries
                    switch (command) {
                        case COPY:
                            getCurrentBasePanel().copy();
                            break;
                        case CUT:
                            getCurrentBasePanel().cut();
                            break;
                        case PASTE:
                            getCurrentBasePanel().paste();
                            break;
                        default:
                            throw new IllegalStateException("Only cut/copy/paste supported but got " + command);
                    }
                }
            }
        }
    }

    private void setDefaultTableFontSize() {
        GUIGlobals.setFont(Globals.prefs.getIntDefault(JabRefPreferences.FONT_SIZE));
        for (BasePanel basePanel : getBasePanelList()) {
            basePanel.updateTableFont();
        }
        setStatus(Localization.lang("Table font size is %0", String.valueOf(GUIGlobals.currentFont.getSize())));
    }

    private void increaseTableFontSize() {
        GUIGlobals.setFont(GUIGlobals.currentFont.getSize() + 1);
        for (BasePanel basePanel : getBasePanelList()) {
            basePanel.updateTableFont();
        }
        setStatus(Localization.lang("Table font size is %0", String.valueOf(GUIGlobals.currentFont.getSize())));
    }

    private void decreaseTableFontSize() {
        int currentSize = GUIGlobals.currentFont.getSize();
        if (currentSize < 2) {
            return;
        }
        GUIGlobals.setFont(currentSize - 1);
        for (BasePanel basePanel : getBasePanelList()) {
            basePanel.updateTableFont();
        }
        setStatus(Localization.lang("Table font size is %0", String.valueOf(GUIGlobals.currentFont.getSize())));
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
