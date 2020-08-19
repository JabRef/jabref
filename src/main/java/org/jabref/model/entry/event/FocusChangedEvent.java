package org.jabref.model.entry.event;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;

/**
 * <code>FocusChangedEvent</code> is fired when the focus of a field of <code>BibEntry</code> has been requested.
 */

public class FocusChangedEvent extends EntryChangedEvent {

    private final Field field;

    /**
     * @param bibEntry Affected BibEntry object
     * @param field    Name of field which focus has been requested
     * @param location location Location affected by this event
     */
    public FocusChangedEvent(BibEntry bibEntry, Field field, EntriesEventSource location) {
        super(bibEntry, location);
        this.field = field;
    }

    /**
     * @param bibEntry Affected BibEntry object
     * @param field    Name of field which focus has been requested
     */
    public FocusChangedEvent(BibEntry bibEntry, Field field) {
        super(bibEntry);
        this.field = field;
    }

    public Field getField() {
        return field;
    }

}
