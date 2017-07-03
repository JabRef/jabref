package org.jabref.logic.autocompleter;

import org.jabref.model.database.event.EntryAddedEvent;
import org.jabref.model.entry.event.EntryChangedEvent;

import com.google.common.eventbus.Subscribe;

/**
 * Ensures that suggestion providers are up to date when entries are changed or added.
 */
public class AutoCompleteListener {

    private final SuggestionProviders suggestionProviders;

    public AutoCompleteListener(SuggestionProviders suggestionProviders) {
        this.suggestionProviders = suggestionProviders;
    }

    @Subscribe
    public void listen(EntryAddedEvent addedEntryEvent) {
        suggestionProviders.indexEntry(addedEntryEvent.getBibEntry());
    }

    @Subscribe
    public void listen(EntryChangedEvent entryChangedEvent) {
        suggestionProviders.indexEntry(entryChangedEvent.getBibEntry());
    }
}
