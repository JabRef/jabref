package net.sf.jabref.search.rules;

import com.google.common.base.Preconditions;
import net.sf.jabref.BibtexEntry;
import net.sf.jabref.search.SearchRule;

/**
 * Inverts result score.
 *
 * Example:
 * 0 --> 1
 * 1 --> 0
 * ELSE --> 0
 */
public class InvertSearchRule implements SearchRule {

    private final SearchRule otherRule;

    public InvertSearchRule(SearchRule otherRule) {
        this.otherRule = Preconditions.checkNotNull(otherRule);
    }

    @Override
    public int applyRule(String query, BibtexEntry bibtexEntry) {
        return otherRule.applyRule(query, bibtexEntry) == 0 ? 1 : 0;
    }

    @Override
    public boolean validateSearchStrings(String query) {
        return this.otherRule.validateSearchStrings(query);
    }
}
