package net.sf.jabref.logic.autocompleter;

import net.sf.jabref.model.database.BibtexDatabase;
import net.sf.jabref.model.entry.BibtexEntry;

import java.util.HashMap;

class AutoCompleters {

    final HashMap<String, AutoCompleter<String>> autoCompleters = new HashMap<>();
    // Hashtable that holds as keys the names of the fields where
    // autocomplete is active, and references to the autocompleter objects.

    public AutoCompleter<String> get(String fieldName) {
        return autoCompleters.get(fieldName);
    }

    void addDatabase(BibtexDatabase database) {
        for (BibtexEntry entry : database.getEntries()) {
            addEntry(entry);
        }
    }

    /**
     * This methods assures all words in the given entry are recorded in their
     * respective Completers, if any.
     */
    public void addEntry(BibtexEntry bibtexEntry) {
        for (AutoCompleter<String> autoCompleter : autoCompleters.values()) {
            autoCompleter.addBibtexEntry(bibtexEntry);
        }
    }

    void put(String field, AutoCompleter<String> autoCompleter) {
        autoCompleters.put(field, autoCompleter);
    }

}
