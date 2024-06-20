package org.jabref.gui;

import java.util.List;
import java.util.Optional;

import javax.swing.undo.UndoManager;

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
import org.jabref.gui.keyboard.KeyBindingRepository;
import org.jabref.gui.keyboard.TextInputKeyBindings;
import org.jabref.gui.openoffice.OOBibBaseConnect;
import org.jabref.gui.remote.CLIMessageHandler;
import org.jabref.gui.theme.ThemeManager;
import org.jabref.gui.undo.CountingUndoManager;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.gui.util.UiTaskExecutor;
import org.jabref.logic.UiCommand;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.net.ProxyRegisterer;
import org.jabref.logic.remote.RemotePreferences;
import org.jabref.logic.remote.server.RemoteListenerServerManager;
import org.jabref.logic.util.BuildInfo;
import org.jabref.logic.util.HeadlessExecutorService;
import org.jabref.logic.util.WebViewStore;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.strings.StringUtil;
import org.jabref.model.util.DirectoryMonitor;
import org.jabref.model.util.FileUpdateMonitor;
import org.jabref.preferences.GuiPreferences;
import org.jabref.preferences.JabRefPreferences;

import com.airhacks.afterburner.injection.Injector;
import com.tobiasdiez.easybind.EasyBind;
import kong.unirest.core.Unirest;
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

    private static StateManager stateManager;
    private static ThemeManager themeManager;
    private static CountingUndoManager countingUndoManager;
    private static TaskExecutor taskExecutor;
    private static ClipBoardManager clipBoardManager;
    private static DialogService dialogService;
    private static JabRefFrame mainFrame;

    private static RemoteListenerServerManager remoteListenerServerManager;

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

        initialize();

        JabRefGUI.mainFrame = new JabRefFrame(
                mainStage,
                dialogService,
                fileUpdateMonitor,
                preferencesService,
                stateManager,
                countingUndoManager,
                Injector.instantiateModelOrService(BibEntryTypesManager.class),
                clipBoardManager,
                taskExecutor);

        openWindow();

        startBackgroundTasks();

        if (!fileUpdateMonitor.isActive()) {
            dialogService.showErrorDialogAndWait(
                    Localization.lang("Unable to monitor file changes. Please close files " +
                            "and processes and restart. You may encounter errors if you continue " +
                            "with this session."));
        }

        BuildInfo buildInfo = Injector.instantiateModelOrService(BuildInfo.class);
        EasyBind.subscribe(preferencesService.getInternalPreferences().versionCheckEnabledProperty(), enabled -> {
            if (enabled) {
                new VersionWorker(buildInfo.version,
                        dialogService,
                        taskExecutor,
                        preferencesService)
                        .checkForNewVersionDelayed();
            }
        });

        setupProxy();
    }

    public void initialize() {
        WebViewStore.init();

        JabRefGUI.remoteListenerServerManager = new RemoteListenerServerManager();
        Injector.setModelOrService(RemoteListenerServerManager.class, remoteListenerServerManager);

        JabRefGUI.stateManager = new StateManager();
        Injector.setModelOrService(StateManager.class, stateManager);

        Injector.setModelOrService(KeyBindingRepository.class, preferencesService.getKeyBindingRepository());

        JabRefGUI.themeManager = new ThemeManager(
                preferencesService.getWorkspacePreferences(),
                fileUpdateMonitor,
                Runnable::run);
        Injector.setModelOrService(ThemeManager.class, themeManager);

        JabRefGUI.countingUndoManager = new CountingUndoManager();
        Injector.setModelOrService(UndoManager.class, countingUndoManager);
        Injector.setModelOrService(CountingUndoManager.class, countingUndoManager);

        JabRefGUI.taskExecutor = new UiTaskExecutor();
        Injector.setModelOrService(TaskExecutor.class, taskExecutor);

        JabRefGUI.dialogService = new JabRefDialogService(mainStage);
        Injector.setModelOrService(DialogService.class, dialogService);

        JabRefGUI.clipBoardManager = new ClipBoardManager();
        Injector.setModelOrService(TaskExecutor.class, taskExecutor);
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
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> TextInputKeyBindings.call(
                scene,
                event,
                preferencesService.getKeyBindingRepository()));

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

    // Background tasks
    public void startBackgroundTasks() {
        RemotePreferences remotePreferences = preferencesService.getRemotePreferences();
        BibEntryTypesManager bibEntryTypesManager = Injector.instantiateModelOrService(BibEntryTypesManager.class);
        if (remotePreferences.useRemoteServer()) {
            remoteListenerServerManager.openAndStart(
                    new CLIMessageHandler(
                            mainFrame,
                            preferencesService,
                            fileUpdateMonitor,
                            bibEntryTypesManager),
                    remotePreferences.getPort());
        }
    }

    @Override
    public void stop() {
        OOBibBaseConnect.closeOfficeConnection();
        stopBackgroundTasks();
        shutdownThreadPools();
    }

    public void stopBackgroundTasks() {
        Unirest.shutDown();
    }

    public static void shutdownThreadPools() {
        taskExecutor.shutdown();
        fileUpdateMonitor.shutdown();
        DirectoryMonitor directoryMonitor = Injector.instantiateModelOrService(DirectoryMonitor.class);
        directoryMonitor.shutdown();
        HeadlessExecutorService.INSTANCE.shutdownEverything();
    }
}
