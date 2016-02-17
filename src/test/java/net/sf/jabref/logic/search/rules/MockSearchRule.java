package net.sf.jabref.logic.search.rules;

import net.sf.jabref.logic.search.SearchMatcher;
import net.sf.jabref.model.entry.BibEntry;

/**
 * Mock search rule that returns the values passed. Useful for testing.
 */
public class MockSearchRule implements SearchMatcher {

    private final boolean result;

    public MockSearchRule(boolean result) {
        this.result = result;
    }

    @Override
    public boolean isMatch(BibEntry entry) {
        return result;
    }
}
