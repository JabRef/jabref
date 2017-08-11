package org.jabref.model.entry.event;

import org.jabref.model.FieldChange;

/**
 * Created by jorglenh on 2017-08-11.
 */
public class FieldAddedEvent extends FieldChangedEvent {

    public FieldAddedEvent(FieldChange fieldChange, EntryEventSource location) {
        super(fieldChange, location);
    }
}
