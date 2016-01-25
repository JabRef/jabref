package net.sf.jabref.logic.search.rules;

import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.logic.search.SearchRule;

import java.util.Objects;

/**
 * Inverts the search result.
 * <p>
 * Example:
 * false --> true
 * true --> false
 */
public class InvertSearchRule implements SearchRule {

    private final SearchRule otherRule;

    public InvertSearchRule(SearchRule otherRule) {
        this.otherRule = Objects.requireNonNull(otherRule);
    }

    @Override
    public boolean applyRule(String query, BibEntry bibEntry) {
        return !otherRule.applyRule(query, bibEntry);
    }

    @Override
    public boolean validateSearchStrings(String query) {
        return this.otherRule.validateSearchStrings(query);
    }
}
