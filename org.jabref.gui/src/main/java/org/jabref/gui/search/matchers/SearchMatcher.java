package org.jabref.gui.search.matchers;

import org.jabref.model.entry.BibEntry;

import ca.odell.glazedlists.matchers.Matcher;

/**
 * Matcher for filtering or sorting the table according to whether entries are
 * tagged as search matches.
 */
public class SearchMatcher implements Matcher<BibEntry> {

    public static final Matcher<BibEntry> INSTANCE = new SearchMatcher();

    @Override
    public boolean matches(BibEntry entry) {
        return entry.isSearchHit();
    }
}
