package org.jabref.preferences;

import java.util.Map;

public class PushToApplicationPreferences {
    private String activeApplicationName;
    private Map<String, String> pushToApplicationCommandPaths;
    private String emacsArguments;
    private String vimServer;

    public PushToApplicationPreferences(String activeApplicationName, Map<String, String> pushToApplicationCommandPaths, String emacsArguments, String vimServer) {
        this.activeApplicationName = activeApplicationName;
        this.pushToApplicationCommandPaths = pushToApplicationCommandPaths;
        this.emacsArguments = emacsArguments;
        this.vimServer = vimServer;
    }

    public String getActiveApplicationName() {
        return activeApplicationName;
    }

    public Map<String, String> getPushToApplicationCommandPaths() {
        return pushToApplicationCommandPaths;
    }

    public PushToApplicationPreferences withPushToApplicationCommandPaths(Map<String, String> commandPaths) {
        this.pushToApplicationCommandPaths = commandPaths;
        return this;
    }

    public String getEmacsArguments() {
        return emacsArguments;
    }

    public PushToApplicationPreferences withEmacsArguments(String emacsArguments) {
        this.emacsArguments = emacsArguments;
        return this;
    }

    public String getVimServer() {
        return vimServer;
    }

    public PushToApplicationPreferences withVimServer(String vimServer) {
        this.vimServer = vimServer;
        return this;
    }
}
