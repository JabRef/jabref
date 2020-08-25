package org.jabref.model.database.event;

import org.jabref.logic.util.DelayTaskThrottler;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.event.FieldChangedEvent;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.UnknownField;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

/**
 * Filters change events and only relays major changes.
 */
public class CoarseChangeFilter {

    private final BibDatabaseContext context;
    private final EventBus eventBus = new EventBus();
    private final DelayTaskThrottler delayPost;

    private Field lastFieldChanged;
    private int totalDelta;

    public CoarseChangeFilter(BibDatabaseContext bibDatabaseContext) {
        // Listen for change events
        bibDatabaseContext.getDatabase().registerListener(this);
        bibDatabaseContext.getMetaData().registerListener(this);
        this.context = bibDatabaseContext;
        // Delay event post by 5 seconds
        this.delayPost = new DelayTaskThrottler(5000);
        // 'null' throws NullPointerException
        this.lastFieldChanged = new UnknownField("unknownField");
        this.totalDelta = 0;
    }

    @Subscribe
    public synchronized void listen(@SuppressWarnings("unused") BibDatabaseContextChangedEvent event) {
        Runnable eventPost = () -> {
            // Reset total change delta
            totalDelta = 0;
            // Post event
            eventBus.post(event);
        };

        if (!(event instanceof FieldChangedEvent)) {
            eventPost.run();
        } else {
            // Only relay event if the field changes are more than one character or a new field is edited
            FieldChangedEvent fieldChange = (FieldChangedEvent) event;
            // Sum up change delta
            totalDelta += fieldChange.getDelta();

            // If editing is started
            boolean isNewEdit = lastFieldChanged.getName().equals("unknownField");
            // If other field is edited
            boolean isEditOnOtherField = !lastFieldChanged.equals(fieldChange.getField());
            // Only deltas of 1 registered by fieldChange, major change means editing much content
            boolean isMajorChange = totalDelta >= 100;

            if (isEditOnOtherField && !isNewEdit) {
                // Submit old changes immediately
                eventPost.run();
            } else {
                if (isNewEdit || isMajorChange) {
                    delayPost.schedule(eventPost);
                }
            }
            // Set new last field
            lastFieldChanged = fieldChange.getField();
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
