package org.jabref.logic.util;

import java.util.Optional;

import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.event.BibDatabaseContextChangedEvent;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.event.FieldChangedEvent;
import org.jabref.model.entry.field.Field;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

/**
 * Filters change events and only relays major changes.
 */
public class CoarseChangeFilter {

    private final BibDatabaseContext context;
    private final EventBus eventBus = new EventBus();

    private Optional<Field> lastFieldChanged;
    private Optional<BibEntry> lastEntryChanged;
    private int totalDelta;

    public CoarseChangeFilter(BibDatabaseContext bibDatabaseContext) {
        // Listen for change events
        this.context = bibDatabaseContext;
        context.getDatabase().registerListener(this);
        context.getMetaData().registerListener(this);
        this.lastFieldChanged = Optional.empty();
        this.lastEntryChanged = Optional.empty();
    }

    @Subscribe
    public synchronized void listen(BibDatabaseContextChangedEvent event) {

        if (event instanceof FieldChangedEvent) {
            // Only relay event if the field changes are more than one character or a new field is edited
            FieldChangedEvent fieldChange = (FieldChangedEvent) event;

            // If editing has started
            boolean isNewEdit = lastFieldChanged.isEmpty() || lastEntryChanged.isEmpty();

            boolean isChangedField = lastFieldChanged.filter(f -> !f.equals(fieldChange.getField())).isPresent();
            boolean isChangedEntry = lastEntryChanged.filter(e -> !e.equals(fieldChange.getBibEntry())).isPresent();
            boolean isEditChanged = !isNewEdit && (isChangedField || isChangedEntry);
            // Only deltas of 1 when typing in manually, major change means pasting something (more than one character)
            boolean isMajorChange = fieldChange.getMajorCharacterChange() > 1;

            fieldChange.setFilteredOut(!(isEditChanged || isMajorChange));
            // Post each FieldChangedEvent - even the ones being marked as "filtered"
            eventBus.post(fieldChange);

            lastFieldChanged = Optional.of(fieldChange.getField());
            lastEntryChanged = Optional.of(fieldChange.getBibEntry());
        } else {
            eventBus.post(event);
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
