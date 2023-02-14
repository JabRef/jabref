package org.jabref.logic.journals;

import java.util.List;

public class JournalAbbreviationPreferences {

    private List<String> externalJournalLists;
    private boolean useFJournalField;

    public JournalAbbreviationPreferences(List<String> externalJournalLists, boolean useFJournalField) {
        this.externalJournalLists = externalJournalLists;
        this.useFJournalField = useFJournalField;
    }

    public List<String> getExternalJournalLists() {
        return externalJournalLists;
    }

    public void setExternalJournalLists(List<String> externalJournalLists) {
        this.externalJournalLists = externalJournalLists;
    }

    public boolean useAMSFJournalFieldForAbbrevAndUnabbrev() {
        return useFJournalField;
    }

    public void setUseAMSFJournalFieldForAbbrevAndUnabbrev(boolean useFJournalField) {
        this.useFJournalField = useFJournalField;
    }
}
