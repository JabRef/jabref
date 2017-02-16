package org.jabref.gui.util.comparator;

import java.util.Comparator;

import org.jabref.gui.EntryMarker;
import org.jabref.model.entry.BibEntry;

public class IsMarkedComparator implements Comparator<BibEntry> {

    public static Comparator<BibEntry> INSTANCE = new IsMarkedComparator();

    @Override
    public int compare(BibEntry e1, BibEntry e2) {
        return -EntryMarker.isMarked(e1) + EntryMarker.isMarked(e2);
    }

}
