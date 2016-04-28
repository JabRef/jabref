package net.sf.jabref.event;

import net.sf.jabref.model.entry.BibEntry;

public class ChangeSpecialFieldEvent {

    private final BibEntry bibEntry;
    private final String fieldName;

    public ChangeSpecialFieldEvent(BibEntry bibEntry, String fieldName) {
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
