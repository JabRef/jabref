package org.jabref.gui.util.comparator;

import java.util.Comparator;

import org.jabref.logic.TypedBibEntry;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

public class FirstColumnComparator implements Comparator<BibEntry> {

    private final BibDatabaseContext database;

    public FirstColumnComparator(BibDatabaseContext database) {
        this.database = database;
    }

    @Override
    public int compare(BibEntry e1, BibEntry e2) {
        int score1 = 0;
        int score2 = 0;

        TypedBibEntry typedEntry1 = new TypedBibEntry(e1, database);
        TypedBibEntry typedEntry2 = new TypedBibEntry(e2, database);
        if (typedEntry1.hasAllRequiredFields()) {
            score1++;
        }

        if (typedEntry2.hasAllRequiredFields()) {
            score2++;
        }

        return score1 - score2;
    }

}
