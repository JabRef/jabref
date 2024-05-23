package org.jabref.gui;

import org.jabref.architecture.AllowedToUseAwt;
import org.jabref.gui.keyboard.KeyBindingRepository;
import org.jabref.gui.util.DefaultDirectoryMonitor;
import org.jabref.gui.util.DefaultFileUpdateMonitor;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.protectedterms.ProtectedTermsLoader;
import org.jabref.logic.remote.server.RemoteListenerServerManager;
import org.jabref.logic.util.BuildInfo;
import org.jabref.model.util.DirectoryMonitor;
import org.jabref.model.util.FileUpdateMonitor;

import com.airhacks.afterburner.injection.Injector;

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

    private static ClipBoardManager clipBoardManager = null;

    private static DefaultFileUpdateMonitor fileUpdateMonitor;
    private static DefaultDirectoryMonitor directoryMonitor;

    private Globals() {
    }

    // Key binding preferences
    public static synchronized KeyBindingRepository getKeyPrefs() {
        return Injector.instantiateModelOrService(KeyBindingRepository.class);
    }

    public static synchronized ClipBoardManager getClipboardManager() {
        if (clipBoardManager == null) {
            clipBoardManager = new ClipBoardManager();
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

    public static DirectoryMonitor getDirectoryMonitor() {
        if (directoryMonitor == null) {
            directoryMonitor = new DefaultDirectoryMonitor();
        }
        return directoryMonitor;
    }
}
