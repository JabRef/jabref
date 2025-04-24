package org.jabref.gui;

import org.jabref.logic.preferences.CliPreferences;
import org.jabref.model.entry.event.EntriesEventSource;
import org.jabref.model.entry.event.EntryChangedEvent;
import org.jabref.model.entry.field.StandardField;

import com.google.common.eventbus.Subscribe;

/**
 * Updates the timestamp of changed entries if the feature is enabled
 */
class UpdateTimestampListener {
    private final CliPreferences preferences;

    UpdateTimestampListener(CliPreferences preferences) {
        this.preferences = preferences;
    }

    @Subscribe
    public void listen(EntryChangedEvent event) {
        // The event source needs to be checked, since the timestamp is always updated on every change. The cleanup formatter is an exception to that behaviour,
        // since it just should move the contents from the timestamp field to modificationdate or creationdate.
        if (preferences.getTimestampPreferences().shouldAddModificationDate() && event.getEntriesEventSource() != EntriesEventSource.CLEANUP_TIMESTAMP) {
            event.getBibEntry().setField(StandardField.MODIFICATIONDATE,
                    preferences.getTimestampPreferences().now());
        }
    }
}
