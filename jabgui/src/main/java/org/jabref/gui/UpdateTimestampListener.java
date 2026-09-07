package org.jabref.gui;

import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.undo.UndoManager;
import org.jabref.model.entry.event.EntriesEventSource;
import org.jabref.model.entry.event.EntryChangedEvent;
import org.jabref.model.entry.event.FieldChangedEvent;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.undo.UndoableFieldChange;

import com.google.common.eventbus.Subscribe;

/// Updates the timestamp of changed entries if the feature is enabled.
///
/// The timestamp is recorded as a derived part of the undo step that caused it, so undoing an
/// edit also brings the modification date back and the entry matches the saved file again.
class UpdateTimestampListener {
    private final CliPreferences preferences;
    private final UndoManager undoManager;

    UpdateTimestampListener(CliPreferences preferences, UndoManager undoManager) {
        this.preferences = preferences;
        this.undoManager = undoManager;
    }

    @Subscribe
    public void listen(EntryChangedEvent event) {
        if (!preferences.getTimestampPreferences().shouldAddModificationDate()) {
            return;
        }
        // The cleanup formatter only moves the timestamp field to modificationdate or creationdate.
        if (event.getEntriesEventSource() == EntriesEventSource.CLEANUP_TIMESTAMP) {
            return;
        }
        // A change of the timestamp itself (its undo included) is not an edit to stamp.
        if ((event instanceof FieldChangedEvent fieldChange) && (fieldChange.getField() == StandardField.MODIFICATIONDATE)) {
            return;
        }
        // Undo and redo replay the recorded timestamp; stamping again would leave a date that was never recorded.
        if (undoManager.isReplaying()) {
            return;
        }
        event.getBibEntry()
             .setField(StandardField.MODIFICATIONDATE, preferences.getTimestampPreferences().now())
             .ifPresent(change -> undoManager.addDerivedEdit(new UndoableFieldChange(change)));
    }
}
