package org.jabref.gui;

import org.jabref.model.entry.event.EntryChangedEvent;
import org.jabref.preferences.PreferencesService;

import com.google.common.eventbus.Subscribe;

/**
 * Updates the timestamp of changed entries if the feature is enabled
 */
class UpdateTimestampListener {
    private final PreferencesService preferencesService;

    UpdateTimestampListener(PreferencesService preferencesService) {
        this.preferencesService = preferencesService;
    }

    @Subscribe
    public void listen(EntryChangedEvent event) {
        if (preferencesService.getTimestampPreferences().includeTimestamps()) {
            event.getBibEntry().setField(preferencesService.getTimestampPreferences().getTimestampField(),
                    preferencesService.getTimestampPreferences().now());
        }
    }
}
