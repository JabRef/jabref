package org.jabref.model.search.matchers;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.search.SearchMatcher;

import org.jspecify.annotations.NonNull;

/**
 * Inverts the search result.
 * <p>
 * Example:
 * false --> true
 * true --> false
 */
public class NotMatcher implements SearchMatcher {

    private final SearchMatcher otherMatcher;

    public NotMatcher(@NonNull SearchMatcher otherMatcher) {
        this.otherMatcher = otherMatcher;
    }

    @Override
    public boolean isMatch(BibEntry entry) {
        return !otherMatcher.isMatch(entry);
    }
}
