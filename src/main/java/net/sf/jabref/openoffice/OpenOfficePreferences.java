package net.sf.jabref.openoffice;

import java.io.File;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.util.OS;

public class OpenOfficePreferences {

    private final JabRefPreferences preferences;


    public OpenOfficePreferences(JabRefPreferences preferences) {
        this.preferences = preferences;
    }

    public void putDefaultPreferences() {
        if (OS.WINDOWS) {
            preferences.putDefaultValue(JabRefPreferences.OO_PATH, "C:\\Program Files\\OpenOffice.org 4");
            preferences.putDefaultValue(JabRefPreferences.OO_EXECUTABLE_PATH,
                    "C:\\Program Files\\OpenOffice.org 4\\program\\soffice.exe");
            preferences.putDefaultValue(JabRefPreferences.OO_JARS_PATH,
                    "C:\\Program Files\\OpenOffice.org 4\\program\\classes");
        } else if (OS.OS_X) {
            preferences.putDefaultValue(JabRefPreferences.OO_EXECUTABLE_PATH,
                    "/Applications/OpenOffice.org.app/Contents/MacOS/soffice.bin");
            preferences.putDefaultValue(JabRefPreferences.OO_PATH, "/Applications/OpenOffice.org.app");
            preferences.putDefaultValue(JabRefPreferences.OO_JARS_PATH,
                    "/Applications/OpenOffice.org.app/Contents/Resources/java");
        } else { // Linux
            preferences.putDefaultValue(JabRefPreferences.OO_PATH, "/opt/openoffice.org3");
            preferences.putDefaultValue(JabRefPreferences.OO_EXECUTABLE_PATH, "/usr/lib/openoffice/program/soffice");
            preferences.putDefaultValue(JabRefPreferences.OO_JARS_PATH, "/opt/openoffice.org/basis3.0");
        }

        preferences.putDefaultValue(JabRefPreferences.OO_SYNC_WHEN_CITING, false);
        preferences.putDefaultValue(JabRefPreferences.OO_SHOW_PANEL, false);
        preferences.putDefaultValue(JabRefPreferences.OO_USE_ALL_OPEN_BASES, true);
        preferences.putDefaultValue(JabRefPreferences.OO_USE_DEFAULT_AUTHORYEAR_STYLE, true);
        preferences.putDefaultValue(JabRefPreferences.OO_USE_DEFAULT_NUMERICAL_STYLE, false);
        preferences.putDefaultValue(JabRefPreferences.OO_CHOOSE_STYLE_DIRECTLY, false);
        preferences.putDefaultValue(JabRefPreferences.OO_DIRECT_FILE, "");
        preferences.putDefaultValue(JabRefPreferences.OO_STYLE_DIRECTORY, "");
    }

    public void updateConnectionParams(String ooPath, String execPath, String jarsPath) {
        preferences.put(JabRefPreferences.OO_PATH, ooPath);
        preferences.put(JabRefPreferences.OO_EXECUTABLE_PATH, execPath);
        setJarsPath(jarsPath);
    }

    public boolean checkAutoDetectedPaths() {
        if (preferences.hasKey(JabRefPreferences.OO_JARS_PATH)
                && preferences.hasKey(JabRefPreferences.OO_EXECUTABLE_PATH)) {
            return new File(getJarsPath(), "jurt.jar").exists()
                    && new File(preferences.get(JabRefPreferences.OO_EXECUTABLE_PATH)).exists();
        } else {
            return false;
        }
    }

    public String clearConnectionSettings() {
        preferences.clear(JabRefPreferences.OO_PATH);
        preferences.clear(JabRefPreferences.OO_EXECUTABLE_PATH);
        preferences.clear(JabRefPreferences.OO_JARS_PATH);
        return Localization.lang("Cleared connection settings.");
    }

    public String getJarsPath() {
        return preferences.get(JabRefPreferences.OO_JARS_PATH);
    }

    public void setJarsPath(String path) {
        preferences.put(JabRefPreferences.OO_JARS_PATH, path);
    }

    public boolean useAllDatabases() {
        return preferences.getBoolean(JabRefPreferences.OO_USE_ALL_OPEN_BASES);
    }

    public void setUseAllDatabases(boolean use) {
        preferences.putBoolean(JabRefPreferences.OO_USE_ALL_OPEN_BASES, use);
    }

    public boolean syncWhenCiting() {
        return Globals.prefs.getBoolean(JabRefPreferences.OO_SYNC_WHEN_CITING);
    }

    public void setSyncWhenCiting(boolean sync) {
        Globals.prefs.putBoolean(JabRefPreferences.OO_SYNC_WHEN_CITING, sync);
    }

    public boolean showPanel() {
        return Globals.prefs.getBoolean(JabRefPreferences.OO_SHOW_PANEL);
    }

    public void setShowPanel(boolean show) {
        Globals.prefs.putBoolean(JabRefPreferences.OO_SHOW_PANEL, show);
    }
}
