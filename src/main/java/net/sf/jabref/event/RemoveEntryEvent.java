package net.sf.jabref.event;

import net.sf.jabref.model.entry.BibEntry;

public class RemoveEntryEvent {

    private BibEntry bibEntry;

    public RemoveEntryEvent(BibEntry bibEntry) {
        setBibEntry(bibEntry);
    }

    public void setBibEntry(BibEntry bibEntry) {
        this.bibEntry = bibEntry;
    }

    public BibEntry getBibEntry() {
        return this.bibEntry;
    }

}
