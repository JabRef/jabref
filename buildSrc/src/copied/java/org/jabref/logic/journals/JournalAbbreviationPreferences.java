package org.jabref.logic.journals;

import java.nio.charset.Charset;
import java.util.List;

public class JournalAbbreviationPreferences {

    private final Charset defaultEncoding;
    private List<String> externalJournalLists;

    public JournalAbbreviationPreferences(List<String> externalJournalLists, Charset defaultEncoding) {
        this.externalJournalLists = externalJournalLists;
        this.defaultEncoding = defaultEncoding;
    }

    public List<String> getExternalJournalLists() {
        return externalJournalLists;
    }

    public void setExternalJournalLists(List<String> externalJournalLists) {
        this.externalJournalLists = externalJournalLists;
    }

    public Charset getDefaultEncoding() {
        return defaultEncoding;
    }
}
