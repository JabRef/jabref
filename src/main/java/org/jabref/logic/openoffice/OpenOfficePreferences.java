package org.jabref.logic.openoffice;

import java.util.Arrays;
import java.util.List;

public class OpenOfficePreferences {

    public static final String DEFAULT_WINDOWS_PATH = "C:\\Program Files\\LibreOffice 5";
    public static final String DEFAULT_WIN_EXEC_PATH = "C:\\Program Files\\LibreOffice 5\\program";
    public static final String WINDOWS_EXECUTABLE = "soffice.exe";

    public static final String DEFAULT_OSX_PATH = "/Applications/OpenOffice.org.app";
    public static final String DEFAULT_OSX_EXEC_PATH = "/Applications/OpenOffice.org.app/Contents/MacOS/soffice";
    public static final String OSX_EXECUTABLE = "soffice";

    public static final String DEFAULT_LINUX_PATH = "/usr/lib/libreoffice";
    public static final String DEFAULT_LINUX_EXEC_PATH = "/usr/lib/libreoffice/program/soffice";
    public static final String LINUX_EXECUTABLE = "soffice";

    public static final List<String> OO_JARS = Arrays.asList("unoil.jar", "jurt.jar", "juh.jar", "ridl.jar");

    private String executablePath;
    private String installationPath;
    private Boolean useAllDatabases;
    private Boolean syncWhenCiting;
    private Boolean showPanel;
    private List<String> externalStyles;
    private String currentStyle;
    private String jarsPath;

    public OpenOfficePreferences(
            String jarsPath,
            String executablePath,
            String installationPath,
            Boolean useAllDatabases,
            Boolean syncWhenCiting,
            Boolean showPanel,
            List<String> externalStyles,
            String currentStyle
    ) {
        this.jarsPath = jarsPath;
        this.executablePath = executablePath;
        this.installationPath = installationPath;
        this.useAllDatabases = useAllDatabases;
        this.syncWhenCiting = syncWhenCiting;
        this.showPanel = showPanel;
        this.externalStyles = externalStyles;
        this.currentStyle = currentStyle;
    }

    public void clearCurrentStyle() {
        this.currentStyle = null;
        // TODO: sync to prefs
    }

    /**
     * path to soffice-file
     */
    public String getExecutablePath() {
        return executablePath;
    }

    public void setExecutablePath(String executablePath) {
        this.executablePath = executablePath;
    }

    /**
     * main directory for OO/LO installation, used to detect location on Win/OS X when using manual connect
     */
    public String getInstallationPath() {
        return installationPath;
    }

    public void setInstallationPath(String installationPath) {
        this.installationPath = installationPath;
    }

    /**
     * true if all databases should be used when citing
     */
    public Boolean getUseAllDatabases() {
        return useAllDatabases;
    }

    public void setUseAllDatabases(Boolean useAllDatabases) {
        this.useAllDatabases = useAllDatabases;
    }

    /**
     * true if the reference list is updated when adding a new citation
     */
    public Boolean getSyncWhenCiting() {
        return syncWhenCiting;
    }

    public void setSyncWhenCiting(Boolean syncWhenCiting) {
        this.syncWhenCiting = syncWhenCiting;
    }

    /**
     * true if the OO panel is shown on startup
     */
    public Boolean getShowPanel() {
        return showPanel;
    }

    public void setShowPanel(Boolean showPanel) {
        this.showPanel = showPanel;
    }

    /**
     * list with paths to external style files
     */
    public List<String> getExternalStyles() {
        return externalStyles;
    }

    public void setExternalStyles(List<String> externalStyles) {
        this.externalStyles = externalStyles;
    }

    /**
     * path to the used style file
     */
    public String getCurrentStyle() {
        return currentStyle;
    }

    public void setCurrentStyle(String currentStyle) {
        this.currentStyle = currentStyle;
    }

    /**
     * directory that contains juh.jar, jurt.jar, ridl.jar, unoil.jar
     */
    public String getJarsPath() {
        return jarsPath;
    }

    public void setJarsPath(String jarsPath) {
        this.jarsPath = jarsPath;
    }

    public void updateConnectionParams(String ooPath, String execPath, String jarsPath) {
        setInstallationPath(ooPath);
        setExecutablePath(execPath);
        setJarsPath(jarsPath);
    }

    public void clearConnectionSettings() {
        this.installationPath = null;
        this.executablePath = null;
        this.jarsPath = null;
    }

}
