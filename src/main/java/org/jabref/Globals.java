package org.jabref;

import java.awt.Toolkit;
import java.util.UUID;

import org.jabref.collab.FileUpdateMonitor;
import org.jabref.gui.GlobalFocusListener;
import org.jabref.gui.StateManager;
import org.jabref.gui.keyboard.KeyBindingRepository;
import org.jabref.gui.util.DefaultTaskExecutor;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.importer.ImportFormatReader;
import org.jabref.logic.journals.JournalAbbreviationLoader;
import org.jabref.logic.protectedterms.ProtectedTermsLoader;
import org.jabref.logic.remote.server.RemoteListenerServerLifecycle;
import org.jabref.logic.util.BuildInfo;
import org.jabref.preferences.JabRefPreferences;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.TelemetryConfiguration;
import com.microsoft.applicationinsights.telemetry.SessionState;
import org.apache.commons.lang3.SystemUtils;

public class Globals {

    // JabRef version info
    public static final BuildInfo BUILD_INFO = new BuildInfo();
    // Remote listener
    public static final RemoteListenerServerLifecycle REMOTE_LISTENER = new RemoteListenerServerLifecycle();

    public static final ImportFormatReader IMPORT_FORMAT_READER = new ImportFormatReader();
    public static final TaskExecutor taskExecutor = new DefaultTaskExecutor();
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
    // Key binding preferences
    private static KeyBindingRepository keyBindingRepository;
    // Background tasks
    private static GlobalFocusListener focusListener;
    private static FileUpdateMonitor fileUpdateMonitor;
    private static TelemetryClient telemetryClient;

    private Globals() {
    }

    // Key binding preferences
    public static KeyBindingRepository getKeyPrefs() {
        if (keyBindingRepository == null) {
            keyBindingRepository = prefs.getKeyBindingRepository();
        }
        return keyBindingRepository;
    }


    // Background tasks
    public static void startBackgroundTasks() {
        Globals.focusListener = new GlobalFocusListener();

        Globals.fileUpdateMonitor = new FileUpdateMonitor();
        JabRefExecutorService.INSTANCE.executeInterruptableTask(Globals.fileUpdateMonitor, "FileUpdateMonitor");

        startTelemetryClient();
    }

    private static void stopTelemetryClient() {
        telemetryClient.trackSessionState(SessionState.End);
        telemetryClient.flush();
    }

    private static void startTelemetryClient() {
        TelemetryConfiguration telemetryConfiguration = TelemetryConfiguration.getActive();
        telemetryConfiguration.setInstrumentationKey(Globals.BUILD_INFO.getAzureInstrumentationKey());
        telemetryConfiguration.setTrackingIsDisabled(!Globals.prefs.shouldCollectTelemetry());
        telemetryClient = new TelemetryClient(telemetryConfiguration);
        telemetryClient.getContext().getProperties().put("JabRef version", Globals.BUILD_INFO.getVersion().toString());
        telemetryClient.getContext().getProperties().put("Java version", SystemUtils.JAVA_RUNTIME_VERSION);
        telemetryClient.getContext().getUser().setId(Globals.prefs.getOrCreateUserId());
        telemetryClient.getContext().getSession().setId(UUID.randomUUID().toString());
        telemetryClient.getContext().getDevice().setOperatingSystem(SystemUtils.OS_NAME);
        telemetryClient.getContext().getDevice().setOperatingSystemVersion(SystemUtils.OS_VERSION);
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
        taskExecutor.shutdown();
        JabRefExecutorService.INSTANCE.shutdownEverything();
    }

    public static void stopBackgroundTasks() {
        stopTelemetryClient();
    }

    public static TelemetryClient getTelemetryClient() {
        return telemetryClient;
    }
}
