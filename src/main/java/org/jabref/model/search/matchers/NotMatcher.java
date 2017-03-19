package org.jabref.model.search.matchers;

import java.util.Objects;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.search.SearchMatcher;

/**
 * Inverts the search result.
 * <p>
 * Example:
 * false --> true
 * true --> false
 */
public class NotMatcher implements SearchMatcher {

    private final SearchMatcher otherMatcher;

    public NotMatcher(SearchMatcher otherMatcher) {
        this.otherMatcher = Objects.requireNonNull(otherMatcher);
    }

    @Override
    public boolean isMatch(BibEntry entry) {
        return !otherMatcher.isMatch(entry);
    }
}
