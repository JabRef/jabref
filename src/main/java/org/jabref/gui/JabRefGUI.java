package org.jabref.gui;

import java.util.List;
import java.util.Optional;

import javax.swing.undo.UndoManager;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
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
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.remote.CLIMessageHandler;
import org.jabref.gui.theme.ThemeManager;
import org.jabref.gui.undo.CountingUndoManager;
import org.jabref.gui.util.DefaultDirectoryMonitor;
import org.jabref.gui.util.UiTaskExecutor;
import org.jabref.logic.UiCommand;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.net.ProxyRegisterer;
import org.jabref.logic.remote.RemotePreferences;
import org.jabref.logic.remote.server.RemoteListenerServerManager;
import org.jabref.logic.search.PostgreServer;
import org.jabref.logic.util.BuildInfo;
import org.jabref.logic.util.FallbackExceptionHandler;
import org.jabref.logic.util.HeadlessExecutorService;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.logic.util.WebViewStore;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.strings.StringUtil;
import org.jabref.model.util.DirectoryMonitor;
import org.jabref.model.util.FileUpdateMonitor;

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
    private static GuiPreferences preferences;
    private static FileUpdateMonitor fileUpdateMonitor;

    // AI Service handles chat messages etc. Therefore, it is tightly coupled to the GUI.
    private static AiService aiService;

    private static StateManager stateManager;
    private static ThemeManager themeManager;
    private static CountingUndoManager countingUndoManager;
    private static TaskExecutor taskExecutor;
    private static ClipBoardManager clipBoardManager;
    private static DialogService dialogService;
    private static JabRefFrame mainFrame;

    private static RemoteListenerServerManager remoteListenerServerManager;

    private Stage mainStage;

    public static void setup(List<UiCommand> uiCommands,
                             GuiPreferences preferences,
                             FileUpdateMonitor fileUpdateMonitor) {
        JabRefGUI.uiCommands = uiCommands;
        JabRefGUI.preferences = preferences;
        JabRefGUI.fileUpdateMonitor = fileUpdateMonitor;
    }

    @Override
    public void start(Stage stage) {
        this.mainStage = stage;

        FallbackExceptionHandler.installExceptionHandler((exception, thread) -> {
            UiTaskExecutor.runInJavaFXThread(() -> {
                DialogService dialogService = Injector.instantiateModelOrService(DialogService.class);
                dialogService.showErrorDialogAndWait("Uncaught exception occurred in " + thread, exception);
            });
        });

        initialize();

        JabRefGUI.mainFrame = new JabRefFrame(
                mainStage,
                dialogService,
                fileUpdateMonitor,
                preferences,
                aiService,
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
        EasyBind.subscribe(preferences.getInternalPreferences().versionCheckEnabledProperty(), enabled -> {
            if (enabled) {
                new VersionWorker(buildInfo.version,
                        dialogService,
                        taskExecutor,
                        preferences)
                        .checkForNewVersionDelayed();
            }
        });

        setupProxy();
    }

    public void initialize() {
        WebViewStore.init();

        DirectoryMonitor directoryMonitor = new DefaultDirectoryMonitor();
        Injector.setModelOrService(DirectoryMonitor.class, directoryMonitor);

        JabRefGUI.remoteListenerServerManager = new RemoteListenerServerManager();
        Injector.setModelOrService(RemoteListenerServerManager.class, remoteListenerServerManager);

        JabRefGUI.stateManager = new StateManager();
        Injector.setModelOrService(StateManager.class, stateManager);

        Injector.setModelOrService(KeyBindingRepository.class, preferences.getKeyBindingRepository());

        JabRefGUI.themeManager = new ThemeManager(
                preferences.getWorkspacePreferences(),
                fileUpdateMonitor,
                Runnable::run);
        Injector.setModelOrService(ThemeManager.class, themeManager);

        JabRefGUI.countingUndoManager = new CountingUndoManager();
        Injector.setModelOrService(UndoManager.class, countingUndoManager);
        Injector.setModelOrService(CountingUndoManager.class, countingUndoManager);

        // our Default task executor is the UITaskExecutor which can use the fx thread
        JabRefGUI.taskExecutor = new UiTaskExecutor();
        Injector.setModelOrService(TaskExecutor.class, taskExecutor);

        JabRefGUI.dialogService = new JabRefDialogService(mainStage);
        Injector.setModelOrService(DialogService.class, dialogService);

        JabRefGUI.clipBoardManager = new ClipBoardManager();
        Injector.setModelOrService(ClipBoardManager.class, clipBoardManager);

        JabRefGUI.aiService = new AiService(
                preferences.getAiPreferences(),
                preferences.getFilePreferences(),
                preferences.getCitationKeyPatternPreferences(),
                dialogService,
                taskExecutor);
        Injector.setModelOrService(AiService.class, aiService);
    }

    private void setupProxy() {
        if (!preferences.getProxyPreferences().shouldUseProxy()
                || !preferences.getProxyPreferences().shouldUseAuthentication()) {
            return;
        }

        if (preferences.getProxyPreferences().shouldPersistPassword()
                && StringUtil.isNotBlank(preferences.getProxyPreferences().getPassword())) {
            ProxyRegisterer.register(preferences.getProxyPreferences());
            return;
        }

        Optional<String> password = dialogService.showPasswordDialogAndWait(
                Localization.lang("Proxy configuration"),
                Localization.lang("Proxy requires password"),
                Localization.lang("Password"));

        if (password.isPresent()) {
            preferences.getProxyPreferences().setPassword(password.get());
            ProxyRegisterer.register(preferences.getProxyPreferences());
        } else {
            LOGGER.warn("No proxy password specified");
        }
    }

    private void openWindow() {
        LOGGER.debug("Initializing frame");

        CoreGuiPreferences coreGuiPreferences = preferences.getGuiPreferences();
        LOGGER.debug("Reading from prefs: isMaximized {}", coreGuiPreferences.isWindowMaximised());

        mainStage.setMinWidth(580);
        mainStage.setMinHeight(330);

        // maximized target state is stored, because "saveWindowState" saves x and y only if not maximized
        boolean windowMaximised = coreGuiPreferences.isWindowMaximised();

        LOGGER.debug("Screens: {}", Screen.getScreens());
        debugLogWindowState(mainStage);

        if (isWindowPositionInBounds()) {
            LOGGER.debug("The JabRef window is inside screen bounds.");
            mainStage.setX(coreGuiPreferences.getPositionX());
            mainStage.setY(coreGuiPreferences.getPositionY());
            mainStage.setWidth(coreGuiPreferences.getSizeX());
            mainStage.setHeight(coreGuiPreferences.getSizeY());
            LOGGER.debug("NOT saving window positions");
        } else {
            LOGGER.info("The JabRef window is outside of screen bounds. Position and size will be corrected to 1024x768. Primary screen will be used.");
            Rectangle2D bounds = Screen.getPrimary().getBounds();
            mainStage.setX(bounds.getMinX());
            mainStage.setY(bounds.getMinY());
            mainStage.setWidth(Math.min(bounds.getWidth(), 1024.0));
            mainStage.setHeight(Math.min(bounds.getHeight(), 786.0));
            LOGGER.debug("Saving window positions");
            saveWindowState();
        }
        // after calling "saveWindowState" the maximized state can be set
        mainStage.setMaximized(windowMaximised);
        debugLogWindowState(mainStage);

        Scene scene = new Scene(JabRefGUI.mainFrame);

        LOGGER.debug("installing CSS");
        themeManager.installCss(scene);

        LOGGER.debug("Handle TextEditor key bindings");
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> TextInputKeyBindings.call(
                scene,
                event,
                preferences.getKeyBindingRepository()));

        mainStage.setTitle(JabRefFrame.FRAME_TITLE);
        mainStage.getIcons().addAll(IconTheme.getLogoSetFX());
        mainStage.setScene(scene);
        mainStage.setOnShowing(this::onShowing);
        mainStage.setOnCloseRequest(this::onCloseRequest);
        mainStage.setOnHiding(this::onHiding);

        LOGGER.debug("Showing mainStage");
        mainStage.show();

        LOGGER.debug("frame initialized");

        Platform.runLater(() -> mainFrame.handleUiCommands(uiCommands));
    }

    public void onShowing(WindowEvent event) {
        Platform.runLater(() -> mainFrame.updateDividerPosition());

        // Open last edited databases
        if (uiCommands.stream().noneMatch(UiCommand.BlankWorkspace.class::isInstance)
            && preferences.getWorkspacePreferences().shouldOpenLastEdited()) {
            mainFrame.openLastEditedDatabases();
        }
    }

    public void onCloseRequest(WindowEvent event) {
        if (!mainFrame.close()) {
            event.consume();
        }
    }

    public void onHiding(WindowEvent event) {
        saveWindowState();
        preferences.flush();
        Platform.exit();
    }

    private void saveWindowState() {
        CoreGuiPreferences preferences = JabRefGUI.preferences.getGuiPreferences();
        if (!mainStage.isMaximized()) {
            preferences.setPositionX(mainStage.getX());
            preferences.setPositionY(mainStage.getY());
            preferences.setSizeX(mainStage.getWidth());
            preferences.setSizeY(mainStage.getHeight());
        }
        preferences.setWindowMaximised(mainStage.isMaximized());
        debugLogWindowState(mainStage);
    }

    /**
     * prints the data from the screen (only in debug mode)
     *
     * @param mainStage JabRef's stage
     */
    private void debugLogWindowState(Stage mainStage) {
        LOGGER.debug("""
                        screen data:
                          mainStage.WINDOW_MAXIMISED: {}
                          mainStage.POS_X: {}
                          mainStage.POS_Y: {}
                          mainStage.SIZE_X: {}
                          mainStage.SIZE_Y: {}
                        """,
                mainStage.isMaximized(), mainStage.getX(), mainStage.getY(), mainStage.getWidth(), mainStage.getHeight());
    }

    /**
     * Tests if the window coordinates are inside any screen
     */
    private boolean isWindowPositionInBounds() {
        CoreGuiPreferences coreGuiPreferences = preferences.getGuiPreferences();

        if (LOGGER.isDebugEnabled()) {
            Screen.getScreens().forEach(screen -> LOGGER.debug("Screen bounds: {}", screen.getBounds()));
        }

        return lowerLeftIsInBounds(coreGuiPreferences) && upperRightIsInBounds(coreGuiPreferences);
    }

    private boolean lowerLeftIsInBounds(CoreGuiPreferences coreGuiPreferences) {
        // Windows/PowerToys somehow removes 10 pixels to the left; they are re-added
        double leftX = coreGuiPreferences.getPositionX() + 10.0;
        double bottomY = coreGuiPreferences.getPositionY() + coreGuiPreferences.getSizeY();
        LOGGER.debug("left x: {}, bottom y: {}", leftX, bottomY);

        boolean inBounds = Screen.getScreens().stream().anyMatch((screen -> screen.getBounds().contains(leftX, bottomY)));
        LOGGER.debug("lower left corner is in bounds: {}", inBounds);
        return inBounds;
    }

    private boolean upperRightIsInBounds(CoreGuiPreferences coreGuiPreferences) {
        // The upper right corner is checked as there are most probably the window controls.
        // Windows/PowerToys somehow adds 10 pixels to the right and top of the screen, they are removed
        double rightX = coreGuiPreferences.getPositionX() + coreGuiPreferences.getSizeX() - 10.0;
        double topY = coreGuiPreferences.getPositionY();
        LOGGER.debug("right x: {}, top y: {}", rightX, topY);

        boolean inBounds = Screen.getScreens().stream().anyMatch((screen -> screen.getBounds().contains(rightX, topY)));
        LOGGER.debug("upper right corner is in bounds: {}", inBounds);
        return inBounds;
    }

    // Background tasks
    public void startBackgroundTasks() {
        RemotePreferences remotePreferences = preferences.getRemotePreferences();
        BibEntryTypesManager bibEntryTypesManager = Injector.instantiateModelOrService(BibEntryTypesManager.class);
        if (remotePreferences.useRemoteServer()) {
            remoteListenerServerManager.openAndStart(
                    new CLIMessageHandler(
                            mainFrame,
                            preferences,
                            fileUpdateMonitor,
                            bibEntryTypesManager),
                    remotePreferences.getPort());
        }
    }

    @Override
    public void stop() {
        LOGGER.trace("Closing AI service");
        try {
            aiService.close();
        } catch (Exception e) {
            LOGGER.error("Unable to close AI service", e);
        }
        LOGGER.trace("Closing OpenOffice connection");
        OOBibBaseConnect.closeOfficeConnection();
        LOGGER.trace("Stopping background tasks");
        stopBackgroundTasks();
        LOGGER.trace("Shutting down thread pools");
        shutdownThreadPools();
        LOGGER.trace("Finished stop");
    }

    public void stopBackgroundTasks() {
        Unirest.shutDown();
    }

    public static void shutdownThreadPools() {
        LOGGER.trace("Shutting down taskExecutor");
        if (taskExecutor != null) {
            taskExecutor.shutdown();
        }
        LOGGER.trace("Shutting down fileUpdateMonitor");
        fileUpdateMonitor.shutdown();
        LOGGER.trace("Shutting down directoryMonitor");
        DirectoryMonitor directoryMonitor = Injector.instantiateModelOrService(DirectoryMonitor.class);
        directoryMonitor.shutdown();
        LOGGER.trace("Shutting down postgreServer");
        PostgreServer postgreServer = Injector.instantiateModelOrService(PostgreServer.class);
        postgreServer.shutdown();
        LOGGER.trace("Shutting down HeadlessExecutorService");
        HeadlessExecutorService.INSTANCE.shutdownEverything();
        LOGGER.trace("Finished shutdownThreadPools");
    }
}
