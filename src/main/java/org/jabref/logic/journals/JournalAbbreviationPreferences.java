package org.jabref.logic.journals;

import java.nio.charset.Charset;
import java.util.List;

public class JournalAbbreviationPreferences {

    private final String personalJournalLists;
    private final Charset defaultEncoding;
    private List<String> externalJournalLists;
    private boolean useIEEEAbbreviations;
    public JournalAbbreviationPreferences(List<String> externalJournalLists, String personalJournalLists,
            boolean useIEEEAbbreviations, Charset defaultEncoding) {
        this.externalJournalLists = externalJournalLists;
        this.personalJournalLists = personalJournalLists;
        this.useIEEEAbbreviations = useIEEEAbbreviations;
        this.defaultEncoding = defaultEncoding;
    }

    public void setUseIEEEAbbreviations(boolean useIEEEAbbreviations) {
        this.useIEEEAbbreviations = useIEEEAbbreviations;
    }

    public List<String> getExternalJournalLists() {
        return externalJournalLists;
    }

    public void setExternalJournalLists(List<String> externalJournalLists) {
        this.externalJournalLists = externalJournalLists;
    }

    public String getPersonalJournalLists() {
        return personalJournalLists;
    }

    public boolean useIEEEAbbreviations() {
        return useIEEEAbbreviations;
    }

    public Charset getDefaultEncoding() {
        return defaultEncoding;
    }
}
