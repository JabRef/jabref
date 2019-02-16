package org.jabref.gui.openoffice;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javafx.concurrent.Task;
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
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import org.jabref.Globals;
import org.jabref.gui.BasePanel;
import org.jabref.gui.DialogService;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.actions.ActionFactory;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.help.HelpAction;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.keyboard.KeyBindingRepository;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.undo.UndoableKeyChange;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.bibtexkeypattern.BibtexKeyGenerator;
import org.jabref.logic.bibtexkeypattern.BibtexKeyPatternPreferences;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.openoffice.OOBibStyle;
import org.jabref.logic.openoffice.OpenOfficePreferences;
import org.jabref.logic.openoffice.StyleLoader;
import org.jabref.logic.openoffice.UndefinedParagraphFormatException;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.Defaults;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.preferences.JabRefPreferences;

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
    private final Button manageCitations = new Button(Localization.lang("Manage citations"));
    private final Button exportCitations = new Button(Localization.lang("Export cited"));
    private final Button settingsB = new Button(Localization.lang("Settings"));
    private final Button help;
    private final VBox vbox = new VBox();

    private OOBibBase ooBase;
    private final JabRefFrame frame;
    private OOBibStyle style;
    private final JabRefPreferences jabRefPreferences;
    private final TaskExecutor taskExecutor;
    private final StyleLoader loader;
    private OpenOfficePreferences ooPrefs;

    public OpenOfficePanel(JabRefFrame frame, JabRefPreferences jabRefPreferences, OpenOfficePreferences ooPrefs, KeyBindingRepository keyBindingRepository) {
        ActionFactory factory = new ActionFactory(keyBindingRepository);
        this.frame = frame;
        this.ooPrefs = ooPrefs;
        this.jabRefPreferences = jabRefPreferences;
        this.taskExecutor = Globals.TASK_EXECUTOR;
        dialogService = frame.getDialogService();

        connect = new Button();
        connect.setGraphic(IconTheme.JabRefIcons.CONNECT_OPEN_OFFICE.getGraphicNode());
        connect.setTooltip(new Tooltip(Localization.lang("Connect")));
        connect.setMaxWidth(Double.MAX_VALUE);

        manualConnect = new Button();
        manualConnect.setGraphic(IconTheme.JabRefIcons.CONNECT_OPEN_OFFICE.getGraphicNode());
        manualConnect.setTooltip(new Tooltip(Localization.lang("Manual connect")));
        manualConnect.setMaxWidth(Double.MAX_VALUE);

        HelpAction helpCommand = new HelpAction(HelpFile.OPENOFFICE_LIBREOFFICE);

        help = factory.createIconButton(StandardActions.HELP, helpCommand.getCommand());
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
                Globals.prefs.getLayoutFormatterPreferences(Globals.journalAbbreviationLoader),
                Globals.prefs.getDefaultEncoding());

        initPanel();
    }

    private static void addURL(List<URL> jarList) throws IOException {
        URLClassLoader sysloader = (URLClassLoader) ClassLoader.getSystemClassLoader();
        Class<URLClassLoader> sysclass = URLClassLoader.class;
        try {
            Method method = sysclass.getDeclaredMethod("addURL", URL.class);
            method.setAccessible(true);
            for (URL anU : jarList) {
                method.invoke(sysloader, anU);
            }
        } catch (SecurityException | NoSuchMethodException | IllegalAccessException | IllegalArgumentException |
                InvocationTargetException e) {
            LOGGER.error("Could not add URL to system classloader", e);
            sysloader.close();
            throw new IOException("Error, could not add URL to system classloader", e);
        }
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
            } catch (UnknownPropertyException | WrappedTargetException | IndexOutOfBoundsException |
                    NoSuchElementException | NoDocumentException ex) {
                LOGGER.warn("Problem connecting", ex);
                dialogService.showErrorDialogAndWait(ex);
            }

        });

        setStyleFile.setMaxWidth(Double.MAX_VALUE);
        setStyleFile.setOnAction(event -> {

            StyleSelectDialogView styleDialog = new StyleSelectDialogView(loader);
            styleDialog.showAndWait().ifPresent(selectedStyle -> {
                style = selectedStyle;
                try {
                    style.ensureUpToDate();
                } catch (IOException e) {
                    LOGGER.warn("Unable to reload style file '" + style.getPath() + "'", e);
                }
                dialogService.notify(Localization.lang("Current style is '%0'", style.getName()));
            });

        });

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
                            Localization.lang("Your OpenOffice/LibreOffice document references the BibTeX key '%0', which could not be found in your current library.",
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
                        "Your OpenOffice/LibreOffice document references the BibTeX key '%0', which could not be found in your current library.",
                        ex.getBibtexKey()));
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
        ContextMenu settingsMenu = createSettingsPopup();
        settingsB.setMaxWidth(Double.MAX_VALUE);
        settingsB.setContextMenu(settingsMenu);
        settingsB.setOnAction(e -> settingsMenu.show(settingsB, Side.BOTTOM, 0, 0));
        manageCitations.setMaxWidth(Double.MAX_VALUE);
        manageCitations.setOnAction(e -> {
            ManageCitationsDialogView dlg = new ManageCitationsDialogView(ooBase);
            dlg.showAndWait();
        });

        exportCitations.setMaxWidth(Double.MAX_VALUE);
        exportCitations.setOnAction(event -> exportEntries());

        selectDocument.setDisable(true);
        pushEntries.setDisable(true);
        pushEntriesInt.setDisable(true);
        pushEntriesEmpty.setDisable(true);
        pushEntriesAdvanced.setDisable(true);
        update.setDisable(true);
        merge.setDisable(true);
        manageCitations.setDisable(true);
        exportCitations.setDisable(true);

        HBox hbox = new HBox();
        hbox.getChildren().addAll(connect, manualConnect, selectDocument, update, help);
        hbox.getChildren().forEach(btn -> HBox.setHgrow(btn, Priority.ALWAYS));

        vbox.setFillWidth(true);
        vbox.getChildren().addAll(hbox, setStyleFile, pushEntries, pushEntriesInt, pushEntriesAdvanced, pushEntriesEmpty, merge, manageCitations, exportCitations, settingsB);
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
                        Localization.lang("Your OpenOffice/LibreOffice document references the BibTeX key '%0', which could not be found in your current library.",
                                unresolvedKeys.get(0)));

            }

            Defaults defaults = new Defaults(jabRefPreferences.getDefaultBibDatabaseMode());
            BibDatabaseContext databaseContext = new BibDatabaseContext(newDatabase, defaults);
            this.frame.addTab(databaseContext, true);

        } catch (BibEntryNotFoundException ex) {
            LOGGER.debug("BibEntry not found", ex);
            dialogService.showErrorDialogAndWait(Localization.lang("Unable to synchronize bibliography"),
                    Localization.lang("Your OpenOffice/LibreOffice document references the BibTeX key '%0', which could not be found in your current library.",
                            ex.getBibtexKey()));

        } catch (com.sun.star.lang.IllegalArgumentException | UnknownPropertyException | PropertyVetoException |
                UndefinedCharacterFormatException | NoSuchElementException | WrappedTargetException | IOException |
                CreationException e) {
            LOGGER.warn("Problem generating new database.", e);
        }

    }

    private List<BibDatabase> getBaseList() {
        List<BibDatabase> databases = new ArrayList<>();
        if (ooPrefs.getUseAllDatabases()) {
            for (BasePanel basePanel : frame.getBasePanelList()) {
                databases.add(basePanel.getDatabase());
            }
        } else {
            databases.add(frame.getCurrentBasePanel().getDatabase());
        }

        return databases;
    }

    private void connectAutomatically() {
        DetectOpenOfficeInstallation officeInstallation = new DetectOpenOfficeInstallation(jabRefPreferences, dialogService);

        if (officeInstallation.isExecutablePathDefined()) {
            connect();
        } else {

            Task<Void> taskConnectIfInstalled = new Task<Void>() {

                @Override
                protected Void call() throws Exception {
                    updateProgress(ProgressBar.INDETERMINATE_PROGRESS, ProgressBar.INDETERMINATE_PROGRESS);

                    boolean installed = officeInstallation.isInstalled();
                    if (!installed) {
                        throw new IllegalStateException("OpenOffice Installation could not be detected.");
                    }
                    return null; // can not use BackgroundTask.wrap(Runnable) because Runnable.run() can't throw exceptions
                }
            };

            taskConnectIfInstalled.setOnSucceeded(value -> connect());
            taskConnectIfInstalled.setOnFailed(value -> dialogService.showErrorDialogAndWait(Localization.lang("Autodetection failed"), Localization.lang("Autodetection failed"), taskConnectIfInstalled.getException()));

            dialogService.showProgressDialogAndWait(Localization.lang("Autodetecting paths..."), Localization.lang("Autodetecting paths..."), taskConnectIfInstalled);
            taskExecutor.execute(taskConnectIfInstalled);
        }
    }

    private void connectManually() {
        showManualConnectionDialog().ifPresent(ok -> connect());
    }

    private void connect() {
        ooPrefs = jabRefPreferences.getOpenOfficePreferences();

        Task<OOBibBase> connectTask = new Task<OOBibBase>() {

            @Override
            protected OOBibBase call() throws Exception {
                updateProgress(ProgressBar.INDETERMINATE_PROGRESS, ProgressBar.INDETERMINATE_PROGRESS);
                loadOpenOfficeJars(Paths.get(ooPrefs.getInstallationPath()));

                return createBibBase();
            }
        };

        connectTask.setOnSucceeded(value -> {
            ooBase = connectTask.getValue();

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

        dialogService.showProgressDialogAndWait(Localization.lang("Autodetecting paths..."), Localization.lang("Autodetecting paths..."), connectTask);
        taskExecutor.execute(connectTask);

    }

    private void loadOpenOfficeJars(Path configurationPath) throws IOException {
        List<Optional<Path>> filePaths = OpenOfficePreferences.OO_JARS.stream().map(jar -> FileUtil.find(jar, configurationPath)).collect(Collectors.toList());

        if (!filePaths.stream().allMatch(Optional::isPresent)) {
            throw new IOException("(Not all) required Open Office Jars were found inside installation path. Searched for " + OpenOfficePreferences.OO_JARS + " in " + configurationPath);
        }

        List<URL> jarURLs = new ArrayList<>(OpenOfficePreferences.OO_JARS.size());
        for (Optional<Path> jarPath : filePaths) {
            jarURLs.add((jarPath.get().toUri().toURL()));
        }
        addURL(jarURLs);
    }

    private OOBibBase createBibBase() throws IOException, InvocationTargetException, IllegalAccessException,
            WrappedTargetException, BootstrapException, UnknownPropertyException, NoDocumentException,
            NoSuchElementException, CreationException {
        // Connect
        return new OOBibBase(ooPrefs.getExecutablePath(), true);
    }

    private Optional<Boolean> showManualConnectionDialog() {
        return new ManualConnectDialogView(dialogService).showAndWait();
    }

    private void pushEntries(boolean inParenthesisIn, boolean withText, boolean addPageInfo) {
        if (!ooBase.isConnectedToDocument()) {
            dialogService.showErrorDialogAndWait(Localization.lang("Error pushing entries"), Localization.lang("Not connected to any Writer document. Please" + " make sure a document is open, and use the 'Select Writer document' button to connect to it."));
            return;
        }

        Boolean inParenthesis = inParenthesisIn;
        String pageInfo = null;
        if (addPageInfo) {

            AdvancedCiteDialogView citeDialog = new AdvancedCiteDialogView();
            Optional<AdvancedCiteDialogViewModel> citeDialogViewModel = citeDialog.showAndWait();

            if (citeDialogViewModel.isPresent()) {

                AdvancedCiteDialogViewModel model = citeDialogViewModel.get();
                if (!model.pageInfoProperty().getValue().isEmpty()) {
                    pageInfo = model.pageInfoProperty().getValue();
                }
                inParenthesis = model.citeInParProperty().getValue();
            }
        }

        BasePanel panel = frame.getCurrentBasePanel();
        if (panel != null) {
            final BibDatabase database = panel.getDatabase();
            List<BibEntry> entries = panel.getSelectedEntries();
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
     * Check that all entries in the list have BibTeX keys, if not ask if they should be generated
     *
     * @param entries A list of entries to be checked
     * @return true if all entries have BibTeX keys, if it so may be after generating them
     */
    private boolean checkThatEntriesHaveKeys(List<BibEntry> entries) {
        // Check if there are empty keys
        boolean emptyKeys = false;
        for (BibEntry entry : entries) {
            if (!entry.getCiteKeyOptional().isPresent()) {
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
                Localization.lang("Cannot cite entries without BibTeX keys. Generate keys now?"),
                Localization.lang("Generate keys"),
                Localization.lang("Cancel"));

        BasePanel panel = frame.getCurrentBasePanel();
        if (citePressed && (panel != null)) {
            // Generate keys
            BibtexKeyPatternPreferences prefs = jabRefPreferences.getBibtexKeyPatternPreferences();
            NamedCompound undoCompound = new NamedCompound(Localization.lang("Cite"));
            for (BibEntry entry : entries) {
                if (!entry.getCiteKeyOptional().isPresent()) {
                    // Generate key
                    new BibtexKeyGenerator(panel.getBibDatabaseContext(), prefs)
                            .generateAndSetKey(entry)
                            .ifPresent(change -> undoCompound.addEdit(new UndoableKeyChange(change)));
                }
            }
            undoCompound.end();
            // Add all undos
            panel.getUndoManager().addEdit(undoCompound);
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
            jabRefPreferences.setOpenOfficePreferences(ooPrefs);
        });
        useAllBases.setOnAction(e -> {
            ooPrefs.setUseAllDatabases(useAllBases.isSelected());
            jabRefPreferences.setOpenOfficePreferences(ooPrefs);
        });
        useActiveBase.setOnAction(e -> {
            ooPrefs.setUseAllDatabases(!useActiveBase.isSelected());
            jabRefPreferences.setOpenOfficePreferences(ooPrefs);
        });
        clearConnectionSettings.setOnAction(e -> {
            ooPrefs.clearConnectionSettings();
            dialogService.notify(Localization.lang("Cleared connection settings"));
            jabRefPreferences.setOpenOfficePreferences(ooPrefs);
        });

        contextMenu.getItems().addAll(autoSync, new SeparatorMenuItem(), useActiveBase, useAllBases, new SeparatorMenuItem(), clearConnectionSettings);

        return contextMenu;

    }
}
