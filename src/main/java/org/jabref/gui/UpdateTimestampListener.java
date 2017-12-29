package org.jabref.gui;

import org.jabref.model.entry.event.EntryChangedEvent;
import org.jabref.preferences.JabRefPreferences;

import com.google.common.eventbus.Subscribe;

/**
 * Updates the timestamp of changed entries if the feature is enabled
 */
class UpdateTimestampListener {
    private final JabRefPreferences jabRefPreferences;

    UpdateTimestampListener(JabRefPreferences jabRefPreferences) {
        this.jabRefPreferences = jabRefPreferences;
    }

    @Subscribe
    public void listen(EntryChangedEvent event) {
        if (jabRefPreferences.getTimestampPreferences().includeTimestamps()) {
            event.getBibEntry().setField(jabRefPreferences.getTimestampPreferences().getTimestampField(),
                    jabRefPreferences.getTimestampPreferences().now());
        }
    }
}
