package org.jabref.model.entry.event;

/**
 * This enum represents the context EntryEvents were sent from.
 */
public enum EntryEventSource {
    LOCAL,
    SHARED,
    UNDO,
    SAVE_ACTION
}
