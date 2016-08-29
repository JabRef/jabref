package net.sf.jabref.gui.util.comparator;

import java.util.Comparator;

import net.sf.jabref.gui.EntryMarker;
import net.sf.jabref.model.entry.BibEntry;

public class IsMarkedComparator implements Comparator<BibEntry> {

    public static Comparator<BibEntry> INSTANCE = new IsMarkedComparator();

    @Override
    public int compare(BibEntry e1, BibEntry e2) {
        return -EntryMarker.isMarked(e1) + EntryMarker.isMarked(e2);
    }

}
