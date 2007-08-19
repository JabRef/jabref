package net.sf.jabref.search;

import net.sf.jabref.BibtexEntry;
import ca.odell.glazedlists.matchers.Matcher;

/**
 * Matcher for filtering or sorting the table according to whether entries are
 * tagged as search matches.
 */
public class SearchMatcher implements Matcher<BibtexEntry> {

	public static SearchMatcher INSTANCE = new SearchMatcher();

	public boolean matches(BibtexEntry entry) {
		return entry.isSearchHit();
	}
}
