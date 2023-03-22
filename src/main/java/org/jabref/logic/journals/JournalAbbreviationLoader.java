package org.jabref.logic.journals;

public class JournalAbbreviationLoader extends AbbreviationLoader {

    public JournalAbbreviationLoader() {
        super("journal-list.mv");
    }

    public AbbreviationRepository loadBuiltInRepository(JournalAbbreviationPreferences journalAbbreviationPreferences) {
        return loadRepository(journalAbbreviationPreferences);
    }
}
