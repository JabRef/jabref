package net.sf.jabref.event;

import net.sf.jabref.model.entry.BibEntry;

public class AddEntryEvent {

    private BibEntry bibEntry;

    public AddEntryEvent(BibEntry bibEntry) {
        setBibEntry(bibEntry);
    }

    public void setBibEntry(BibEntry bibEntry) {
        this.bibEntry = bibEntry;
    }

    public BibEntry getBibEntry() {
        return this.bibEntry;
    }

}
