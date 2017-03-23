package org.jabref.logic.journals;

import java.nio.charset.Charset;
import java.util.List;

import org.jabref.preferences.JabRefPreferences;

public class JournalAbbreviationPreferences {

    private final List<String> externalJournalLists;
    private final String personalJournalLists;
    private final boolean useIEEEAbbreviations;
    private final Charset defaultEncoding;


    public JournalAbbreviationPreferences(List<String> externalJournalLists, String personalJournalLists,
            boolean useIEEEAbbreviations, Charset defaultEncoding) {
        this.externalJournalLists = externalJournalLists;
        this.personalJournalLists = personalJournalLists;
        this.useIEEEAbbreviations = useIEEEAbbreviations;
        this.defaultEncoding = defaultEncoding;
    }

    public static JournalAbbreviationPreferences fromPreferences(JabRefPreferences jabRefPreferences) {
        return new JournalAbbreviationPreferences(
                jabRefPreferences.getStringList(JabRefPreferences.EXTERNAL_JOURNAL_LISTS),
                jabRefPreferences.get(JabRefPreferences.PERSONAL_JOURNAL_LIST),
                jabRefPreferences.getBoolean(JabRefPreferences.USE_IEEE_ABRV), jabRefPreferences.getDefaultEncoding());
    }

    public List<String> getExternalJournalLists() {
        return externalJournalLists;
    }

    public String getPersonalJournalLists() {
        return personalJournalLists;
    }

    public boolean isUseIEEEAbbreviations() {
        return useIEEEAbbreviations;
    }

    public Charset getDefaultEncoding() {
        return defaultEncoding;
    }
}
