package net.sf.jabref;

import java.util.Optional;

import net.sf.jabref.collab.FileUpdateMonitor;
import net.sf.jabref.gui.GlobalFocusListener;
import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.gui.exporter.AutoSaveManager;
import net.sf.jabref.gui.keyboard.KeyBindingPreferences;
import net.sf.jabref.logic.error.StreamEavesdropper;
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

    // Key binding preferences
    private static KeyBindingPreferences keyPrefs;

    // Background tasks
    private static GlobalFocusListener focusListener;
    private static FileUpdateMonitor fileUpdateMonitor;
    private static StreamEavesdropper streamEavesdropper;

    // Autosave manager
    private static AutoSaveManager autoSaveManager;

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

    // Autosave manager
    public static void startAutoSaveManager(JabRefFrame frame) {
        Globals.autoSaveManager = new AutoSaveManager(frame);
        Globals.autoSaveManager.startAutoSaveTimer();
    }

    // Stop the autosave manager if it has been started
    public static void stopAutoSaveManager() {
        if (Globals.autoSaveManager != null) {
            Globals.autoSaveManager.stopAutoSaveTimer();
            Globals.autoSaveManager.clearAutoSaves();
            Globals.autoSaveManager = null;
        }
    }

    public static Optional<AutoSaveManager> getAutoSaveManager() {
        return Optional.ofNullable(Globals.autoSaveManager);
    }
}
