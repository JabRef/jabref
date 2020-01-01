package org.jabref.gui.autocompleter;

import java.util.List;

import org.jabref.model.database.event.EntriesAddedEvent;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.event.EntryChangedEvent;

import com.google.common.eventbus.Subscribe;

/**
 * Ensures that suggestion providers are up to date when entries are changed or added.
 */
public class AutoCompleteUpdater {

    private final SuggestionProviders suggestionProviders;

    public AutoCompleteUpdater(SuggestionProviders suggestionProviders) {
        this.suggestionProviders = suggestionProviders;
    }

    @Subscribe
    public void listen(EntriesAddedEvent entryAddedEvent) {
        List<BibEntry> entries = entryAddedEvent.getBibEntries();
        for (BibEntry entry : entries) {
            suggestionProviders.indexEntry(entry);
        }
    }

    @Subscribe
    public void listen(EntryChangedEvent entryChangedEvent) {
        suggestionProviders.indexEntry(entryChangedEvent.getBibEntry());
    }
}
