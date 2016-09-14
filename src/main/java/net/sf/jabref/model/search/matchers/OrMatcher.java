package net.sf.jabref.model.search.matchers;

import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.search.SearchMatcher;

/**
 * Subclass of MatcherSet that ANDs or ORs between its rules, returning 0 or
 * 1.
 */
public class OrMatcher extends MatcherSet {

    @Override
    public boolean isMatch(BibEntry bibEntry) {
        int score = 0;

        // We let each rule add a maximum of 1 to the score.
        for (SearchMatcher rule : matchers) {
            if(rule.isMatch(bibEntry)) {
                score++;
            }
        }

        // OR rule demands score > 0.
        return score > 0;
    }
}
