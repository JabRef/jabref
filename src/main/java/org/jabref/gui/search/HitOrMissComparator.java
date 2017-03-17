package org.jabref.gui.search;

import java.util.Comparator;
import java.util.Objects;

import org.jabref.model.entry.BibEntry;

import ca.odell.glazedlists.matchers.Matcher;

/**
 * This Comparator compares two objects based on whether none, one of them, or both
 * match a given Matcher. It is used to "float" group and search hits in the main table.
 */
public class HitOrMissComparator implements Comparator<BibEntry> {

    private final Matcher<BibEntry> hitOrMiss;

    public HitOrMissComparator(Matcher<BibEntry> hitOrMiss) {
        this.hitOrMiss = Objects.requireNonNull(hitOrMiss);
    }

    @Override
    public int compare(BibEntry o1, BibEntry o2) {
        if (hitOrMiss == null) {
            return 0;
        }

        return Boolean.compare(hitOrMiss.matches(o2), hitOrMiss.matches(o1));
    }
}
