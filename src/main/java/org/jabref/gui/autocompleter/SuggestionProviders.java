package org.jabref.gui.autocompleter;

import java.util.Objects;
import java.util.Set;

import org.jabref.logic.journals.JournalAbbreviationLoader;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldProperty;
import org.jabref.model.entry.field.StandardField;

public class SuggestionProviders {

    private final boolean isEmpty;
    private BibDatabase database;
    private AutoCompletePreferences preferences;
    private JournalAbbreviationLoader abbreviationLoader;

    public SuggestionProviders(BibDatabase database, AutoCompletePreferences preferences, JournalAbbreviationLoader abbreviationLoader) {
        this.database = database;
        this.preferences = Objects.requireNonNull(preferences);
        this.abbreviationLoader = abbreviationLoader;
        this.isEmpty = false;
    }

    public SuggestionProviders() {
        this.isEmpty = true;
    }

    public SuggestionProvider<?> getForField(Field field) {
        if (isEmpty) {
            return new EmptySuggestionProvider();
        }

        Set<FieldProperty> fieldProperties = field.getProperties();
        if (fieldProperties.contains(FieldProperty.PERSON_NAMES)) {
            return new PersonNameSuggestionProvider(field, database);
        } else if (fieldProperties.contains(FieldProperty.SINGLE_ENTRY_LINK)) {
            return new BibEntrySuggestionProvider(database);
        } else if (fieldProperties.contains(FieldProperty.JOURNAL_NAME)
                || StandardField.PUBLISHER.equals(field)) {
            return new JournalsSuggestionProvider(field, database, preferences, abbreviationLoader);
        } else {
            return new WordSuggestionProvider(field, database);
        }
    }
}
