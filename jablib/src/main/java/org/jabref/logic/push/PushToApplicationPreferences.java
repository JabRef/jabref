package org.jabref.logic.push;

import java.util.Map;

import javafx.beans.property.MapProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleMapProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;

public class PushToApplicationPreferences {
    private final StringProperty activeApplicationName;
    private final MapProperty<String, String> commandPaths;
    private final StringProperty emacsArguments;
    private final StringProperty vimServer;

    private final ObjectProperty<CitationCommandString> citeCommand;
    private final ObjectProperty<CitationCommandString> defaultCiteCommand;

    public PushToApplicationPreferences(String activeApplicationName,
                                        Map<String, String> commandPaths,
                                        String emacsArguments,
                                        String vimServer,
                                        CitationCommandString citeCommand,
                                        CitationCommandString defaultCiteCommand) {
        this.activeApplicationName = new SimpleStringProperty(activeApplicationName);
        this.commandPaths = new SimpleMapProperty<>(FXCollections.observableMap(commandPaths));
        this.emacsArguments = new SimpleStringProperty(emacsArguments);
        this.vimServer = new SimpleStringProperty(vimServer);
        this.citeCommand = new SimpleObjectProperty<>(citeCommand);
        this.defaultCiteCommand = new SimpleObjectProperty<>(defaultCiteCommand);
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

    public CitationCommandString getCiteCommand() {
        return citeCommand.get();
    }

    public ObjectProperty<CitationCommandString> citeCommandProperty() {
        return citeCommand;
    }

    public void setCiteCommand(CitationCommandString citeCommand) {
        this.citeCommand.set(citeCommand);
    }

    public CitationCommandString getDefaultCiteCommand() {
        return defaultCiteCommand.getValue();
    }

    /// Modifies the current instance to set a new default citation command
    ///
    /// @return a new independent instance with the updated default citation command
    public PushToApplicationPreferences withCitationCommand(CitationCommandString config) {
        return new PushToApplicationPreferences(
                this.activeApplicationName.get(),
                this.commandPaths.get(),
                this.emacsArguments.get(),
                this.vimServer.get(),
                config,
                this.defaultCiteCommand.get());
    }
}
