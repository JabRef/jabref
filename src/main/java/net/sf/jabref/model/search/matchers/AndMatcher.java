package net.sf.jabref.model.search.matchers;

import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.search.SearchMatcher;

/**
 * Subclass of MatcherSet that ANDs or ORs between its rules, returning 0 or
 * 1.
 */
public class AndMatcher extends MatcherSet {

    @Override
    public boolean isMatch(BibEntry bibEntry) {
        int score = 0;

        // We let each rule add a maximum of 1 to the score.
        for (SearchMatcher rule : matchers) {
            if(rule.isMatch(bibEntry)) {
                score++;
            }
        }

        // Then an AND rule demands that score == number of rules
        return score == matchers.size();
    }
}
