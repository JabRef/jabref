package org.jabref.gui.autocompleter;

import java.util.Set;

import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldProperty;
import org.jabref.model.entry.field.StandardField;

public class SuggestionProviders {

    private final boolean isEmpty;
    private BibDatabase database;
    private JournalAbbreviationRepository abbreviationRepository;
    private AutoCompletePreferences autoCompletePreferences;

    public SuggestionProviders(BibDatabase database, JournalAbbreviationRepository abbreviationRepository, AutoCompletePreferences autoCompletePreferences) {
        this.database = database;
        this.abbreviationRepository = abbreviationRepository;
        this.autoCompletePreferences = autoCompletePreferences;
        this.isEmpty = false;
    }

    public SuggestionProviders() {
        this.isEmpty = true;
    }

    public SuggestionProvider<?> getForField(Field field) {
        if (isEmpty || !autoCompletePreferences.getCompleteFields().contains(field)) {
            return new EmptySuggestionProvider();
        }

        Set<FieldProperty> fieldProperties = field.getProperties();
        if (fieldProperties.contains(FieldProperty.PERSON_NAMES)) {
            return new PersonNameSuggestionProvider(field, database);
        } else if (fieldProperties.contains(FieldProperty.SINGLE_ENTRY_LINK) || fieldProperties.contains(FieldProperty.MULTIPLE_ENTRY_LINK)) {
            return new BibEntrySuggestionProvider(database);
        } else if (fieldProperties.contains(FieldProperty.JOURNAL_NAME) || StandardField.PUBLISHER.equals(field)) {
            return new JournalsSuggestionProvider(field, database, abbreviationRepository);
        } else {
            return new WordSuggestionProvider(field, database);
        }
    }
}
