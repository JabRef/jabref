package org.jabref.model.search.matchers;

import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.util.ListUtil;

/**
 * A set of matchers that returns true if any matcher matches the given entry.
 */
public class OrMatcher extends MatcherSet {

    @Override
    public boolean isMatch(BibDatabaseContext databaseContext, BibEntry entry) {
        return ListUtil.anyMatch(matchers, rule -> rule.isMatch(databaseContext, entry));
    }
}
