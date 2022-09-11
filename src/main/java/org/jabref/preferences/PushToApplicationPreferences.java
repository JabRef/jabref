package org.jabref.preferences;

import java.util.Map;

import javafx.beans.property.MapProperty;
import javafx.beans.property.SimpleMapProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;

public class PushToApplicationPreferences {
    private final StringProperty activeApplicationName;
    private final MapProperty<String, String> commandPaths;
    private final StringProperty emacsArguments;
    private final StringProperty vimServer;

    public PushToApplicationPreferences(String activeApplicationName,
                                        Map<String, String> commandPaths,
                                        String emacsArguments,
                                        String vimServer) {
        this.activeApplicationName = new SimpleStringProperty(activeApplicationName);
        this.commandPaths = new SimpleMapProperty<>(FXCollections.observableMap(commandPaths));
        this.emacsArguments = new SimpleStringProperty(emacsArguments);
        this.vimServer = new SimpleStringProperty(vimServer);
    }

    public String getActiveApplicationName() {
        return activeApplicationName.getValue();
    }

    public StringProperty activeApplicationNameProperty() {
        return activeApplicationName;
    }

    public void setActiveApplicationName(String activeApplicationName) {
        this.activeApplicationName.set(activeApplicationName);
    }

    public MapProperty<String, String> getCommandPaths() {
        return commandPaths;
    }

    public void setCommandPaths(Map<String, String> commandPaths) {
        this.commandPaths.clear();
        this.commandPaths.putAll(commandPaths);
    }

    public String getEmacsArguments() {
        return emacsArguments.get();
    }

    public StringProperty emacsArgumentsProperty() {
        return emacsArguments;
    }

    public void setEmacsArguments(String emacsArguments) {
        this.emacsArguments.set(emacsArguments);
    }

    public String getVimServer() {
        return vimServer.get();
    }

    public StringProperty vimServerProperty() {
        return vimServer;
    }

    public void setVimServer(String vimServer) {
        this.vimServer.set(vimServer);
    }
}
