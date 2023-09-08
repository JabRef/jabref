package org.jabref.preferences;

import org.glassfish.hk2.api.Factory;

public class PreferenceServiceFactory implements Factory<PreferencesService> {
    @Override
    public PreferencesService provide() {
        return JabRefPreferences.getInstance();
    }

    @Override
    public void dispose(PreferencesService instance) {
    }
}
