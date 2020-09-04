package org.jabref.logic.util;

import java.util.Optional;
import java.util.concurrent.ScheduledFuture;

import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.event.BibDatabaseContextChangedEvent;
import org.jabref.model.entry.event.FieldChangedEvent;
import org.jabref.model.entry.field.Field;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.microsoft.applicationinsights.internal.channel.TelemetrySerializer;

/**
 * Filters change events and only relays major changes.
 */
public class CoarseChangeFilter {

    private final BibDatabaseContext context;
    private final EventBus eventBus = new EventBus();

    private Optional<Field> lastFieldChanged;
    private int totalDelta;

    public CoarseChangeFilter(BibDatabaseContext bibDatabaseContext) {
        // Listen for change events
        this.context = bibDatabaseContext;
        context.getDatabase().registerListener(this);
        context.getMetaData().registerListener(this);
        this.lastFieldChanged = Optional.empty();
        this.totalDelta = 0;
    }

    @Subscribe
    public synchronized void listen(BibDatabaseContextChangedEvent event) {

        if (event instanceof FieldChangedEvent) {
            // Only relay event if the field changes are more than one character or a new field is edited
            FieldChangedEvent fieldChange = (FieldChangedEvent) event;
            // Sum up change delta
            totalDelta += fieldChange.getDelta();

            // If editing is started
            boolean isNewEdit = lastFieldChanged.isEmpty();
            // If other field is edited
            boolean isEditChanged = !isNewEdit && !lastFieldChanged.get().equals(fieldChange.getField());
            // Only deltas of 1 registered by fieldChange, major change means editing much content
            boolean isMajorChange = totalDelta >= 30;

            // Event is filtered out if neither the edited field has changed nor a major change has occurred
            fieldChange.setFilteredOut(!(isEditChanged || isMajorChange));
            // Post every FieldChangedEvent, but some have been marked (filtered)
            eventPost(fieldChange);
            // Set new last field
            lastFieldChanged = Optional.of(fieldChange.getField());

        }
        else {
            eventPost(event);
        }
    }

    private void eventPost(BibDatabaseContextChangedEvent event) {
        // Reset total change delta
        totalDelta = 0;
        // Reset last field that changed
        lastFieldChanged = Optional.empty();
        // Post event
        eventBus.post(event);
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
