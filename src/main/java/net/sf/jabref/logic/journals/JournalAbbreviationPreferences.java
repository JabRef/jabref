package net.sf.jabref.logic.journals;

import java.nio.charset.Charset;
import java.util.List;

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
