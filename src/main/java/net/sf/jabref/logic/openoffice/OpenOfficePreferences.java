/*  Copyright (C) 2016 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.sf.jabref.logic.openoffice;

import java.io.File;
import java.util.List;

import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.util.OS;

/**
 * The JabRef preferences are:
 * OO_PATH main directory for OO/LO installation, used to detect location on Win/OS X when using manual connect
 * OO_EXECUTABLE_PATH path to soffice-file
 * OO_JARS_PATH directory that contains juh.jar, jurt.jar, ridl.jar, unoil.jar
 * OO_SYNC_WHEN_CITING true if the reference list is updated when adding a new citation
 * OO_SHOW_PANEL true if the OO panel is shown on startup
 * OO_USE_ALL_OPEN_DATABASES true if all databases should be used when citing
 * OO_BIBLIOGRAPHY_STYLE_FILE path to the used style file
 * OO_EXTERNAL_STYLE_FILES list with paths to external style files
 *
 */
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
        preferences.putDefaultValue(JabRefPreferences.OO_BIBLIOGRAPHY_STYLE_FILE,
                StyleLoader.DEFAULT_AUTHORYEAR_STYLE_PATH);
        preferences.putDefaultValue(JabRefPreferences.OO_EXTERNAL_STYLE_FILES, "");
    }

    public void updateConnectionParams(String ooPath, String execPath, String jarsPath) {
        setOOPath(ooPath);
        setExecutablePath(execPath);
        setJarsPath(jarsPath);
    }

    public boolean checkAutoDetectedPaths() {
        if (preferences.hasKey(JabRefPreferences.OO_JARS_PATH)
                && preferences.hasKey(JabRefPreferences.OO_EXECUTABLE_PATH)) {
            return new File(getJarsPath(), "jurt.jar").exists() && new File(getExecutablePath()).exists();
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

    public String getExecutablePath() {
        return preferences.get(JabRefPreferences.OO_EXECUTABLE_PATH);
    }

    public void setExecutablePath(String path) {
        preferences.put(JabRefPreferences.OO_EXECUTABLE_PATH, path);
    }

    public String getOOPath() {
        return preferences.get(JabRefPreferences.OO_PATH);
    }

    public void setOOPath(String path) {
        preferences.put(JabRefPreferences.OO_PATH, path);
    }

    public boolean useAllDatabases() {
        return preferences.getBoolean(JabRefPreferences.OO_USE_ALL_OPEN_BASES);
    }

    public void setUseAllDatabases(boolean use) {
        preferences.putBoolean(JabRefPreferences.OO_USE_ALL_OPEN_BASES, use);
    }

    public boolean syncWhenCiting() {
        return preferences.getBoolean(JabRefPreferences.OO_SYNC_WHEN_CITING);
    }

    public void setSyncWhenCiting(boolean sync) {
        preferences.putBoolean(JabRefPreferences.OO_SYNC_WHEN_CITING, sync);
    }

    public boolean showPanel() {
        return preferences.getBoolean(JabRefPreferences.OO_SHOW_PANEL);
    }

    public void setShowPanel(boolean show) {
        preferences.putBoolean(JabRefPreferences.OO_SHOW_PANEL, show);
    }

    public List<String> getExternalStyles() {
        return preferences.getStringList(JabRefPreferences.OO_EXTERNAL_STYLE_FILES);
    }

    public void setExternalStyles(List<String> filenames) {
        preferences.putStringList(JabRefPreferences.OO_EXTERNAL_STYLE_FILES, filenames);
    }

    public String getCurrentStyle() {
        return preferences.get(JabRefPreferences.OO_BIBLIOGRAPHY_STYLE_FILE);
    }

    public void setCurrentStyle(String path) {
        preferences.put(JabRefPreferences.OO_BIBLIOGRAPHY_STYLE_FILE, path);
    }
}
