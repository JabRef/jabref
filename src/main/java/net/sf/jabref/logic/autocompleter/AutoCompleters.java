package net.sf.jabref.logic.autocompleter;

import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.entry.BibEntry;

import java.util.HashMap;
import java.util.Map;

class AutoCompleters {

    protected final Map<String, AutoCompleter<String>> autoCompleters = new HashMap<>();
    // Hashtable that holds as keys the names of the fields where
    // autocomplete is active, and references to the autocompleter objects.

    public AutoCompleter<String> get(String fieldName) {
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
        for (AutoCompleter<String> autoCompleter : autoCompleters.values()) {
            autoCompleter.addBibtexEntry(bibEntry);
        }
    }

    protected void put(String field, AutoCompleter<String> autoCompleter) {
        autoCompleters.put(field, autoCompleter);
    }

}
