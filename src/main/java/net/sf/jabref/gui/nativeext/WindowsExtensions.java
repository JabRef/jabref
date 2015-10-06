package net.sf.jabref.gui.nativeext;

import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.WString;
import com.sun.jna.ptr.PointerByReference;
import net.sf.jabref.Globals;
import net.sf.jabref.logic.util.OS;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Native extensions for Windows.
 */
public class WindowsExtensions {
    private static final Log LOGGER = LogFactory.getLog(WindowsExtensions.class);

    // Register native calls for pin to taskbar functionality
    static {
        if (OS.WINDOWS) {
            Native.register("shell32");
            LOGGER.info("Registered Shell32 DLL");
        }
    }

    /**
     * Sets the application user model id so that JabRef can be pinned to the taskbar
     * Only supported by Windows 7 and up!
     *
     * AppUserModelId must also be set in NSIS setup.nsi
     * WinShell::SetLnkAUMI "$INSTDIR\$(^Name).lnk" "${AppUserModelId}"
     * Structure: JabRef.${VERSION}
     *
     * Based on http://stackoverflow.com/a/1928830
     * http://stackoverflow.com/questions/5438651/launch4j-nsis-and-duplicate-pinned-windows-7-taskbar-icons
     */
    public static void enablePinToTaskbar() {
        if(supportsPinToTaskbar()) {
            setCurrentProcessExplicitAppUserModelID("JabRef." + Globals.BUILD_INFO.getVersion());
        } else {
            LOGGER.info("Does not support pin to taskbar.");
        }
    }

    private static void setCurrentProcessExplicitAppUserModelID(final String appID) {
        if (SetCurrentProcessExplicitAppUserModelID(new WString(appID)).longValue() != 0) {
            throw new RuntimeException("unable to set current process explicit AppUserModelID to: " + appID);
        }
    }

    private static native NativeLong SetCurrentProcessExplicitAppUserModelID(WString appID);

    private static boolean supportsPinToTaskbar() {
        if (!OS.WINDOWS) {
            return false;
        }

        try {
            Float version = Float.parseFloat(System.getProperty("os.version"));
            // Windows 7 == 6.1
            return version >= 6.1;
        } catch (NumberFormatException ex) {
            return false;
        }
    }
}
