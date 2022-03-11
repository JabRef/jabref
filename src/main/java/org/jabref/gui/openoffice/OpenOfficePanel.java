package org.jabref.gui.openoffice;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.swing.undo.UndoManager;

import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import org.jabref.gui.DialogService;
import org.jabref.gui.Globals;
import org.jabref.gui.JabRefGUI;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionFactory;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.help.HelpAction;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.keyboard.KeyBindingRepository;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.undo.UndoableKeyChange;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.DirectoryDialogConfiguration;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.citationkeypattern.CitationKeyGenerator;
import org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.openoffice.OpenOfficeFileSearch;
import org.jabref.logic.openoffice.OpenOfficePreferences;
import org.jabref.logic.openoffice.action.Update;
import org.jabref.logic.openoffice.style.OOBibStyle;
import org.jabref.logic.openoffice.style.StyleLoader;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.openoffice.style.CitationType;
import org.jabref.model.openoffice.uno.CreationException;
import org.jabref.preferences.PreferencesService;

import com.sun.star.comp.helper.BootstrapException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Pane to manage the interaction between JabRef and OpenOffice.
 */
public class OpenOfficePanel {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenOfficePanel.class);
    private final DialogService dialogService;

    private final Button connect;
    private final Button manualConnect;
    private final Button selectDocument;
    private final Button setStyleFile = new Button(Localization.lang("Select style"));
    private final Button pushEntries = new Button(Localization.lang("Cite"));
    private final Button pushEntriesInt = new Button(Localization.lang("Cite in-text"));
    private final Button pushEntriesEmpty = new Button(Localization.lang("Insert empty citation"));
    private final Button pushEntriesAdvanced = new Button(Localization.lang("Cite special"));
    private final Button update;
    private final Button merge = new Button(Localization.lang("Merge citations"));
    private final Button unmerge = new Button(Localization.lang("Separate citations"));
    private final Button manageCitations = new Button(Localization.lang("Manage citations"));
    private final Button exportCitations = new Button(Localization.lang("Export cited"));
    private final Button settingsB = new Button(Localization.lang("Settings"));
    private final Button help;
    private final VBox vbox = new VBox();

    private final PreferencesService preferencesService;
    private final StateManager stateManager;
    private final UndoManager undoManager;
    private final TaskExecutor taskExecutor;
    private final StyleLoader loader;
    private OpenOfficePreferences openOfficePreferences;
    private OOBibBase ooBase;
    private OOBibStyle style;

    public OpenOfficePanel(PreferencesService preferencesService,
                           OpenOfficePreferences openOfficePreferences,
                           KeyBindingRepository keyBindingRepository,
                           TaskExecutor taskExecutor,
                           DialogService dialogService,
                           StateManager stateManager,
                           UndoManager undoManager) {
        ActionFactory factory = new ActionFactory(keyBindingRepository);
        this.openOfficePreferences = openOfficePreferences;
        this.preferencesService = preferencesService;
        this.taskExecutor = taskExecutor;
        this.dialogService = dialogService;
        this.stateManager = stateManager;
        this.undoManager = undoManager;

        connect = new Button();
        connect.setGraphic(IconTheme.JabRefIcons.CONNECT_OPEN_OFFICE.getGraphicNode());
        connect.setTooltip(new Tooltip(Localization.lang("Connect")));
        connect.setMaxWidth(Double.MAX_VALUE);

        manualConnect = new Button();
        manualConnect.setGraphic(IconTheme.JabRefIcons.CONNECT_OPEN_OFFICE.getGraphicNode());
        manualConnect.setTooltip(new Tooltip(Localization.lang("Manual connect")));
        manualConnect.setMaxWidth(Double.MAX_VALUE);

        help = factory.createIconButton(StandardActions.HELP, new HelpAction(HelpFile.OPENOFFICE_LIBREOFFICE));
        help.setMaxWidth(Double.MAX_VALUE);

        selectDocument = new Button();
        selectDocument.setGraphic(IconTheme.JabRefIcons.OPEN.getGraphicNode());
        selectDocument.setTooltip(new Tooltip(Localization.lang("Select Writer document")));
        selectDocument.setMaxWidth(Double.MAX_VALUE);

        update = new Button();
        update.setGraphic(IconTheme.JabRefIcons.REFRESH.getGraphicNode());
        update.setTooltip(new Tooltip(Localization.lang("Sync OpenOffice/LibreOffice bibliography")));
        update.setMaxWidth(Double.MAX_VALUE);

        loader = new StyleLoader(openOfficePreferences,
                preferencesService.getLayoutFormatterPreferences(Globals.journalAbbreviationRepository),
                preferencesService.getGeneralPreferences().getDefaultEncoding());

        initPanel();
    }

    public Node getContent() {
        return vbox;
    }

    /* Note: the style may still be null on return.
     *
     * Return true if failed. In this case the dialog is already shown.
     */
    private boolean getOrUpdateTheStyle(String title) {
        final boolean FAIL = true;
        final boolean PASS = false;

        if (style == null) {
            style = loader.getUsedStyle();
        } else {
            try {
                style.ensureUpToDate();
            } catch (IOException ex) {
                LOGGER.warn("Unable to reload style file '" + style.getPath() + "'", ex);
                String msg = (Localization.lang("Unable to reload style file")
                        + "'" + style.getPath() + "'"
                        + "\n" + ex.getMessage());
                new OOError(title, msg, ex).showErrorDialog(dialogService);
                return FAIL;
            }
        }
        return PASS;
    }

    private void initPanel() {

        connect.setOnAction(e -> connectAutomatically());
        manualConnect.setOnAction(e -> connectManually());

        selectDocument.setTooltip(new Tooltip(Localization.lang("Select which open Writer document to work on")));
        selectDocument.setOnAction(e -> ooBase.guiActionSelectDocument(false));

        setStyleFile.setMaxWidth(Double.MAX_VALUE);
        setStyleFile.setOnAction(event ->
                dialogService.showCustomDialogAndWait(new StyleSelectDialogView(loader))
                             .ifPresent(selectedStyle -> {
                                 style = selectedStyle;
                                 try {
                                     style.ensureUpToDate();
                                 } catch (IOException e) {
                                     LOGGER.warn("Unable to reload style file '" + style.getPath() + "'", e);
                                 }
                                 dialogService.notify(Localization.lang("Current style is '%0'", style.getName()));
                             }));

        pushEntries.setTooltip(new Tooltip(Localization.lang("Cite selected entries between parenthesis")));
        pushEntries.setOnAction(e -> pushEntries(CitationType.AUTHORYEAR_PAR, false));
        pushEntries.setMaxWidth(Double.MAX_VALUE);
        pushEntriesInt.setTooltip(new Tooltip(Localization.lang("Cite selected entries with in-text citation")));
        pushEntriesInt.setOnAction(e -> pushEntries(CitationType.AUTHORYEAR_INTEXT, false));
        pushEntriesInt.setMaxWidth(Double.MAX_VALUE);
        pushEntriesEmpty.setTooltip(new Tooltip(Localization.lang("Insert a citation without text (the entry will appear in the reference list)")));
        pushEntriesEmpty.setOnAction(e -> pushEntries(CitationType.INVISIBLE_CIT, false));
        pushEntriesEmpty.setMaxWidth(Double.MAX_VALUE);
        pushEntriesAdvanced.setTooltip(new Tooltip(Localization.lang("Cite selected entries with extra information")));
        pushEntriesAdvanced.setOnAction(e -> pushEntries(CitationType.AUTHORYEAR_INTEXT, true));
        pushEntriesAdvanced.setMaxWidth(Double.MAX_VALUE);

        update.setTooltip(new Tooltip(Localization.lang("Ensure that the bibliography is up-to-date")));

        update.setOnAction(event -> {
            String title = Localization.lang("Could not update bibliography");
            if (getOrUpdateTheStyle(title)) {
                return;
            }
            List<BibDatabase> databases = getBaseList();
            ooBase.guiActionUpdateDocument(databases, style);
        });

        merge.setMaxWidth(Double.MAX_VALUE);
        merge.setTooltip(new Tooltip(Localization.lang("Combine pairs of citations that are separated by spaces only")));
        merge.setOnAction(e -> ooBase.guiActionMergeCitationGroups(getBaseList(), style));

        unmerge.setMaxWidth(Double.MAX_VALUE);
        unmerge.setTooltip(new Tooltip(Localization.lang("Separate merged citations")));
        unmerge.setOnAction(e -> ooBase.guiActionSeparateCitations(getBaseList(), style));

        ContextMenu settingsMenu = createSettingsPopup();
        settingsB.setMaxWidth(Double.MAX_VALUE);
        settingsB.setContextMenu(settingsMenu);
        settingsB.setOnAction(e -> settingsMenu.show(settingsB, Side.BOTTOM, 0, 0));
        manageCitations.setMaxWidth(Double.MAX_VALUE);
        manageCitations.setOnAction(e -> {
            ManageCitationsDialogView dialog = new ManageCitationsDialogView(ooBase);
            if (dialog.isOkToShowThisDialog()) {
                dialogService.showCustomDialogAndWait(dialog);
            }
        });

        exportCitations.setMaxWidth(Double.MAX_VALUE);
        exportCitations.setOnAction(event -> exportEntries());

        updateButtonAvailability();

        HBox hbox = new HBox();
        hbox.getChildren().addAll(connect, manualConnect, selectDocument, update, help);
        hbox.getChildren().forEach(btn -> HBox.setHgrow(btn, Priority.ALWAYS));

        FlowPane flow = new FlowPane();
        flow.setPadding(new Insets(5, 5, 5, 5));
        flow.setVgap(4);
        flow.setHgap(4);
        flow.setPrefWrapLength(200);
        flow.getChildren().addAll(setStyleFile, pushEntries, pushEntriesInt);
        flow.getChildren().addAll(pushEntriesAdvanced, pushEntriesEmpty, merge, unmerge);
        flow.getChildren().addAll(manageCitations, exportCitations, settingsB);

        vbox.setFillWidth(true);
        vbox.getChildren().addAll(hbox, flow);
    }

    private void exportEntries() {
        List<BibDatabase> databases = getBaseList();
        boolean returnPartialResult = false;
        Optional<BibDatabase> newDatabase = ooBase.exportCitedHelper(databases, returnPartialResult);
        if (newDatabase.isPresent()) {
            BibDatabaseContext databaseContext = new BibDatabaseContext(newDatabase.get());
            JabRefGUI.getMainFrame().addTab(databaseContext, true);
        }
    }

    private List<BibDatabase> getBaseList() {
        List<BibDatabase> databases = new ArrayList<>();
        if (openOfficePreferences.getUseAllDatabases()) {
            for (BibDatabaseContext database : stateManager.getOpenDatabases()) {
                databases.add(database.getDatabase());
            }
        } else {
            databases.add(stateManager.getActiveDatabase()
                                      .map(BibDatabaseContext::getDatabase)
                                      .orElse(new BibDatabase()));
        }

        return databases;
    }

    private void connectAutomatically() {
        DetectOpenOfficeInstallation officeInstallation = new DetectOpenOfficeInstallation(preferencesService, dialogService);

        if (officeInstallation.isExecutablePathDefined()) {
            connect();
        } else {

            Task<List<Path>> taskConnectIfInstalled = new Task<>() {

                @Override
                protected List<Path> call() {
                    return OpenOfficeFileSearch.detectInstallations();
                }
            };

            taskConnectIfInstalled.setOnSucceeded(evt -> {
                var installations = new ArrayList<>(taskConnectIfInstalled.getValue());
                if (installations.isEmpty()) {
                    officeInstallation.selectInstallationPath().ifPresent(installations::add);
                }
                Optional<Path> actualFile = officeInstallation.chooseAmongInstallations(installations);
                if (actualFile.isPresent() && officeInstallation.setOpenOfficePreferences(actualFile.get())) {
                    connect();
                }
            });

            taskConnectIfInstalled.setOnFailed(value -> dialogService.showErrorDialogAndWait(Localization.lang("Autodetection failed"), Localization.lang("Autodetection failed"), taskConnectIfInstalled.getException()));

            dialogService.showProgressDialog(Localization.lang("Autodetecting paths..."), Localization.lang("Autodetecting paths..."), taskConnectIfInstalled);
            taskExecutor.execute(taskConnectIfInstalled);
        }
    }

    private void connectManually() {
        var fileDialogConfiguration = new DirectoryDialogConfiguration.Builder().withInitialDirectory(System.getProperty("user.home")).build();
        Optional<Path> selectedPath = dialogService.showDirectorySelectionDialog(fileDialogConfiguration);

        DetectOpenOfficeInstallation officeInstallation = new DetectOpenOfficeInstallation(preferencesService, dialogService);

        if (selectedPath.isPresent()) {

            BackgroundTask.wrap(() -> officeInstallation.setOpenOfficePreferences(selectedPath.get()))
                          .withInitialMessage("Searching for executable")
                          .onFailure(dialogService::showErrorDialogAndWait).onSuccess(value -> {
                              if (value) {
                                  connect();
                              } else {
                                  dialogService.showErrorDialogAndWait(Localization.lang("Could not connect to running OpenOffice/LibreOffice."), Localization.lang("If connecting manually, please verify program and library paths."));
                              }
                          })
                          .executeWith(taskExecutor);
        } else {
            dialogService.showErrorDialogAndWait(Localization.lang("Could not connect to running OpenOffice/LibreOffice."), Localization.lang("If connecting manually, please verify program and library paths."));
        }
    }

    private void updateButtonAvailability() {
        boolean isConnected = (ooBase != null);
        boolean isConnectedToDocument = isConnected && !ooBase.isDocumentConnectionMissing();

        // For these, we need to watch something
        boolean hasStyle = true; // (style != null);
        boolean hasDatabase = true; // !getBaseList().isEmpty();
        boolean hasSelectedBibEntry = true;

        selectDocument.setDisable(!(isConnected));
        pushEntries.setDisable(!(isConnectedToDocument && hasStyle && hasDatabase));

        boolean canCite = isConnectedToDocument && hasStyle && hasSelectedBibEntry;
        pushEntriesInt.setDisable(!canCite);
        pushEntriesEmpty.setDisable(!canCite);
        pushEntriesAdvanced.setDisable(!canCite);

        boolean canRefreshDocument = isConnectedToDocument && hasStyle;
        update.setDisable(!canRefreshDocument);
        merge.setDisable(!canRefreshDocument);
        unmerge.setDisable(!canRefreshDocument);
        manageCitations.setDisable(!canRefreshDocument);

        exportCitations.setDisable(!(isConnectedToDocument && hasDatabase));
    }

    private void connect() {
        openOfficePreferences = preferencesService.getOpenOfficePreferences();

        Task<OOBibBase> connectTask = new Task<>() {

            @Override
            protected OOBibBase call() throws Exception {
                updateProgress(ProgressBar.INDETERMINATE_PROGRESS, ProgressBar.INDETERMINATE_PROGRESS);

                var path = Path.of(openOfficePreferences.getExecutablePath());
                return createBibBase(path);
            }
        };

        connectTask.setOnSucceeded(value -> {
            ooBase = connectTask.getValue();

            ooBase.guiActionSelectDocument(true);

            // Enable actions that depend on Connect:
            updateButtonAvailability();
        });

        connectTask.setOnFailed(value -> {
            Throwable ex = connectTask.getException();
            if (ex instanceof UnsatisfiedLinkError) {
                LOGGER.warn("Could not connect to running OpenOffice/LibreOffice", ex);

                dialogService.showErrorDialogAndWait(Localization.lang("Unable to connect. One possible reason is that JabRef "
                        + "and OpenOffice/LibreOffice are not both running in either 32 bit mode or 64 bit mode."));
            } else if (ex instanceof IOException) {
                LOGGER.warn("Could not connect to running OpenOffice/LibreOffice", ex);

                dialogService.showErrorDialogAndWait(Localization.lang("Could not connect to running OpenOffice/LibreOffice."),
                        Localization.lang("Could not connect to running OpenOffice/LibreOffice.")
                                + "\n"
                                + Localization.lang("Make sure you have installed OpenOffice/LibreOffice with Java support.") + "\n"
                                + Localization.lang("If connecting manually, please verify program and library paths.") + "\n" + "\n" + Localization.lang("Error message:"),
                        ex);
            } else {
                dialogService.showErrorDialogAndWait(Localization.lang("Autodetection failed"), Localization.lang("Autodetection failed"), ex);
            }
        });

        dialogService.showProgressDialog(Localization.lang("Autodetecting paths..."), Localization.lang("Autodetecting paths..."), connectTask);
        taskExecutor.execute(connectTask);
    }

    private OOBibBase createBibBase(Path loPath) throws BootstrapException, CreationException {
        return new OOBibBase(loPath, dialogService);
    }

    /**
     * Given the withText and inParenthesis options, return the corresponding citationType.
     *
     * @param withText      False means invisible citation (no text).
     * @param inParenthesis True means "(Au and Thor 2000)". False means "Au and Thor (2000)".
     */
    private static CitationType citationTypeFromOptions(boolean withText, boolean inParenthesis) {
        if (!withText) {
            return CitationType.INVISIBLE_CIT;
        }
        return (inParenthesis
                ? CitationType.AUTHORYEAR_PAR
                : CitationType.AUTHORYEAR_INTEXT);
    }

    private void pushEntries(CitationType citationType, boolean addPageInfo) {
        final String errorDialogTitle = Localization.lang("Error pushing entries");

        if (stateManager.getActiveDatabase().isEmpty()
                || (stateManager.getActiveDatabase().get().getDatabase() == null)) {
            OOError.noDataBaseIsOpenForCiting()
                   .setTitle(errorDialogTitle)
                   .showErrorDialog(dialogService);
            return;
        }

        final BibDatabase database = stateManager.getActiveDatabase().get().getDatabase();
        if (database == null) {
            OOError.noDataBaseIsOpenForCiting()
                   .setTitle(errorDialogTitle)
                   .showErrorDialog(dialogService);
            return;
        }

        List<BibEntry> entries = stateManager.getSelectedEntries();
        if (entries.isEmpty()) {
            OOError.noEntriesSelectedForCitation()
                   .setTitle(errorDialogTitle)
                   .showErrorDialog(dialogService);
            return;
        }

        if (getOrUpdateTheStyle(errorDialogTitle)) {
            return;
        }

        String pageInfo = null;
        if (addPageInfo) {
            boolean withText = citationType.withText();

            Optional<AdvancedCiteDialogViewModel> citeDialogViewModel = dialogService.showCustomDialogAndWait(new AdvancedCiteDialogView());
            if (citeDialogViewModel.isPresent()) {

                AdvancedCiteDialogViewModel model = citeDialogViewModel.get();
                if (!model.pageInfoProperty().getValue().isEmpty()) {
                    pageInfo = model.pageInfoProperty().getValue();
                }
                citationType = citationTypeFromOptions(withText, model.citeInParProperty().getValue());
            } else {
                // user canceled
                return;
            }
        }

        if (!checkThatEntriesHaveKeys(entries)) {
            // Not all entries have keys and key generation was declined.
            return;
        }

        Optional<Update.SyncOptions> syncOptions =
                (openOfficePreferences.getSyncWhenCiting()
                        ? Optional.of(new Update.SyncOptions(getBaseList()))
                        : Optional.empty());

        ooBase.guiActionInsertEntry(entries,
                database,
                style,
                citationType,
                pageInfo,
                syncOptions);
    }

    /**
     * Check that all entries in the list have citation keys, if not ask if they should be generated
     *
     * @param entries A list of entries to be checked
     * @return true if all entries have citation keys, if it so may be after generating them
     */
    private boolean checkThatEntriesHaveKeys(List<BibEntry> entries) {
        // Check if there are empty keys
        boolean emptyKeys = false;
        for (BibEntry entry : entries) {
            if (entry.getCitationKey().isEmpty()) {
                // Found one, no need to look further for now
                emptyKeys = true;
                break;
            }
        }

        // If no empty keys, return true
        if (!emptyKeys) {
            return true;
        }

        // Ask if keys should be generated
        boolean citePressed = dialogService.showConfirmationDialogAndWait(Localization.lang("Cite"),
                Localization.lang("Cannot cite entries without citation keys. Generate keys now?"),
                Localization.lang("Generate keys"),
                Localization.lang("Cancel"));

        Optional<BibDatabaseContext> databaseContext = stateManager.getActiveDatabase();
        if (citePressed && databaseContext.isPresent()) {
            // Generate keys
            CitationKeyPatternPreferences prefs = preferencesService.getCitationKeyPatternPreferences();
            NamedCompound undoCompound = new NamedCompound(Localization.lang("Cite"));
            for (BibEntry entry : entries) {
                if (entry.getCitationKey().isEmpty()) {
                    // Generate key
                    new CitationKeyGenerator(databaseContext.get(), prefs)
                            .generateAndSetKey(entry)
                            .ifPresent(change -> undoCompound.addEdit(new UndoableKeyChange(change)));
                }
            }
            undoCompound.end();
            // Add all undos
            undoManager.addEdit(undoCompound);
            // Now every entry has a key
            return true;
        } else {
            // No, we canceled (or there is no panel to get the database from, highly unlikely)
            return false;
        }
    }

    private ContextMenu createSettingsPopup() {

        ContextMenu contextMenu = new ContextMenu();

        CheckMenuItem autoSync = new CheckMenuItem(Localization.lang("Automatically sync bibliography when inserting citations"));
        autoSync.selectedProperty().set(openOfficePreferences.getSyncWhenCiting());

        ToggleGroup toggleGroup = new ToggleGroup();
        RadioMenuItem useActiveBase = new RadioMenuItem(Localization.lang("Look up BibTeX entries in the active tab only"));
        RadioMenuItem useAllBases = new RadioMenuItem(Localization.lang("Look up BibTeX entries in all open libraries"));
        useActiveBase.setToggleGroup(toggleGroup);
        useAllBases.setToggleGroup(toggleGroup);

        MenuItem clearConnectionSettings = new MenuItem(Localization.lang("Clear connection settings"));

        if (openOfficePreferences.getUseAllDatabases()) {
            useAllBases.setSelected(true);
        } else {
            useActiveBase.setSelected(true);
        }

        autoSync.setOnAction(e -> {
            openOfficePreferences.setSyncWhenCiting(autoSync.isSelected());
            preferencesService.setOpenOfficePreferences(openOfficePreferences);
        });
        useAllBases.setOnAction(e -> {
            openOfficePreferences.setUseAllDatabases(useAllBases.isSelected());
            preferencesService.setOpenOfficePreferences(openOfficePreferences);
        });
        useActiveBase.setOnAction(e -> {
            openOfficePreferences.setUseAllDatabases(!useActiveBase.isSelected());
            preferencesService.setOpenOfficePreferences(openOfficePreferences);
        });
        clearConnectionSettings.setOnAction(e -> {
            openOfficePreferences.clearConnectionSettings();
            dialogService.notify(Localization.lang("Cleared connection settings"));
            preferencesService.setOpenOfficePreferences(openOfficePreferences);
        });

        contextMenu.getItems().addAll(
                autoSync,
                new SeparatorMenuItem(),
                useActiveBase,
                useAllBases,
                new SeparatorMenuItem(),
                clearConnectionSettings);

        return contextMenu;
    }
}
