package org.jabref.logic.remote.server;

import org.jabref.preferences.PreferencesService;

@FunctionalInterface
public interface MessageHandler {

    void handleCommandLineArguments(String[] message, PreferencesService preferencesService);
}
