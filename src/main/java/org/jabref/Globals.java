package org.jabref;

import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.util.Optional;
import java.util.UUID;

import org.jabref.gui.GlobalFocusListener;
import org.jabref.gui.StateManager;
import org.jabref.gui.keyboard.KeyBindingRepository;
import org.jabref.gui.util.DefaultFileUpdateMonitor;
import org.jabref.gui.util.DefaultTaskExecutor;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.exporter.ExporterFactory;
import org.jabref.logic.importer.ImportFormatReader;
import org.jabref.logic.journals.JournalAbbreviationLoader;
import org.jabref.logic.protectedterms.ProtectedTermsLoader;
import org.jabref.logic.remote.server.RemoteListenerServerLifecycle;
import org.jabref.logic.util.BuildInfo;
import org.jabref.model.util.FileUpdateMonitor;
import org.jabref.preferences.JabRefPreferences;

import com.google.common.base.StandardSystemProperty;
import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.TelemetryConfiguration;
import com.microsoft.applicationinsights.telemetry.SessionState;

public class Globals {

    // JabRef version info
    public static final BuildInfo BUILD_INFO = new BuildInfo();
    // Remote listener
    public static final RemoteListenerServerLifecycle REMOTE_LISTENER = new RemoteListenerServerLifecycle();
    public static final ImportFormatReader IMPORT_FORMAT_READER = new ImportFormatReader();
    public static final TaskExecutor TASK_EXECUTOR = new DefaultTaskExecutor();
    // In the main program, this field is initialized in JabRef.java
    // Each test case initializes this field if required
    public static JabRefPreferences prefs;
    /**
     * This field is initialized upon startup.
     * Only GUI code is allowed to access it, logic code should use dependency injection.
     */
    public static JournalAbbreviationLoader journalAbbreviationLoader;
    /**
     * This field is initialized upon startup.
     * Only GUI code is allowed to access it, logic code should use dependency injection.
     */
    public static ProtectedTermsLoader protectedTermsLoader;
    /**
     * Manager for the state of the GUI.
     */
    public static StateManager stateManager = new StateManager();
    public static ExporterFactory exportFactory;
    // Key binding preferences
    private static KeyBindingRepository keyBindingRepository;
    // Background tasks
    private static GlobalFocusListener focusListener;
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


    // Background tasks
    public static void startBackgroundTasks() {
        Globals.focusListener = new GlobalFocusListener();

        Globals.fileUpdateMonitor = new DefaultFileUpdateMonitor();
        JabRefExecutorService.INSTANCE.executeInterruptableTask(Globals.fileUpdateMonitor, "FileUpdateMonitor");

        if (Globals.prefs.shouldCollectTelemetry() && !GraphicsEnvironment.isHeadless()) {
            startTelemetryClient();
        }
    }

    private static void stopTelemetryClient() {
        if (Globals.prefs.shouldCollectTelemetry()) {
            getTelemetryClient().ifPresent(client -> client.trackSessionState(SessionState.End));
            getTelemetryClient().ifPresent(client -> client.flush());
        }
    }

    private static void startTelemetryClient() {
        TelemetryConfiguration telemetryConfiguration = TelemetryConfiguration.getActive();
        telemetryConfiguration.setInstrumentationKey(Globals.BUILD_INFO.getAzureInstrumentationKey());
        telemetryConfiguration.setTrackingIsDisabled(!Globals.prefs.shouldCollectTelemetry());
        telemetryClient = new TelemetryClient(telemetryConfiguration);
        telemetryClient.getContext().getProperties().put("JabRef version", Globals.BUILD_INFO.getVersion().toString());
        telemetryClient.getContext().getProperties().put("Java version", StandardSystemProperty.JAVA_VERSION.value());
        telemetryClient.getContext().getUser().setId(Globals.prefs.getOrCreateUserId());
        telemetryClient.getContext().getSession().setId(UUID.randomUUID().toString());
        telemetryClient.getContext().getDevice().setOperatingSystem(StandardSystemProperty.OS_NAME.value());
        telemetryClient.getContext().getDevice().setOperatingSystemVersion(StandardSystemProperty.OS_VERSION.value());
        telemetryClient.getContext().getDevice().setScreenResolution(
                Toolkit.getDefaultToolkit().getScreenSize().toString());

        telemetryClient.trackSessionState(SessionState.Start);
    }

    public static GlobalFocusListener getFocusListener() {
        return focusListener;
    }

    public static FileUpdateMonitor getFileUpdateMonitor() {
        return fileUpdateMonitor;
    }

    public static void shutdownThreadPools() {
        TASK_EXECUTOR.shutdown();
        JabRefExecutorService.INSTANCE.shutdownEverything();
    }

    public static void stopBackgroundTasks() {
        stopTelemetryClient();
    }

    public static Optional<TelemetryClient> getTelemetryClient() {
        return Optional.ofNullable(telemetryClient);
    }
}
