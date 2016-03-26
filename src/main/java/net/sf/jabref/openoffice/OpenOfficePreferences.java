package net.sf.jabref.openoffice;

import java.io.File;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.logic.util.OS;

public class OpenOfficePreferences {

    public static void putDefaultPreferences() {
        if (OS.WINDOWS) {
            Globals.prefs.putDefaultValue(JabRefPreferences.OO_PATH, "C:\\Program Files\\OpenOffice.org 4");
            Globals.prefs.putDefaultValue(JabRefPreferences.OO_EXECUTABLE_PATH,
                    "C:\\Program Files\\OpenOffice.org 4\\program\\soffice.exe");
            Globals.prefs.putDefaultValue(JabRefPreferences.OO_JARS_PATH,
                    "C:\\Program Files\\OpenOffice.org 4\\program\\classes");
        } else if (OS.OS_X) {
            Globals.prefs.putDefaultValue(JabRefPreferences.OO_EXECUTABLE_PATH,
                    "/Applications/OpenOffice.org.app/Contents/MacOS/soffice.bin");
            Globals.prefs.putDefaultValue(JabRefPreferences.OO_PATH, "/Applications/OpenOffice.org.app");
            Globals.prefs.putDefaultValue(JabRefPreferences.OO_JARS_PATH,
                    "/Applications/OpenOffice.org.app/Contents/Resources/java");
        } else { // Linux
            Globals.prefs.putDefaultValue(JabRefPreferences.OO_PATH, "/opt/openoffice.org3");
            Globals.prefs.putDefaultValue(JabRefPreferences.OO_EXECUTABLE_PATH, "/usr/lib/openoffice/program/soffice");
            Globals.prefs.putDefaultValue(JabRefPreferences.OO_JARS_PATH, "/opt/openoffice.org/basis3.0");
        }

        Globals.prefs.putDefaultValue(JabRefPreferences.SYNC_OO_WHEN_CITING, false);
        Globals.prefs.putDefaultValue(JabRefPreferences.SHOW_OO_PANEL, false);
        Globals.prefs.putDefaultValue(JabRefPreferences.USE_ALL_OPEN_BASES, true);
        Globals.prefs.putDefaultValue(JabRefPreferences.OO_USE_DEFAULT_AUTHORYEAR_STYLE, true);
        Globals.prefs.putDefaultValue(JabRefPreferences.OO_USE_DEFAULT_NUMERICAL_STYLE, false);
        Globals.prefs.putDefaultValue(JabRefPreferences.OO_CHOOSE_STYLE_DIRECTLY, false);
        Globals.prefs.putDefaultValue(JabRefPreferences.OO_DIRECT_FILE, "");
        Globals.prefs.putDefaultValue(JabRefPreferences.OO_STYLE_DIRECTORY, "");
    }

     public static void updateConnectionParams(String ooPath, String ooExec, String ooJars) {
        Globals.prefs.put(JabRefPreferences.OO_PATH, ooPath);
        Globals.prefs.put(JabRefPreferences.OO_EXECUTABLE_PATH, ooExec);
        Globals.prefs.put(JabRefPreferences.OO_JARS_PATH, ooJars);
    }

    public static boolean checkAutoDetectedPaths() {

        if (Globals.prefs.hasKey(JabRefPreferences.OO_JARS_PATH)
                && Globals.prefs.hasKey(JabRefPreferences.OO_EXECUTABLE_PATH)) {
            return new File(Globals.prefs.get(JabRefPreferences.OO_JARS_PATH), "jurt.jar").exists()
                    && new File(Globals.prefs.get(JabRefPreferences.OO_EXECUTABLE_PATH)).exists();
        } else {
            return false;
        }
    }

}
