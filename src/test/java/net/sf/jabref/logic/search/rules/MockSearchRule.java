package net.sf.jabref.logic.search.rules;

import net.sf.jabref.model.entry.BibtexEntry;
import net.sf.jabref.logic.search.SearchRule;

/**
 * Mock search rule that returns the values passed. Useful for testing.
 */
public class MockSearchRule implements SearchRule {

    private final boolean result;
    private final boolean valid;

    public MockSearchRule(boolean result, boolean valid) {
        this.result = result;
        this.valid = valid;
    }

    @Override
    public boolean applyRule(String query, BibtexEntry bibtexEntry) {
        return result;
    }

    @Override
    public boolean validateSearchStrings(String query) {
        return valid;
    }
}
