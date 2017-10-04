package org.jabref.model.database.event;

import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.event.FieldChangedEvent;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

/**
 * Filters change events and only relays major changes.
 */
public class CoarseChangeFilter {

    private final EventBus eventBus = new EventBus();
    private String lastFieldChanged;
    private final BibDatabaseContext context;

    public CoarseChangeFilter(BibDatabaseContext bibDatabaseContext) {
        // Listen for change events
        bibDatabaseContext.getDatabase().registerListener(this);
        bibDatabaseContext.getMetaData().registerListener(this);
        this.context = bibDatabaseContext;
    }

    @Subscribe
    public synchronized void listen(@SuppressWarnings("unused") BibDatabaseContextChangedEvent event) {
        if (!(event instanceof FieldChangedEvent)) {
            eventBus.post(event);
        } else {
            // Only relay event if the field changes are more than one character or a new field is edited
            FieldChangedEvent fieldChange = (FieldChangedEvent) event;
            boolean isEditOnNewField = lastFieldChanged == null || !lastFieldChanged.equals(fieldChange.getFieldName());

            if (fieldChange.getDelta() > 1 || isEditOnNewField) {
                lastFieldChanged = fieldChange.getFieldName();
                eventBus.post(event);
            }
        }
    }

    public void registerListener(Object listener) {
        eventBus.register(listener);
    }

    public void unregisterListener(Object listener) {
        eventBus.unregister(listener);
    }

    public void shutdown() {
        context.getDatabase().unregisterListener(this);
        context.getMetaData().unregisterListener(this);
    }
}
