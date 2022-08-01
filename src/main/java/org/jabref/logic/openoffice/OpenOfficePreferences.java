package org.jabref.logic.openoffice;

import java.util.List;

public class OpenOfficePreferences {

    public static final String DEFAULT_WINDOWS_PATH = "C:\\Program Files\\LibreOffice 5";
    public static final String DEFAULT_WIN_EXEC_PATH = "C:\\Program Files\\LibreOffice 5\\program";
    public static final String WINDOWS_EXECUTABLE = "soffice.exe";

    public static final String DEFAULT_OSX_PATH = "/Applications/LibreOffice.app";
    public static final String DEFAULT_OSX_EXEC_PATH = "/Applications/LibreOffice.app/Contents/MacOS/soffice";
    public static final String OSX_EXECUTABLE = "soffice";

    public static final String DEFAULT_LINUX_PATH = "/usr/lib/libreoffice";
    public static final String DEFAULT_LINUX_EXEC_PATH = "/usr/lib/libreoffice/program/soffice";
    public static final String DEFAULT_LINUX_FLATPAK_EXEC_PATH = "/app/bin/soffice";
    public static final String LINUX_EXECUTABLE = "soffice";

    private String executablePath;
    private String installationPath;
    private boolean useAllDatabases;
    private boolean syncWhenCiting;
    private List<String> externalStyles;
    private String currentStyle;

    public OpenOfficePreferences(String executablePath,
                                 String installationPath,
                                 boolean useAllDatabases,
                                 boolean syncWhenCiting,
                                 List<String> externalStyles,
                                 String currentStyle) {
        this.executablePath = executablePath;
        this.installationPath = installationPath;
        this.useAllDatabases = useAllDatabases;
        this.syncWhenCiting = syncWhenCiting;
        this.externalStyles = externalStyles;
        this.currentStyle = currentStyle;
    }

    public void clearCurrentStyle() {
        this.currentStyle = null;
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

    public void setSyncWhenCiting(boolean syncWhenCiting) {
        this.syncWhenCiting = syncWhenCiting;
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

    public void clearConnectionSettings() {
        this.installationPath = "";
        this.executablePath = "";
    }
}
