package org.jabref.model.entry.event;

import org.jabref.model.FieldChange;

public class FieldAddedOrRemovedEvent extends FieldChangedEvent {

    public FieldAddedOrRemovedEvent(FieldChange fieldChange, EntryEventSource location) {
        super(fieldChange, location);
    }
}
