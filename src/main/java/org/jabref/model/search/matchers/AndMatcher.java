package org.jabref.model.search.matchers;

import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

/**
 * A set of matchers that returns true if all matcher match the given entry.
 */
public class AndMatcher extends MatcherSet {

    @Override
    public boolean isMatch(BibDatabaseContext databaseContext, BibEntry entry) {
        return matchers.stream()
                       .allMatch(rule -> rule.isMatch(databaseContext, entry));
    }
}
