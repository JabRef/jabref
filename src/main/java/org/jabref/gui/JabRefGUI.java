package org.jabref.gui;

import java.util.List;
import java.util.Optional;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.input.KeyEvent;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import org.jabref.gui.frame.JabRefFrame;
import org.jabref.gui.help.VersionWorker;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.keyboard.TextInputKeyBindings;
import org.jabref.gui.openoffice.OOBibBaseConnect;
import org.jabref.gui.telemetry.Telemetry;
import org.jabref.gui.theme.ThemeManager;
import org.jabref.logic.UiCommand;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.net.ProxyRegisterer;
import org.jabref.logic.util.WebViewStore;
import org.jabref.model.strings.StringUtil;
import org.jabref.model.util.FileUpdateMonitor;
import org.jabref.preferences.GuiPreferences;
import org.jabref.preferences.JabRefPreferences;

import com.tobiasdiez.easybind.EasyBind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents the outer stage and the scene of the JabRef window.
 */
public class JabRefGUI extends Application {

    private static final Logger LOGGER = LoggerFactory.getLogger(JabRefGUI.class);

    private static List<UiCommand> uiCommands;
    private static JabRefPreferences preferencesService;
    private static FileUpdateMonitor fileUpdateMonitor;
    private static JabRefFrame mainFrame;
    private static DialogService dialogService;
    private static ThemeManager themeManager;

    private boolean correctedWindowPos = false;
    private Stage mainStage;

    public static void setup(List<UiCommand> uiCommands,
                             JabRefPreferences preferencesService,
                             FileUpdateMonitor fileUpdateMonitor) {
        JabRefGUI.uiCommands = uiCommands;
        JabRefGUI.preferencesService = preferencesService;
        JabRefGUI.fileUpdateMonitor = fileUpdateMonitor;
    }

    @Override
    public void start(Stage stage) {
        this.mainStage = stage;

        FallbackExceptionHandler.installExceptionHandler();
        Globals.startBackgroundTasks();

        WebViewStore.init();

        JabRefGUI.themeManager = new ThemeManager(
                preferencesService.getWorkspacePreferences(),
                fileUpdateMonitor,
                Runnable::run);
        JabRefGUI.dialogService = new JabRefDialogService(mainStage);
        JabRefGUI.mainFrame = new JabRefFrame(
                mainStage,
                dialogService,
                fileUpdateMonitor,
                preferencesService,
                Globals.stateManager,
                Globals.undoManager,
                Globals.entryTypesManager,
                Globals.TASK_EXECUTOR);

        openWindow();

        if (!fileUpdateMonitor.isActive()) {
            dialogService.showErrorDialogAndWait(
                    Localization.lang("Unable to monitor file changes. Please close files " +
                            "and processes and restart. You may encounter errors if you continue " +
                            "with this session."));
        }

        EasyBind.subscribe(preferencesService.getInternalPreferences().versionCheckEnabledProperty(), enabled -> {
            if (enabled) {
                new VersionWorker(Globals.BUILD_INFO.version,
                        dialogService,
                        Globals.TASK_EXECUTOR,
                        preferencesService)
                        .checkForNewVersionDelayed();
            }
        });

        setupProxy();
    }

    @Override
    public void stop() {
        OOBibBaseConnect.closeOfficeConnection();
        Globals.stopBackgroundTasks();
        Globals.shutdownThreadPools();
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

    private void openWindow() {
        LOGGER.debug("Initializing frame");

        GuiPreferences guiPreferences = preferencesService.getGuiPreferences();

        mainStage.setMinHeight(330);
        mainStage.setMinWidth(580);
        mainStage.setFullScreen(guiPreferences.isWindowFullscreen());
        mainStage.setMaximized(guiPreferences.isWindowMaximised());
        if ((Screen.getScreens().size() == 1) && isWindowPositionOutOfBounds()) {
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

        Scene scene = new Scene(JabRefGUI.mainFrame);
        themeManager.installCss(scene);

        // Handle TextEditor key bindings
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> TextInputKeyBindings.call(scene, event));

        mainStage.setTitle(JabRefFrame.FRAME_TITLE);
        mainStage.getIcons().addAll(IconTheme.getLogoSetFX());
        mainStage.setScene(scene);
        mainStage.setOnShowing(this::onShowing);
        mainStage.setOnCloseRequest(this::onCloseRequest);
        mainStage.setOnHiding(this::onHiding);
        mainStage.show();

        Platform.runLater(() -> mainFrame.handleUiCommands(uiCommands));
    }

    public void onShowing(WindowEvent event) {
        Platform.runLater(() -> mainFrame.updateDividerPosition());

        // Open last edited databases
        if (uiCommands.stream().noneMatch(UiCommand.BlankWorkspace.class::isInstance)
            && preferencesService.getWorkspacePreferences().shouldOpenLastEdited()) {
            mainFrame.openLastEditedDatabases();
        }

        Telemetry.initTrackingNotification(dialogService, preferencesService.getTelemetryPreferences());
    }

    public void onCloseRequest(WindowEvent event) {
        if (!mainFrame.close()) {
            event.consume();
        }
    }

    public void onHiding(WindowEvent event) {
        if (!correctedWindowPos) {
            // saves the window position only if its not corrected -> the window will rest at the old Position,
            // if the external Screen is connected again.
            saveWindowState();
        }

        preferencesService.flush();

        // Goodbye!
        Platform.exit();
    }

    private void saveWindowState() {
        GuiPreferences preferences = preferencesService.getGuiPreferences();
        preferences.setPositionX(mainStage.getX());
        preferences.setPositionY(mainStage.getY());
        preferences.setSizeX(mainStage.getWidth());
        preferences.setSizeY(mainStage.getHeight());
        preferences.setWindowMaximised(mainStage.isMaximized());
        preferences.setWindowFullScreen(mainStage.isFullScreen());
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

    public static JabRefFrame getMainFrame() {
        return mainFrame;
    }

    public static DialogService getDialogService() {
        return dialogService;
    }

    public static ThemeManager getThemeManager() {
        return themeManager;
    }
}
