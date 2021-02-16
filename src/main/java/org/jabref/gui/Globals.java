package org.jabref.gui;

import java.awt.GraphicsEnvironment;
import java.util.Optional;
import java.util.UUID;

import javafx.stage.Screen;

import org.jabref.architecture.AllowedToUseAwt;
import org.jabref.gui.keyboard.KeyBindingRepository;
import org.jabref.gui.undo.CountingUndoManager;
import org.jabref.gui.util.DefaultFileUpdateMonitor;
import org.jabref.gui.util.DefaultTaskExecutor;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.exporter.ExporterFactory;
import org.jabref.logic.importer.ImportFormatReader;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.protectedterms.ProtectedTermsLoader;
import org.jabref.logic.remote.server.RemoteListenerServerLifecycle;
import org.jabref.logic.util.BuildInfo;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.strings.StringUtil;
import org.jabref.model.util.FileUpdateMonitor;
import org.jabref.preferences.JabRefPreferences;

import com.google.common.base.StandardSystemProperty;
import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.TelemetryConfiguration;
import com.microsoft.applicationinsights.telemetry.SessionState;
import kong.unirest.Unirest;

/**
 * @deprecated try to use {@link StateManager} and {@link org.jabref.preferences.PreferencesService}
 */
@Deprecated
@AllowedToUseAwt("Requires AWT for headless check")
public class Globals {

    /**
     * JabRef version info
     */
    public static final BuildInfo BUILD_INFO = new BuildInfo();

    // Remote listener
    public static final RemoteListenerServerLifecycle REMOTE_LISTENER = new RemoteListenerServerLifecycle();

    /**
     * Manager for the state of the GUI.
     */
    public static StateManager stateManager = new StateManager();

    public static final ImportFormatReader IMPORT_FORMAT_READER = new ImportFormatReader();
    public static final TaskExecutor TASK_EXECUTOR = new DefaultTaskExecutor(stateManager);

    /**
     * Each test case initializes this field if required
     */
    public static JabRefPreferences prefs;

    /**
     * This field is initialized upon startup.
     * <p>
     * Only GUI code is allowed to access it, logic code should use dependency injection.
     */
    public static JournalAbbreviationRepository journalAbbreviationRepository;

    /**
     * This field is initialized upon startup.
     * <p>
     * Only GUI code is allowed to access it, logic code should use dependency injection.
     */
    public static ProtectedTermsLoader protectedTermsLoader;

    public static ExporterFactory exportFactory;
    public static CountingUndoManager undoManager = new CountingUndoManager();
    public static BibEntryTypesManager entryTypesManager = new BibEntryTypesManager();

    private static ClipBoardManager clipBoardManager = null;

    // Key binding preferences
    private static KeyBindingRepository keyBindingRepository;

    private static DefaultFileUpdateMonitor fileUpdateMonitor;
    private static TelemetryClient telemetryClient;

    private Globals() {
    }

    // Key binding preferences
    public static synchronized KeyBindingRepository getKeyPrefs() {
        if (keyBindingRepository == null) {
            keyBindingRepository = prefs.getKeyBindingRepository();
        }
        return keyBindingRepository;
    }

    public static synchronized ClipBoardManager getClipboardManager() {
        if (clipBoardManager == null) {
            clipBoardManager = new ClipBoardManager(prefs);
        }
        return clipBoardManager;
    }

    // Background tasks
    public static void startBackgroundTasks() {
        Globals.fileUpdateMonitor = new DefaultFileUpdateMonitor();
        JabRefExecutorService.INSTANCE.executeInterruptableTask(Globals.fileUpdateMonitor, "FileUpdateMonitor");

        if (Globals.prefs.getTelemetryPreferences().shouldCollectTelemetry() && !GraphicsEnvironment.isHeadless()) {
            startTelemetryClient();
        }
    }

    private static void stopTelemetryClient() {
        getTelemetryClient().ifPresent(client -> {
            client.trackSessionState(SessionState.End);
            client.flush();
        });
    }

    private static void startTelemetryClient() {
        TelemetryConfiguration telemetryConfiguration = TelemetryConfiguration.getActive();
        if (!StringUtil.isNullOrEmpty(Globals.BUILD_INFO.azureInstrumentationKey)) {
            telemetryConfiguration.setInstrumentationKey(Globals.BUILD_INFO.azureInstrumentationKey);
        }
        telemetryConfiguration.setTrackingIsDisabled(!Globals.prefs.getTelemetryPreferences().shouldCollectTelemetry());
        telemetryClient = new TelemetryClient(telemetryConfiguration);
        telemetryClient.getContext().getProperties().put("JabRef version", Globals.BUILD_INFO.version.toString());
        telemetryClient.getContext().getProperties().put("Java version", StandardSystemProperty.JAVA_VERSION.value());
        telemetryClient.getContext().getUser().setId(Globals.prefs.getOrCreateUserId());
        telemetryClient.getContext().getSession().setId(UUID.randomUUID().toString());
        telemetryClient.getContext().getDevice().setOperatingSystem(StandardSystemProperty.OS_NAME.value());
        telemetryClient.getContext().getDevice().setOperatingSystemVersion(StandardSystemProperty.OS_VERSION.value());
        telemetryClient.getContext().getDevice().setScreenResolution(Screen.getPrimary().getVisualBounds().toString());

        telemetryClient.trackSessionState(SessionState.Start);
    }

    public static FileUpdateMonitor getFileUpdateMonitor() {
        return fileUpdateMonitor;
    }

    public static void shutdownThreadPools() {
        TASK_EXECUTOR.shutdown();
        fileUpdateMonitor.shutdown();
        JabRefExecutorService.INSTANCE.shutdownEverything();
    }

    public static void stopBackgroundTasks() {
        stopTelemetryClient();
        Unirest.shutDown();
    }

    public static Optional<TelemetryClient> getTelemetryClient() {
        return Optional.ofNullable(telemetryClient);
    }
}
