package org.jabref;

import org.jabref.collab.FileUpdateMonitor;
import org.jabref.gui.GlobalFocusListener;
import org.jabref.gui.StateManager;
import org.jabref.gui.keyboard.KeyBindingPreferences;
import org.jabref.gui.util.DefaultTaskExecutor;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.importer.ImportFormatReader;
import org.jabref.logic.journals.JournalAbbreviationLoader;
import org.jabref.logic.protectedterms.ProtectedTermsLoader;
import org.jabref.logic.remote.server.RemoteListenerServerLifecycle;
import org.jabref.logic.util.BuildInfo;
import org.jabref.preferences.JabRefPreferences;

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
    private static KeyBindingPreferences keyPrefs;
    // Background tasks
    private static GlobalFocusListener focusListener;
    private static FileUpdateMonitor fileUpdateMonitor;

    private Globals() {
    }

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

        Globals.fileUpdateMonitor = new FileUpdateMonitor();
        JabRefExecutorService.INSTANCE.executeInterruptableTask(Globals.fileUpdateMonitor, "FileUpdateMonitor");
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
}
