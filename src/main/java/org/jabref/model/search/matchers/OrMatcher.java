package org.jabref.model.search.matchers;

import org.jabref.model.entry.BibEntry;

/**
 * A set of matchers that returns true if any matcher matches the given entry.
 */
public class OrMatcher extends MatcherSet {

    @Override
    public boolean isMatch(BibEntry entry) {
        return matchers.stream()
                       .anyMatch(rule -> rule.isMatch(entry));
    }
}
