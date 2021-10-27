package org.jabref.gui.openoffice;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
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
import org.jabref.logic.openoffice.UndefinedParagraphFormatException;
import org.jabref.logic.openoffice.style.OOBibStyle;
import org.jabref.logic.openoffice.style.StyleLoader;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.openoffice.uno.CreationException;
import org.jabref.model.openoffice.uno.NoDocumentException;
import org.jabref.preferences.PreferencesService;

import com.sun.star.beans.IllegalTypeException;
import com.sun.star.beans.NotRemoveableException;
import com.sun.star.beans.PropertyExistException;
import com.sun.star.beans.PropertyVetoException;
import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.comp.helper.BootstrapException;
import com.sun.star.container.NoSuchElementException;
import com.sun.star.lang.WrappedTargetException;
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
    private OpenOfficePreferences ooPrefs;
    private OOBibBase ooBase;
    private OOBibStyle style;

    public OpenOfficePanel(PreferencesService preferencesService,
                           OpenOfficePreferences ooPrefs,
                           KeyBindingRepository keyBindingRepository,
                           TaskExecutor taskExecutor,
                           DialogService dialogService,
                           StateManager stateManager,
                           UndoManager undoManager) {
        ActionFactory factory = new ActionFactory(keyBindingRepository);
        this.ooPrefs = ooPrefs;
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

        loader = new StyleLoader(ooPrefs,
                preferencesService.getLayoutFormatterPreferences(Globals.journalAbbreviationRepository),
                preferencesService.getGeneralPreferences().getDefaultEncoding());

        initPanel();
    }

    public Node getContent() {
        return vbox;
    }

    private void initPanel() {

        connect.setOnAction(e -> connectAutomatically());
        manualConnect.setOnAction(e -> connectManually());

        selectDocument.setTooltip(new Tooltip(Localization.lang("Select which open Writer document to work on")));
        selectDocument.setOnAction(e -> {

            try {
                ooBase.selectDocument();
                dialogService.notify(Localization.lang("Connected to document") + ": "
                                     + ooBase.getCurrentDocumentTitle().orElse(""));
            } catch (WrappedTargetException | IndexOutOfBoundsException |
                     NoSuchElementException | NoDocumentException ex) {
                LOGGER.warn("Problem connecting", ex);
                dialogService.showErrorDialogAndWait(ex);
            }
        });

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
        pushEntries.setOnAction(e -> pushEntries(true, true, false));
        pushEntries.setMaxWidth(Double.MAX_VALUE);
        pushEntriesInt.setTooltip(new Tooltip(Localization.lang("Cite selected entries with in-text citation")));
        pushEntriesInt.setOnAction(e -> pushEntries(false, true, false));
        pushEntriesInt.setMaxWidth(Double.MAX_VALUE);
        pushEntriesEmpty.setTooltip(new Tooltip(Localization.lang("Insert a citation without text (the entry will appear in the reference list)")));
        pushEntriesEmpty.setOnAction(e -> pushEntries(false, false, false));
        pushEntriesEmpty.setMaxWidth(Double.MAX_VALUE);
        pushEntriesAdvanced.setTooltip(new Tooltip(Localization.lang("Cite selected entries with extra information")));
        pushEntriesAdvanced.setOnAction(e -> pushEntries(false, true, true));
        pushEntriesAdvanced.setMaxWidth(Double.MAX_VALUE);

        update.setTooltip(new Tooltip(Localization.lang("Ensure that the bibliography is up-to-date")));

        update.setOnAction(event -> {
            try {
                if (style == null) {
                    style = loader.getUsedStyle();
                } else {
                    style.ensureUpToDate();
                }

                ooBase.updateSortedReferenceMarks();

                List<BibDatabase> databases = getBaseList();
                List<String> unresolvedKeys = ooBase.refreshCiteMarkers(databases, style);
                ooBase.rebuildBibTextSection(databases, style);
                if (!unresolvedKeys.isEmpty()) {
                    dialogService.showErrorDialogAndWait(Localization.lang("Unable to synchronize bibliography"),
                                                         Localization.lang("Your OpenOffice/LibreOffice document references the citation key '%0', which could not be found in your current library.",
                                                                           unresolvedKeys.get(0)));
                }
            } catch (UndefinedCharacterFormatException ex) {
                reportUndefinedCharacterFormat(ex);
            } catch (UndefinedParagraphFormatException ex) {
                reportUndefinedParagraphFormat(ex);
            } catch (ConnectionLostException ex) {
                showConnectionLostErrorMessage();
            } catch (IOException ex) {
                LOGGER.warn("Problem with style file", ex);
                dialogService.showErrorDialogAndWait(Localization.lang("No valid style file defined"),
                                                     Localization.lang("You must select either a valid style file, or use one of the default styles."));
            } catch (BibEntryNotFoundException ex) {
                LOGGER.debug("BibEntry not found", ex);
                dialogService.showErrorDialogAndWait(Localization.lang("Unable to synchronize bibliography"), Localization.lang(
                                                                                                                                "Your OpenOffice/LibreOffice document references the citation key '%0', which could not be found in your current library.",
                                                                                                                                ex.getCitationKey()));
            } catch (com.sun.star.lang.IllegalArgumentException | PropertyVetoException | UnknownPropertyException | WrappedTargetException | NoSuchElementException |
                     CreationException ex) {
                LOGGER.warn("Could not update bibliography", ex);
            }
        });

        merge.setMaxWidth(Double.MAX_VALUE);
        merge.setTooltip(new Tooltip(Localization.lang("Combine pairs of citations that are separated by spaces only")));
        merge.setOnAction(e -> {
            try {
                ooBase.combineCiteMarkers(getBaseList(), style);
            } catch (UndefinedCharacterFormatException ex) {
                reportUndefinedCharacterFormat(ex);
            } catch (com.sun.star.lang.IllegalArgumentException | UnknownPropertyException | PropertyVetoException |
                     CreationException | NoSuchElementException | WrappedTargetException | IOException |
                     BibEntryNotFoundException ex) {
                LOGGER.warn("Problem combining cite markers", ex);
            }
        });

        unmerge.setMaxWidth(Double.MAX_VALUE);
        unmerge.setTooltip(new Tooltip(Localization.lang("Separate merged citations")));
        unmerge.setOnAction(e -> {
            try {
                ooBase.unCombineCiteMarkers(getBaseList(), style);
            } catch (UndefinedCharacterFormatException ex) {
                reportUndefinedCharacterFormat(ex);
            } catch (com.sun.star.lang.IllegalArgumentException | UnknownPropertyException | PropertyVetoException |
                     CreationException | NoSuchElementException | WrappedTargetException | IOException |
                     BibEntryNotFoundException ex) {
                LOGGER.warn("Problem uncombining cite markers", ex);
            }
        });

        ContextMenu settingsMenu = createSettingsPopup();
        settingsB.setMaxWidth(Double.MAX_VALUE);
        settingsB.setContextMenu(settingsMenu);
        settingsB.setOnAction(e -> settingsMenu.show(settingsB, Side.BOTTOM, 0, 0));
        manageCitations.setMaxWidth(Double.MAX_VALUE);
        manageCitations.setOnAction(e -> dialogService.showCustomDialogAndWait(new ManageCitationsDialogView(ooBase)));

        exportCitations.setMaxWidth(Double.MAX_VALUE);
        exportCitations.setOnAction(event -> exportEntries());

        selectDocument.setDisable(true);
        pushEntries.setDisable(true);
        pushEntriesInt.setDisable(true);
        pushEntriesEmpty.setDisable(true);
        pushEntriesAdvanced.setDisable(true);
        update.setDisable(true);
        merge.setDisable(true);
        unmerge.setDisable(true);
        manageCitations.setDisable(true);
        exportCitations.setDisable(true);

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
        try {
            if (style == null) {
                style = loader.getUsedStyle();
            } else {
                style.ensureUpToDate();
            }

            ooBase.updateSortedReferenceMarks();

            List<BibDatabase> databases = getBaseList();
            List<String> unresolvedKeys = ooBase.refreshCiteMarkers(databases, style);
            BibDatabase newDatabase = ooBase.generateDatabase(databases);
            if (!unresolvedKeys.isEmpty()) {
                dialogService.showErrorDialogAndWait(Localization.lang("Unable to generate new library"),
                                                     Localization.lang("Your OpenOffice/LibreOffice document references the citation key '%0', which could not be found in your current library.",
                                                                       unresolvedKeys.get(0)));
            }

            BibDatabaseContext databaseContext = new BibDatabaseContext(newDatabase);

            JabRefGUI.getMainFrame().addTab(databaseContext, true);
        } catch (BibEntryNotFoundException ex) {
            LOGGER.debug("BibEntry not found", ex);
            dialogService.showErrorDialogAndWait(Localization.lang("Unable to synchronize bibliography"),
                                                 Localization.lang("Your OpenOffice/LibreOffice document references the citation key '%0', which could not be found in your current library.",
                                                                   ex.getCitationKey()));
        } catch (com.sun.star.lang.IllegalArgumentException | UnknownPropertyException | PropertyVetoException |
                 UndefinedCharacterFormatException | NoSuchElementException | WrappedTargetException | IOException |
                 CreationException e) {
            LOGGER.warn("Problem generating new database.", e);
        }
    }

    private List<BibDatabase> getBaseList() {
        List<BibDatabase> databases = new ArrayList<>();
        if (ooPrefs.getUseAllDatabases()) {
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

    private void connect() {
        ooPrefs = preferencesService.getOpenOfficePreferences();

        Task<OOBibBase> connectTask = new Task<>() {

            @Override
            protected OOBibBase call() throws Exception {
                updateProgress(ProgressBar.INDETERMINATE_PROGRESS, ProgressBar.INDETERMINATE_PROGRESS);

                var path = Path.of(ooPrefs.getExecutablePath());
                return createBibBase(path);
            }
        };

        connectTask.setOnSucceeded(value -> {
            ooBase = connectTask.getValue();

            try {
                ooBase.selectDocument();
            } catch (NoSuchElementException | WrappedTargetException | NoDocumentException ex) {
                dialogService.showErrorDialogAndWait(Localization.lang("Error connecting to Writer document"), Localization.lang("You need to open Writer with a document before connecting"), ex);
                LOGGER.error("Error connecting to writer document", ex);
            }

            if (ooBase.isConnectedToDocument()) {
                dialogService.notify(Localization.lang("Connected to document") + ": " + ooBase.getCurrentDocumentTitle().orElse(""));
            }

            // Enable actions that depend on Connect:
            selectDocument.setDisable(false);
            pushEntries.setDisable(false);
            pushEntriesInt.setDisable(false);
            pushEntriesEmpty.setDisable(false);
            pushEntriesAdvanced.setDisable(false);
            update.setDisable(false);
            merge.setDisable(false);
            unmerge.setDisable(false);
            manageCitations.setDisable(false);
            exportCitations.setDisable(false);
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

    private OOBibBase createBibBase(Path loPath) throws IOException, InvocationTargetException, IllegalAccessException,
        BootstrapException, CreationException, ClassNotFoundException {
        return new OOBibBase(loPath, true, dialogService);
    }

    private void pushEntries(boolean inParenthesisIn, boolean withText, boolean addPageInfo) {
        if (!ooBase.isConnectedToDocument()) {
            dialogService.showErrorDialogAndWait(Localization.lang("Error pushing entries"), Localization.lang("Not connected to any Writer document. Please" + " make sure a document is open, and use the 'Select Writer document' button to connect to it."));
            return;
        }

        Boolean inParenthesis = inParenthesisIn;
        String pageInfo = null;
        if (addPageInfo) {

            Optional<AdvancedCiteDialogViewModel> citeDialogViewModel = dialogService.showCustomDialogAndWait(new AdvancedCiteDialogView());
            if (citeDialogViewModel.isPresent()) {

                AdvancedCiteDialogViewModel model = citeDialogViewModel.get();
                if (!model.pageInfoProperty().getValue().isEmpty()) {
                    pageInfo = model.pageInfoProperty().getValue();
                }
                inParenthesis = model.citeInParProperty().getValue();
            }
        }

        Optional<BibDatabaseContext> databaseContext = stateManager.getActiveDatabase();
        if (databaseContext.isPresent()) {
            final BibDatabase database = databaseContext.get().getDatabase();
            final List<BibEntry> entries = stateManager.getSelectedEntries();
            if (!entries.isEmpty() && checkThatEntriesHaveKeys(entries)) {

                try {
                    if (style == null) {
                        style = loader.getUsedStyle();
                    }
                    ooBase.insertEntry(entries, database, getBaseList(), style, inParenthesis, withText, pageInfo,
                                       ooPrefs.getSyncWhenCiting());
                } catch (FileNotFoundException ex) {

                    dialogService.showErrorDialogAndWait(
                                                         Localization.lang("No valid style file defined"),
                                                         Localization.lang("You must select either a valid style file, or use one of the default styles."));

                    LOGGER.warn("Problem with style file", ex);
                } catch (ConnectionLostException ex) {
                    showConnectionLostErrorMessage();
                } catch (UndefinedCharacterFormatException ex) {
                    reportUndefinedCharacterFormat(ex);
                } catch (UndefinedParagraphFormatException ex) {
                    reportUndefinedParagraphFormat(ex);
                } catch (com.sun.star.lang.IllegalArgumentException | UnknownPropertyException | PropertyVetoException |
                         CreationException | NoSuchElementException | WrappedTargetException | IOException |
                         BibEntryNotFoundException | IllegalTypeException | PropertyExistException |
                         NotRemoveableException ex) {
                    LOGGER.warn("Could not insert entry", ex);
                }
            }
        }
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

    private void showConnectionLostErrorMessage() {
        dialogService.showErrorDialogAndWait(Localization.lang("Connection lost"),
                                             Localization.lang("Connection to OpenOffice/LibreOffice has been lost. " + "Please make sure OpenOffice/LibreOffice is running, and try to reconnect."));
    }

    private void reportUndefinedParagraphFormat(UndefinedParagraphFormatException ex) {
        dialogService.showErrorDialogAndWait(Localization.lang("Undefined paragraph format"),
                                             Localization.lang("Your style file specifies the paragraph format '%0', "
                                                               + "which is undefined in your current OpenOffice/LibreOffice document.",
                                                               ex.getFormatName()) + "\n" + Localization.lang("The paragraph format is controlled by the property 'ReferenceParagraphFormat' or 'ReferenceHeaderParagraphFormat' in the style file."));
    }

    private void reportUndefinedCharacterFormat(UndefinedCharacterFormatException ex) {
        dialogService.showErrorDialogAndWait(Localization.lang("Undefined character format"),
                                             Localization.lang("Your style file specifies the character format '%0', "
                                                               + "which is undefined in your current OpenOffice/LibreOffice document.",
                                                               ex.getFormatName()) + "\n" + Localization.lang("The character format is controlled by the citation property 'CitationCharacterFormat' in the style file.")

        );
    }

    private ContextMenu createSettingsPopup() {

        ContextMenu contextMenu = new ContextMenu();

        CheckMenuItem autoSync = new CheckMenuItem(Localization.lang("Automatically sync bibliography when inserting citations"));
        autoSync.selectedProperty().set(ooPrefs.getSyncWhenCiting());

        ToggleGroup toggleGroup = new ToggleGroup();
        RadioMenuItem useActiveBase = new RadioMenuItem(Localization.lang("Look up BibTeX entries in the active tab only"));
        RadioMenuItem useAllBases = new RadioMenuItem(Localization.lang("Look up BibTeX entries in all open libraries"));
        useActiveBase.setToggleGroup(toggleGroup);
        useAllBases.setToggleGroup(toggleGroup);

        MenuItem clearConnectionSettings = new MenuItem(Localization.lang("Clear connection settings"));

        if (ooPrefs.getUseAllDatabases()) {
            useAllBases.setSelected(true);
        } else {
            useActiveBase.setSelected(true);
        }

        autoSync.setOnAction(e -> {
            ooPrefs.setSyncWhenCiting(autoSync.isSelected());
            preferencesService.setOpenOfficePreferences(ooPrefs);
        });
        useAllBases.setOnAction(e -> {
            ooPrefs.setUseAllDatabases(useAllBases.isSelected());
            preferencesService.setOpenOfficePreferences(ooPrefs);
        });
        useActiveBase.setOnAction(e -> {
            ooPrefs.setUseAllDatabases(!useActiveBase.isSelected());
            preferencesService.setOpenOfficePreferences(ooPrefs);
        });
        clearConnectionSettings.setOnAction(e -> {
            ooPrefs.clearConnectionSettings();
            dialogService.notify(Localization.lang("Cleared connection settings"));
            preferencesService.setOpenOfficePreferences(ooPrefs);
        });

        contextMenu.getItems().addAll(autoSync, new SeparatorMenuItem(), useActiveBase, useAllBases, new SeparatorMenuItem(), clearConnectionSettings);

        return contextMenu;
    }
}
