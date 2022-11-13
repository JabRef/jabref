package org.jabref.logic.journals;

import java.nio.charset.Charset;
import java.util.List;

public class JournalAbbreviationPreferences {

    private final Charset defaultEncoding;
    private List<String> externalJournalLists;
    private boolean useFJournalField;

    public JournalAbbreviationPreferences(List<String> externalJournalLists, Charset defaultEncoding, boolean useFJournalField) {
        this.externalJournalLists = externalJournalLists;
        this.defaultEncoding = defaultEncoding;
        this.useFJournalField = useFJournalField;
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

    public boolean useAMSFJournalFieldForAbbrevAndUnabbrev() {
        return useFJournalField;
    }

    public void setUseAMSFJournalFieldForAbbrevAndUnabbrev(boolean useFJournalField) {
        this.useFJournalField = useFJournalField;
    }
}
