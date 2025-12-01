package org.jabref.http.server;

import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.preferences.JabRefCliPreferences;

import org.glassfish.hk2.api.Factory;

public class PreferencesFactory implements Factory<CliPreferences> {

    private final CliPreferences preferences;

    public PreferencesFactory(CliPreferences preferences) {
        this.preferences = preferences;
    }

    @Override
    public CliPreferences provide() {
        return preferences;
    }

    @Override
    public void dispose(CliPreferences instance) {
    }
}
