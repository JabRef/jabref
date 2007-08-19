package net.sf.jabref.search;

import net.sf.jabref.BibtexEntry;

import ca.odell.glazedlists.matchers.Matcher;

/**
 * Matcher that accepts all entries. Used for filtering when so search is
 * active.
 */
public class NoSearchMatcher implements Matcher<BibtexEntry> {
	public static final Matcher<BibtexEntry> INSTANCE = new NoSearchMatcher();

	public boolean matches(BibtexEntry object) {
		return true;
	}
}
