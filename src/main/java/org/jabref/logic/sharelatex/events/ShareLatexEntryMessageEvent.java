package org.jabref.logic.sharelatex.events;

import java.util.ArrayList;
import java.util.List;

import org.jabref.model.entry.BibEntry;

public class ShareLatexEntryMessageEvent {

    private List<BibEntry> entries = new ArrayList<>();

    private final String database;
    private final int position;

    public ShareLatexEntryMessageEvent(List<BibEntry> entries, String database, int position) {
        this.entries = entries;
        this.database = database;
        this.position = position;
    }

    public List<BibEntry> getEntries() {
        return this.entries;
    }

    public String getNewDatabaseContent() {
        return this.database;
    }

    public int getPosition() {
        return this.position;
    }

}
