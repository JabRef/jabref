package org.jabref.logic.journals;

public class ConferenceAbbreviationLoader extends AbbreviationLoader {

    public ConferenceAbbreviationLoader() {
        super("conference-list.mv");
    }

    public AbbreviationRepository loadBuiltInRepository(ConferenceAbbreviationPreferences conferenceAbbreviationPreferences) {
        return loadRepository(conferenceAbbreviationPreferences);
    }
}
