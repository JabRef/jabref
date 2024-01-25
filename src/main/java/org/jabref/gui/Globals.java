package org.jabref.gui;

import org.jabref.architecture.AllowedToUseAwt;
import org.jabref.gui.keyboard.KeyBindingRepository;
import org.jabref.gui.remote.CLIMessageHandler;
import org.jabref.gui.undo.CountingUndoManager;
import org.jabref.gui.util.DefaultFileUpdateMonitor;
import org.jabref.gui.util.DefaultTaskExecutor;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.journals.predatory.PredatoryJournalRepository;
import org.jabref.logic.protectedterms.ProtectedTermsLoader;
import org.jabref.logic.remote.RemotePreferences;
import org.jabref.logic.remote.server.RemoteListenerServerManager;
import org.jabref.logic.util.BuildInfo;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.util.FileUpdateMonitor;
import org.jabref.preferences.PreferencesService;

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

    public static final RemoteListenerServerManager REMOTE_LISTENER = new RemoteListenerServerManager();

    /**
     * Manager for the state of the GUI.
     */
    public static StateManager stateManager = new StateManager();

    public static final TaskExecutor TASK_EXECUTOR = new DefaultTaskExecutor(stateManager);

    /**
     * Each test case initializes this field if required
     */
    public static PreferencesService prefs;

    /**
     * This field is initialized upon startup.
     * <p>
     * Only GUI code is allowed to access it, logic code should use dependency injection.
     */
    public static JournalAbbreviationRepository journalAbbreviationRepository;
    public static PredatoryJournalRepository predatoryJournalRepository;

    /**
     * This field is initialized upon startup.
     * <p>
     * Only GUI code is allowed to access it, logic code should use dependency injection.
     */
    public static ProtectedTermsLoader protectedTermsLoader;

    public static CountingUndoManager undoManager = new CountingUndoManager();
    public static BibEntryTypesManager entryTypesManager;

    private static ClipBoardManager clipBoardManager = null;
    private static KeyBindingRepository keyBindingRepository;

    private static DefaultFileUpdateMonitor fileUpdateMonitor;

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

    public static synchronized FileUpdateMonitor getFileUpdateMonitor() {
        if (fileUpdateMonitor == null) {
            fileUpdateMonitor = new DefaultFileUpdateMonitor();
            JabRefExecutorService.INSTANCE.executeInterruptableTask(fileUpdateMonitor, "FileUpdateMonitor");
        }
        return fileUpdateMonitor;
    }

    // Background tasks
    public static void startBackgroundTasks() {
        // TODO Currently deactivated due to incompatibilities in XML
      /*  if (Globals.prefs.getTelemetryPreferences().shouldCollectTelemetry() && !GraphicsEnvironment.isHeadless()) {
            Telemetry.start(prefs.getTelemetryPreferences());
        } */
        RemotePreferences remotePreferences = prefs.getRemotePreferences();
        if (remotePreferences.useRemoteServer()) {
            Globals.REMOTE_LISTENER.openAndStart(new CLIMessageHandler(prefs, fileUpdateMonitor, entryTypesManager), remotePreferences.getPort());
        }
    }

    public static void shutdownThreadPools() {
        TASK_EXECUTOR.shutdown();
        if (fileUpdateMonitor != null) {
            fileUpdateMonitor.shutdown();
        }
        JabRefExecutorService.INSTANCE.shutdownEverything();
    }

    public static void stopBackgroundTasks() {
        Telemetry.shutdown();
        Unirest.shutDown();
    }
}
