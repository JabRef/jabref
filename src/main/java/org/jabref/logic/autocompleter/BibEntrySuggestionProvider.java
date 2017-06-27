package org.jabref.logic.autocompleter;

import java.util.Comparator;

import org.jabref.logic.bibtex.comparator.EntryComparator;
import org.jabref.model.entry.BibEntry;

import impl.org.controlsfx.autocompletion.SuggestionProvider;
import org.controlsfx.control.textfield.AutoCompletionBinding;

/**
 * Delivers possible completions as a list of {@link BibEntry} based on their cite key.
 */
class BibEntrySuggestionProvider extends SuggestionProvider<BibEntry> implements AutoCompleteSuggestionProvider<BibEntry> {

    @Override
    public void indexBibtexEntry(BibEntry entry) {
        if (entry == null) {
            return;
        }

        addPossibleSuggestions(entry);
    }

    @Override
    protected Comparator<BibEntry> getComparator() {
        return new EntryComparator(false, true, BibEntry.KEY_FIELD);
    }

    @Override
    protected boolean isMatch(BibEntry suggestion, AutoCompletionBinding.ISuggestionRequest request) {
        String userTextLower = request.getUserText().toLowerCase();
        return suggestion.getCiteKeyOptional()
                .map(key -> key.toLowerCase().contains(userTextLower))
                .orElse(false);
    }
}
