package net.sf.jabref.model.database;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import net.sf.jabref.model.entry.BibEntry;

public class EntrySorter {

    private final List<BibEntry> entries;

    public EntrySorter(List<BibEntry> entries, Comparator<BibEntry> comparator) {
        this.entries = new ArrayList<>(entries);
        Collections.sort(this.entries, comparator);
    }

    public BibEntry getEntryAt(int pos) {
        return entries.get(pos);
    }

    public int getEntryCount() {
        return entries.size();
    }

}
