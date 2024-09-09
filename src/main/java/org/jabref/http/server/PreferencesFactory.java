package org.jabref.http.server;

import org.jabref.logic.preferences.JabRefCliCliPreferences;
import org.jabref.logic.preferences.CliPreferences;

import org.glassfish.hk2.api.Factory;

public class PreferencesFactory implements Factory<CliPreferences> {
    @Override
    public CliPreferences provide() {
        return JabRefCliCliPreferences.getInstance();
    }

    @Override
    public void dispose(CliPreferences instance) {
    }
}
