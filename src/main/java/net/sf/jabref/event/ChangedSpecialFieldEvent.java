package net.sf.jabref.event;

import net.sf.jabref.model.entry.BibEntry;

public class ChangedSpecialFieldEvent {

    private final BibEntry bibEntry;
    private final String fieldName;

    public ChangedSpecialFieldEvent(BibEntry bibEntry, String fieldName) {
        this.bibEntry = bibEntry;
        this.fieldName = fieldName;
    }

    public BibEntry getBibEntry() {
        return this.bibEntry;
    }

    public String getFieldName() {
        return this.fieldName;
    }
}
