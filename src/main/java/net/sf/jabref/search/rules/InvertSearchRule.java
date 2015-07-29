package net.sf.jabref.search.rules;

import net.sf.jabref.BibtexEntry;
import net.sf.jabref.search.SearchRule;

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
    public boolean applyRule(String query, BibtexEntry bibtexEntry) {
        return !otherRule.applyRule(query, bibtexEntry);
    }

    @Override
    public boolean validateSearchStrings(String query) {
        return this.otherRule.validateSearchStrings(query);
    }
}
