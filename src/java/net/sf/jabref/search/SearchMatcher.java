package net.sf.jabref.search;

import net.sf.jabref.BibtexEntry;

import java.util.Hashtable;

import ca.odell.glazedlists.matchers.Matcher;

/**
 * Matcher for filtering or sorting the table according to whether entries
 * are tagged as search matches.
 */
public class SearchMatcher implements Matcher {

        public static SearchMatcher INSTANCE = new SearchMatcher();

        public boolean matches(Object object) {
            BibtexEntry entry = (BibtexEntry)object;
            return entry.isSearchHit();
        }
}
