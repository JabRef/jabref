package org.jabref.logic.search;

import org.jabref.model.entry.BibEntry;

@FunctionalInterface
public interface SearchMatcher {
    boolean isMatch(BibEntry entry);
}
