package org.jabref.logic.push;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.prefs.Preferences;

import javafx.beans.property.MapProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleMapProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;

import org.jabref.logic.os.OS;

public class PushToApplicationPreferences {
    private final StringProperty activeApplicationName;
    private final MapProperty<String, String> commandPaths;
    private final StringProperty emacsArguments;
    private final StringProperty vimServer;

    private final ObjectProperty<CitationCommandString> citeCommand;
    private final ObjectProperty<CitationCommandString> defaultCiteCommand;


    private PushToApplicationPreferences(){
        this.activeApplicationName = new SimpleStringProperty(PushApplications.TEXSTUDIO.getDisplayName());
        Map<String, String> commands = new HashMap<>();
        commands.put(PushApplications.TEXMAKER.getDisplayName(), getEmptyIsDefault(PushApplications.TEXMAKER.getKey(), OS.detectProgramPath("texmaker", "Texmaker")));
        commands.put(PushApplications.WIN_EDT.getDisplayName(),  getEmptyIsDefault(PushApplications.WIN_EDT.getKey(), OS.detectProgramPath("WinEdt", "WinEdt Team\\WinEdt")));
        commands.put(PushApplications.TEXSTUDIO.getDisplayName(),  getEmptyIsDefault(PushApplications.TEXSTUDIO.getKey(), OS.detectProgramPath("texstudio", "TeXstudio")));
        commands.put(PushApplications.TEXWORKS.getDisplayName(), getEmptyIsDefault(PushApplications.TEXWORKS.getKey(), OS.detectProgramPath("texworks", "TeXworks")));
        commands.put(PushApplications.SUBLIME_TEXT.getDisplayName(), getEmptyIsDefault(PushApplications.SUBLIME_TEXT.getKey(), OS.detectProgramPath("subl", "Sublime")));
        commands.put(PushApplications.LYX.getDisplayName(), getEmptyIsDefault(PushApplications.LYX.getKey(), System.getProperty("user.home") + File.separator + ".lyx/lyxpipe"));
        commands.put(PushApplications.VSCODE.getDisplayName(), getEmptyIsDefault(PushApplications.VSCODE.getKey(), OS.detectProgramPath("Code", "Microsoft VS Code")));
        commands.put(PushApplications.VIM.getDisplayName(), getEmptyIsDefault(PushApplications.VIM.getKey(), "vim"));

        if(OS.WINDOWS){
            commands.put(PushApplications.EMACS.getDisplayName(), "emacsclient.exe");
        }else if(OS.OS_X || OS.LINUX){
            commands.put(PushApplications.EMACS.getDisplayName(), "emacsclient");
        }
        this.commandPaths = new SimpleMapProperty<>(FXCollections.observableMap(commands));

        this.emacsArguments = new SimpleStringProperty("-n -e");
        this.vimServer = new SimpleStringProperty("vim");
        this.citeCommand = new SimpleObjectProperty<>(CitationCommandString.from("\\cite{key1,key2}"));
        this.defaultCiteCommand = new SimpleObjectProperty<>(CitationCommandString.from("\\cite{key1,key2}"));
    }

    private String getEmptyIsDefault(String key, String defaultValue) {
        final Preferences PREFS_NODE = Preferences.userRoot().node("/org/jabref");
        String result = PREFS_NODE.get(key, defaultValue);
        if ("".equals(result)) {
            return defaultValue;
        }
        return result;
    }


    public static PushToApplicationPreferences getDefault(){
        return new PushToApplicationPreferences();
    }

    public void setAll(PushToApplicationPreferences preferences){
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
