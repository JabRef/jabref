package org.jabref.gui;

import org.jabref.model.entry.event.EntryChangedEvent;
import org.jabref.model.entry.field.StandardField;
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
        if (preferencesService.getTimestampPreferences().shouldAddModificationDate()) {
            event.getBibEntry().setField(StandardField.MODIFICATIONDATE,
                    preferencesService.getTimestampPreferences().now());
        }
    }
}
