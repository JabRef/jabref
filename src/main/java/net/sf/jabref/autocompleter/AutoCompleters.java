package net.sf.jabref.autocompleter;

import net.sf.jabref.BibtexDatabase;
import net.sf.jabref.BibtexEntry;

import java.util.HashMap;

class AutoCompleters {

    final HashMap<String, AutoCompleter> autoCompleters = new HashMap<String, AutoCompleter>();
    // Hashtable that holds as keys the names of the fields where
    // autocomplete is active, and references to the autocompleter objects.

    public AutoCompleter get(String fieldName) {
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
        for (AutoCompleter autoCompleter : autoCompleters.values()) {
            autoCompleter.addBibtexEntry(bibtexEntry);
        }
    }

    void put(String field, AutoCompleter autoCompleter) {
        autoCompleters.put(field, autoCompleter);
    }

}
