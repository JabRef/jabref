package org.jabref.gui.autocompleter;

import org.jabref.logic.journals.JournalAbbreviationLoader;
import org.jabref.model.entry.field.Field;

public class JournalsSuggestionProvider extends FieldValueSuggestionProvider {

    JournalsSuggestionProvider(Field field, AutoCompletePreferences preferences,
                               JournalAbbreviationLoader abbreviationLoader) {
        super(field);

        // TODO: Reimplement
        /*
        JournalAbbreviationPreferences journalAbbreviationPreferences = preferences.getJournalAbbreviationPreferences();
        List<String> journals = abbreviationLoader.getRepository(journalAbbreviationPreferences)
                .getAbbreviations().stream()
                .map(Abbreviation::getName)
                .collect(Collectors.toList());
        addPossibleSuggestions(journals);
         */
    }
}
