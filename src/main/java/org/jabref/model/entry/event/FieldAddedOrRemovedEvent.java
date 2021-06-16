package org.jabref.model.entry.event;

import org.jabref.model.FieldChange;

public class FieldAddedOrRemovedEvent extends FieldChangedEvent {

    public FieldAddedOrRemovedEvent(FieldChange fieldChange, EntriesEventSource location) {
        super(fieldChange, location);
    }
}
