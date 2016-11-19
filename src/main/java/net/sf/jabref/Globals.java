package net.sf.jabref;

import java.awt.Toolkit;
import java.util.UUID;

import net.sf.jabref.collab.FileUpdateMonitor;
import net.sf.jabref.gui.GlobalFocusListener;
import net.sf.jabref.gui.keyboard.KeyBindingPreferences;
import net.sf.jabref.logic.error.StreamEavesdropper;
import net.sf.jabref.logic.importer.ImportFormatReader;
import net.sf.jabref.logic.journals.JournalAbbreviationLoader;
import net.sf.jabref.logic.protectedterms.ProtectedTermsLoader;
import net.sf.jabref.logic.remote.server.RemoteListenerServerLifecycle;
import net.sf.jabref.logic.util.BuildInfo;
import net.sf.jabref.preferences.JabRefPreferences;

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

    // Key binding preferences
    private static KeyBindingPreferences keyPrefs;

    // Background tasks
    private static GlobalFocusListener focusListener;
    private static FileUpdateMonitor fileUpdateMonitor;
    private static StreamEavesdropper streamEavesdropper;
    private static TelemetryClient telemetryClient;

    // Key binding preferences
    public static KeyBindingPreferences getKeyPrefs() {
        if (keyPrefs == null) {
            keyPrefs = new KeyBindingPreferences(prefs);
        }
        return keyPrefs;
    }


    // Background tasks
    public static void startBackgroundTasks() {
        Globals.focusListener = new GlobalFocusListener();

        Globals.streamEavesdropper = StreamEavesdropper.eavesdropOnSystem();

        Globals.fileUpdateMonitor = new FileUpdateMonitor();
        JabRefExecutorService.INSTANCE.executeWithLowPriorityInOwnThread(Globals.fileUpdateMonitor,
                "FileUpdateMonitor");

        startTelemetryClient();
    }

    private static void stopTelemetryClient() {
        telemetryClient.trackSessionState(SessionState.End);
        telemetryClient.flush();
    }

    private static void startTelemetryClient() {
        TelemetryConfiguration telemetryConfiguration = TelemetryConfiguration.getActive();
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

    public static StreamEavesdropper getStreamEavesdropper() {
        return streamEavesdropper;
    }

    public static void stopBackgroundTasks() {
        stopTelemetryClient();
    }

    public static TelemetryClient getTelemetryClient() {
        return telemetryClient;
    }
}
