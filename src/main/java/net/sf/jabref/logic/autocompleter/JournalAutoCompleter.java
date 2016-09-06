package net.sf.jabref.logic.autocompleter;

import java.util.List;
import java.util.Objects;

import net.sf.jabref.logic.journals.Abbreviation;
import net.sf.jabref.logic.journals.JournalAbbreviationLoader;
import net.sf.jabref.logic.journals.JournalAbbreviationPreferences;

public class JournalAutoCompleter extends EntireFieldAutoCompleter {

    private final JournalAbbreviationLoader abbreviationLoader;
    private final JournalAbbreviationPreferences journalAbbreviationPreferences;


    JournalAutoCompleter(String fieldName, AutoCompletePreferences preferences,
            JournalAbbreviationLoader abbreviationLoader) {
        super(fieldName, preferences);
        this.abbreviationLoader = Objects.requireNonNull(abbreviationLoader);
        this.journalAbbreviationPreferences = preferences.getJournalAbbreviationPreferences();
    }

    @Override
    public List<String> complete(String toComplete) {
        List<String> completions = super.complete(toComplete);

        // Also return journal names in the journal abbreviation list
        for (Abbreviation abbreviation : abbreviationLoader
                .getRepository(journalAbbreviationPreferences).getAbbreviations()) {
            if (abbreviation.getName().startsWith(toComplete)) {
                completions.add(abbreviation.getName());
            }
        }

        return completions;
    }
}
