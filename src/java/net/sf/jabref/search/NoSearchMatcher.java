package net.sf.jabref.search;

import ca.odell.glazedlists.matchers.Matcher;

/**
 * Matcher that accepts all entries. Used for filtering when so search is active.
 */
public class NoSearchMatcher implements Matcher {
    public static final Matcher INSTANCE = new NoSearchMatcher();

    private NoSearchMatcher() {
        
    }

    public boolean matches(Object object) {
        return true;
    }
}
