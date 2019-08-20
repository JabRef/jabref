package org.jabref.gui.autocompleter;

import java.util.List;
import java.util.stream.Collectors;

import org.jabref.logic.journals.Abbreviation;
import org.jabref.logic.journals.JournalAbbreviationLoader;
import org.jabref.logic.journals.JournalAbbreviationPreferences;
import org.jabref.model.entry.field.Field;

public class JournalsSuggestionProvider extends FieldValueSuggestionProvider {

    JournalsSuggestionProvider(Field field, AutoCompletePreferences preferences,
                               JournalAbbreviationLoader abbreviationLoader) {
        super(field);

        JournalAbbreviationPreferences journalAbbreviationPreferences = preferences.getJournalAbbreviationPreferences();
        List<String> journals = abbreviationLoader.getRepository(journalAbbreviationPreferences)
                .getAbbreviations().stream()
                .map(Abbreviation::getName)
                .collect(Collectors.toList());
        addPossibleSuggestions(journals);
    }
}
