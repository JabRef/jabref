package org.jabref.logic.autocompleter;

import java.util.HashMap;
import java.util.Map;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;

class AutoCompleters {

    protected final Map<String, AutoCompleteSuggestionProvider<?>> autoCompleters = new HashMap<>();
    // Hashtable that holds as keys the names of the fields where
    // autocomplete is active, and references to the autocompleter objects.

    public AutoCompleteSuggestionProvider<?> get(String fieldName) {
        return autoCompleters.get(fieldName);
    }

    protected void addDatabase(BibDatabase database) {
        for (BibEntry entry : database.getEntries()) {
            addEntry(entry);
        }
    }

    /**
     * This methods assures all words in the given entry are recorded in their
     * respective Completers, if any.
     */
    public void addEntry(BibEntry bibEntry) {
        for (AutoCompleteSuggestionProvider<?> autoCompleter : autoCompleters.values()) {
            autoCompleter.indexBibtexEntry(bibEntry);
        }
    }

    protected void put(String field, AutoCompleteSuggestionProvider<?> autoCompleter) {
        autoCompleters.put(field, autoCompleter);
    }

}
