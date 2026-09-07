package org.jabref.gui.openoffice;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
import javafx.scene.control.SeparatorMenuItem;
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
import org.jabref.gui.help.HelpAction;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.util.DirectoryDialogConfiguration;
import org.jabref.gui.util.UiTaskExecutor;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.citationkeypattern.CitationKeyGenerator;
import org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences;
import org.jabref.logic.citationstyle.CSLStyleLoader;
import org.jabref.logic.citationstyle.CitationStyle;
import org.jabref.logic.git.util.GitHandlerRegistry;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.openoffice.OpenOfficeFileSearch;
import org.jabref.logic.openoffice.OpenOfficePreferences;
import org.jabref.logic.openoffice.action.Update;
import org.jabref.logic.openoffice.style.BstStyle;
import org.jabref.logic.openoffice.style.BstStyleLoader;
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
import org.jabref.model.openoffice.util.OOResult;
import org.jabref.model.openoffice.util.OOVoidResult;
import org.jabref.model.util.FileUpdateMonitor;

import com.sun.star.comp.helper.BootstrapException;
import com.sun.star.container.NoSuchElementException;
import com.sun.star.lang.WrappedTargetException;
import com.tobiasdiez.easybind.EasyBind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Pane to manage the interaction between JabRef and OpenOffice.
public class OpenOfficePanel {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenOfficePanel.class);
    private final DialogService dialogService;

    private final Button connect;
    private final Button manualConnect;
    private final Button selectDocument;
    private final Button setStyleFile = new Button(Localization.lang("Select style"));
    private final Button cite = new Button(Localization.lang("Cite"));
    private final Button citeInText = new Button(Localization.lang("Cite in-text"));
    private final Button citeEmpty = new Button(Localization.lang("Insert empty citation"));
    private final Button citeSpecial = new Button(Localization.lang("Cite special"));
    private final Button updateBibliography;
    private final Button merge = new Button(Localization.lang("Merge citations"));
    private final Button unmerge = new Button(Localization.lang("Separate citations"));
    private final Button manageCitations = new Button(Localization.lang("Manage citations"));
    private final Button exportCitations = new Button(Localization.lang("Export cited"));
    private final Button modifyBibliographyProperties = new Button(Localization.lang("Bibliography properties"));
    private final Button settingsB = new Button(Localization.lang("Settings"));
    private final Button inferStyle = new Button(Localization.lang("Infer style"));
    private final Button help;
    private final VBox vbox = new VBox();

    private final GuiPreferences preferences;
    private final OpenOfficePreferences openOfficePreferences;
    private final CitationKeyPatternPreferences citationKeyPatternPreferences;
    private final JournalAbbreviationRepository journalAbbreviationRepository;

    private final StateManager stateManager;
    private final ClipBoardManager clipBoardManager;
    private final UiTaskExecutor taskExecutor;
    private final AiService aiService;
    private final JStyleLoader jStyleLoader;
    private final CSLStyleLoader cslStyleLoader;
    private final BstStyleLoader bstStyleLoader;
    private final LibraryTabContainer tabContainer;
    private final FileUpdateMonitor fileUpdateMonitor;
    private final BibEntryTypesManager entryTypesManager;
    private final GitHandlerRegistry gitHandlerRegistry;
    private OOBibBase ooBase;
    private OOStyle currentStyle;

    private final SimpleObjectProperty<OOStyle> currentStyleProperty;

    public OpenOfficePanel(LibraryTabContainer tabContainer,
                           GuiPreferences preferences,
                           JournalAbbreviationRepository abbreviationRepository,
                           UiTaskExecutor taskExecutor,
                           DialogService dialogService,
                           AiService aiService,
                           StateManager stateManager,
                           FileUpdateMonitor fileUpdateMonitor,
                           BibEntryTypesManager entryTypesManager,
                           ClipBoardManager clipBoardManager,
                           GitHandlerRegistry gitHandlerRegistry) {
        this.tabContainer = tabContainer;
        this.fileUpdateMonitor = fileUpdateMonitor;
        this.entryTypesManager = entryTypesManager;
        this.gitHandlerRegistry = gitHandlerRegistry;
        this.stateManager = stateManager;
        this.clipBoardManager = clipBoardManager;
        this.taskExecutor = taskExecutor;
        this.dialogService = dialogService;
        this.aiService = aiService;

        this.preferences = preferences;
        this.journalAbbreviationRepository = abbreviationRepository;
        this.openOfficePreferences = preferences.getOpenOfficePreferences(journalAbbreviationRepository);
        this.citationKeyPatternPreferences = preferences.getCitationKeyPatternPreferences();
        this.currentStyle = openOfficePreferences.getCurrentStyle();

        this.currentStyleProperty = new SimpleObjectProperty<>(currentStyle);

        jStyleLoader = new JStyleLoader(
                openOfficePreferences,
                preferences.getLayoutFormatterPreferences(),
                abbreviationRepository);

        cslStyleLoader = new CSLStyleLoader(openOfficePreferences);
        bstStyleLoader = new BstStyleLoader(openOfficePreferences);

        ActionFactory factory = new ActionFactory();

        connect = new Button();
        connect.setGraphic(IconTheme.JabRefIcons.CONNECT_OPEN_OFFICE.getGraphicNode());
        connect.setTooltip(new Tooltip(Localization.lang("Connect")));
        connect.setMaxWidth(Double.MAX_VALUE);

        manualConnect = new Button();
        manualConnect.setGraphic(IconTheme.JabRefIcons.CONNECT_OPEN_OFFICE.getGraphicNode());
        manualConnect.setTooltip(new Tooltip(Localization.lang("Manual connect")));
        manualConnect.setMaxWidth(Double.MAX_VALUE);

        help = factory.createIconButton(StandardActions.HELP, new HelpAction(HelpFile.OPENOFFICE_LIBREOFFICE, dialogService, preferences.getExternalApplicationsPreferences()));
        help.setMaxWidth(Double.MAX_VALUE);

        selectDocument = new Button();
        selectDocument.setGraphic(IconTheme.JabRefIcons.OPEN.getGraphicNode());
        selectDocument.setTooltip(new Tooltip(Localization.lang("Select Writer document")));
        selectDocument.setMaxWidth(Double.MAX_VALUE);

        updateBibliography = new Button();
        updateBibliography.setGraphic(IconTheme.JabRefIcons.ADD_OR_MAKE_BIBLIOGRAPHY.getGraphicNode());
        updateBibliography.setTooltip(new Tooltip(Localization.lang("Sync OpenOffice/LibreOffice bibliography")));
        updateBibliography.setMaxWidth(Double.MAX_VALUE);

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

        currentStyle = openOfficePreferences.getCurrentStyle();
        currentStyleProperty.set(currentStyle);
        updateButtonAvailability();

        if (!(currentStyle instanceof JStyle jStyle)) {
            return PASS;
        }

        try {
            jStyle = jStyleLoader.getUsedJstyle();
            jStyle.ensureUpToDate();
            currentStyle = openOfficePreferences.getCurrentStyle();
            currentStyleProperty.set(currentStyle);
            updateButtonAvailability();
        } catch (IOException ex) {
            LOGGER.warn("Unable to reload style file '{}'", jStyle.getPath(), ex);
            String msg = Localization.lang("Unable to reload style file '%0'. %1", jStyle.getPath(), String.valueOf(ex.getMessage()));
            new OOError(title, msg, ex).showErrorDialog(dialogService);
            return FAIL;
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
                currentStyle = openOfficePreferences.getCurrentStyle();
                currentStyleProperty.set(currentStyle);
                updateButtonAvailability();
            } catch (WrappedTargetException
                     | NoSuchElementException ex) {
                LOGGER.warn("Unable to select document to work on", ex);
                OOError.fromMisc(ex).setTitle("Unable to select document to work on").showErrorDialog(dialogService);
            }
        });

        setStyleFile.setMaxWidth(Double.MAX_VALUE);
        setStyleFile.setOnAction(_ -> {
            StyleSelectDialogView styleDialog = new StyleSelectDialogView(cslStyleLoader, jStyleLoader, bstStyleLoader, journalAbbreviationRepository);
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
                                 OOVoidResult<OOError> result = ooBase.writeDocumentCslStyle(cslStyle);
                                 if (ooBase.testDialog(Localization.lang("Problem modifying citation"), result)) {
                                     return;
                                 }
                                 dialogService.notify(Localization.lang("Currently selected CSL Style: '%0'", cslStyle.getName()));
                             } else if (currentStyle instanceof BstStyle bstStyle) {
                                 dialogService.notify(Localization.lang("Currently selected BST style: '%0'", bstStyle.getName()));
                             }
                             updateButtonAvailability();
                         });
        });

        cite.setTooltip(new Tooltip(Localization.lang("Cite selected entries between parenthesis")));
        cite.setOnAction(_ -> cite(CitationType.AUTHORYEAR_PAR, false));
        cite.setMaxWidth(Double.MAX_VALUE);
        citeInText.setTooltip(new Tooltip(Localization.lang("Cite selected entries with in-text citation")));
        citeInText.setOnAction(_ -> cite(CitationType.AUTHORYEAR_INTEXT, false));
        citeInText.setMaxWidth(Double.MAX_VALUE);
        citeEmpty.setTooltip(new Tooltip(Localization.lang("Insert a citation without text (the entry will appear in the reference list)")));
        citeEmpty.setOnAction(_ -> cite(CitationType.INVISIBLE_CIT, false));
        citeEmpty.setMaxWidth(Double.MAX_VALUE);
        openOfficePreferences.zoteroCompatibilityModeProperty().addListener((_, _, _) -> updateButtonAvailability());
        citeSpecial.setTooltip(new Tooltip(Localization.lang("Cite selected entries with extra information")));
        citeSpecial.setOnAction(_ -> cite(CitationType.AUTHORYEAR_INTEXT, true));
        citeSpecial.setMaxWidth(Double.MAX_VALUE);
        inferStyle.setTooltip(new Tooltip(Localization.lang("Infer CSL style from document")));
        inferStyle.setOnAction(_ -> inferStyleFromDocument());
        inferStyle.setMaxWidth(Double.MAX_VALUE);

        updateBibliography.setTooltip(new Tooltip(Localization.lang("Make/Sync bibliography")));

        updateBibliography.setOnAction(_ -> {
            String title = Localization.lang("Could not update bibliography");
            if (getOrUpdateTheStyle(title)) {
                return;
            }
            List<BibDatabase> databases = getDatabaseList();
            ooBase.guiActionUpdateDocument(databases, currentStyle);
        });

        merge.setMaxWidth(Double.MAX_VALUE);
        merge.setTooltip(new Tooltip(Localization.lang("Combine pairs of citations that are separated by spaces only")));
        merge.setOnAction(_ -> ooBase.guiActionMergeCitationGroups(getDatabaseList(), currentStyle));

        unmerge.setMaxWidth(Double.MAX_VALUE);
        unmerge.setTooltip(new Tooltip(Localization.lang("Separate merged citations")));
        unmerge.setOnAction(_ -> ooBase.guiActionSeparateCitations(getDatabaseList(), currentStyle));

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
        modifyBibliographyProperties.setTooltip(new Tooltip(Localization.lang("Modify formatting of the references list")));
        modifyBibliographyProperties.setOnAction(_ -> modifyBibliographyProperties());

        exportCitations.setMaxWidth(Double.MAX_VALUE);
        exportCitations.setTooltip(new Tooltip(Localization.lang("Collect cited entries into a new library")));
        exportCitations.setOnAction(_ -> exportEntries());

        updateButtonAvailability();

        HBox hbox = new HBox();
        hbox.getChildren().addAll(connect, manualConnect, selectDocument, updateBibliography, help);
        hbox.getChildren().forEach(btn -> HBox.setHgrow(btn, Priority.ALWAYS));

        FlowPane flow = new FlowPane();
        flow.setPadding(new Insets(5, 5, 5, 5));
        flow.setVgap(4);
        flow.setHgap(4);
        flow.setPrefWrapLength(200);
        flow.getChildren().addAll(setStyleFile, cite, citeInText, inferStyle);
        flow.getChildren().addAll(citeSpecial, citeEmpty, merge, unmerge);
        flow.getChildren().addAll(manageCitations, exportCitations, modifyBibliographyProperties, settingsB);

        vbox.setFillWidth(true);
        vbox.getChildren().addAll(hbox, flow);
    }

    private void modifyBibliographyProperties() {
        ModifyBibliographyPropertiesDialogView modifyBibliographyPropertiesDialogView = new ModifyBibliographyPropertiesDialogView(openOfficePreferences);
        dialogService.showCustomDialog(modifyBibliographyPropertiesDialogView);
    }

    private void exportEntries() {
        List<BibDatabase> databases = getDatabaseList();
        boolean returnPartialResult = false;
        Optional<BibDatabase> newDatabase = ooBase.exportCitedHelper(databases, currentStyle, returnPartialResult);
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
                    entryTypesManager,
                    clipBoardManager,
                    taskExecutor,
                    gitHandlerRegistry);
            tabContainer.addTab(libraryTab, true);
        }
    }

    private List<BibDatabase> getDatabaseList() {
        if (openOfficePreferences.getUseAllDatabases()) {
            return new ArrayList<>(stateManager.getOpenDatabases().stream()
                                               .map(BibDatabaseContext::getDatabase)
                                               .toList());
        }
        return new ArrayList<>(List.of(
                stateManager.getActiveDatabase()
                            .map(BibDatabaseContext::getDatabase)
                            .orElseGet(BibDatabase::new)));
    }

    private void connectAutomatically() {
        DetectOpenOfficeInstallation officeInstallation = new DetectOpenOfficeInstallation(openOfficePreferences, dialogService);

        final String errorTitle = Localization.lang("Autodetection failed");
        final String progressMessage = Localization.lang("Autodetecting paths...");

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

            taskConnectIfInstalled.setOnFailed(_ -> dialogService.showErrorDialogAndWait(errorTitle, errorTitle, taskConnectIfInstalled.getException()));

            dialogService.showProgressDialog(progressMessage, progressMessage, taskConnectIfInstalled);
            taskExecutor.execute(taskConnectIfInstalled);
        }
    }

    private void connectManually() {
        DirectoryDialogConfiguration fileDialogConfiguration = new DirectoryDialogConfiguration.Builder().withInitialDirectory(System.getProperty("user.home")).build();
        Optional<Path> selectedPath = dialogService.showDirectorySelectionDialog(fileDialogConfiguration);

        final String errorTitle = Localization.lang("Could not connect to running OpenOffice/LibreOffice.");
        final String extendedErrorTitle = Localization.lang("If connecting manually, please verify program and library paths.");

        DetectOpenOfficeInstallation officeInstallation = new DetectOpenOfficeInstallation(openOfficePreferences, dialogService);

        if (selectedPath.isPresent()) {
            BackgroundTask.wrap(() -> officeInstallation.setOpenOfficePreferences(selectedPath.get()))
                          .withInitialMessage("Searching for executable")
                          .onFailure(dialogService::showErrorDialogAndWait).onSuccess(value -> {
                              if (value) {
                                  connect();
                              } else {
                                  dialogService.showErrorDialogAndWait(errorTitle, extendedErrorTitle);
                              }
                          })
                          .executeWith(taskExecutor);
        } else {
            dialogService.showErrorDialogAndWait(errorTitle, extendedErrorTitle);
        }
    }

    private void updateButtonAvailability() {
        boolean isConnectedToDocument = ooBase != null && !ooBase.isDocumentConnectionMissing();
        boolean hasStyle = currentStyle != null;
        boolean hasDatabase = !getDatabaseList().isEmpty();
        boolean canCite = isConnectedToDocument && hasStyle && hasDatabase;
        boolean jstyleSelected = currentStyle instanceof JStyle;
        boolean cslStyleSelected = currentStyle instanceof CitationStyle;
        boolean emptyCitationSupported = jstyleSelected || (cslStyleSelected && !openOfficePreferences.getZoteroCompatibilityMode());
        boolean bstStyleSelected = currentStyle instanceof BstStyle;
        boolean specialCitationSupported = currentStyle instanceof JStyle jStyle
                && !jStyle.isNumberEntries()
                && !jStyle.isCitationKeyCiteMarkers();
        boolean canGenerateBibliography = (isConnectedToDocument && hasDatabase)
                && (jstyleSelected || bstStyleSelected || (currentStyle instanceof CitationStyle citationStyle && citationStyle.hasBibliography()));

        selectDocument.setDisable(!isConnectedToDocument);
        setStyleFile.setDisable(!isConnectedToDocument);

        cite.setDisable(!canCite);
        citeInText.setDisable(!canCite);
        citeEmpty.setDisable(!canCite || !emptyCitationSupported);
        citeSpecial.setDisable(!canCite || !specialCitationSupported);
        inferStyle.setDisable(!isConnectedToDocument || !cslStyleSelected);

        updateBibliography.setDisable(!canGenerateBibliography);
        merge.setDisable(!isConnectedToDocument || !jstyleSelected);
        unmerge.setDisable(!isConnectedToDocument || !jstyleSelected);
        manageCitations.setDisable(!isConnectedToDocument || !jstyleSelected);
        exportCitations.setDisable(!(isConnectedToDocument && hasDatabase));
        modifyBibliographyProperties.setDisable(!canGenerateBibliography);
    }

    private void connect() {
        final String connectionError = Localization.lang("Could not connect to running OpenOffice/LibreOffice.");
        final String autodetectionFailedError = Localization.lang("Autodetection failed");
        final String progressMessage = Localization.lang("Autodetecting paths...");
        final String loggerMessage = "Could not connect to running OpenOffice/LibreOffice";

        Task<OOBibBase> connectTask = new Task<>() {
            @Override
            protected OOBibBase call() throws BootstrapException, CreationException, IOException, InterruptedException {
                updateProgress(ProgressBar.INDETERMINATE_PROGRESS, ProgressBar.INDETERMINATE_PROGRESS);

                Path path = Path.of(openOfficePreferences.getExecutablePath());
                return createBibBase(path);
            }
        };

        connectTask.setOnSucceeded(_ -> {
            if (ooBase != null) {
                ooBase.dispose();
            }
            ooBase = connectTask.getValue();

            try {
                ooBase.guiActionSelectDocument(true);
            } catch (WrappedTargetException
                     | NoSuchElementException e) {
                LOGGER.warn("Unable to connect to document", e);
                OOError.fromMisc(e).showErrorDialog(dialogService);
                return;
            }

            currentStyle = openOfficePreferences.getCurrentStyle();
            currentStyleProperty.set(currentStyle);
            updateButtonAvailability();
        });

        connectTask.setOnFailed(_ -> {
            Throwable ex = connectTask.getException();
            LOGGER.error("autodetect failed", ex);
            switch (ex) {
                case UnsatisfiedLinkError unsatisfiedLinkError -> {
                    LOGGER.warn(loggerMessage, unsatisfiedLinkError);

                    dialogService.showErrorDialogAndWait(Localization.lang("Unable to connect. One possible reason is that JabRef "
                            + "and OpenOffice/LibreOffice are not both running in either 32 bit mode or 64 bit mode."));
                }
                case IOException ioException -> {
                    LOGGER.warn(loggerMessage, ioException);

                    dialogService.showErrorDialogAndWait(connectionError,
                            connectionError
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
                        dialogService.showErrorDialogAndWait(autodetectionFailedError, autodetectionFailedError, ex);
            }
        });

        dialogService.showProgressDialog(progressMessage, progressMessage, connectTask);
        taskExecutor.execute(connectTask);
    }

    private OOBibBase createBibBase(Path loPath) throws BootstrapException, CreationException, IOException, InterruptedException {
        return new OOBibBase(loPath, dialogService, openOfficePreferences, entryTypesManager);
    }

    private void cite(CitationType citationType, boolean addPageInfo) {
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
            Optional<CiteSpecialDialogViewModel> citeDialogViewModel = dialogService.showCustomDialogAndWait(new CiteSpecialDialogView(openOfficePreferences.getCiteSpecialCitationType()));
            if (citeDialogViewModel.isPresent()) {
                CiteSpecialDialogViewModel model = citeDialogViewModel.get();
                if (!model.pageInfoProperty().getValue().isEmpty()) {
                    pageInfo = model.pageInfoProperty().getValue();
                }
                citationType = model.citationTypeProperty().getValue();
                openOfficePreferences.setCiteSpecialCitationType(citationType);
            } else {
                // user canceled
                return;
            }
        }

        if (!checkThatEntriesHaveKeys(entries)) {
            // Not all entries have keys and key generation was declined.
            return;
        }

        List<BibDatabase> selectedDatabases = getDatabaseList();
        Optional<Update.SyncOptions> syncOptions =
                openOfficePreferences.getSyncWhenCiting()
                ? Optional.of(new Update.SyncOptions(selectedDatabases))
                : Optional.empty();

        // Sync options are non-null only when "Automatically sync bibliography when inserting citations" is enabled
        if (syncOptions.isPresent() && openOfficePreferences.getSyncWhenCiting()) {
            syncOptions.get().setUpdateBibliography(true);
        }
        ooBase.guiActionInsertEntry(entries,
                bibDatabaseContext,
                selectedDatabases,
                currentStyle,
                citationType,
                pageInfo,
                syncOptions);
    }

    private void inferStyleFromDocument() {
        final String errorDialogTitle = Localization.lang("Could not infer CSL style from document");
        OOResult<Optional<CitationStyle>, OOError> result = ooBase.inferCslStyleFromDocument();
        if (result.ifError(error -> ooBase.showDialog(error.setTitle(errorDialogTitle))).isError()) {
            return;
        }

        result.get().ifPresentOrElse(style -> {
            currentStyle = style;
            currentStyleProperty.set(style);
            updateButtonAvailability();
            dialogService.notify(Localization.lang("Inferred CSL style: '%0'", style.getName()));
        }, () -> dialogService.notify(Localization.lang("No CSL style could be inferred from the document.")));
    }

    /// Check that all entries in the list have citation keys, if not ask if they should be generated
    ///
    /// @param entries A list of entries to be checked
    /// @return true if all entries have citation keys, if it so may be after generating them
    private boolean checkThatEntriesHaveKeys(List<BibEntry> entries) {
        // Check if there are empty keys
        // Found one, no need to look further for now
        boolean emptyKeys = entries.stream()
                                   .anyMatch(entry -> entry.getCitationKey().isEmpty());
        // If no empty keys, return true
        if (!emptyKeys) {
            return true;
        }

        // Ask if keys should be generated
        boolean citePressed = dialogService.showConfirmationDialogAndWait(Localization.lang("Cite"),
                Localization.lang("Cannot cite entries without citation keys. Generate keys now?"),
                Localization.lang("Generate keys"),
                Localization.lang("Cancel"));

        if (!citePressed) {
            // The user canceled
            return false;
        }

        return stateManager.getActiveDatabase().map(databaseContext -> {
            // Generate keys
            stateManager.getUndoManager(databaseContext).addEdit(Localization.lang("Cite"), edit -> {
                for (BibEntry entry : entries) {
                    if (entry.getCitationKey().isEmpty()) {
                        // Generate key
                        edit.addEdit(new CitationKeyGenerator(databaseContext, citationKeyPatternPreferences)
                                .generateAndSetKey(entry));
                    }
                }
            });
            // Now every entry has a key
            return true;
            // There is no panel to get the database from, highly unlikely
        }).orElse(false);
    }

    private ContextMenu createSettingsPopup() {
        ContextMenu contextMenu = new ContextMenu();

        CheckMenuItem autoSync = new CheckMenuItem(Localization.lang("Automatically sync bibliography when inserting citations"));
        autoSync.selectedProperty().set(openOfficePreferences.getSyncWhenCiting());

        CheckMenuItem addSpaceBefore = new CheckMenuItem(Localization.lang("Add space before citation"));
        addSpaceBefore.selectedProperty().set(openOfficePreferences.getAddSpaceBefore());
        addSpaceBefore.setOnAction(_ -> openOfficePreferences.setAddSpaceBefore(addSpaceBefore.isSelected()));

        CheckMenuItem addSpaceAfter = new CheckMenuItem(Localization.lang("Add space after citation"));
        addSpaceAfter.selectedProperty().set(openOfficePreferences.getAddSpaceAfter());
        addSpaceAfter.setOnAction(_ -> openOfficePreferences.setAddSpaceAfter(addSpaceAfter.isSelected()));

        CheckMenuItem alwaysAddCitedOnPagesText = new CheckMenuItem(Localization.lang("Automatically add \"Cited on pages...\" at the end of bibliographic entries"));
        alwaysAddCitedOnPagesText.selectedProperty().set(openOfficePreferences.getAlwaysAddCitedOnPages());
        alwaysAddCitedOnPagesText.setOnAction(_ -> openOfficePreferences.setAlwaysAddCitedOnPages(alwaysAddCitedOnPagesText.isSelected()));
        alwaysAddCitedOnPagesText.disableProperty().bind(currentStyleProperty.map(style -> !(style instanceof JStyle)));

        CheckMenuItem onlyUseActiveTab = new CheckMenuItem(Localization.lang("Look up BibTeX entries in the currently selected library only"));
        onlyUseActiveTab.setSelected(!openOfficePreferences.getUseAllDatabases());

        MenuItem clearConnectionSettings = new MenuItem(Localization.lang("Clear connection settings"));

        autoSync.setOnAction(_ -> openOfficePreferences.setSyncWhenCiting(autoSync.isSelected()));
        onlyUseActiveTab.setOnAction(_ -> openOfficePreferences.setUseAllDatabases(!onlyUseActiveTab.isSelected()));
        clearConnectionSettings.setOnAction(_ -> {
            openOfficePreferences.clearConnectionSettings();
            dialogService.notify(Localization.lang("Cleared connection settings"));
        });

        contextMenu.getItems().addAll(
                autoSync,
                alwaysAddCitedOnPagesText,
                addSpaceBefore,
                addSpaceAfter,
                new SeparatorMenuItem(),
                onlyUseActiveTab,
                new SeparatorMenuItem(),
                clearConnectionSettings);

        EasyBind.subscribe(currentStyleProperty, newValue -> {
            updatePreferences(newValue);
        });

        return contextMenu;
    }

    private void updatePreferences(OOStyle currentStyle) {
        if (!(currentStyle instanceof CitationStyle)) {
            openOfficePreferences.setZoteroCompatibilityMode(false);
            openOfficePreferences.setInferCslStyleFromDocument(false);
        }
    }
}
