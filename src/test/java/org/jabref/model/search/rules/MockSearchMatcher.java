package org.jabref.model.search.rules;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.search.SearchMatcher;

/**
 * Mock search rule that returns the values passed. Useful for testing.
 */
public class MockSearchMatcher implements SearchMatcher {

    private final boolean result;

    public MockSearchMatcher(boolean result) {
        this.result = result;
    }

    @Override
    public boolean isMatch(BibEntry entry) {
        return result;
    }
}
