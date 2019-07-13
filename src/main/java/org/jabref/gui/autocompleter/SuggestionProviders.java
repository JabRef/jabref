package org.jabref.gui.autocompleter;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.jabref.logic.journals.JournalAbbreviationLoader;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldProperty;
import org.jabref.model.entry.field.StandardField;

public class SuggestionProviders {

    /**
     * key: field name
     */
    private final Map<Field, AutoCompleteSuggestionProvider<?>> providers = new HashMap<>();

    /**
     * Empty
     */
    public SuggestionProviders() {

    }

    public SuggestionProviders(AutoCompletePreferences preferences,
            JournalAbbreviationLoader abbreviationLoader) {
        Objects.requireNonNull(preferences);

        Set<Field> completeFields = preferences.getCompleteFields();
        for (Field field : completeFields) {
            AutoCompleteSuggestionProvider<?> autoCompleter = initalizeSuggestionProvider(field, preferences, abbreviationLoader);
            providers.put(field, autoCompleter);
        }
    }

    public AutoCompleteSuggestionProvider<?> getForField(Field field) {
        return providers.get(field);
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

    private AutoCompleteSuggestionProvider<?> initalizeSuggestionProvider(Field field, AutoCompletePreferences preferences, JournalAbbreviationLoader abbreviationLoader) {
        Set<FieldProperty> fieldProperties = field.getProperties();
        if (fieldProperties.contains(FieldProperty.PERSON_NAMES)) {
            return new PersonNameSuggestionProvider(field);
        } else if (fieldProperties.contains(FieldProperty.SINGLE_ENTRY_LINK)) {
            return new BibEntrySuggestionProvider();
        } else if (fieldProperties.contains(FieldProperty.JOURNAL_NAME)
                || StandardField.PUBLISHER.equals(field)) {
            return new JournalsSuggestionProvider(field, preferences, abbreviationLoader);
        } else {
            return new WordSuggestionProvider(field);
        }
    }
}
