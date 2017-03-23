package org.jabref.model.search;

import org.jabref.model.entry.BibEntry;

@FunctionalInterface
public interface SearchMatcher {

    boolean isMatch(BibEntry entry);

}
