package net.sf.jabref.search.rules;

import com.google.common.base.Preconditions;
import net.sf.jabref.BibtexEntry;
import net.sf.jabref.search.SearchRule;

import java.util.Map;

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
    public int applyRule(Map<String, String> searchStrings, BibtexEntry bibtexEntry) {
        return otherRule.applyRule(searchStrings, bibtexEntry) == 0 ? 1 : 0;
    }

    @Override
    public boolean validateSearchStrings(Map<String, String> searchStrings) {
        return this.otherRule.validateSearchStrings(searchStrings);
    }
}
