package net.sf.jabref.event;

import net.sf.jabref.model.entry.BibEntry;

import com.google.common.eventbus.Subscribe;

public class TestHierarchicalEventListener {

    private BibEntry bibEntry;
    private BibEntry parentBibEntry;


    @Subscribe
    public void listen(AddOrChangeEntryEvent aocee) {
        this.parentBibEntry = (BibEntry) aocee.getObject();
    }

    @Subscribe
    public void listen(AddEntryEvent aee) {
        this.bibEntry = (BibEntry) aee.getObject();
    }

    @Subscribe
    public void listen(ChangeEntryEvent cee) {
        this.bibEntry = (BibEntry) cee.getObject();
    }

    public BibEntry getBibEntry() {
        return this.bibEntry;
    }

    public BibEntry getParentBibEntry() {
        return this.parentBibEntry;
    }

}
