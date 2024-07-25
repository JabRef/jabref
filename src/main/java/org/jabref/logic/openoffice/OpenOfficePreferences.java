package org.jabref.logic.openoffice;

import java.util.List;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.logic.openoffice.style.OOStyle;

public class OpenOfficePreferences {

    public static final String DEFAULT_WIN_EXEC_PATH = "C:\\Program Files\\LibreOffice\\program";
    public static final String WINDOWS_EXECUTABLE = "soffice.exe";

    public static final String DEFAULT_OSX_EXEC_PATH = "/Applications/LibreOffice.app/Contents/MacOS/soffice";
    public static final String OSX_EXECUTABLE = "soffice";

    public static final String DEFAULT_LINUX_EXEC_PATH = "/usr/lib/libreoffice/program/soffice";
    public static final String DEFAULT_LINUX_FLATPAK_EXEC_PATH = "/app/bin/soffice";
    public static final String LINUX_EXECUTABLE = "soffice";

    private final StringProperty executablePath;
    private final BooleanProperty useAllDatabases;
    private final BooleanProperty syncWhenCiting;
    private final ObservableList<String> externalStyles;
    private final StringProperty currentJStyle;
    private final ObjectProperty<OOStyle> currentStyle;

    public OpenOfficePreferences(String executablePath,
                                 boolean useAllDatabases,
                                 boolean syncWhenCiting,
                                 List<String> externalStyles,
                                 String currentJStyle,
                                 OOStyle currentStyle) {
        this.executablePath = new SimpleStringProperty(executablePath);
        this.useAllDatabases = new SimpleBooleanProperty(useAllDatabases);
        this.syncWhenCiting = new SimpleBooleanProperty(syncWhenCiting);
        this.externalStyles = FXCollections.observableArrayList(externalStyles);
        this.currentJStyle = new SimpleStringProperty(currentJStyle);
        this.currentStyle = new SimpleObjectProperty<>(currentStyle);
    }

    public void clearConnectionSettings() {
        this.executablePath.set("");
    }

    public void clearCurrentJStyle() {
        this.currentJStyle.set("");
    }

    /**
     * path to soffice-file
     */
    public String getExecutablePath() {
        return executablePath.get();
    }

    public StringProperty executablePathProperty() {
        return executablePath;
    }

    public void setExecutablePath(String executablePath) {
        this.executablePath.setValue(executablePath);
    }

    /**
     * true if all databases should be used when citing
     */
    public boolean getUseAllDatabases() {
        return useAllDatabases.get();
    }

    public BooleanProperty useAllDatabasesProperty() {
        return useAllDatabases;
    }

    public void setUseAllDatabases(boolean useAllDatabases) {
        this.useAllDatabases.set(useAllDatabases);
    }

    /**
     * true if the reference list is updated when adding a new citation
     */
    public boolean getSyncWhenCiting() {
        return syncWhenCiting.get();
    }

    public BooleanProperty syncWhenCitingProperty() {
        return syncWhenCiting;
    }

    public void setSyncWhenCiting(boolean syncWhenCiting) {
        this.syncWhenCiting.setValue(syncWhenCiting);
    }

    /**
     * list with paths to external style files
     */
    public ObservableList<String> getExternalStyles() {
        return externalStyles;
    }

    public void setExternalStyles(List<String> list) {
        externalStyles.clear();
        externalStyles.addAll(list);
    }

    /**
     * path to the used style file
     */
    public String getCurrentJStyle() {
        return currentJStyle.get();
    }

    public StringProperty currentJStyleProperty() {
        return currentJStyle;
    }

    public void setCurrentJStyle(String currentJStyle) {
        this.currentJStyle.set(currentJStyle);
    }

    public OOStyle getCurrentStyle() {
        return currentStyle.get();
    }

    public ObjectProperty<OOStyle> currentStyleProperty() {
        return currentStyle;
    }

    public void setCurrentStyle(OOStyle style) {
        this.currentStyle.set(style);
    }
}
