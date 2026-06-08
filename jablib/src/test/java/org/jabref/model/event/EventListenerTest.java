package org.jabref.model.event;

import java.util.List;
import java.util.Optional;

import org.jabref.model.database.event.EntriesAddedEvent;
import org.jabref.model.database.event.EntriesRemovedEvent;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.event.EntryChangedEvent;

import com.google.common.eventbus.Subscribe;
import org.jspecify.annotations.Nullable;

public class EventListenerTest {

    private List<BibEntry> addedEntries;
    @Nullable private BibEntry firstInsertedEntry;
    private List<BibEntry> removedEntries;
    private BibEntry changedEntry;

    @Subscribe
    public void listen(EntriesAddedEvent event) {
        this.addedEntries = event.getBibEntries();
        this.firstInsertedEntry = event.getFirstEntry().orElse(null);
    }

    @Subscribe
    public void listen(EntriesRemovedEvent event) {
        this.removedEntries = event.getBibEntries();
    }

    @Subscribe
    public void listen(EntryChangedEvent event) {
        this.changedEntry = event.getBibEntry();
    }

    public List<BibEntry> getAddedEntries() {
        return addedEntries;
    }

    public Optional<BibEntry> getFirstInsertedEntry() {
        return Optional.ofNullable(firstInsertedEntry);
    }

    public List<BibEntry> getRemovedEntries() {
        return removedEntries;
    }

    public BibEntry getChangedEntry() {
        return changedEntry;
    }
}
