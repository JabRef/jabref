package net.sf.jabref.event;

import net.sf.jabref.model.entry.BibEntry;

public class RemoveEntryEvent implements Event {

    private BibEntry bibEntry;


    public RemoveEntryEvent(Object object) {
        setObject(object);
    }

    @Override
    public Object getObject() {
        return bibEntry;
    }

    @Override
    public void setObject(Object object) {
        this.bibEntry = (BibEntry) object;
    }

    public BibEntry getEntry() {
        return bibEntry;
    }

}
