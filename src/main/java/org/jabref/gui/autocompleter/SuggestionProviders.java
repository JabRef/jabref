package org.jabref.gui.autocompleter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.jabref.logic.journals.JournalAbbreviationLoader;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;
import org.jabref.model.entry.FieldProperty;
import org.jabref.model.entry.InternalBibtexFields;

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

        List<String> completeFields = preferences.getCompleteFields();
        for (String field : completeFields) {
            AutoCompleteSuggestionProvider<?> autoCompleter = initalizeSuggestionProvider(field, preferences, abbreviationLoader);
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

    private AutoCompleteSuggestionProvider<?> initalizeSuggestionProvider(String fieldName, AutoCompletePreferences preferences, JournalAbbreviationLoader abbreviationLoader) {
        if (InternalBibtexFields.getFieldProperties(fieldName).contains(FieldProperty.PERSON_NAMES)) {
            return new PersonNameSuggestionProvider(fieldName);
        } else if (InternalBibtexFields.getFieldProperties(fieldName).contains(FieldProperty.SINGLE_ENTRY_LINK)) {
            return new BibEntrySuggestionProvider();
        } else if (InternalBibtexFields.getFieldProperties(fieldName).contains(FieldProperty.JOURNAL_NAME)
                || FieldName.PUBLISHER.equals(fieldName)) {
            return new JournalsSuggestionProvider(fieldName, preferences, abbreviationLoader);
        } else {
            return new WordSuggestionProvider(fieldName);
        }
    }
}
