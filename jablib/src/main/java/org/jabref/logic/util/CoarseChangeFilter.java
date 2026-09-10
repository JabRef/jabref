package org.jabref.logic.util;

import java.util.Optional;

import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.event.BibDatabaseContextChangedEvent;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.event.FieldChangedEvent;
import org.jabref.model.entry.field.Field;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Filters change events and only relays major changes.
///
/// This asks the same question as [org.jabref.logic.undo.CoalescingPolicy] — is this change the
/// continuation of a run of typing? — for a different consumer, and the two answers are not
/// interchangeable. The policy decides how much one Ctrl+Z takes back, where being wrong costs a
/// keystroke either way. Here a change marked filtered arms no backup and is held back from the
/// shared database until the next major change (see [org.jabref.logic.shared.DBMSSynchronizer]),
/// so filtering too eagerly delays what other clients see, while filtering too little only writes
/// a backup more often. That is why this stays with the conservative rule below — a change to
/// another field or entry, or more than one character at once, is major — instead of the policy's
/// contiguity and word rules.
///
/// The `charactersChangedCount` test is a guess at what the caller now says outright: a keystroke
/// records as [org.jabref.logic.undo.EditSource#TYPING], everything else as a command. Should the
/// two granularities ever have to agree, that source is the thing to carry into the event, rather
/// than either side re-deriving it.
public class CoarseChangeFilter {
    private static final Logger LOGGER = LoggerFactory.getLogger(CoarseChangeFilter.class);

    private final BibDatabaseContext context;
    private final EventBus eventBus = new EventBus();

    private Optional<Field> lastFieldChanged;
    private Optional<BibEntry> lastEntryChanged;

    public CoarseChangeFilter(BibDatabaseContext bibDatabaseContext) {
        this.context = bibDatabaseContext;

        // Listen for change events
        context.getDatabase().registerListener(this);
        context.getMetaData().registerListener(this);

        this.lastFieldChanged = Optional.empty();
        this.lastEntryChanged = Optional.empty();
    }

    @Subscribe
    public synchronized void listen(BibDatabaseContextChangedEvent event) {
        if (event instanceof FieldChangedEvent fieldChange) {
            // If editing has started
            boolean isNewEdit = lastFieldChanged.isEmpty() || lastEntryChanged.isEmpty();

            boolean isChangedField = lastFieldChanged.filter(f -> !f.equals(fieldChange.getField())).isPresent();
            boolean isChangedEntry = lastEntryChanged.filter(e -> !e.equals(fieldChange.getBibEntry())).isPresent();
            boolean isEditChanged = !isNewEdit && (isChangedField || isChangedEntry);
            // Only deltas of 1 when typing in manually, major change means pasting something (more than one character)
            boolean isMajorChange = fieldChange.charactersChangedCount() > 1;

            fieldChange.setFiltered(!(isEditChanged || isMajorChange));
            // Post each FieldChangedEvent - even the ones being marked as "filtered"
            // Explanation at https://github.com/JabRef/jabref/pull/6868. - especially necessary for BackupManager and AutoSaveManager
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
        try {
            eventBus.unregister(listener);
        } catch (IllegalArgumentException e) {
            LOGGER.debug("Listener was not registered before: {}", listener, e);
        }
    }

    public void shutdown() {
        context.getDatabase().unregisterListener(this);
        context.getMetaData().unregisterListener(this);
    }
}
