package org.jabref.gui;

import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.input.KeyEvent;
import javafx.stage.Screen;
import javafx.stage.Stage;

import org.jabref.gui.help.VersionWorker;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.importer.ParserResultWarningDialog;
import org.jabref.gui.importer.actions.OpenDatabaseAction;
import org.jabref.gui.keyboard.TextInputKeyBindings;
import org.jabref.gui.shared.SharedDatabaseUIManager;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.net.ProxyRegisterer;
import org.jabref.logic.shared.DatabaseNotSupportedException;
import org.jabref.logic.shared.exception.InvalidDBMSConnectionPropertiesException;
import org.jabref.logic.shared.exception.NotASharedDatabaseException;
import org.jabref.logic.util.WebViewStore;
import org.jabref.model.strings.StringUtil;
import org.jabref.model.util.FileUpdateMonitor;
import org.jabref.preferences.GuiPreferences;
import org.jabref.preferences.PreferencesService;

import com.airhacks.afterburner.injection.Injector;
import impl.org.controlsfx.skin.DecorationPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JabRefGUI {

    private static final Logger LOGGER = LoggerFactory.getLogger(JabRefGUI.class);

    private static JabRefFrame mainFrame;
    private final PreferencesService preferencesService;
    private final FileUpdateMonitor fileUpdateMonitor;

    private final List<ParserResult> bibDatabases;
    private final boolean isBlank;
    private boolean correctedWindowPos;
    private final List<ParserResult> failed = new ArrayList<>();
    private final List<ParserResult> toOpenTab = new ArrayList<>();

    public JabRefGUI(Stage mainStage,
                     List<ParserResult> databases,
                     boolean isBlank,
                     PreferencesService preferencesService,
                     FileUpdateMonitor fileUpdateMonitor) {
        this.bibDatabases = databases;
        this.isBlank = isBlank;
        this.preferencesService = preferencesService;
        this.fileUpdateMonitor = fileUpdateMonitor;
        this.correctedWindowPos = false;

        WebViewStore.init();

        mainFrame = new JabRefFrame(mainStage);

        openWindow(mainStage);

        new VersionWorker(Globals.BUILD_INFO.version,
                mainFrame.getDialogService(),
                Globals.TASK_EXECUTOR,
                preferencesService.getInternalPreferences())
                .checkForNewVersionDelayed();

        setupProxy();
    }

    private void setupProxy() {
        if (!preferencesService.getProxyPreferences().shouldUseProxy()
                || !preferencesService.getProxyPreferences().shouldUseAuthentication()) {
            return;
        }

        if (preferencesService.getProxyPreferences().shouldPersistPassword()
                && StringUtil.isNotBlank(preferencesService.getProxyPreferences().getPassword())) {
            ProxyRegisterer.register(preferencesService.getProxyPreferences());
            return;
        }

        DialogService dialogService = Injector.instantiateModelOrService(DialogService.class);
        Optional<String> password = dialogService.showPasswordDialogAndWait(
                Localization.lang("Proxy configuration"),
                Localization.lang("Proxy requires password"),
                Localization.lang("Password"));

        if (password.isPresent()) {
            preferencesService.getProxyPreferences().setPassword(password.get());
            ProxyRegisterer.register(preferencesService.getProxyPreferences());
        } else {
            LOGGER.warn("No proxy password specified");
        }
    }

    private void openWindow(Stage mainStage) {
        LOGGER.debug("Initializing frame");
        mainFrame.init();

        GuiPreferences guiPreferences = preferencesService.getGuiPreferences();

        // Set the min-width and min-height for the main window
        mainStage.setMinHeight(330);
        mainStage.setMinWidth(580);

        // Restore window location and/or maximised state
        if (guiPreferences.isWindowMaximised()) {
            mainStage.setMaximized(true);
        } else if ((Screen.getScreens().size() == 1) && isWindowPositionOutOfBounds()) {
            // corrects the Window, if it is outside the mainscreen
            LOGGER.debug("The Jabref window is outside the main screen");
            mainStage.setX(0);
            mainStage.setY(0);
            mainStage.setWidth(1024);
            mainStage.setHeight(768);
            correctedWindowPos = true;
        } else {
            mainStage.setX(guiPreferences.getPositionX());
            mainStage.setY(guiPreferences.getPositionY());
            mainStage.setWidth(guiPreferences.getSizeX());
            mainStage.setHeight(guiPreferences.getSizeY());
        }
        debugLogWindowState(mainStage);

        // We create a decoration pane ourselves for performance reasons
        // (otherwise it has to be injected later, leading to a complete redraw/relayout of the complete scene)
        DecorationPane root = new DecorationPane();
        root.getChildren().add(JabRefGUI.mainFrame);

        Scene scene = new Scene(root, 800, 800);
        Globals.getThemeManager().installCss(scene);

        // Handle TextEditor key bindings
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> TextInputKeyBindings.call(scene, event));

        mainStage.setTitle(JabRefFrame.FRAME_TITLE);
        mainStage.getIcons().addAll(IconTheme.getLogoSetFX());
        mainStage.setScene(scene);
        mainStage.show();

        mainStage.setOnCloseRequest(event -> {
            if (!correctedWindowPos) {
                // saves the window position only if its not  corrected -> the window will rest at the old Position,
                // if the external Screen is connected again.
                saveWindowState(mainStage);
            }
            boolean reallyQuit = mainFrame.quit();
            if (!reallyQuit) {
                event.consume();
            }
        });
        Platform.runLater(this::openDatabases);

        if (!(fileUpdateMonitor.isActive())) {
            getMainFrame().getDialogService()
                          .showErrorDialogAndWait(Localization.lang("Unable to monitor file changes. Please close files " +
                                  "and processes and restart. You may encounter errors if you continue " +
                                  "with this session."));
        }
    }

    private void openDatabases() {
        // If the option is enabled, open the last edited libraries, if any.
        if (!isBlank && preferencesService.getWorkspacePreferences().shouldOpenLastEdited()) {
            openLastEditedDatabases();
        }

        // From here on, the libraries provided by command line arguments are treated

        // Remove invalid databases
        List<ParserResult> invalidDatabases = bibDatabases.stream()
                                                          .filter(ParserResult::isInvalid)
                                                          .toList();
        failed.addAll(invalidDatabases);
        bibDatabases.removeAll(invalidDatabases);

        // passed file (we take the first one) should be focused
        Path focusedFile = bibDatabases.stream()
                                       .findFirst()
                                       .flatMap(ParserResult::getPath)
                                       .orElse(preferencesService.getGuiPreferences()
                                                                 .getLastFocusedFile())
                                       .toAbsolutePath();

        // Add all bibDatabases databases to the frame:
        boolean first = false;
        for (ParserResult pr : bibDatabases) {
            // Define focused tab
            if (pr.getPath().filter(path -> path.toAbsolutePath().equals(focusedFile)).isPresent()) {
                first = true;
            }

            if (pr.getDatabase().isShared()) {
                try {
                    new SharedDatabaseUIManager(mainFrame, preferencesService, fileUpdateMonitor)
                            .openSharedDatabaseFromParserResult(pr);
                } catch (SQLException | DatabaseNotSupportedException | InvalidDBMSConnectionPropertiesException |
                        NotASharedDatabaseException e) {
                    pr.getDatabaseContext().clearDatabasePath(); // do not open the original file
                    pr.getDatabase().clearSharedDatabaseID();

                    LOGGER.error("Connection error", e);
                    mainFrame.getDialogService().showErrorDialogAndWait(
                            Localization.lang("Connection error"),
                            Localization.lang("A local copy will be opened."),
                            e);
                }
                toOpenTab.add(pr);
            } else if (pr.toOpenTab()) {
                // things to be appended to an opened tab should be done after opening all tabs
                // add them to the list
                toOpenTab.add(pr);
            } else {
                mainFrame.addParserResult(pr, first);
                first = false;
            }
        }

        // finally add things to the currently opened tab
        for (ParserResult pr : toOpenTab) {
            mainFrame.addParserResult(pr, first);
            first = false;
        }

        for (ParserResult pr : failed) {
            String message = Localization.lang("Error opening file '%0'.",
                    pr.getPath().map(Path::toString).orElse("(File name unknown)")) + "\n" +
                    pr.getErrorMessage();

            mainFrame.getDialogService().showErrorDialogAndWait(Localization.lang("Error opening file"), message);
        }

        // Display warnings, if any
        int tabNumber = 0;
        for (ParserResult pr : bibDatabases) {
            ParserResultWarningDialog.showParserResultWarningDialog(pr, mainFrame, tabNumber++);
        }

        // After adding the databases, go through each and see if
        // any post open actions need to be done. For instance, checking
        // if we found new entry types that can be imported, or checking
        // if the database contents should be modified due to new features
        // in this version of JabRef.
        // Note that we have to check whether i does not go over getBasePanelCount().
        // This is because importToOpen might have been used, which adds to
        // loadedDatabases, but not to getBasePanelCount()

        for (int i = 0; (i < bibDatabases.size()) && (i < mainFrame.getBasePanelCount()); i++) {
            ParserResult pr = bibDatabases.get(i);
            LibraryTab libraryTab = mainFrame.getLibraryTabAt(i);

            OpenDatabaseAction.performPostOpenActions(libraryTab, pr);
        }

        LOGGER.debug("Finished adding panels");
    }

    private void saveWindowState(Stage mainStage) {
        GuiPreferences preferences = preferencesService.getGuiPreferences();
        preferences.setPositionX(mainStage.getX());
        preferences.setPositionY(mainStage.getY());
        preferences.setSizeX(mainStage.getWidth());
        preferences.setSizeY(mainStage.getHeight());
        preferences.setWindowMaximised(mainStage.isMaximized());
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

    /**
     * Tests if the window coordinates are out of the mainscreen
     *
     * @return outbounds
     */
    private boolean isWindowPositionOutOfBounds() {
        return !Screen.getPrimary().getBounds().contains(
                preferencesService.getGuiPreferences().getPositionX(),
                preferencesService.getGuiPreferences().getPositionY());
    }

    private void openLastEditedDatabases() {
        List<String> lastFiles = preferencesService.getGuiPreferences().getLastFilesOpened();
        if (lastFiles.isEmpty()) {
            return;
        }

        List<Path> filesToOpen = lastFiles.stream().map(Path::of).collect(Collectors.toList());
        getMainFrame().getOpenDatabaseAction().openFiles(filesToOpen);
    }

    public static JabRefFrame getMainFrame() {
        return mainFrame;
    }
}
