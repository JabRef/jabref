package org.jabref.model.entry.event;

/**
 * This enum represents the context EntriesEvents were sent from.
 */
public enum EntriesEventSource {
    LOCAL,
    SHARED,
    UNDO,
    CLEANUP_TIMESTAMP,
    SAVE_ACTION
}
