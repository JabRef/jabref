package org.jabref.gui.search.matchers;

import org.jabref.model.entry.BibEntry;

import ca.odell.glazedlists.matchers.Matcher;

/**
 * Matcher that accepts all entries. Used for filtering when so search is
 * active.
 */
public class EverythingMatcher implements Matcher<BibEntry> {

    public static final Matcher<BibEntry> INSTANCE = new EverythingMatcher();

    @Override
    public boolean matches(BibEntry object) {
        return true;
    }
}
