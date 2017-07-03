package org.jabref.logic.autocompleter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.jabref.logic.journals.JournalAbbreviationLoader;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;

public class SuggestionProviders {

    /**
     * key: field name
     */
    private final Map<String, AutoCompleteSuggestionProvider<?>> providers = new HashMap<>();

    /**
     * Empty
     */
    public SuggestionProviders() {

    }

    public SuggestionProviders(AutoCompletePreferences preferences,
                               JournalAbbreviationLoader abbreviationLoader) {
        Objects.requireNonNull(preferences);

        AutoCompleterFactory autoCompleterFactory = new AutoCompleterFactory(preferences, abbreviationLoader);
        List<String> completeFields = preferences.getCompleteNames();
        for (String field : completeFields) {
            AutoCompleteSuggestionProvider<?> autoCompleter = autoCompleterFactory.getForField(field);
            providers.put(field, autoCompleter);
        }
    }

    public AutoCompleteSuggestionProvider<?> getForField(String fieldName) {
        return providers.get(fieldName);
    }

    public void indexDatabase(BibDatabase database) {
        for (BibEntry entry : database.getEntries()) {
            indexEntry(entry);
        }
    }

    /**
     * This methods assures all information in the given entry is included as suggestions.
     */
    public void indexEntry(BibEntry bibEntry) {
        for (AutoCompleteSuggestionProvider<?> autoCompleter : providers.values()) {
            autoCompleter.indexEntry(bibEntry);
        }
    }
}
