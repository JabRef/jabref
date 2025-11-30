package org.jabref.gui.openoffice;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.swing.undo.UndoManager;

import javafx.beans.property.SimpleObjectProperty;
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
import org.jabref.gui.LibraryTab;
import org.jabref.gui.LibraryTabContainer;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionFactory;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.clipboard.ClipBoardManager;
import org.jabref.gui.frame.ExternalApplicationsPreferences;
import org.jabref.gui.help.HelpAction;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.undo.NamedCompoundEdit;
import org.jabref.gui.undo.UndoableKeyChange;
import org.jabref.gui.util.DirectoryDialogConfiguration;
import org.jabref.gui.util.UiTaskExecutor;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.citationkeypattern.CitationKeyGenerator;
import org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences;
import org.jabref.logic.citationstyle.CSLStyleLoader;
import org.jabref.logic.citationstyle.CitationStyle;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.logic.openoffice.OpenOfficeFileSearch;
import org.jabref.logic.openoffice.OpenOfficePreferences;
import org.jabref.logic.openoffice.action.Update;
import org.jabref.logic.openoffice.style.JStyle;
import org.jabref.logic.openoffice.style.JStyleLoader;
import org.jabref.logic.openoffice.style.OOStyle;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.openoffice.style.CitationType;
import org.jabref.model.openoffice.uno.CreationException;
import org.jabref.model.util.DirectoryUpdateMonitor;
import org.jabref.model.util.FileUpdateMonitor;

import com.sun.star.comp.helper.BootstrapException;
import com.sun.star.container.NoSuchElementException;
import com.sun.star.lang.WrappedTargetException;
import com.tobiasdiez.easybind.EasyBind;
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
    private final Button modifyBibliographyProperties = new Button(Localization.lang("Bibliography properties"));
    private final Button settingsB = new Button(Localization.lang("Settings"));
    private final Button help;
    private final VBox vbox = new VBox();

    private final GuiPreferences preferences;
    private final OpenOfficePreferences openOfficePreferences;
    private final CitationKeyPatternPreferences citationKeyPatternPreferences;
    private final StateManager stateManager;
    private final ClipBoardManager clipBoardManager;
    private final UndoManager undoManager;
    private final UiTaskExecutor taskExecutor;
    private final AiService aiService;
    private final JStyleLoader jStyleLoader;
    private final CSLStyleLoader cslStyleLoader;
    private final LibraryTabContainer tabContainer;
    private final FileUpdateMonitor fileUpdateMonitor;
    private final DirectoryUpdateMonitor directoryUpdateMonitor;
    private final BibEntryTypesManager entryTypesManager;
    private OOBibBase ooBase;
    private OOStyle currentStyle;

    private final SimpleObjectProperty<OOStyle> currentStyleProperty;

    public OpenOfficePanel(LibraryTabContainer tabContainer,
                           GuiPreferences preferences,
                           OpenOfficePreferences openOfficePreferences,
                           ExternalApplicationsPreferences externalApplicationsPreferences,
                           LayoutFormatterPreferences layoutFormatterPreferences,
                           CitationKeyPatternPreferences citationKeyPatternPreferences,
                           JournalAbbreviationRepository abbreviationRepository,
                           UiTaskExecutor taskExecutor,
                           DialogService dialogService,
                           AiService aiService,
                           StateManager stateManager,
                           FileUpdateMonitor fileUpdateMonitor,
                           DirectoryUpdateMonitor directoryUpdateMonitor,
                           BibEntryTypesManager entryTypesManager,
                           ClipBoardManager clipBoardManager,
                           UndoManager undoManager) {
        this.tabContainer = tabContainer;
        this.fileUpdateMonitor = fileUpdateMonitor;
        this.directoryUpdateMonitor = directoryUpdateMonitor;
        this.entryTypesManager = entryTypesManager;
        this.preferences = preferences;
        this.openOfficePreferences = openOfficePreferences;
        this.citationKeyPatternPreferences = citationKeyPatternPreferences;
        this.taskExecutor = taskExecutor;
        this.dialogService = dialogService;
        this.aiService = aiService;
        this.stateManager = stateManager;
        this.clipBoardManager = clipBoardManager;
        this.undoManager = undoManager;
        this.currentStyle = openOfficePreferences.getCurrentStyle();

        this.currentStyleProperty = new SimpleObjectProperty<>(currentStyle);

        jStyleLoader = new JStyleLoader(
                openOfficePreferences,
                layoutFormatterPreferences,
                abbreviationRepository);

        cslStyleLoader = new CSLStyleLoader(openOfficePreferences);

        ActionFactory factory = new ActionFactory();

        connect = new Button();
        connect.setGraphic(IconTheme.JabRefIcons.CONNECT_OPEN_OFFICE.getGraphicNode());
        connect.setTooltip(new Tooltip(Localization.lang("Connect")));
        connect.setMaxWidth(Double.MAX_VALUE);

        manualConnect = new Button();
        manualConnect.setGraphic(IconTheme.JabRefIcons.CONNECT_OPEN_OFFICE.getGraphicNode());
        manualConnect.setTooltip(new Tooltip(Localization.lang("Manual connect")));
        manualConnect.setMaxWidth(Double.MAX_VALUE);

        help = factory.createIconButton(StandardActions.HELP, new HelpAction(HelpFile.OPENOFFICE_LIBREOFFICE, dialogService, externalApplicationsPreferences));
        help.setMaxWidth(Double.MAX_VALUE);

        selectDocument = new Button();
        selectDocument.setGraphic(IconTheme.JabRefIcons.OPEN.getGraphicNode());
        selectDocument.setTooltip(new Tooltip(Localization.lang("Select Writer document")));
        selectDocument.setMaxWidth(Double.MAX_VALUE);

        update = new Button();
        update.setGraphic(IconTheme.JabRefIcons.ADD_OR_MAKE_BIBLIOGRAPHY.getGraphicNode());
        update.setTooltip(new Tooltip(Localization.lang("Sync OpenOffice/LibreOffice bibliography")));
        update.setMaxWidth(Double.MAX_VALUE);

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

        if (currentStyle == null) {
            currentStyle = openOfficePreferences.getCurrentStyle();
            currentStyleProperty.set(currentStyle);
        } else {
            if (currentStyle instanceof JStyle jStyle) {
                try {
                    jStyle = jStyleLoader.getUsedJstyle();
                    jStyle.ensureUpToDate();
                } catch (IOException ex) {
                    LOGGER.warn("Unable to reload style file '{}'", jStyle.getPath(), ex);
                    String msg = Localization.lang("Unable to reload style file")
                            + "'" + jStyle.getPath() + "'"
                            + "\n" + ex.getMessage();
                    new OOError(title, msg, ex).showErrorDialog(dialogService);
                    return FAIL;
                }
            } else {
                // CSL Styles don't need to be updated
                return PASS;
            }
        }
        return PASS;
    }

    private void initPanel() {
        connect.setOnAction(_ -> connectAutomatically());
        manualConnect.setOnAction(_ -> connectManually());

        selectDocument.setTooltip(new Tooltip(Localization.lang("Select which open Writer document to work on")));
        selectDocument.setOnAction(_ -> {
            try {
                ooBase.guiActionSelectDocument(false);
            } catch (WrappedTargetException
                     | NoSuchElementException ex) {
                throw new RuntimeException(ex);
            }
        });

        setStyleFile.setMaxWidth(Double.MAX_VALUE);
        setStyleFile.setOnAction(_ -> {
            StyleSelectDialogView styleDialog = new StyleSelectDialogView(cslStyleLoader, jStyleLoader);
            dialogService.showCustomDialogAndWait(styleDialog)
                         .ifPresent(selectedStyle -> {
                             currentStyle = selectedStyle;
                             currentStyleProperty.set(currentStyle);

                             if (currentStyle instanceof JStyle jStyle) {
                                 try {
                                     jStyle.ensureUpToDate();
                                 } catch (IOException e) {
                                     LOGGER.warn("Unable to reload style file '{}'", jStyle.getPath(), e);
                                 }
                                 dialogService.notify(Localization.lang("Currently selected JStyle: '%0'", jStyle.getName()));
                             } else if (currentStyle instanceof CitationStyle cslStyle) {
                                 dialogService.notify(Localization.lang("Currently selected CSL Style: '%0'", cslStyle.getName()));
                             }
                             updateButtonAvailability();
                         });
        });

        pushEntries.setTooltip(new Tooltip(Localization.lang("Cite selected entries between parenthesis")));
        pushEntries.setOnAction(_ -> pushEntries(CitationType.AUTHORYEAR_PAR, false));
        pushEntries.setMaxWidth(Double.MAX_VALUE);
        pushEntriesInt.setTooltip(new Tooltip(Localization.lang("Cite selected entries with in-text citation")));
        pushEntriesInt.setOnAction(_ -> pushEntries(CitationType.AUTHORYEAR_INTEXT, false));
        pushEntriesInt.setMaxWidth(Double.MAX_VALUE);
        pushEntriesEmpty.setTooltip(new Tooltip(Localization.lang("Insert a citation without text (the entry will appear in the reference list)")));
        pushEntriesEmpty.setOnAction(_ -> pushEntries(CitationType.INVISIBLE_CIT, false));
        pushEntriesEmpty.setMaxWidth(Double.MAX_VALUE);
        pushEntriesAdvanced.setTooltip(new Tooltip(Localization.lang("Cite selected entries with extra information")));
        pushEntriesAdvanced.setOnAction(_ -> pushEntries(CitationType.AUTHORYEAR_INTEXT, true));
        pushEntriesAdvanced.setMaxWidth(Double.MAX_VALUE);

        update.setTooltip(new Tooltip(Localization.lang("Make/Sync bibliography")));

        update.setOnAction(_ -> {
            String title = Localization.lang("Could not update bibliography");
            if (getOrUpdateTheStyle(title)) {
                return;
            }
            List<BibDatabase> databases = getBaseList();
            ooBase.guiActionUpdateDocument(databases, currentStyle);
        });

        merge.setMaxWidth(Double.MAX_VALUE);
        merge.setTooltip(new Tooltip(Localization.lang("Combine pairs of citations that are separated by spaces only")));
        merge.setOnAction(_ -> ooBase.guiActionMergeCitationGroups(getBaseList(), currentStyle));

        unmerge.setMaxWidth(Double.MAX_VALUE);
        unmerge.setTooltip(new Tooltip(Localization.lang("Separate merged citations")));
        unmerge.setOnAction(_ -> ooBase.guiActionSeparateCitations(getBaseList(), currentStyle));

        ContextMenu settingsMenu = createSettingsPopup();
        settingsB.setMaxWidth(Double.MAX_VALUE);
        settingsB.setContextMenu(settingsMenu);
        settingsB.setOnAction(_ -> settingsMenu.show(settingsB, Side.BOTTOM, 0, 0));
        manageCitations.setMaxWidth(Double.MAX_VALUE);
        manageCitations.setOnAction(_ -> {
            ManageCitationsDialogView dialog = new ManageCitationsDialogView(ooBase);
            if (dialog.isOkToShowThisDialog()) {
                dialogService.showCustomDialogAndWait(dialog);
            }
        });

        modifyBibliographyProperties.setMaxWidth(Double.MAX_VALUE);
        modifyBibliographyProperties.setOnAction(_ -> modifyBibliographyProperties());

        exportCitations.setMaxWidth(Double.MAX_VALUE);
        exportCitations.setOnAction(_ -> exportEntries());

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
        flow.getChildren().addAll(manageCitations, exportCitations, modifyBibliographyProperties, settingsB);

        vbox.setFillWidth(true);
        vbox.getChildren().addAll(hbox, flow);
    }

    private void modifyBibliographyProperties() {
        ModifyCSLBibliographyPropertiesDialogView modifyBibliographyPropertiesDialogView = new ModifyCSLBibliographyPropertiesDialogView(openOfficePreferences);
        dialogService.showCustomDialog(modifyBibliographyPropertiesDialogView);
    }

    private void exportEntries() {
        List<BibDatabase> databases = getBaseList();
        boolean returnPartialResult = false;
        Optional<BibDatabase> newDatabase = ooBase.exportCitedHelper(databases, returnPartialResult);
        if (newDatabase.isPresent()) {
            BibDatabaseContext databaseContext = new BibDatabaseContext(newDatabase.get());
            LibraryTab libraryTab = LibraryTab.createLibraryTab(
                    databaseContext,
                    tabContainer,
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
            tabContainer.addTab(libraryTab, true);
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
        DetectOpenOfficeInstallation officeInstallation = new DetectOpenOfficeInstallation(openOfficePreferences, dialogService);

        if (officeInstallation.isExecutablePathDefined()) {
            connect();
        } else {
            Task<List<Path>> taskConnectIfInstalled = new Task<>() {
                @Override
                protected List<Path> call() {
                    return OpenOfficeFileSearch.detectInstallations();
                }
            };

            taskConnectIfInstalled.setOnSucceeded(_ -> {
                List<Path> installations = new ArrayList<>(taskConnectIfInstalled.getValue());
                if (installations.isEmpty()) {
                    officeInstallation.selectInstallationPath().ifPresent(installations::add);
                }
                Optional<Path> chosenInstallationDirectory = officeInstallation.chooseAmongInstallations(installations);
                if (chosenInstallationDirectory.isPresent() && officeInstallation.setOpenOfficePreferences(chosenInstallationDirectory.get())) {
                    connect();
                }
            });

            taskConnectIfInstalled.setOnFailed(_ -> dialogService.showErrorDialogAndWait(Localization.lang("Autodetection failed"), Localization.lang("Autodetection failed"), taskConnectIfInstalled.getException()));

            dialogService.showProgressDialog(Localization.lang("Autodetecting paths..."), Localization.lang("Autodetecting paths..."), taskConnectIfInstalled);
            taskExecutor.execute(taskConnectIfInstalled);
        }
    }

    private void connectManually() {
        DirectoryDialogConfiguration fileDialogConfiguration = new DirectoryDialogConfiguration.Builder().withInitialDirectory(System.getProperty("user.home")).build();
        Optional<Path> selectedPath = dialogService.showDirectorySelectionDialog(fileDialogConfiguration);

        DetectOpenOfficeInstallation officeInstallation = new DetectOpenOfficeInstallation(openOfficePreferences, dialogService);

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
        boolean isConnectedToDocument = ooBase != null && !ooBase.isDocumentConnectionMissing();
        boolean hasStyle = currentStyle != null;
        boolean hasDatabase = !getBaseList().isEmpty();
        boolean canCite = isConnectedToDocument && hasStyle && hasDatabase;
        boolean canRefreshDocument = isConnectedToDocument && hasStyle;
        boolean cslStyleSelected = currentStyle instanceof CitationStyle;
        boolean canGenerateBibliography = (currentStyle instanceof JStyle) || (currentStyle instanceof CitationStyle citationStyle && citationStyle.hasBibliography());

        selectDocument.setDisable(!isConnectedToDocument);

        pushEntries.setDisable(!canCite);
        pushEntriesInt.setDisable(!canCite);
        pushEntriesEmpty.setDisable(!canCite);
        pushEntriesAdvanced.setDisable(!canCite || cslStyleSelected);

        update.setDisable(!canRefreshDocument || !canGenerateBibliography);
        merge.setDisable(!canRefreshDocument || cslStyleSelected);
        unmerge.setDisable(!canRefreshDocument || cslStyleSelected);
        manageCitations.setDisable(!canRefreshDocument || cslStyleSelected);
        exportCitations.setDisable(!(isConnectedToDocument && hasDatabase) || cslStyleSelected);
        modifyBibliographyProperties.setDisable(!cslStyleSelected);
    }

    private void connect() {
        Task<OOBibBase> connectTask = new Task<>() {
            @Override
            protected OOBibBase call() throws BootstrapException, CreationException, IOException, InterruptedException {
                updateProgress(ProgressBar.INDETERMINATE_PROGRESS, ProgressBar.INDETERMINATE_PROGRESS);

                Path path = Path.of(openOfficePreferences.getExecutablePath());
                return createBibBase(path);
            }
        };

        connectTask.setOnSucceeded(_ -> {
            ooBase = connectTask.getValue();

            try {
                ooBase.guiActionSelectDocument(true);
            } catch (WrappedTargetException
                     | NoSuchElementException e) {
                throw new RuntimeException(e);
            }

            // Enable actions that depend on a connection
            updateButtonAvailability();
        });

        connectTask.setOnFailed(_ -> {
            Throwable ex = connectTask.getException();
            LOGGER.error("autodetect failed", ex);
            switch (ex) {
                case UnsatisfiedLinkError unsatisfiedLinkError -> {
                    LOGGER.warn("Could not connect to running OpenOffice/LibreOffice", unsatisfiedLinkError);

                    dialogService.showErrorDialogAndWait(Localization.lang("Unable to connect. One possible reason is that JabRef "
                            + "and OpenOffice/LibreOffice are not both running in either 32 bit mode or 64 bit mode."));
                }
                case IOException ioException -> {
                    LOGGER.warn("Could not connect to running OpenOffice/LibreOffice", ioException);

                    dialogService.showErrorDialogAndWait(Localization.lang("Could not connect to running OpenOffice/LibreOffice."),
                            Localization.lang("Could not connect to running OpenOffice/LibreOffice.")
                                    + "\n"
                                    + Localization.lang("Make sure you have installed OpenOffice/LibreOffice with Java support.") + "\n"
                                    + Localization.lang("If connecting manually, please verify program and library paths.") + "\n" + "\n" + Localization.lang("Error message:"),
                            ex);
                }
                case BootstrapException bootstrapEx -> {
                    LOGGER.error("Exception boostrap cause", bootstrapEx.getTargetException());
                    dialogService.showErrorDialogAndWait("Bootstrap error", bootstrapEx.getTargetException());
                }
                case null,
                     default ->
                        dialogService.showErrorDialogAndWait(Localization.lang("Autodetection failed"), Localization.lang("Autodetection failed"), ex);
            }
        });

        dialogService.showProgressDialog(Localization.lang("Autodetecting paths..."), Localization.lang("Autodetecting paths..."), connectTask);
        taskExecutor.execute(connectTask);
    }

    private OOBibBase createBibBase(Path loPath) throws BootstrapException, CreationException, IOException, InterruptedException {
        return new OOBibBase(loPath, dialogService, openOfficePreferences);
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
        return inParenthesis
               ? CitationType.AUTHORYEAR_PAR
               : CitationType.AUTHORYEAR_INTEXT;
    }

    private void pushEntries(CitationType citationType, boolean addPageInfo) {
        final String errorDialogTitle = Localization.lang("Error pushing entries");

        final Optional<BibDatabaseContext> activeDatabase = stateManager.getActiveDatabase();

        if (activeDatabase.isEmpty() || (activeDatabase.get().getDatabase() == null)) {
            OOError.noDataBaseIsOpenForCiting()
                   .setTitle(errorDialogTitle)
                   .showErrorDialog(dialogService);
            return;
        }

        final BibDatabaseContext bibDatabaseContext = activeDatabase.get();

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
                openOfficePreferences.getSyncWhenCiting()
                ? Optional.of(new Update.SyncOptions(getBaseList()))
                : Optional.empty();

        // Sync options are non-null only when "Automatically sync bibliography when inserting citations" is enabled
        if (syncOptions.isPresent() && openOfficePreferences.getSyncWhenCiting()) {
            syncOptions.get().setUpdateBibliography(true);
        }
        ooBase.guiActionInsertEntry(entries,
                bibDatabaseContext,
                entryTypesManager,
                currentStyle,
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
            NamedCompoundEdit undoCompound = new NamedCompoundEdit(Localization.lang("Cite"));
            for (BibEntry entry : entries) {
                if (entry.getCitationKey().isEmpty()) {
                    // Generate key
                    new CitationKeyGenerator(databaseContext.get(), citationKeyPatternPreferences)
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

        CheckMenuItem addSpaceAfter = new CheckMenuItem(Localization.lang("Add space after citation"));
        addSpaceAfter.selectedProperty().set(openOfficePreferences.getAddSpaceAfter());
        addSpaceAfter.setOnAction(_ -> openOfficePreferences.setAddSpaceAfter(addSpaceAfter.isSelected()));

        CheckMenuItem alwaysAddCitedOnPagesText = new CheckMenuItem(Localization.lang("Automatically add \"Cited on pages...\" at the end of bibliographic entries"));
        alwaysAddCitedOnPagesText.selectedProperty().set(openOfficePreferences.getAlwaysAddCitedOnPages());
        alwaysAddCitedOnPagesText.setOnAction(_ -> openOfficePreferences.setAlwaysAddCitedOnPages(alwaysAddCitedOnPagesText.isSelected()));

        EasyBind.listen(currentStyleProperty, (_, _, newValue) -> {
            switch (newValue) {
                case JStyle _ -> {
                    if (!contextMenu.getItems().contains(alwaysAddCitedOnPagesText)) {
                        contextMenu.getItems().add(1, alwaysAddCitedOnPagesText);
                    }
                }
                case CitationStyle _ ->
                        contextMenu.getItems().remove(alwaysAddCitedOnPagesText);
                default -> {
                }
            }
        });

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

        autoSync.setOnAction(_ -> openOfficePreferences.setSyncWhenCiting(autoSync.isSelected()));
        useAllBases.setOnAction(_ -> openOfficePreferences.setUseAllDatabases(useAllBases.isSelected()));
        useActiveBase.setOnAction(_ -> openOfficePreferences.setUseAllDatabases(!useActiveBase.isSelected()));
        clearConnectionSettings.setOnAction(_ -> {
            openOfficePreferences.clearConnectionSettings();
            dialogService.notify(Localization.lang("Cleared connection settings"));
        });

        contextMenu.getItems().addAll(
                autoSync,
                addSpaceAfter,
                new SeparatorMenuItem(),
                useActiveBase,
                useAllBases,
                new SeparatorMenuItem(),
                clearConnectionSettings);

        if (currentStyle instanceof JStyle) {
            contextMenu.getItems().add(1, alwaysAddCitedOnPagesText);
        }

        return contextMenu;
    }
}
