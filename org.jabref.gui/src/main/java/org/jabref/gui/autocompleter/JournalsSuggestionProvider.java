package org.jabref.gui.autocompleter;

import java.util.List;
import java.util.stream.Collectors;

import org.jabref.logic.journals.Abbreviation;
import org.jabref.logic.journals.JournalAbbreviationLoader;
import org.jabref.logic.journals.JournalAbbreviationPreferences;

public class JournalsSuggestionProvider extends FieldValueSuggestionProvider {


    JournalsSuggestionProvider(String fieldName, AutoCompletePreferences preferences,
                               JournalAbbreviationLoader abbreviationLoader) {
        super(fieldName);

        JournalAbbreviationPreferences journalAbbreviationPreferences = preferences.getJournalAbbreviationPreferences();
        List<String> journals = abbreviationLoader.getRepository(journalAbbreviationPreferences)
                .getAbbreviations().stream()
                .map(Abbreviation::getName)
                .collect(Collectors.toList());
        addPossibleSuggestions(journals);
    }
}
