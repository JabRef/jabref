package org.jabref.logic.push;

import java.io.File;
import java.util.Map;

import javafx.beans.property.MapProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleMapProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;

import org.jabref.logic.os.OS;

import static com.google.common.base.StandardSystemProperty.USER_HOME;

public class PushToApplicationPreferences {
    private final StringProperty activeApplicationName;
    private final MapProperty<String, String> commandPaths;
    private final StringProperty emacsArguments;
    private final StringProperty vimServer;

    private final ObjectProperty<CitationCommandString> citeCommand;
    private final ObjectProperty<CitationCommandString> defaultCiteCommand;

    private PushToApplicationPreferences() {
        this.activeApplicationName = new SimpleStringProperty("Texmaker");
        ObservableMap<String, String> commands = FXCollections.observableHashMap();
        commands.put("Texmaker", OS.detectProgramPath("texmaker", "Texmaker"));
        commands.put("WinEdt", OS.detectProgramPath("WinEdt", "WinEdt Team\\WinEdt"));
        commands.put("TeXstudio", OS.detectProgramPath("texstudio", "TeXstudio"));
        commands.put("TeXworks", OS.detectProgramPath("texworks", "TeXworks"));
        commands.put("Sublime Text", OS.detectProgramPath("subl", "Sublime"));
        commands.put("LyX/Kile", USER_HOME + File.separator + ".lyx/lyxpipe");
        commands.put("VScode", OS.detectProgramPath("Code", "Microsoft VS Code"));
        commands.put("Vim", "vim");
        commands.put("Emacs", OS.WINDOWS ? "emacsclient.exe" : "emacsclient");
        this.commandPaths = new SimpleMapProperty<>(commands);

        this.emacsArguments = new SimpleStringProperty("-n -e");
        this.vimServer = new SimpleStringProperty("vim");
        this.citeCommand = new SimpleObjectProperty<>(CitationCommandString.from("\\cite{key1,key2}"));
        this.defaultCiteCommand = new SimpleObjectProperty<>(CitationCommandString.from("\\cite{key1,key2}"));
    }

    public static PushToApplicationPreferences getDefault() {
        return new PushToApplicationPreferences();
    }

    public void setAll(PushToApplicationPreferences preferences) {
        this.activeApplicationName.set(preferences.activeApplicationName.get());
        this.commandPaths.set(preferences.commandPaths);
        this.vimServer.set(preferences.getVimServer());
        this.emacsArguments.set(preferences.getEmacsArguments());
        this.citeCommand.set(preferences.getCiteCommand());
        this.defaultCiteCommand.set(preferences.getDefaultCiteCommand());
    }

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
