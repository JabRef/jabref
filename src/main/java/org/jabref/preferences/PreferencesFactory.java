package org.jabref.preferences;

import org.glassfish.hk2.api.Factory;

public class PreferencesFactory implements Factory<Preferences> {
    @Override
    public Preferences provide() {
        return JabRefPreferences.getInstance();
    }

    @Override
    public void dispose(Preferences instance) {
    }
}
