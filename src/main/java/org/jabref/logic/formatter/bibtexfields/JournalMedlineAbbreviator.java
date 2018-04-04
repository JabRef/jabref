package org.jabref.logic.formatter.bibtexfields;

import java.util.Objects;

import org.jabref.logic.journals.JournalAbbreviationLoader;
import org.jabref.logic.journals.JournalAbbreviationPreferences;
import org.jabref.model.cleanup.Formatter;

public class JournalMedlineAbbreviator implements Formatter {

    private final JournalAbbreviationLoader repostioryLoader;
    private final JournalAbbreviationPreferences journalAbbreviationPreferences;

    //TODO: How do I pass the prefs at best?
    public JournalMedlineAbbreviator(JournalAbbreviationLoader repostioryLoader,
            JournalAbbreviationPreferences journalAbbreviationPreferences) {
        this.repostioryLoader = Objects.requireNonNull(repostioryLoader);
        this.journalAbbreviationPreferences = Objects.requireNonNull(journalAbbreviationPreferences);
    }

    @Override
    public String getName() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getKey() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String format(String fieldText) {
        return repostioryLoader.getRepository(journalAbbreviationPreferences)
                .getIsoAbbreviation(fieldText)
                .orElse(fieldText);
    }

    @Override
    public String getDescription() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getExampleInput() {
        // TODO Auto-generated method stub
        return null;
    }

}
