package org.jabref.gui;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
import org.jabref.gui.keyboard.SelectableTextFlowKeyBindings;
import org.jabref.gui.keyboard.TextInputKeyBindings;
import org.jabref.gui.keyboard.WalkthroughKeyBindings;
import org.jabref.gui.openoffice.OOBibBaseConnect;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.remote.CLIMessageHandler;
import org.jabref.gui.theme.ThemeManager;
import org.jabref.gui.undo.CountingUndoManager;
import org.jabref.gui.util.DefaultFileUpdateMonitor;
import org.jabref.gui.util.DirectoryMonitor;
import org.jabref.gui.util.UiTaskExecutor;
import org.jabref.gui.util.WebViewStore;
import org.jabref.http.manager.HttpServerManager;
import org.jabref.languageserver.controller.LanguageServerController;
import org.jabref.logic.UiCommand;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.citation.SearchCitationsRelationsService;
import org.jabref.logic.git.util.GitHandlerRegistry;
import org.jabref.logic.journals.JournalAbbreviationLoader;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.net.ProxyRegisterer;
import org.jabref.logic.os.OS;
import org.jabref.logic.protectedterms.ProtectedTermsLoader;
import org.jabref.logic.remote.RemotePreferences;
import org.jabref.logic.remote.server.RemoteListenerServerManager;
import org.jabref.logic.search.IndexManager;
import org.jabref.logic.search.PostgreServer;
import org.jabref.logic.util.BuildInfo;
import org.jabref.logic.util.FallbackExceptionHandler;
import org.jabref.logic.util.HeadlessExecutorService;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.logic.util.strings.StringUtil;
import org.jabref.model.entry.BibEntryTypesManager;
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

    // AI Service handles chat messages etc. Therefore, it is tightly coupled to the GUI.
    private static AiService aiService;
    // CitationsAndRelationsSearchService is here configured for a local machine and so to the GUI.
    private static SearchCitationsRelationsService citationsAndRelationsSearchService;

    private static FileUpdateMonitor fileUpdateMonitor;
    private static StateManager stateManager;
    private static ThemeManager themeManager;
    private static CountingUndoManager countingUndoManager;
    private static TaskExecutor taskExecutor;
    private static ClipBoardManager clipBoardManager;
    private static DialogService dialogService;
    private static JabRefFrame mainFrame;
    private static GitHandlerRegistry gitHandlerRegistry;

    private static RemoteListenerServerManager remoteListenerServerManager;
    private static HttpServerManager httpServerManager;
    private static LanguageServerController languageServerController;

    private Stage mainStage;

    public static void setup(List<UiCommand> uiCommands,
                             GuiPreferences preferences) {
        JabRefGUI.uiCommands = uiCommands;
        JabRefGUI.preferences = preferences;
    }

    @Override
    public void start(Stage stage) {
        try {
            this.mainStage = stage;
            Injector.setModelOrService(Stage.class, mainStage);

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
                    taskExecutor,
                    gitHandlerRegistry);

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
        } catch (Throwable throwable) {
            LOGGER.error("Error during initialization", throwable);
            throw throwable;
        }

        FallbackExceptionHandler.installExceptionHandler((exception, thread) -> UiTaskExecutor.runInJavaFXThread(() -> {
            DialogService dialogService = Injector.instantiateModelOrService(DialogService.class);
            dialogService.showErrorDialogAndWait("Uncaught exception occurred in " + thread, exception);
        }));
    }

    public void initialize() {
        WebViewStore.init();

        DefaultFileUpdateMonitor fileUpdateMonitor = new DefaultFileUpdateMonitor();
        JabRefGUI.fileUpdateMonitor = fileUpdateMonitor;
        HeadlessExecutorService.INSTANCE.executeInterruptableTask(fileUpdateMonitor, "FileUpdateMonitor");
        Injector.setModelOrService(FileUpdateMonitor.class, fileUpdateMonitor);

        DirectoryMonitor directoryMonitor = new DirectoryMonitor();
        Injector.setModelOrService(DirectoryMonitor.class, directoryMonitor);

        gitHandlerRegistry = new GitHandlerRegistry();
        Injector.setModelOrService(GitHandlerRegistry.class, gitHandlerRegistry);

        BibEntryTypesManager entryTypesManager = preferences.getCustomEntryTypesRepository();
        JournalAbbreviationRepository journalAbbreviationRepository = JournalAbbreviationLoader.loadRepository(preferences.getJournalAbbreviationPreferences());
        Injector.setModelOrService(BibEntryTypesManager.class, entryTypesManager);
        Injector.setModelOrService(JournalAbbreviationRepository.class, journalAbbreviationRepository);
        Injector.setModelOrService(ProtectedTermsLoader.class, new ProtectedTermsLoader(preferences.getProtectedTermsPreferences()));

        IndexManager.clearOldSearchIndices();

        JabRefGUI.remoteListenerServerManager = new RemoteListenerServerManager();
        Injector.setModelOrService(RemoteListenerServerManager.class, JabRefGUI.remoteListenerServerManager);

        JabRefGUI.httpServerManager = new HttpServerManager();
        Injector.setModelOrService(HttpServerManager.class, JabRefGUI.httpServerManager);

        JabRefGUI.languageServerController = new LanguageServerController(preferences, journalAbbreviationRepository);
        Injector.setModelOrService(LanguageServerController.class, JabRefGUI.languageServerController);

        JabRefGUI.stateManager = new JabRefGuiStateManager();
        Injector.setModelOrService(StateManager.class, stateManager);

        Injector.setModelOrService(KeyBindingRepository.class, preferences.getKeyBindingRepository());

        JabRefGUI.themeManager = new ThemeManager(
                preferences.getWorkspacePreferences(),
                fileUpdateMonitor
        );
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

        JabRefGUI.citationsAndRelationsSearchService = new SearchCitationsRelationsService(
                preferences.getImporterPreferences(),
                preferences.getImportFormatPreferences(),
                preferences.getFieldPreferences(),
                entryTypesManager
        );
        Injector.setModelOrService(SearchCitationsRelationsService.class, citationsAndRelationsSearchService);
    }

    private void setupProxy() {
        if (!preferences.getProxyPreferences().shouldUseProxy()) {
            return;
        }

        if (!preferences.getProxyPreferences().shouldUseAuthentication()) {
            ProxyRegisterer.register(preferences.getProxyPreferences());
            return;
        }

        assert preferences.getProxyPreferences().shouldUseAuthentication();

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
            mainStage.setHeight(Math.min(bounds.getHeight(), 768.0));
            LOGGER.debug("Saving window positions");
            saveWindowState();
        }
        // after calling "saveWindowState" the maximized state can be set
        mainStage.setMaximized(windowMaximised);
        debugLogWindowState(mainStage);

        Scene scene = new Scene(JabRefGUI.mainFrame);

        LOGGER.debug("installing CSS");
        themeManager.installCssImmediately(scene);

        LOGGER.debug("Handle TextEditor key bindings");
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            TextInputKeyBindings.call(scene, event, preferences.getKeyBindingRepository());
            SelectableTextFlowKeyBindings.call(scene, event, preferences.getKeyBindingRepository());
            WalkthroughKeyBindings.call(event, stateManager, preferences.getKeyBindingRepository());
        });

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

        // Lifecycle note: after this method, #onShowing will be called
    }

    public void onShowing(WindowEvent event) {
        Platform.runLater(() -> {
            mainFrame.updateHorizontalDividerPosition();
            mainFrame.updateVerticalDividerPosition();
        });

        // Open last edited databases
        if (uiCommands.stream().noneMatch(UiCommand.BlankWorkspace.class::isInstance)
                && preferences.getWorkspacePreferences().shouldOpenLastEdited()) {
            mainFrame.openLastEditedDatabases();
        }

        Platform.runLater(() -> {
            // We need to check at this point, because here, all libraries are loaded (e.g., load previously opened libraries) and all UI commands (e.g., load libraries, blank workspace, ...) are handled.
            if (stateManager.getOpenDatabases().isEmpty()) {
                mainFrame.showWelcomeTab();
            }
        });
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
        // workaround for mac, maximize will always report true
        if (!mainStage.isMaximized() || OS.OS_X) {
            preferences.setPositionX(mainStage.getX());
            preferences.setPositionY(mainStage.getY());
            preferences.setSizeX(mainStage.getWidth());
            preferences.setSizeY(mainStage.getHeight());
        }
        // maximize does not correctly work on OSX, reports true, although the window was resized!
        if (OS.OS_X) {
            preferences.setWindowMaximised(false);
        } else {
            preferences.setWindowMaximised(mainStage.isMaximized());
        }
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

        boolean inBounds = Screen.getScreens().stream().anyMatch(screen -> screen.getBounds().contains(leftX, bottomY));
        LOGGER.debug("lower left corner is in bounds: {}", inBounds);
        return inBounds;
    }

    private boolean upperRightIsInBounds(CoreGuiPreferences coreGuiPreferences) {
        // The upper right corner is checked as there are most probably the window controls.
        // Windows/PowerToys somehow adds 10 pixels to the right and top of the screen, they are removed
        double rightX = coreGuiPreferences.getPositionX() + coreGuiPreferences.getSizeX() - 10.0;
        double topY = coreGuiPreferences.getPositionY();
        LOGGER.debug("right x: {}, top y: {}", rightX, topY);

        boolean inBounds = Screen.getScreens().stream().anyMatch(screen -> screen.getBounds().contains(rightX, topY));
        LOGGER.debug("upper right corner is in bounds: {}", inBounds);
        return inBounds;
    }

    // Background tasks
    public void startBackgroundTasks() {
        RemotePreferences remotePreferences = preferences.getRemotePreferences();
        CLIMessageHandler cliMessageHandler = new CLIMessageHandler(mainFrame, preferences);
        if (remotePreferences.useRemoteServer()) {
            remoteListenerServerManager.openAndStart(
                    cliMessageHandler,
                    remotePreferences.getPort());
        }

        if (remotePreferences.enableHttpServer()) {
            httpServerManager.start(stateManager, remotePreferences.getHttpServerUri());
        }
        if (remotePreferences.enableLanguageServer()) {
            languageServerController.start(cliMessageHandler, remotePreferences.getLanguageServerPort());
        }
    }

    @Override
    public void stop() {
        LOGGER.trace("Stopping JabRef GUI");
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            LOGGER.trace("Stopping JabRef GUI using a virtual thread executor");

            // Shutdown everything in parallel to prevent causing non-shutdown of something in case of issues
            executor.submit(() -> {
                LOGGER.trace("Closing citations and relations search service");
                citationsAndRelationsSearchService.close();
                LOGGER.trace("Citations and relations search service closed");
            });

            executor.submit(() -> {
                LOGGER.trace("Closing AI service");
                try {
                    aiService.close();
                } catch (Exception e) {
                    LOGGER.error("Unable to close AI service", e);
                }
                LOGGER.trace("AI service closed");
            });

            executor.submit(() -> {
                LOGGER.trace("Closing OpenOffice connection");
                OOBibBaseConnect.closeOfficeConnection();
                LOGGER.trace("OpenOffice connection closed");
            });

            executor.submit(() -> {
                LOGGER.trace("Shutting down remote server manager");
                remoteListenerServerManager.stop();
                LOGGER.trace("RemoteListenerServerManager shut down");
            });

            executor.submit(() -> {
                LOGGER.trace("Shutting down http server manager");
                httpServerManager.stop();
                LOGGER.trace("HttpServerManager shut down");
            });

            executor.submit(() -> {
                LOGGER.trace("Shutting down language server controller");
                languageServerController.stop();
                LOGGER.trace("LanguageServerController shut down");
            });

            executor.submit(() -> {
                LOGGER.trace("Stopping background tasks");
                Unirest.shutDown();
                LOGGER.trace("Unirest shut down");
            });

            // region All threading related shutdowns
            executor.submit(() -> {
                LOGGER.trace("Shutting down taskExecutor");
                if (taskExecutor != null) {
                    taskExecutor.shutdown();
                }
                LOGGER.trace("TaskExecutor shut down");
            });

            executor.submit(() -> {
                LOGGER.trace("Shutting down fileUpdateMonitor");
                fileUpdateMonitor.shutdown();
                LOGGER.trace("FileUpdateMonitor shut down");
            });

            executor.submit(() -> {
                LOGGER.trace("Shutting down directoryMonitor");
                DirectoryMonitor directoryMonitor = Injector.instantiateModelOrService(DirectoryMonitor.class);
                directoryMonitor.shutdown();
                LOGGER.trace("DirectoryMonitor shut down");
            });

            executor.submit(() -> {
                LOGGER.trace("Shutting down postgreServer");
                PostgreServer postgreServer = Injector.instantiateModelOrService(PostgreServer.class);
                postgreServer.shutdown();
                LOGGER.trace("PostgreServer shut down");
            });

            executor.submit(() -> {
                LOGGER.trace("Shutting down HeadlessExecutorService");
                HeadlessExecutorService.INSTANCE.shutdownEverything();
                LOGGER.trace("HeadlessExecutorService shut down");
            });
            // endregion

            HeadlessExecutorService.gracefullyShutdown("HeadlessExecutorService", executor, 30);
        }

        LOGGER.trace("Finished stop");

        // Just to be sure that we do not leave any threads running
        System.exit(0);
    }
}
