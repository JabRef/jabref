package org.jabref;

import org.jabref.collab.FileUpdateMonitor;
import net.sf.jabref.gui.GlobalFocusListener;
import net.sf.jabref.gui.StateManager;
import net.sf.jabref.gui.keyboard.KeyBindingPreferences;
import net.sf.jabref.logic.importer.ImportFormatReader;
import net.sf.jabref.logic.journals.JournalAbbreviationLoader;
import net.sf.jabref.logic.protectedterms.ProtectedTermsLoader;
import net.sf.jabref.logic.remote.server.RemoteListenerServerLifecycle;
import net.sf.jabref.logic.util.BuildInfo;
import net.sf.jabref.preferences.JabRefPreferences;

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

    /**
     * Manager for the state of the GUI.
     */
    public static StateManager stateManager = new StateManager();

    // Key binding preferences
    private static KeyBindingPreferences keyPrefs;

    // Background tasks
    private static GlobalFocusListener focusListener;
    private static FileUpdateMonitor fileUpdateMonitor;

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
}
